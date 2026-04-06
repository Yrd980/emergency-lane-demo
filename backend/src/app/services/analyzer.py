from __future__ import annotations

import shutil
import threading
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any

import cv2
import numpy as np

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
            self._reset_generated_assets()
            db.reset_demo_data()
            db.create_run(run_id, "demo_highway.mp4", self.mode)

            hits, analyzed_frames = self._sample_and_infer(run_id, manifest)
            events = self._build_events(run_id, manifest, hits)
            cases = self._build_cases(run_id, manifest, events)

            for case in cases:
                db.insert_case(case)
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
                message=f"分析完成，识别 {len(events)} 条事件并归档 {len(cases)} 个案件。",
            )
            return {
                "task_id": run_id,
                "mode": self.mode,
                "analyzed_frames": analyzed_frames,
                "events_created": len(events),
                "cases_created": len(cases),
                "started_at": started_at,
                "finished_at": finished_at,
                "message": "演示视频分析完成，已生成案件库与证据片段。",
            }

    def _reset_generated_assets(self) -> None:
        for directory in (settings.frame_dir, settings.evidence_dir, settings.clip_dir, settings.report_dir):
            directory.mkdir(parents=True, exist_ok=True)
            for child in directory.iterdir():
                if child.is_file() or child.is_symlink():
                    child.unlink(missing_ok=True)
                elif child.is_dir():
                    shutil.rmtree(child)

    def _sample_and_infer(self, run_id: str, manifest: dict[str, Any]) -> tuple[list[FrameHit], int]:
        cap = cv2.VideoCapture(str(settings.demo_video_path))
        if not cap.isOpened():
            raise RuntimeError("演示视频无法打开。")

        fps = cap.get(cv2.CAP_PROP_FPS) or manifest.get("fps") or 12
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        duration = total_frames / fps if fps else manifest["duration_seconds"]

        sample_times: list[float] = []
        second = 0.0
        while second <= duration:
            sample_times.append(round(second, 1))
            second += settings.frame_interval_seconds

        hits: list[FrameHit] = []
        expected_plate = manifest["expected_violation_plate"]

        for index, sample_second in enumerate(sample_times, start=1):
            frame_idx = min(int(sample_second * fps), max(total_frames - 1, 0))
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
            db.insert_capture(
                {
                    "id": f"CAP-{uuid.uuid4().hex[:10].upper()}",
                    "run_id": run_id,
                    "sample_second": sample_second,
                    "image_path": frame_path.name,
                    "has_violation": result["has_violation"],
                    "plate_number": result.get("plate_number", "") or "",
                    "confidence": float(result.get("confidence", 0.0) or 0.0),
                    "reason": result.get("reason", ""),
                    "created_at": db.utc_now(),
                }
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
        return hits, len(sample_times)

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
                    "case_id": None,
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

    def _build_cases(self, run_id: str, manifest: dict[str, Any], events: list[dict[str, Any]]) -> list[dict[str, Any]]:
        if not events:
            return []

        grouped: dict[str, list[dict[str, Any]]] = {}
        for event in events:
            grouped.setdefault(event["plate_number"], []).append(event)

        created_at = db.utc_now()
        cases: list[dict[str, Any]] = []
        for plate, grouped_events in grouped.items():
            grouped_events.sort(key=lambda item: item["start_second"])
            case_id = f"CASE-{uuid.uuid4().hex[:8].upper()}"
            start_second = grouped_events[0]["start_second"]
            end_second = grouped_events[-1]["end_second"]
            confidence = round(sum(item["confidence"] for item in grouped_events) / len(grouped_events), 2)
            total_duration = round(sum(item["duration_seconds"] for item in grouped_events), 1)
            clip_path = self._render_case_clip(case_id, start_second, end_second, manifest)
            report_content, report_path = self._write_case_report(
                case_id=case_id,
                plate=plate,
                location=manifest["location"],
                first_seen=self._second_to_time_iso(start_second),
                last_seen=self._second_to_time_iso(end_second),
                duration_seconds=total_duration,
                confidence=confidence,
            )
            summary = f"案件 {case_id} 已归档车牌 {plate} 的应急车道占用证据、15 秒片段与举报文书。"

            for event in grouped_events:
                event["case_id"] = case_id
                event["report_content"] = report_content
                for evidence in event["evidence"]:
                    evidence["case_id"] = case_id

            cases.append(
                {
                    "id": case_id,
                    "run_id": run_id,
                    "plate_number": plate,
                    "status": "待举报",
                    "location": manifest["location"],
                    "confidence": confidence,
                    "created_at": created_at,
                    "updated_at": created_at,
                    "first_seen": self._second_to_time_iso(start_second),
                    "last_seen": self._second_to_time_iso(end_second),
                    "start_second": start_second,
                    "end_second": end_second,
                    "duration_seconds": total_duration,
                    "summary": summary,
                    "clip_path": clip_path,
                    "report_path": report_path,
                    "report_content": report_content,
                    "report_submitted_at": None,
                }
            )
        return cases

    def _write_case_report(
        self,
        *,
        case_id: str,
        plate: str,
        location: str,
        first_seen: str,
        last_seen: str,
        duration_seconds: float,
        confidence: float,
    ) -> tuple[str, str]:
        report_content = (
            f"【高速公路应急车道占用辅助举报文书】\n"
            f"案件编号：{case_id}\n"
            f"车牌号码：{plate}\n"
            f"事发地点：{location}\n"
            f"首次发现：{first_seen}\n"
            f"最后记录：{last_seen}\n"
            f"持续时长：{duration_seconds:.1f} 秒\n"
            f"模型综合置信度：{confidence * 100:.0f}%\n"
            f"处置建议：该车辆在应急车道内持续行驶，证据链包含关键帧、时序记录与 15 秒证据片段，可用于模拟举报展示。\n"
        )
        report_path = settings.report_dir / f"{case_id}.txt"
        report_path.write_text(report_content, encoding="utf-8")
        return report_content, report_path.name

    def _render_case_clip(
        self,
        case_id: str,
        event_start_second: float,
        event_end_second: float,
        manifest: dict[str, Any],
    ) -> str:
        source_path = settings.demo_video_path
        cap = cv2.VideoCapture(str(source_path))
        if not cap.isOpened():
            raise RuntimeError("证据视频裁剪失败：无法打开演示视频。")

        fps = cap.get(cv2.CAP_PROP_FPS) or manifest.get("fps") or 12
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        duration = total_frames / fps if fps else manifest["duration_seconds"]
        target_duration = min(settings.case_clip_seconds, max(duration, 1.0))

        center = (event_start_second + event_end_second) / 2
        clip_start = center - target_duration / 2
        clip_end = center + target_duration / 2
        if clip_start < 0:
            clip_end += -clip_start
            clip_start = 0.0
        if clip_end > duration:
            shift = clip_end - duration
            clip_start = max(0.0, clip_start - shift)
            clip_end = duration

        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)) or 1280
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT)) or 720
        output_path = settings.clip_dir / f"{case_id}.mp4"
        writer = cv2.VideoWriter(
            str(output_path),
            cv2.VideoWriter_fourcc(*"mp4v"),
            fps,
            (width, height),
        )
        if not writer.isOpened():
            cap.release()
            raise RuntimeError("证据视频裁剪失败：无法创建输出文件。")

        first_frame = self._safe_read_frame(cap, 0)
        last_frame = self._safe_read_frame(cap, max(total_frames - 1, 0))
        target_frames = max(1, int(round(target_duration * fps)))

        for index in range(target_frames):
            current_second = clip_start + (index / fps)
            frame_idx = int(round(current_second * fps))
            if frame_idx < 0:
                frame = first_frame
            elif frame_idx >= total_frames:
                frame = last_frame
            else:
                frame = self._safe_read_frame(cap, frame_idx)
            writer.write(frame)

        writer.release()
        cap.release()
        return output_path.name

    def _safe_read_frame(self, cap: cv2.VideoCapture, frame_idx: int) -> np.ndarray:
        cap.set(cv2.CAP_PROP_POS_FRAMES, max(frame_idx, 0))
        ok, frame = cap.read()
        if ok:
            return frame
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)) or 1280
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT)) or 720
        return np.zeros((height, width, 3), dtype=np.uint8)

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

    def _second_to_time_iso(self, second: float) -> str:
        base = datetime.fromisoformat(db.utc_now())
        return (base + timedelta(seconds=second)).isoformat()
