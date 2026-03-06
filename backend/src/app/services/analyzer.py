from __future__ import annotations

import shutil
import threading
import uuid
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import cv2

from .. import db
from ..config import settings
from .demo_assets import ensure_demo_assets
from .zhipu_client import ZhipuVisionService


@dataclass
class FrameHit:
    second: float
    frame_path: Path
    result: dict[str, Any]


class VideoAnalysisService:
    def __init__(self) -> None:
        self.vision = ZhipuVisionService()
        self.lock = threading.Lock()
        self.thread: threading.Thread | None = None
        self.current_run_id: str | None = None

    @property
    def mode(self) -> str:
        return self.vision.mode

    def run_demo_analysis_sync(self) -> dict[str, Any]:
        with self.lock:
            run_id = f"RUN-{datetime.utcnow().strftime('%Y%m%d%H%M%S')}"
            started_at = db.utc_now()
            manifest = ensure_demo_assets()
            db.reset_demo_data()
            db.create_run(run_id, "demo_highway.mp4", self.mode)
            hits = self._sample_and_infer(run_id, manifest)
            events = self._build_events(run_id, manifest, hits)
            for event in events:
                db.insert_event(event)
                for evidence in event["evidence"]:
                    db.insert_evidence(event["id"], evidence)
            finished_at = db.utc_now()
            db.update_run(
                run_id,
                status="done",
                finished_at=finished_at,
                progress_percent=100,
                message=f"分析完成，识别 {len(events)} 条疑似违章事件。",
            )
            return {
                "task_id": run_id,
                "mode": self.mode,
                "analyzed_frames": len(hits) + max(0, int(manifest["duration_seconds"] / settings.frame_interval_seconds) + 1 - len(hits)),
                "events_created": len(events),
                "started_at": started_at,
                "finished_at": finished_at,
                "message": "演示视频分析完成",
            }

    def trigger_demo_analysis(self) -> dict[str, str]:
        if self.thread and self.thread.is_alive():
            return {"run_id": self.current_run_id or "", "status": "running"}

        run_id = f"RUN-{datetime.utcnow().strftime('%Y%m%d%H%M%S')}"
        self.current_run_id = run_id
        self.thread = threading.Thread(target=self._run_demo_analysis, args=(run_id,), daemon=True)
        self.thread.start()
        return {"run_id": run_id, "status": "started"}

    def _run_demo_analysis(self, run_id: str) -> None:
        with self.lock:
            manifest = ensure_demo_assets()
            db.reset_demo_data()
            db.create_run(run_id, "demo_highway.mp4", self.mode)
            try:
                hits = self._sample_and_infer(run_id, manifest)
                events = self._build_events(run_id, manifest, hits)
                for event in events:
                    db.insert_event(event)
                    for evidence in event["evidence"]:
                        db.insert_evidence(event["id"], evidence)
                db.update_run(
                    run_id,
                    status="done",
                    finished_at=db.utc_now(),
                    progress_percent=100,
                    message=f"分析完成，识别 {len(events)} 条疑似违章事件。",
                )
            except Exception as exc:
                db.update_run(
                    run_id,
                    status="failed",
                    finished_at=db.utc_now(),
                    progress_percent=100,
                    message=f"分析失败：{exc}",
                )

    def _sample_and_infer(self, run_id: str, manifest: dict[str, Any]) -> list[FrameHit]:
        cap = cv2.VideoCapture(str(settings.demo_video_path))
        if not cap.isOpened():
            raise RuntimeError("演示视频无法打开。")

        fps = cap.get(cv2.CAP_PROP_FPS) or manifest.get("fps") or 12
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        duration = total_frames / fps if fps else manifest["duration_seconds"]
        sample_times = []
        second = 0.0
        while second <= duration:
            sample_times.append(round(second, 1))
            second += settings.frame_interval_seconds

        hits: list[FrameHit] = []
        expected_plate = manifest["expected_violation_plate"]
        for index, sample_second in enumerate(sample_times, start=1):
            frame_idx = int(sample_second * fps)
            cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
            ok, frame = cap.read()
            if not ok:
                continue

            frame_path = settings.frame_dir / f"{run_id}_{int(sample_second * 10):04d}.jpg"
            cv2.imwrite(str(frame_path), frame)
            result = self.vision.analyze_frame(
                image_path=frame_path,
                second=sample_second,
                manifest=manifest,
                fallback_plate=expected_plate,
            )
            if result["has_violation"]:
                hits.append(FrameHit(second=sample_second, frame_path=frame_path, result=result))

            progress = int(index / max(len(sample_times), 1) * 100)
            db.update_run(
                run_id,
                progress_percent=progress,
                message=f"正在抽样分析第 {index}/{len(sample_times)} 个关键帧",
            )

        cap.release()
        return hits

    def _build_events(self, run_id: str, manifest: dict[str, Any], hits: list[FrameHit]) -> list[dict[str, Any]]:
        if not hits:
            return []

        groups: list[list[FrameHit]] = []
        current_group = [hits[0]]
        for hit in hits[1:]:
            if hit.second - current_group[-1].second <= settings.frame_interval_seconds * 1.6:
                current_group.append(hit)
            else:
                groups.append(current_group)
                current_group = [hit]
        groups.append(current_group)

        events: list[dict[str, Any]] = []
        for group in groups:
            duration = group[-1].second - group[0].second + settings.frame_interval_seconds
            if duration < settings.violation_hold_seconds:
                continue
            event_id = f"EV-{uuid.uuid4().hex[:8].upper()}"
            confidence = round(sum(item.result["confidence"] for item in group) / len(group), 2)
            evidence = self._pick_evidence(event_id, group)
            plate = group[0].result["plate_number"] or manifest["expected_violation_plate"]
            summary = f"车辆 {plate} 在应急车道持续占用约 {duration:.1f} 秒，已形成辅助举报证据链。"
            events.append(
                {
                    "id": event_id,
                    "run_id": run_id,
                    "plate_number": plate,
                    "lane_label": manifest["lane_label"],
                    "status": "待举报",
                    "severity": "高风险",
                    "location": manifest["location"],
                    "confidence": confidence,
                    "created_at": db.utc_now(),
                    "start_second": group[0].second,
                    "end_second": group[-1].second,
                    "duration_seconds": round(duration, 1),
                    "summary": summary,
                    "raw_reason": group[-1].result["reason"],
                    "evidence": evidence,
                }
            )
        return events

    def _pick_evidence(self, event_id: str, group: list[FrameHit]) -> list[dict[str, Any]]:
        chosen: list[FrameHit] = []
        if group:
            chosen.append(group[0])
        if len(group) > 2:
            chosen.append(group[len(group) // 2])
        if len(group) > 1:
            chosen.append(group[-1])

        evidence_items = []
        seen_names = set()
        for index, hit in enumerate(chosen, start=1):
            if hit.frame_path.name in seen_names:
                continue
            seen_names.add(hit.frame_path.name)
            destination = settings.evidence_dir / f"{event_id}_{index}.jpg"
            shutil.copyfile(hit.frame_path, destination)
            evidence_items.append(
                {
                    "id": f"{event_id}-IMG-{index}",
                    "image_path": destination.name,
                    "second": hit.second,
                    "note": f"关键证据帧 #{index}",
                    "raw_result": hit.result,
                }
            )
        return evidence_items
