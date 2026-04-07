from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

from ..config import settings

ROAD_W = 1280
ROAD_H = 720
FPS = 12
DURATION_SECONDS = 16
REFERENCE_MODE = "网页大屏 + Android 移动端"
SUPPORTED_VIDEO_SUFFIXES = {".mp4", ".mov", ".avi", ".mkv"}


@dataclass(frozen=True)
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


@dataclass(frozen=True)
class DemoSourceSpec:
    name: str
    title: str
    location: str
    lane_label: str
    expected_violation_plate: str
    expected_violation_range: tuple[float, float]
    vehicles: tuple[VehicleTrack, ...]
    legacy_cover_name: str | None = None
    legacy_manifest_name: str | None = None


DEMO_SPECS: tuple[DemoSourceSpec, ...] = (
    DemoSourceSpec(
        name="demo_highway.mp4",
        title="演示视频 A / 主路段",
        location=settings.location_label,
        lane_label="应急车道",
        expected_violation_plate="沪A12345",
        expected_violation_range=(4.0, 11.0),
        vehicles=(
            VehicleTrack("沪A12345", (62, 114, 255), "emergency", 555, -260, 1260, 4.0, 11.0),
            VehicleTrack("浙B67890", (255, 108, 66), "main", 395, -200, 1420, 0.0, 16.0),
            VehicleTrack("苏C88888", (94, 196, 129), "main", 295, 1280, -220, 1.0, 14.0),
        ),
        legacy_cover_name="demo_cover.jpg",
        legacy_manifest_name="demo_manifest.json",
    ),
    DemoSourceSpec(
        name="demo_highway_alt.mp4",
        title="演示视频 B / 分流路段",
        location="G60 沪昆高速 K18+900 分流测试路段",
        lane_label="应急车道",
        expected_violation_plate="皖D55667",
        expected_violation_range=(2.5, 9.5),
        vehicles=(
            VehicleTrack("皖D55667", (255, 179, 71), "emergency", 545, -240, 1300, 2.5, 9.5),
            VehicleTrack("沪F10293", (93, 211, 158), "main", 390, -220, 1380, 0.0, 16.0),
            VehicleTrack("苏A77118", (114, 161, 255), "main", 292, 1280, -260, 1.5, 13.5),
        ),
    ),
)


def _find_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    font_candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
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


def _base_frame(second: float, title: str, location: str) -> Image.Image:
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
    draw.text((50, 40), f"高速公路应急车道智能抓拍演示视频 · {title}", fill=(255, 255, 255), font=font_big)
    draw.text((52, 86), f"{location} · 红色高亮区域为应急车道", fill=(196, 214, 242), font=font_small)
    draw.text((830, 610), "应急车道 / Emergency Lane", fill=(255, 220, 220), font=_find_font(32))
    draw.text((52, 650), f"采样时间 {second:04.1f}s", fill=(255, 255, 255), font=_find_font(28))
    return canvas


def _manifest_path_for_spec(spec: DemoSourceSpec) -> Path:
    if spec.legacy_manifest_name:
        return settings.data_dir / spec.legacy_manifest_name
    return settings.data_dir / f"{Path(spec.name).stem}.json"


def _cover_path_for_spec(spec: DemoSourceSpec) -> Path:
    if spec.legacy_cover_name:
        return settings.data_dir / spec.legacy_cover_name
    return settings.data_dir / f"{Path(spec.name).stem}.jpg"


def _video_path_for_name(source_name: str) -> Path:
    return settings.data_dir / source_name


def _build_manifest(spec: DemoSourceSpec) -> dict[str, Any]:
    return {
        "name": spec.name,
        "title": spec.title,
        "video_filename": spec.name,
        "preview_filename": _cover_path_for_spec(spec).name,
        "fps": FPS,
        "duration_seconds": DURATION_SECONDS,
        "frame_interval_seconds": settings.frame_interval_seconds,
        "location": spec.location,
        "lane_label": spec.lane_label,
        "vehicles": [asdict(item) for item in spec.vehicles],
        "expected_violation_plate": spec.expected_violation_plate,
        "expected_violation_range": list(spec.expected_violation_range),
        "reference_mode": REFERENCE_MODE,
    }


