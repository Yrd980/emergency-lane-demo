"""时间一致性约束 — 滑动窗口 + 占用帧比例阈值判定违规."""
from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ViolationSegment:
    """违规时间段。"""
    start_frame: int
    end_frame: int
    duration_frames: int
    occupancy_ratio: float
    confidence: float


@dataclass
class FrameVerdict:
    """单帧判定结果。"""
    frame_idx: int
    has_violation: bool
    vehicle_count: int = 0
    max_confidence: float = 0.0


class ViolationDecider:
    """滑动时间窗口违规判定器。

    策略：在 window_frames 帧窗口内，violation 帧占比 > ratio_threshold 则判定违规。
    """

    def __init__(self, window_seconds: float = 15.0,
                 fps: float = 30.0,
                 ratio_threshold: float = 0.6):
        self.window_frames = int(window_seconds * fps)
        self.ratio_threshold = ratio_threshold
        self.fps = fps
        self._buffer: list[FrameVerdict] = []
        self._segments: list[ViolationSegment] = []
        self._in_violation = False
        self._segment_start = -1

    def update(self, verdict: FrameVerdict) -> None:
        """输入一帧的判定结果。"""
        self._buffer.append(verdict)

        if len(self._buffer) < self.window_frames:
            return

        if len(self._buffer) > self.window_frames:
            self._buffer = self._buffer[-self.window_frames:]

        violation_count = sum(1 for v in self._buffer if v.has_violation)
        ratio = violation_count / self.window_frames

        if ratio >= self.ratio_threshold:
            if not self._in_violation:
                self._in_violation = True
                self._segment_start = self._buffer[0].frame_idx
        else:
            if self._in_violation:
                self._in_violation = False
                end_frame = self._buffer[-1].frame_idx
                duration = end_frame - self._segment_start
                confidences = [v.max_confidence for v in self._buffer
                               if v.has_violation]
                avg_conf = sum(confidences) / max(len(confidences), 1)
                self._segments.append(ViolationSegment(
                    start_frame=self._segment_start,
                    end_frame=end_frame,
                    duration_frames=duration,
                    occupancy_ratio=ratio,
                    confidence=avg_conf,
                ))

    def finalize(self) -> list[ViolationSegment]:
        """处理缓冲区剩余帧，返回所有违规段。"""
        if not self._buffer:
            return self._segments

        if self._in_violation:
            end_frame = self._buffer[-1].frame_idx
            duration = end_frame - self._segment_start
            confidences = [v.max_confidence for v in self._buffer
                           if v.has_violation]
            avg_conf = sum(confidences) / max(len(confidences), 1)
            self._segments.append(ViolationSegment(
                start_frame=self._segment_start,
                end_frame=end_frame,
                duration_frames=duration,
                occupancy_ratio=1.0,
                confidence=avg_conf,
            ))
            self._in_violation = False
        elif len(self._buffer) < self.window_frames and len(self._buffer) > 0:
            # 视频总长不足窗口大小时，用可用帧判定
            violation_count = sum(1 for v in self._buffer if v.has_violation)
            ratio = violation_count / len(self._buffer)
            if ratio >= self.ratio_threshold:
                confidences = [v.max_confidence for v in self._buffer
                               if v.has_violation]
                avg_conf = sum(confidences) / max(len(confidences), 1)
                self._segments.append(ViolationSegment(
                    start_frame=self._buffer[0].frame_idx,
                    end_frame=self._buffer[-1].frame_idx,
                    duration_frames=self._buffer[-1].frame_idx - self._buffer[0].frame_idx,
                    occupancy_ratio=ratio,
                    confidence=avg_conf,
                ))
        return self._segments
