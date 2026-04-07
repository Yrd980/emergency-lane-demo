from __future__ import annotations

import json
from datetime import datetime, timedelta
from typing import Any

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from . import db
from .config import settings
from .schemas import AnalyzeDemoRequest, CaseUpdateRequest, ReportRequest
from .services.analyzer import VideoAnalysisService
from .services.demo_assets import default_source_name, ensure_demo_assets, get_source_info, list_video_sources

settings.ensure_dirs()
db.init_db()
ensure_demo_assets()

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


def _source_payload(request: Request, source: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": source["name"],
        "title": source.get("title") or source["name"],
        "video_url": _absolute_media_url(request, f"/media/data/{source['video_filename']}"),
        "preview_url": _absolute_media_url(request, f"/media/data/{source['preview_filename']}") if source.get("preview_filename") else None,
        "fps": source.get("fps", 0),
        "frame_count": source.get("frame_count", 0),
        "duration_seconds": source.get("duration_seconds", 0),
        "sample_interval_seconds": source.get("sample_interval_seconds", settings.frame_interval_seconds),
        "case_clip_seconds": source.get("case_clip_seconds", settings.case_clip_seconds),
        "reference_mode": source.get("reference_mode", "网页大屏 + Android 移动端"),
        "location": source.get("location", settings.location_label),
        "lane_label": source.get("lane_label", "应急车道"),
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
        "run_id": row["run_id"],
        "case_id": row["case_id"],
        "plate_number": row["plate_number"],
        "corrected_plate_number": row["corrected_plate_number"],
        "review_status": row["review_status"] or db.CASE_REVIEW_PENDING,
        "source_name": row["source_name"],
        "status": row["status"],
        "location": row["location"],
        "lane_name": row["lane_label"],
        "first_seen": first_seen,
        "last_seen": last_seen,
        "duration_seconds": round(row["duration_seconds"], 1),
        "confidence": round(row["confidence"], 2),
        "summary": row["summary"],
        "report_number": report_number if row["status"] == db.CASE_STATUS_REPORTED else None,
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
        "run_id": row["run_id"],
        "source_name": row["source_name"],
        "plate_number": row["plate_number"],
        "corrected_plate_number": row["corrected_plate_number"],
        "review_status": row["review_status"],
        "operator_note": row["operator_note"],
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
        "report_number": report_number if row["status"] == db.CASE_STATUS_REPORTED else None,
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


def _run_summary(row) -> dict[str, Any]:
    return {
        "id": row["id"],
        "source_name": row["source_name"],
        "source_video": row["source_video"],
        "status": row["status"],
        "mode": row["mode"],
        "started_at": row["started_at"],
        "finished_at": row["finished_at"],
        "progress_percent": row["progress_percent"],
        "message": row["message"],
        "error_message": row["error_message"],
        "events_created": int(row["events_created"] or 0),
        "cases_created": int(row["cases_created"] or 0),
        "event_count": int(row["event_count"] or 0),
        "case_count": int(row["case_count"] or 0),
    }


def _pipeline_payload() -> list[dict[str, Any]]:
    return [
        {
            "key": "sampling",
            "title": "关键帧抽样",
            "owner": "Backend",
            "summary": "按时间间隔抽帧并保留每次 run 的独立取证输入。",
            "evidence": ["关键帧 JPG", "抽样时间点", "source video"],
        },
        {
            "key": "vision",
            "title": "视觉识别",
            "owner": "Backend + 智谱",
            "summary": "识别应急车道占用与车牌信息，保留每帧原始识别原因和置信度。",
            "evidence": ["车牌号", "违规原因", "置信度"],
        },
        {
            "key": "fusion",
            "title": "时序融合",
            "owner": "Backend",
            "summary": "把连续命中帧聚合为事件，再按车牌归档为案件，并等待人工复核。",
            "evidence": ["事件时间窗", "案件聚合结果", "review status"],
        },
        {
            "key": "evidence",
            "title": "证据链生成",
            "owner": "Backend",
            "summary": "自动输出证据图、15 秒证据片段和正式举报文书，支撑答辩演示。",
            "evidence": ["证据图", "15 秒片段", "TXT 文书"],
        },
    ]


def _derive_case_update(existing_row, payload: CaseUpdateRequest) -> dict[str, Any]:
    if existing_row["status"] == db.CASE_STATUS_REPORTED and (payload.review_status is not None or payload.status is not None):
        raise HTTPException(status_code=409, detail="已举报案件不能再修改复核/状态流转")

    fields: dict[str, Any] = {}
    if payload.corrected_plate_number is not None:
        fields["corrected_plate_number"] = payload.corrected_plate_number.strip() or None
    if payload.operator_note is not None:
        fields["operator_note"] = payload.operator_note.strip()

    current_review_status = existing_row["review_status"]
    current_status = existing_row["status"]
    next_review_status = payload.review_status or current_review_status
    next_status = payload.status or current_status

    if payload.review_status is not None and payload.status is None:
        if payload.review_status == db.CASE_REVIEW_APPROVED:
            next_status = db.CASE_STATUS_READY_TO_REPORT
        else:
            next_status = db.CASE_STATUS_PENDING_REVIEW

    if payload.status is not None and payload.review_status is None:
        if payload.status == db.CASE_STATUS_READY_TO_REPORT:
            next_review_status = db.CASE_REVIEW_APPROVED
        elif payload.status == db.CASE_STATUS_PENDING_REVIEW and current_review_status == db.CASE_REVIEW_APPROVED:
            next_review_status = db.CASE_REVIEW_PENDING

    if next_status == db.CASE_STATUS_READY_TO_REPORT and next_review_status != db.CASE_REVIEW_APPROVED:
        raise HTTPException(status_code=400, detail="只有复核通过的案件才能进入待举报")
    if next_status == db.CASE_STATUS_PENDING_REVIEW and next_review_status == db.CASE_REVIEW_APPROVED:
        raise HTTPException(status_code=400, detail="复核通过后状态不能仍为待复核")

    fields["review_status"] = next_review_status
    fields["status"] = next_status
    return fields


@app.get(f"{settings.api_prefix}/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get(f"{settings.api_prefix}/sources")
def sources(request: Request) -> list[dict[str, Any]]:
    ensure_demo_assets()
    return [_source_payload(request, source) for source in list_video_sources()]


@app.get(f"{settings.api_prefix}/runs")
def runs() -> list[dict[str, Any]]:
    return [_run_summary(row) for row in db.list_runs()]


@app.get(f"{settings.api_prefix}/runs/{{run_id}}")
def run_detail(run_id: str) -> dict[str, Any]:
    row = db.get_run(run_id)
    if not row:
        raise HTTPException(status_code=404, detail="分析任务不存在")
    events = db.list_events(run_id=run_id)
    cases = db.list_cases(run_id=run_id)
    return {
        **_run_summary(row),
        "events": [_event_summary(item) for item in events],
        "cases": [
            {
                "id": item["id"],
                "plate_number": item["plate_number"],
                "corrected_plate_number": item["corrected_plate_number"],
                "review_status": item["review_status"],
                "status": item["status"],
            }
            for item in cases
        ],
    }


@app.get(f"{settings.api_prefix}/overview")
def overview(request: Request) -> dict[str, Any]:
    runs = db.list_runs()
    events = db.list_events()
    cases = db.list_cases()
    latest_run = runs[0] if runs else None
    source_list = list_video_sources()
    active_source = latest_run["source_name"] if latest_run else default_source_name()
    active_source_payload = _source_payload(request, get_source_info(active_source))
    avg_confidence = round(sum(row["confidence"] for row in events) / len(events), 2) if events else 0.0

    trend_map: dict[str, int] = {}
    for row in events:
        label = datetime.fromisoformat(row["created_at"]).strftime("%H:%M")
        trend_map[label] = trend_map.get(label, 0) + 1

    return {
        "summary": {
            "total_events": len(events),
            "pending_events": sum(1 for row in events if row["status"] != db.CASE_STATUS_REPORTED),
            "reported_events": sum(1 for row in events if row["status"] == db.CASE_STATUS_REPORTED),
            "recognized_plates": len({row["plate_number"] for row in events}),
            "avg_confidence": avg_confidence,
            "last_run_at": latest_run["finished_at"] if latest_run else None,
            "active_source": active_source,
            "total_cases": len(cases),
            "reported_cases": sum(1 for row in cases if row["status"] == db.CASE_STATUS_REPORTED),
            "pending_review_cases": sum(1 for row in cases if row["status"] == db.CASE_STATUS_PENDING_REVIEW),
        },
        "latest_run": _run_summary(latest_run) if latest_run else None,
        "runs": [_run_summary(row) for row in runs[:5]],
        "trend": [{"label": label, "count": count} for label, count in sorted(trend_map.items())],
        "recent_events": [_event_summary(row) for row in events[:5]],
        "recent_cases": [_case_summary(row, request) for row in cases[:5]],
        "source": active_source_payload,
        "sources": [_source_payload(request, item) for item in source_list],
        "pipeline": _pipeline_payload(),
        "system": {
            "web_role": "总览 / run 历史 / 事件案件筛选 / 证据链展示",
            "android_role": "移动协同查看 / 案件复核辅助 / 举报状态同步",
            "reference_basis": "newnew 仅作思路参考，不并轨 CameraX/JNI 主链",
        },
    }


@app.get(f"{settings.api_prefix}/events")
def events(
    status: str | None = Query(default=None),
    plate: str | None = Query(default=None),
    run_id: str | None = Query(default=None),
    review_status: str | None = Query(default=None),
) -> list[dict[str, Any]]:
    return [_event_summary(row) for row in db.list_events(status=status, plate=plate, run_id=run_id, review_status=review_status)]


@app.get(f"{settings.api_prefix}/events/{{event_id}}")
def event_detail(request: Request, event_id: str) -> dict[str, Any]:
    row = db.get_event(event_id)
    if not row:
        raise HTTPException(status_code=404, detail="事件不存在")
    evidence_rows = db.get_evidence(event_id)
    return _event_detail(row, evidence_rows, request)


@app.get(f"{settings.api_prefix}/cases")
def cases(
    request: Request,
    status: str | None = Query(default=None),
    plate: str | None = Query(default=None),
    run_id: str | None = Query(default=None),
    review_status: str | None = Query(default=None),
) -> list[dict[str, Any]]:
    return [_case_summary(row, request) for row in db.list_cases(status=status, plate=plate, run_id=run_id, review_status=review_status)]


@app.get(f"{settings.api_prefix}/cases/{{case_id}}")
def case_detail(request: Request, case_id: str) -> dict[str, Any]:
    row = db.get_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="案件不存在")
    event_rows = db.get_case_events(case_id)
    evidence_rows = db.get_case_evidence(case_id)
    return _case_detail(row, event_rows, evidence_rows, request)


@app.patch(f"{settings.api_prefix}/cases/{{case_id}}")
def patch_case(request: Request, case_id: str, payload: CaseUpdateRequest) -> dict[str, Any]:
    row = db.get_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="案件不存在")
    fields = _derive_case_update(row, payload)
    db.update_case(case_id, **fields)
    if "status" in fields:
        with db.get_conn() as conn:
            conn.execute("UPDATE events SET status = ? WHERE case_id = ?", (fields["status"], case_id))
    updated = db.get_case(case_id)
    event_rows = db.get_case_events(case_id)
    evidence_rows = db.get_case_evidence(case_id)
    return _case_detail(updated, event_rows, evidence_rows, request)


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
        case_row = db.get_case(row["case_id"])
        if case_row and case_row["status"] not in {db.CASE_STATUS_READY_TO_REPORT, db.CASE_STATUS_REPORTED}:
            raise HTTPException(status_code=409, detail="案件尚未复核通过，不能直接举报")
        db.report_case(row["case_id"], report_content)
    else:
        db.report_event(event_id, report_content)
    updated = db.get_event(event_id)
    return {
        "event_id": event_id,
        "case_id": updated["case_id"],
        "status": db.CASE_STATUS_REPORTED,
        "report_number": report_number,
        "reported_at": updated["report_submitted_at"],
        "message": "模拟举报提交成功",
    }


