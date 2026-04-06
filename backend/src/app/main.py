from __future__ import annotations

import json
from datetime import datetime, timedelta
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from . import db
from .config import settings
from .schemas import ReportRequest
from .services.analyzer import VideoAnalysisService
from .services.demo_assets import ensure_demo_assets


settings.ensure_dirs()
db.init_db()
MANIFEST = ensure_demo_assets()

app = FastAPI(title=settings.project_name)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.mount("/media/evidence", StaticFiles(directory=settings.evidence_dir), name="evidence")
app.mount("/media/data", StaticFiles(directory=settings.data_dir), name="data")
app.mount("/media/clips", StaticFiles(directory=settings.clip_dir), name="clips")
app.mount("/media/reports", StaticFiles(directory=settings.report_dir), name="reports")
app.state.analysis_service = VideoAnalysisService()


def _absolute_media_url(request: Request, relative_path: str) -> str:
    return str(request.base_url).rstrip("/") + relative_path


def _source_payload(request: Request) -> dict[str, Any]:
    frame_count = int(MANIFEST["fps"] * MANIFEST["duration_seconds"])
    return {
        "name": "demo-highway-camera-01",
        "video_url": _absolute_media_url(request, "/media/data/demo_highway.mp4"),
        "preview_url": _absolute_media_url(request, "/media/data/demo_cover.jpg"),
        "fps": MANIFEST["fps"],
        "frame_count": frame_count,
        "duration_seconds": MANIFEST["duration_seconds"],
        "sample_interval_seconds": settings.frame_interval_seconds,
        "case_clip_seconds": settings.case_clip_seconds,
        "reference_mode": "网页大屏 + Android 移动端",
    }


def _event_time_window(row) -> tuple[str, str]:
    created_at = datetime.fromisoformat(row["created_at"])
    first_seen = created_at + timedelta(seconds=float(row["start_second"]))
    last_seen = created_at + timedelta(seconds=float(row["end_second"]))
    return first_seen.isoformat(), last_seen.isoformat()


def _event_summary(row) -> dict[str, Any]:
    first_seen, last_seen = _event_time_window(row)
    report_number = f"RP-{row['id'].split('-')[-1]}"
    return {
        "id": row["id"],
        "case_id": row["case_id"],
        "plate_number": row["plate_number"],
        "status": row["status"],
        "location": row["location"],
        "lane_name": row["lane_label"],
        "first_seen": first_seen,
        "last_seen": last_seen,
        "duration_seconds": round(row["duration_seconds"], 1),
        "confidence": round(row["confidence"], 2),
        "summary": row["summary"],
        "report_number": report_number if row["status"] == "已举报" else None,
    }


def _event_detail(row, evidence_rows, request: Request) -> dict[str, Any]:
    evidence = []
    timeline = []
    for item in evidence_rows:
        parsed = json.loads(item["raw_result"])
        captured_at = datetime.fromisoformat(row["created_at"]) + timedelta(seconds=float(item["second"]))
        evidence.append(
            {
                "label": item["note"],
                "image_url": _absolute_media_url(request, f"/media/evidence/{item['image_path']}"),
                "captured_at": captured_at.isoformat(),
            }
        )
        timeline.append(
            {
                "timestamp_seconds": item["second"],
                "plate_number": parsed.get("plate_number", row["plate_number"]),
                "confidence": parsed.get("confidence", row["confidence"]),
                "description": parsed.get("reason", row["summary"]),
                "frame_path": item["image_path"],
            }
        )

    summary = _event_summary(row)
    return {
        **summary,
        "source_name": "demo-highway-camera-01",
        "description": row["raw_reason"] or row["summary"],
        "vehicle_count": 1,
        "reported_at": row["report_submitted_at"],
        "report_content": row["report_content"],
        "evidence": evidence,
        "raw_analysis": {
            "source_mode": [app.state.analysis_service.mode],
            "timeline": timeline,
        },
    }


def _case_summary(row, request: Request) -> dict[str, Any]:
    report_number = f"CASE-RP-{row['id'].split('-')[-1]}"
    return {
        "id": row["id"],
        "plate_number": row["plate_number"],
        "status": row["status"],
        "location": row["location"],
        "confidence": round(row["confidence"], 2),
        "summary": row["summary"],
        "duration_seconds": round(row["duration_seconds"], 1),
        "first_seen": row["first_seen"],
        "last_seen": row["last_seen"],
        "event_count": int(row["event_count"]),
        "evidence_count": int(row["evidence_count"]),
        "clip_url": _absolute_media_url(request, f"/media/clips/{row['clip_path']}") if row["clip_path"] else None,
        "report_number": report_number if row["status"] == "已举报" else None,
    }


def _case_detail(row, event_rows, evidence_rows, request: Request) -> dict[str, Any]:
    evidence = []
    timeline = []
    for item in evidence_rows:
        parsed = json.loads(item["raw_result"])
        evidence.append(
            {
                "label": item["note"],
                "image_url": _absolute_media_url(request, f"/media/evidence/{item['image_path']}"),
                "captured_at": (datetime.fromisoformat(row["created_at"]) + timedelta(seconds=float(item["second"]))).isoformat(),
            }
        )
        timeline.append(
            {
                "timestamp_seconds": item["second"],
                "plate_number": parsed.get("plate_number", row["plate_number"]),
                "confidence": parsed.get("confidence", row["confidence"]),
                "description": parsed.get("reason", row["summary"]),
                "frame_path": item["image_path"],
            }
        )

    return {
        **_case_summary(row, request),
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
        "clip_url": _absolute_media_url(request, f"/media/clips/{row['clip_path']}") if row["clip_path"] else None,
        "report_content": row["report_content"],
        "report_file_url": _absolute_media_url(request, f"/media/reports/{row['report_path']}") if row["report_path"] else None,
        "reported_at": row["report_submitted_at"],
        "events": [_event_summary(event_row) for event_row in event_rows],
        "evidence": evidence,
        "raw_analysis": {
            "timeline": timeline,
            "source_mode": [app.state.analysis_service.mode],
        },
    }


