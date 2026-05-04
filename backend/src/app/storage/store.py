"""文件存储管理 — 视频和结果 JSON 的读写."""
from __future__ import annotations

import json
from pathlib import Path
from uuid import uuid4

from ..config import settings


def create_video_id() -> str:
    return uuid4().hex[:12]


def video_path(video_id: str) -> Path:
    return settings.videos_dir / f"{video_id}.mp4"


def video_url(video_id: str) -> str:
    return f"/media/videos/{video_id}.mp4"


def result_path(video_id: str) -> Path:
    return settings.results_dir / f"{video_id}.json"


def result_frame_dir(video_id: str) -> Path:
    return settings.results_dir / video_id / "frames"


def result_timeline_path(video_id: str) -> Path:
    return settings.results_dir / video_id / "timeline.png"


def save_result(video_id: str, data: dict) -> Path:
    p = result_path(video_id)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2))
    return p


def load_result(video_id: str) -> dict | None:
    p = result_path(video_id)
    if not p.exists():
        return None
    return json.loads(p.read_text())
