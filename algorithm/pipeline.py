"""四模块编排器 — 串联车道提取→车辆检测→空间判定→时间判定."""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import cv2
import numpy as np

from algorithm.lane_extraction import apply_region_mask, detect_lane_line, draw_mask_overlay
from algorithm.vehicle_detection import YOLODetector, filter_vehicles
from algorithm.spatial_reasoning import check_vehicle_in_region
from algorithm.temporal_judgment import FrameVerdict, ViolationDecider, ViolationSegment
from algorithm.vehicle_detection.yolo_detector import Detection


@dataclass
class FrameResult:
    """单帧分析结果。"""
    frame_idx: int
    has_violation: bool
    mask: np.ndarray | None = None
    detections: list[Detection] = field(default_factory=list)
    violations: list[Detection] = field(default_factory=list)
    overlay_bgr: np.ndarray | None = None


@dataclass
class PipelineResult:
    """完整 pipeline 输出。"""
    frame_results: list[FrameResult]
    violation_segments: list[ViolationSegment]
    fps: float
    total_frames: int


class Pipeline:
    """应急车道违规检测管线。"""

    def __init__(self, conf_threshold: float = 0.3,
                 window_seconds: float = 15.0,
                 ratio_threshold: float = 0.6,
                 device: str = "cpu"):
        self.conf_threshold = conf_threshold
        self.window_seconds = window_seconds
        self.ratio_threshold = ratio_threshold
        self._detector = YOLODetector(device=device)
        self._decider: ViolationDecider | None = None
        self._fps: float = 30.0

    def run(self, video_path: Path, sample_every: int = 1,
            ground_truth: dict[str, list[dict]] | None = None) -> PipelineResult:
        cap = cv2.VideoCapture(str(video_path))
        self._fps = cap.get(cv2.CAP_PROP_FPS)
        if self._fps <= 0:
            self._fps = 30.0

        self._decider = ViolationDecider(
            window_seconds=self.window_seconds,
            fps=self._fps,
            ratio_threshold=self.ratio_threshold,
        )

        frame_results: list[FrameResult] = []
        frame_idx = 0

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            if frame_idx % sample_every != 0:
                frame_idx += 1
                continue

            fr = self._process_frame(frame, frame_idx, ground_truth)
            frame_results.append(fr)
            self._decider.update(FrameVerdict(
                frame_idx=frame_idx,
                has_violation=fr.has_violation,
                vehicle_count=len(fr.violations),
                max_confidence=max((d.score for d in fr.violations), default=0.0),
            ))
            frame_idx += 1

        cap.release()
        segments = self._decider.finalize()

        return PipelineResult(
            frame_results=frame_results,
            violation_segments=segments,
            fps=self._fps,
            total_frames=frame_idx,
        )

    def _process_frame(self, frame: np.ndarray, frame_idx: int,
                       ground_truth: dict[str, list[dict]] | None = None) -> FrameResult:
        line = detect_lane_line(frame)
        masked, mask = apply_region_mask(frame, line)

        if ground_truth is not None and str(frame_idx) in ground_truth:
            vehicles = [
                Detection(
                    x=g["x"], y=g["y"], w=g["w"], h=g["h"],
                    score=1.0, cls=2, label=g["label"],
                )
                for g in ground_truth[str(frame_idx)]
            ]
        else:
            raw_dets = self._detector.detect(masked, conf_threshold=self.conf_threshold)
            vehicles = filter_vehicles(raw_dets, conf_threshold=self.conf_threshold)

        violations = check_vehicle_in_region(vehicles, mask)

        overlay = draw_mask_overlay(frame, mask)
        for det in violations:
            cv2.rectangle(overlay,
                          (int(det.x), int(det.y)),
                          (int(det.x + det.w), int(det.y + det.h)),
                          (0, 0, 255), 2)
            cv2.putText(overlay, f"{det.label} {det.score:.2f}",
                        (int(det.x), int(det.y) - 8),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        return FrameResult(
            frame_idx=frame_idx,
            has_violation=len(violations) > 0,
            mask=mask,
            detections=vehicles,
            violations=violations,
            overlay_bgr=overlay,
        )
