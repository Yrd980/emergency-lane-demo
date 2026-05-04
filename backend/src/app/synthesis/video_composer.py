"""视频合成器 — 帧合成 → mp4 文件."""
from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np

from .highway_renderer import render_highway_frame
from .vehicle_placer import create_vehicles, draw_vehicles, update_vehicles


def compose_video(output_path: Path,
                  duration_seconds: float = 30.0,
                  fps: int = 30,
                  num_normal: int = 4,
                  num_violating: int = 2,
                  seed: int = 42) -> Path:
    total_frames = int(duration_seconds * fps)
    vehicles = create_vehicles(num_normal, num_violating, seed=seed)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    writer = cv2.VideoWriter(
        str(output_path), fourcc, fps, (1280, 720)
    )

    for frame_idx in range(total_frames):
        img = render_highway_frame(seed=seed)
        vehicles = update_vehicles(vehicles, frame_idx, seed=seed)
        img = draw_vehicles(img, vehicles)

        frame_bgr = cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)
        cv2.putText(frame_bgr, f"frame {frame_idx:05d}",
                    (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)
        writer.write(frame_bgr)

    writer.release()
    return output_path
