"""掩膜区域可视化 — 在帧上绘制掩膜边界线和半透明覆盖."""
from __future__ import annotations

import cv2
import numpy as np


def draw_mask_overlay(frame: np.ndarray, mask: np.ndarray) -> np.ndarray:
    """在帧上绘制掩膜区域边界和半透明绿色覆盖。

    Args:
        frame: 原始 BGR 图像
        mask: 二值掩膜（255=保留的应急车道区域）

    Returns:
        带可视化叠加的 BGR 图像副本
    """
    h, w = frame.shape[:2]
    overlay = frame.copy()
    quarter_w = w // 4

    # 半透明绿色覆盖保留区域
    green = np.zeros_like(frame, dtype=np.uint8)
    green[:] = (0, 255, 0)
    green_region = cv2.bitwise_and(green, green, mask=mask)
    overlay = cv2.addWeighted(overlay, 1.0, green_region, 0.18, 0)

    # 绘制掩膜边界线
    cv2.line(overlay, (quarter_w, 0), (quarter_w, h), (0, 255, 0), 2)
    cv2.line(overlay, (0, h // 2), (w // 2, h), (0, 255, 0), 2)

    # 标注区域名称
    cv2.putText(overlay, "emergency lane", (quarter_w + 10, 30),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

    return overlay
