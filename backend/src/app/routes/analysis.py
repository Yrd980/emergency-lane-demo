"""POST /api/analyze/{video_id} — 运行算法管线."""
from __future__ import annotations

import json

from fastapi import APIRouter, HTTPException

from algorithm.pipeline import Pipeline
from ..config import settings
from ..storage.store import (
    load_result, result_frame_dir, result_timeline_path,
    save_result, video_path,
)
from algorithm.visualize import draw_timeline, save_keyframes

router = APIRouter()


@router.post("/analyze/{video_id}")
def analyze(video_id: str) -> dict:
    vp = video_path(video_id)
    if not vp.exists():
        raise HTTPException(status_code=404, detail="视频不存在，请先合成")

    existing = load_result(video_id)
    if existing:
        return {"status": "done", "video_id": video_id, "message": "分析已完成"}

    gt_path = vp.with_suffix(".gt.json")
    ground_truth = None
    if gt_path.exists():
        ground_truth = json.loads(gt_path.read_text())

    pipeline = Pipeline(
        conf_threshold=settings.detection_conf_threshold,
        window_seconds=settings.window_seconds,
        ratio_threshold=settings.violation_ratio_threshold,
        device="cpu",
    )

    result = pipeline.run(vp, sample_every=2, ground_truth=ground_truth)

    frame_dir = result_frame_dir(video_id)
    frame_dir.mkdir(parents=True, exist_ok=True)

    keyframe_paths = save_keyframes(
        result.frame_results, result.violation_segments, frame_dir,
    )

    timeline_path = draw_timeline(
        result.violation_segments, result.total_frames, result.fps,
        result_timeline_path(video_id),
    )

    data = {
        "video_id": video_id,
        "total_frames": result.total_frames,
        "fps": result.fps,
        "violation_segments": [
            {
                "start_frame": s.start_frame,
                "end_frame": s.end_frame,
                "duration_frames": s.duration_frames,
                "duration_seconds": round(s.duration_frames / result.fps, 1),
                "occupancy_ratio": round(s.occupancy_ratio, 2),
                "confidence": round(s.confidence, 2),
            }
            for s in result.violation_segments
        ],
        "frame_results": [
            {
                "frame_idx": fr.frame_idx,
                "has_violation": fr.has_violation,
                "vehicle_count": len(fr.violations),
                "detections": [
                    {
                        "label": d.label,
                        "score": round(d.score, 2),
                        "x": round(d.x, 1),
                        "y": round(d.y, 1),
                        "w": round(d.w, 1),
                        "h": round(d.h, 1),
                    }
                    for d in fr.violations
                ],
            }
            for fr in result.frame_results
            if fr.has_violation or fr.frame_idx % 30 == 0
        ],
        "keyframe_count": len(keyframe_paths),
        "timeline_url": f"/media/results/{video_id}/timeline.png",
    }
    save_result(video_id, data)

    return {"status": "done", "video_id": video_id, **data}
