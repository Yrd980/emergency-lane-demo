"""固定几何约束掩膜 — 左 1/4 + 斜线三角区域外全部填充白色."""
from __future__ import annotations

import cv2
import numpy as np


def apply_region_mask(frame: np.ndarray,
                      line: tuple[int, int, int, int] | None = None
                      ) -> tuple[np.ndarray, np.ndarray]:
    """对图像施加固定几何约束掩膜，仅保留应急车道区域。

    掩膜策略：
    1. 画面左 1/4 填充白色
    2. 从 (0, h/2) 到 (w/2, h) 的斜线下方三角填充白色

    Returns:
        (masked_frame, mask) — masked_frame 是掩膜后的图像，mask 是二值掩膜(255=保留区域)
    """
    h, w = frame.shape[:2]

    # 创建掩膜：255=保留，0=填充白
    mask = np.ones((h, w), dtype=np.uint8) * 255

    # 1. 左 1/4 填充
    quarter_w = w // 4
    cv2.rectangle(mask, (0, 0), (quarter_w, h), 0, -1)

    # 2. 斜线下方三角填充
    p1 = (0, h // 2)
    p2 = (w // 2, h)
    p3 = (0, h)
    triangle = np.array([p1, p2, p3], np.int32)
    cv2.fillPoly(mask, [triangle], 0)

    # 应用掩膜
    masked = cv2.bitwise_and(frame, frame, mask=mask)

    # 非保留区域填充白色
    mask_inv = cv2.bitwise_not(mask)
    white_bg = np.ones_like(frame) * 255
    white_part = cv2.bitwise_and(white_bg, white_bg, mask=mask_inv)
    result = cv2.add(masked, white_part)

    return result, mask
