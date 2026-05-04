"""POST /api/synthesis — 生成合成视频."""
from __future__ import annotations

from fastapi import APIRouter
from pydantic import BaseModel, Field

from ..storage.store import create_video_id, video_path, video_url
from ..synthesis.video_composer import compose_video

router = APIRouter()


class SynthesisRequest(BaseModel):
    duration_seconds: float = Field(default=30.0, ge=5.0, le=120.0)
    num_normal: int = Field(default=4, ge=0, le=10)
    num_violating: int = Field(default=2, ge=0, le=5)
    seed: int = Field(default=42)
    fps: int = Field(default=30, ge=10, le=60)


class SynthesisResponse(BaseModel):
    video_id: str
    video_url: str
    duration_seconds: float
    fps: int
    seed: int


@router.post("/synthesis")
def synthesis(payload: SynthesisRequest) -> SynthesisResponse:
    video_id = create_video_id()
    vp = video_path(video_id)
    compose_video(
        vp,
        duration_seconds=payload.duration_seconds,
        fps=payload.fps,
        num_normal=payload.num_normal,
        num_violating=payload.num_violating,
        seed=payload.seed,
    )
    return SynthesisResponse(
        video_id=video_id,
        video_url=video_url(video_id),
        duration_seconds=payload.duration_seconds,
        fps=payload.fps,
        seed=payload.seed,
    )
