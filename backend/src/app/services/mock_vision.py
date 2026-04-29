from __future__ import annotations

import random
from pathlib import Path
from typing import Any


class MockVisionService:
    """纯 Mock 视觉推理服务 — 根据 manifest 中的预期违规区间返回结果，不调用外部 API。"""

    mode: str = "mock"

    def analyze_frame(
        self,
        *,
        image_path: Path,
        second: float,
        manifest: dict[str, Any],
        fallback_plate: str = "",
    ) -> dict[str, Any]:
        expected_range = manifest.get("expected_violation_range")
        expected_plate = fallback_plate or manifest.get("expected_violation_plate", "")

        if expected_range and expected_range[0] <= second <= expected_range[1]:
            return {
                "has_violation": True,
                "plate_number": expected_plate,
                "vehicle_type": "小汽车",
                "confidence": round(random.uniform(0.82, 0.96), 2),
                "reason": f"第 {second:.1f} 秒检测到车辆 {expected_plate} 占用应急车道。",
            }

        if expected_plate and random.random() < 0.15:
            return {
                "has_violation": False,
                "plate_number": expected_plate,
                "vehicle_type": "小汽车",
                "confidence": round(random.uniform(0.72, 0.88), 2),
                "reason": f"第 {second:.1f} 秒检测车辆 {expected_plate}，但未在应急车道范围内。",
            }

        return {
            "has_violation": False,
            "plate_number": "",
            "vehicle_type": "",
            "confidence": 0.0,
            "reason": f"第 {second:.1f} 秒未检测到占用应急车道的车辆。",
        }
