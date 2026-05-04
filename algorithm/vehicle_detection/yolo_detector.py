"""YOLOv8n 推理器 — 轻量化车辆目标检测."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np
from ultralytics import YOLO


@dataclass
class Detection:
    """检测结果。"""
    x: float       # bbox 左上 x (像素坐标)
    y: float       # bbox 左上 y
    w: float       # bbox 宽
    h: float       # bbox 高
    score: float   # 置信度
    cls: int        # COCO 类别 ID (2=car, 5=bus, 7=truck)
    label: str      # 类别名称


MODEL_PATH = Path(__file__).resolve().parents[1] / "yolov8n.pt"


class YOLODetector:
    """YOLOv8n 目标检测器，自动下载模型。"""

    def __init__(self, model_path: str | Path = MODEL_PATH, device: str = "cpu"):
        self.model = YOLO(str(model_path))
        self.device = device

    def detect(self, frame: np.ndarray,
               conf_threshold: float = 0.3) -> list[Detection]:
        """对单帧图像运行目标检测。

        Args:
            frame: BGR 图像 (H, W, 3)
            conf_threshold: 置信度阈值

        Returns:
            Detection 列表
        """
        results = self.model(frame, verbose=False, device=self.device)
        detections: list[Detection] = []

        for result in results:
            if result.boxes is None:
                continue
            for box in result.boxes:
                cls = int(box.cls[0])
                conf = float(box.conf[0])
                if conf < conf_threshold:
                    continue
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                detections.append(Detection(
                    x=x1, y=y1,
                    w=x2 - x1, h=y2 - y1,
                    score=conf,
                    cls=cls,
                    label=self.model.names.get(cls, str(cls)),
                ))

        return detections
