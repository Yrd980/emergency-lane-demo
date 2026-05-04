"""可视化工具 — 时间轴图表、关键帧保存."""
from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np

from algorithm.temporal_judgment.violation_decider import ViolationSegment


def draw_timeline(segments: list[ViolationSegment],
                  total_frames: int,
                  fps: float,
                  output_path: Path) -> Path:
    """绘制违规时间轴 PNG。"""
    width = 1200
    height = 80
    img = np.ones((height, width, 3), dtype=np.uint8) * 240

    for seg in segments:
        x1 = int(seg.start_frame / max(total_frames, 1) * width)
        x2 = int(seg.end_frame / max(total_frames, 1) * width)
        x1 = max(0, min(width, x1))
        x2 = max(0, min(width, x2))
        cv2.rectangle(img, (x1, 10), (x2, height - 10), (60, 60, 240), -1)

    cv2.rectangle(img, (0, 0), (width - 1, height - 1), (180, 180, 180), 1)
    total_sec = total_frames / max(fps, 1.0)
    cv2.putText(img, f"0s", (4, height - 4),
                cv2.FONT_HERSHEY_SIMPLEX, 0.4, (100, 100, 100), 1)
    cv2.putText(img, f"{total_sec:.0f}s", (width - 40, height - 4),
                cv2.FONT_HERSHEY_SIMPLEX, 0.4, (100, 100, 100), 1)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    cv2.imwrite(str(output_path), img)
    return output_path


def save_keyframes(frame_results: list,
                   segments: list[ViolationSegment],
                   output_dir: Path) -> list[Path]:
    """保存违规段中的关键帧（每段首/中/尾）。"""
    output_dir.mkdir(parents=True, exist_ok=True)
    paths: list[Path] = []

    frame_map = {fr.frame_idx: fr for fr in frame_results}

    for i, seg in enumerate(segments):
        for label, offset in [("start", 0), ("mid", seg.duration_frames // 2),
                               ("end", seg.duration_frames - 1)]:
            idx = seg.start_frame + offset
            if idx in frame_map and frame_map[idx].overlay_bgr is not None:
                fname = f"seg{i+1:02d}_{label}_f{idx:06d}.jpg"
                fp = output_dir / fname
                cv2.imwrite(str(fp), frame_map[idx].overlay_bgr)
                paths.append(fp)

    return paths
