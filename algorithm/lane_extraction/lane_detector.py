"""车道线检测 — 白色阈值 + Canny + HoughLinesP 找最长白实线."""
from __future__ import annotations

import cv2
import numpy as np


def detect_lane_line(frame: np.ndarray) -> tuple[int, int, int, int] | None:
    """检测画面中最长的白色实线（应急车道分界线）。

    Returns:
        (x1, y1, x2, y2) 或 None（未检测到）
    """
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    # 白色阈值：提取路面白线
    _, white_mask = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY)

    # Canny 边缘检测
    edges = cv2.Canny(white_mask, 50, 150, apertureSize=3)

    # HoughLinesP 找线段
    lines = cv2.HoughLinesP(edges, 1, np.pi / 180, threshold=30,
                            minLineLength=50, maxLineGap=100)

    if lines is None:
        return None

    longest_line: tuple[int, int, int, int] | None = None
    max_len = 0.0

    for line in lines:
        x1, y1, x2, y2 = line[0]
        dx = x2 - x1
        dy = y2 - y1

        if dx == 0:
            continue

        slope = dy / dx
        if slope < 0.3:
            continue

        length = np.sqrt(float(dx)**2 + float(dy)**2)
        if length > max_len:
            max_len = length
            longest_line = (x1, y1, x2, y2)

    return longest_line