@app.get(f"{settings.api_prefix}/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get(f"{settings.api_prefix}/overview")
def overview(request: Request) -> dict[str, Any]:
    events = db.list_events()
    cases = db.list_cases()
    last_run = db.latest_run()

    avg_confidence = round(sum(row["confidence"] for row in events) / len(events), 2) if events else 0.0

    trend_map: dict[str, int] = {}
    for row in events:
        label = datetime.fromisoformat(row["created_at"]).strftime("%H:%M")
        trend_map[label] = trend_map.get(label, 0) + 1

    return {
        "summary": {
            "total_events": len(events),
            "pending_events": sum(1 for row in events if row["status"] != "已举报"),
            "reported_events": sum(1 for row in events if row["status"] == "已举报"),
            "recognized_plates": len({row["plate_number"] for row in events}),
            "avg_confidence": avg_confidence,
            "last_run_at": last_run["finished_at"] if last_run else None,
            "active_source": "demo-highway-camera-01",
            "total_cases": len(cases),
            "reported_cases": sum(1 for row in cases if row["status"] == "已举报"),
        },
        "trend": [{"label": label, "count": count} for label, count in sorted(trend_map.items())],
        "recent_events": [_event_summary(row) for row in events[:5]],
        "recent_cases": [_case_summary(row, request) for row in cases[:3]],
        "source": _source_payload(request),
        "system": {
            "web_role": "政府大屏总览 / 案件库 / 证据链展示",
            "android_role": "移动协同查看 / 案件详情 / 举报辅助",
            "reference_basis": "newnew Android 原型文档已抽象为当前项目的双端方案说明",
        },
    }


@app.get(f"{settings.api_prefix}/events")
def events() -> list[dict[str, Any]]:
    return [_event_summary(row) for row in db.list_events()]


@app.get(f"{settings.api_prefix}/events/{{event_id}}")
def event_detail(request: Request, event_id: str) -> dict[str, Any]:
    row = db.get_event(event_id)
    if not row:
        raise HTTPException(status_code=404, detail="事件不存在")
    evidence_rows = db.get_evidence(event_id)
    return _event_detail(row, evidence_rows, request)


@app.get(f"{settings.api_prefix}/cases")
def cases(request: Request) -> list[dict[str, Any]]:
    return [_case_summary(row, request) for row in db.list_cases()]


@app.get(f"{settings.api_prefix}/cases/{{case_id}}")
def case_detail(request: Request, case_id: str) -> dict[str, Any]:
    row = db.get_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="案件不存在")
    event_rows = db.get_case_events(case_id)
    evidence_rows = db.get_case_evidence(case_id)
    return _case_detail(row, event_rows, evidence_rows, request)


@app.post(f"{settings.api_prefix}/events/{{event_id}}/report")
def report_event(event_id: str, payload: ReportRequest | None = None) -> dict[str, Any]:
    row = db.get_event(event_id)
    if not row:
        raise HTTPException(status_code=404, detail="事件不存在")
    payload = payload or ReportRequest()
    report_number = f"RP-{event_id.split('-')[-1]}"
    report_content = (
        f"举报人：{payload.reporter_name}；联系电话：{payload.reporter_phone}；"
        f"举报内容：车辆 {row['plate_number']} 疑似占用应急车道。备注：{payload.note}；"
        f"模拟举报编号：{report_number}"
    )
    if row["case_id"]:
        db.report_case(row["case_id"], report_content)
    else:
        db.report_event(event_id, report_content)
    updated = db.get_event(event_id)
    return {
        "event_id": event_id,
        "case_id": updated["case_id"],
        "status": "已举报",
        "report_number": report_number,
        "reported_at": updated["report_submitted_at"],
        "message": "模拟举报提交成功",
    }


@app.post(f"{settings.api_prefix}/cases/{{case_id}}/report")
def report_case(case_id: str, payload: ReportRequest | None = None) -> dict[str, Any]:
    row = db.get_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="案件不存在")
    payload = payload or ReportRequest()
    report_number = f"CASE-RP-{case_id.split('-')[-1]}"
    report_content = (
        f"举报人：{payload.reporter_name}；联系电话：{payload.reporter_phone}；"
        f"举报内容：车辆 {row['plate_number']} 疑似占用应急车道。备注：{payload.note}；"
        f"模拟举报编号：{report_number}"
    )
    db.report_case(case_id, report_content)
    updated = db.get_case(case_id)
    return {
        "case_id": case_id,
        "status": "已举报",
        "report_number": report_number,
        "reported_at": updated["report_submitted_at"],
        "message": "案件模拟举报提交成功",
    }


@app.post(f"{settings.api_prefix}/tasks/analyze-demo")
def analyze_demo(request: Request) -> dict[str, Any]:
    result = app.state.analysis_service.run_demo_analysis_sync()
    result["source"] = _source_payload(request)
    return result