def _ensure_demo_asset(spec: DemoSourceSpec) -> None:
    settings.ensure_dirs()
    video_path = _video_path_for_name(spec.name)
    manifest_path = _manifest_path_for_spec(spec)
    cover_path = _cover_path_for_spec(spec)
    if video_path.exists() and manifest_path.exists() and cover_path.exists():
        return

    writer = cv2.VideoWriter(
        str(video_path),
        cv2.VideoWriter_fourcc(*"mp4v"),
        FPS,
        (ROAD_W, ROAD_H),
    )
    if not writer.isOpened():
        raise RuntimeError(f"演示视频创建失败：{spec.name}")

    manifest = _build_manifest(spec)
    cover_saved = False
    for frame_index in range(FPS * DURATION_SECONDS):
        second = frame_index / FPS
        canvas = _base_frame(second, spec.title, spec.location)
        draw = ImageDraw.Draw(canvas)

        for track in spec.vehicles:
            box = _vehicle_position(track, second)
            if box:
                _draw_car(draw, box, track.color, track.plate)

        frame_bgr = cv2.cvtColor(np.array(canvas), cv2.COLOR_RGB2BGR)
        writer.write(frame_bgr)
        if not cover_saved and second >= 5.0:
            canvas.save(cover_path)
            cover_saved = True

    writer.release()
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")


def ensure_demo_assets() -> dict[str, dict[str, Any]]:
    for spec in DEMO_SPECS:
        _ensure_demo_asset(spec)
    return {item["name"]: item for item in list_video_sources()}


def _load_manifest_for_video(video_path: Path) -> dict[str, Any] | None:
    candidates = [
        video_path.with_suffix(".json"),
        video_path.with_name(f"{video_path.stem}_manifest.json"),
    ]
    if video_path.name == "demo_highway.mp4":
        candidates.append(settings.data_dir / "demo_manifest.json")
    for candidate in candidates:
        if candidate.exists():
            try:
                return json.loads(candidate.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                return None
    return None


def _cover_path_for_video(video_path: Path) -> Path | None:
    candidates = [
        video_path.with_suffix(".jpg"),
        video_path.with_name(f"{video_path.stem}_cover.jpg"),
        video_path.with_suffix(".png"),
    ]
    if video_path.name == "demo_highway.mp4":
        candidates.append(settings.data_dir / "demo_cover.jpg")
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None


def _video_meta(video_path: Path) -> tuple[float, int, float]:
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        return 0.0, 0, 0.0
    fps = float(cap.get(cv2.CAP_PROP_FPS) or 0.0)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
    duration = frame_count / fps if fps else 0.0
    cap.release()
    return fps, frame_count, duration


def list_video_sources() -> list[dict[str, Any]]:
    settings.ensure_dirs()
    videos = sorted(
        [path for path in settings.data_dir.iterdir() if path.is_file() and path.suffix.lower() in SUPPORTED_VIDEO_SUFFIXES],
        key=lambda item: item.name,
    )
    sources: list[dict[str, Any]] = []
    for video_path in videos:
        manifest = _load_manifest_for_video(video_path) or {}
        cover_path = _cover_path_for_video(video_path)
        fps, frame_count, duration_seconds = _video_meta(video_path)
        sources.append(
            {
                "name": video_path.name,
                "title": manifest.get("title") or video_path.stem,
                "video_filename": video_path.name,
                "preview_filename": cover_path.name if cover_path else None,
                "fps": manifest.get("fps") or fps,
                "frame_count": frame_count,
                "duration_seconds": manifest.get("duration_seconds") or duration_seconds,
                "sample_interval_seconds": settings.frame_interval_seconds,
                "case_clip_seconds": settings.case_clip_seconds,
                "reference_mode": manifest.get("reference_mode") or REFERENCE_MODE,
                "location": manifest.get("location") or settings.location_label,
                "lane_label": manifest.get("lane_label") or "应急车道",
                "expected_violation_plate": manifest.get("expected_violation_plate") or "",
                "expected_violation_range": manifest.get("expected_violation_range") or [0.0, 0.0],
                "manifest": manifest,
                "video_path": str(video_path),
                "preview_path": str(cover_path) if cover_path else None,
            }
        )
    return sources


def get_source_info(source_name: str) -> dict[str, Any]:
    ensure_demo_assets()
    for item in list_video_sources():
        if item["name"] == source_name:
            return item
    raise FileNotFoundError(f"未找到视频源：{source_name}")


def default_source_name() -> str:
    ensure_demo_assets()
    sources = list_video_sources()
    return sources[0]["name"] if sources else "demo_highway.mp4"
