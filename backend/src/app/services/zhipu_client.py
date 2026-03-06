from __future__ import annotations

import base64
import json
import re
from pathlib import Path
from typing import Any

from zhipuai import ZhipuAI

from ..config import settings


PLATE_PATTERN = re.compile(
    r"[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-Z][A-Z0-9]{5,6}"
)


class ZhipuVisionService:
    def __init__(self) -> None:
        self.mode = "mock" if settings.use_mock_zhipu or not settings.zhipu_api_key else "zhipu"
        self.client = ZhipuAI(api_key=settings.zhipu_api_key) if self.mode == "zhipu" else None

    def analyze_frame(
        self,
        *,
        image_path: Path,
        second: float,
        manifest: dict[str, Any],
        fallback_plate: str | None = None,
    ) -> dict[str, Any]:
        if self.mode == "mock":
            return self._mock_result(second=second, manifest=manifest, fallback_plate=fallback_plate)

        prompt = f"""
你是高速公路应急车道违章识别助手。
图像右下方红色高亮区域是应急车道。请只关注应急车道内的机动车，并输出严格 JSON：
{{
  "has_violation": true,
  "plate_number": "车牌号或空字符串",
  "vehicle_type": "car|truck|bus|unknown",
  "confidence": 0.0,
  "reason": "一句中文原因说明"
}}

要求：
1. 若无车辆占用应急车道，has_violation 为 false，plate_number 为空。
2. 若有车辆占用，尽量读取车牌。
3. 只返回 JSON，不要解释。
当前抽样时间点：{second:.1f} 秒。
"""
        image_base64 = base64.b64encode(image_path.read_bytes()).decode("utf-8")
        response = self.client.chat.completions.create(
            model=settings.zhipu_model,
            response_format={"type": "json_object"},
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {"url": f"data:image/jpeg;base64,{image_base64}"},
                        },
                    ],
                }
            ],
        )
        content = response.choices[0].message.content
        parsed = self._parse_json_content(content)
        plate = self._normalize_plate(parsed.get("plate_number", "")) or fallback_plate or ""
        return {
            "has_violation": bool(parsed.get("has_violation")),
            "plate_number": plate,
            "vehicle_type": parsed.get("vehicle_type", "unknown"),
            "confidence": float(parsed.get("confidence", 0.0) or 0.0),
            "reason": parsed.get("reason", "智谱视觉识别返回结果。"),
        }

    def _mock_result(
        self,
        *,
        second: float,
        manifest: dict[str, Any],
        fallback_plate: str | None = None,
    ) -> dict[str, Any]:
        start_second, end_second = manifest["expected_violation_range"]
        has_violation = start_second <= second <= end_second
        plate = manifest["expected_violation_plate"] if has_violation else ""
        return {
            "has_violation": has_violation,
            "plate_number": fallback_plate or plate,
            "vehicle_type": "car" if has_violation else "unknown",
            "confidence": 0.92 if has_violation else 0.05,
            "reason": "根据演示视频预设轨迹识别应急车道占用。",
        }

    @staticmethod
    def _parse_json_content(content: Any) -> dict[str, Any]:
        if isinstance(content, dict):
            return content
        if isinstance(content, list):
            text = "".join(
                item.get("text", "") if isinstance(item, dict) else str(item)
                for item in content
            )
        else:
            text = str(content)
        text = text.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", text, flags=re.S)
            if match:
                try:
                    return json.loads(match.group(0))
                except json.JSONDecodeError:
                    pass
        return {"has_violation": False, "plate_number": "", "vehicle_type": "unknown", "confidence": 0.0, "reason": "结果解析失败"}

    @staticmethod
    def _normalize_plate(plate: str) -> str | None:
        if not plate:
            return None
        cleaned = re.sub(r"[\s·.:-]", "", plate).upper()
        match = PLATE_PATTERN.search(cleaned)
        return match.group(0) if match else None

