"""GET /api/results/{video_id} — 获取分析结果."""
from __future__ import annotations

from fastapi import APIRouter, HTTPException

from ..storage.store import load_result

router = APIRouter()


@router.get("/results/{video_id}")
def results(video_id: str) -> dict:
    data = load_result(video_id)
    if data is None:
        raise HTTPException(status_code=404, detail="分析结果不存在，请先运行分析")
    return data
