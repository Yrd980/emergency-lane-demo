from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

from ..config import settings


ROAD_W = 1280
ROAD_H = 720
FPS = 12
DURATION_SECONDS = 16


@dataclass
class VehicleTrack:
    plate: str
    color: tuple[int, int, int]
    lane: str
    y: int
    start_x: int
    end_x: int
    start_s: float
    end_s: float
    width: int = 170
    height: int = 82


VEHICLES = [
    VehicleTrack("沪A12345", (62, 114, 255), "emergency", 555, -260, 1260, 4.0, 11.0),
    VehicleTrack("浙B67890", (255, 108, 66), "main", 395, -200, 1420, 0.0, 16.0),
    VehicleTrack("苏C88888", (94, 196, 129), "main", 295, 1280, -220, 1.0, 14.0),
]


def _find_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    font_candidates = [
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\msyhbd.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
    ]
    for candidate in font_candidates:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


def _vehicle_position(track: VehicleTrack, second: float) -> tuple[int, int, int, int] | None:
    if second < track.start_s or second > track.end_s:
        return None
    span = max(track.end_s - track.start_s, 0.001)
    progress = (second - track.start_s) / span
    x = int(track.start_x + (track.end_x - track.start_x) * progress)
    return x, track.y, track.width, track.height


def _draw_plate(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], plate: str) -> None:
    x, y, w, h = box
    plate_w = int(w * 0.58)
    plate_h = int(h * 0.22)
    plate_x = x + (w - plate_w) // 2
    plate_y = y + int(h * 0.63)
    draw.rounded_rectangle(
        (plate_x, plate_y, plate_x + plate_w, plate_y + plate_h),
        radius=6,
        fill=(31, 90, 220),
        outline=(255, 255, 255),
        width=2,
    )
    font = _find_font(max(18, plate_h - 6))
    draw.text((plate_x + 8, plate_y + 2), plate, fill=(255, 255, 255), font=font)


def _draw_car(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: tuple[int, int, int], plate: str) -> None:
    x, y, w, h = box
    draw.rounded_rectangle((x, y, x + w, y + h), radius=18, fill=color, outline=(28, 36, 46), width=4)
    roof_margin = int(w * 0.18)
    draw.rounded_rectangle(
        (x + roof_margin, y - int(h * 0.28), x + w - roof_margin, y + int(h * 0.18)),
        radius=14,
        fill=(220, 232, 244),
        outline=(28, 36, 46),
        width=3,
    )
    for wheel_x in (x + int(w * 0.18), x + int(w * 0.72)):
        draw.ellipse((wheel_x, y + h - 10, wheel_x + 32, y + h + 22), fill=(20, 24, 28))
    _draw_plate(draw, box, plate)


def _base_frame(second: float) -> Image.Image:
    canvas = Image.new("RGB", (ROAD_W, ROAD_H), color=(18, 32, 53))
    draw = ImageDraw.Draw(canvas, "RGBA")

    draw.rectangle((0, 0, ROAD_W, ROAD_H), fill=(20, 33, 55))
    draw.polygon([(90, 115), (1190, 115), (1270, 690), (10, 690)], fill=(55, 59, 68))

    for y in (210, 325, 470):
        draw.line((50, y, 1230, y + 40), fill=(250, 250, 250, 180), width=6)
        for dash in range(8):
            x1 = 150 + dash * 120
            draw.line((x1, y + 18, x1 + 60, y + 21), fill=(255, 255, 255, 220), width=4)

    emergency_polygon = [(40, 510), (1235, 555), (1270, 690), (0, 690)]
    draw.polygon(emergency_polygon, fill=(190, 32, 32, 96), outline=(255, 70, 70, 220))

    font_big = _find_font(34)
    font_small = _find_font(26)
    draw.text((50, 40), "高速公路应急车道智能抓拍演示视频", fill=(255, 255, 255), font=font_big)
    draw.text((52, 86), "右侧红色高亮区域为应急车道，蓝色车辆将持续占用触发告警", fill=(196, 214, 242), font=font_small)
    draw.text((830, 610), "应急车道 / Emergency Lane", fill=(255, 220, 220), font=_find_font(32))
    draw.text((52, 650), f"采样时间 {second:04.1f}s", fill=(255, 255, 255), font=_find_font(28))
    return canvas


def ensure_demo_assets() -> dict:
    settings.ensure_dirs()
    if settings.demo_video_path.exists() and settings.demo_manifest_path.exists() and settings.demo_cover_path.exists():
        return json.loads(settings.demo_manifest_path.read_text(encoding="utf-8"))

    writer = cv2.VideoWriter(
        str(settings.demo_video_path),
        cv2.VideoWriter_fourcc(*"mp4v"),
        FPS,
        (ROAD_W, ROAD_H),
    )
    if not writer.isOpened():
        raise RuntimeError("演示视频创建失败，请检查 OpenCV 视频编码支持。")

    manifest = {
        "fps": FPS,
        "duration_seconds": DURATION_SECONDS,
        "frame_interval_seconds": settings.frame_interval_seconds,
        "location": settings.location_label,
        "lane_label": "应急车道",
        "vehicles": [asdict(item) for item in VEHICLES],
        "expected_violation_plate": "沪A12345",
        "expected_violation_range": [4.0, 11.0],
    }

    cover_saved = False
    for frame_index in range(FPS * DURATION_SECONDS):
        second = frame_index / FPS
        canvas = _base_frame(second)
        draw = ImageDraw.Draw(canvas)

        for track in VEHICLES:
            box = _vehicle_position(track, second)
            if box:
                _draw_car(draw, box, track.color, track.plate)

        frame_bgr = cv2.cvtColor(np.array(canvas), cv2.COLOR_RGB2BGR)
        writer.write(frame_bgr)
        if not cover_saved and second >= 5.0:
            canvas.save(settings.demo_cover_path)
            cover_saved = True

    writer.release()
    settings.demo_manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return manifest
