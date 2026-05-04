"""空间关系建模 — 计算车辆检测框与应急车道掩膜的重叠关系."""
from __future__ import annotations

import numpy as np

from algorithm.vehicle_detection.yolo_detector import Detection


def check_vehicle_in_region(detections: list[Detection],
                            mask: np.ndarray,
                            overlap_ratio: float = 0.3
                            ) -> list[Detection]:
    """判断车辆是否位于应急车道区域内。

    判定准则：检测框中心点落在掩膜保留区域内 或
             检测框与掩膜区域的重叠面积 > 30% 检测框面积。
    """
    violations: list[Detection] = []

    for det in detections:
        x1, y1 = int(det.x), int(det.y)
        x2, y2 = int(det.x + det.w), int(det.y + det.h)

        h, w = mask.shape
        x1, y1 = max(0, x1), max(0, y1)
        x2, y2 = min(w, x2), min(h, y2)

        if x2 <= x1 or y2 <= y1:
            continue

        cx, cy = int(det.x + det.w / 2), int(det.y + det.h / 2)
        center_in_region = (0 <= cx < w and 0 <= cy < h
                            and mask[cy, cx] == 255)

        bbox_area = det.w * det.h
        if bbox_area <= 0:
            continue
        bbox_region = mask[y1:y2, x1:x2]
        overlap_area = np.count_nonzero(bbox_region == 255)

        ratio = overlap_area / max(bbox_area, 1.0)

        if center_in_region or ratio > overlap_ratio:
            violations.append(det)

    return violations