@app.post(f"{settings.api_prefix}/cases/{{case_id}}/report")
def report_case(case_id: str, payload: ReportRequest | None = None) -> dict[str, Any]:
    row = db.get_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="案件不存在")
    if row["status"] == db.CASE_STATUS_REPORTED:
        report_number = f"CASE-RP-{case_id.split('-')[-1]}"
        return {
            "case_id": case_id,
            "status": db.CASE_STATUS_REPORTED,
            "report_number": report_number,
            "reported_at": row["report_submitted_at"],
            "message": "案件已处于已举报状态",
        }
    if row["status"] != db.CASE_STATUS_READY_TO_REPORT:
        raise HTTPException(status_code=409, detail="案件需先完成复核通过后才能举报")
    payload = payload or ReportRequest()
    report_number = f"CASE-RP-{case_id.split('-')[-1]}"
    plate_number = row["corrected_plate_number"] or row["plate_number"]
    report_content = (
        f"举报人：{payload.reporter_name}；联系电话：{payload.reporter_phone}；"
        f"举报内容：车辆 {plate_number} 疑似占用应急车道。备注：{payload.note}；"
        f"模拟举报编号：{report_number}"
    )
    db.report_case(case_id, report_content)
    updated = db.get_case(case_id)
    return {
        "case_id": case_id,
        "status": db.CASE_STATUS_REPORTED,
        "report_number": report_number,
        "reported_at": updated["report_submitted_at"],
        "message": "案件模拟举报提交成功",
    }


@app.post(f"{settings.api_prefix}/tasks/analyze-demo")
def analyze_demo(request: Request, payload: AnalyzeDemoRequest | None = None) -> dict[str, Any]:
    payload = payload or AnalyzeDemoRequest()
    source_name = payload.source_name or default_source_name()
    try:
        result = app.state.analysis_service.run_demo_analysis_sync(source_name=source_name)
    except FileNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    result["source"] = _source_payload(request, get_source_info(source_name))
    result["run"] = _run_summary(db.get_run(result["run_id"]))
    return result
