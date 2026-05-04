"""后处理 — 车辆类别过滤."""
from __future__ import annotations

from .yolo_detector import Detection

# COCO: 2=car, 3=motorcycle, 5=bus, 7=truck
VEHICLE_CLASSES: set[int] = {2, 3, 5, 7}


def filter_vehicles(detections: list[Detection],
                    conf_threshold: float = 0.3
                    ) -> list[Detection]:
    """过滤非车辆类别，保留置信度高于阈值的检测结果。

    Args:
        detections: YOLO 原始检测结果
        conf_threshold: 车辆置信度阈值

    Returns:
        仅包含车辆类别的 Detection 列表
    """
    return [d for d in detections
            if d.cls in VEHICLE_CLASSES and d.score >= conf_threshold]
