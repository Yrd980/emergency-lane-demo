from __future__ import annotations

from uuid import uuid4
from typing import Any

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from . import db
from .config import settings
from .schemas import AnalyzeDemoRequest, CaseUpdateRequest, DeviceCaseImportRequest, ReportRequest
from .services.analyzer import VideoAnalysisService
from .services.demo_assets import default_source_name, ensure_demo_assets, get_source_info, list_video_sources
from .services.presenters import (
    case_detail as present_case_detail,
    case_summary as present_case_summary,
    device_case_detail as present_device_case_detail,
    device_case_summary as present_device_case_summary,
    derive_case_update,
    event_detail as present_event_detail,
    event_summary as present_event_summary,
    pipeline_payload,
    run_summary,
    source_payload,
)
from .time_utils import parse_timestamp

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

@app.get(f"{settings.api_prefix}/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get(f"{settings.api_prefix}/sources")
def sources(request: Request) -> list[dict[str, Any]]:
    ensure_demo_assets()
    return [source_payload(request, source) for source in list_video_sources()]


@app.get(f"{settings.api_prefix}/runs")
def runs() -> list[dict[str, Any]]:
    return [run_summary(row) for row in db.list_runs()]


@app.get(f"{settings.api_prefix}/runs/{{run_id}}")
def run_detail(run_id: str) -> dict[str, Any]:
    row = db.get_run(run_id)
    if not row:
        raise HTTPException(status_code=404, detail="分析任务不存在")
    events = db.list_events(run_id=run_id)
    cases = db.list_cases(run_id=run_id)
    return {
        **run_summary(row),
        "events": [present_event_summary(item) for item in events],
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
    active_source_payload = source_payload(request, get_source_info(active_source))
    avg_confidence = round(sum(row["confidence"] for row in events) / len(events), 2) if events else 0.0

    trend_map: dict[str, int] = {}
    for row in events:
        label = parse_timestamp(row["created_at"]).strftime("%H:%M")
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
        "latest_run": run_summary(latest_run) if latest_run else None,
        "runs": [run_summary(row) for row in runs[:5]],
        "trend": [{"label": label, "count": count} for label, count in sorted(trend_map.items())],
        "recent_events": [present_event_summary(row) for row in events[:5]],
        "recent_cases": [present_case_summary(row, request) for row in cases[:5]],
        "source": active_source_payload,
        "sources": [source_payload(request, item) for item in source_list],
        "pipeline": pipeline_payload(),
        "system": {
            "web_role": "总览 / run 历史 / 事件案件筛选 / 证据链展示",
            "android_role": "云侧协同查看 / 端侧本地检测演示 / 案件复核辅助 / 举报状态同步",
            "reference_basis": "Android 已并入 newnew 端侧链路，用于 CameraX/JNI/Room 本地检测演示；FastAPI 主链继续承担案件归档、证据链与举报闭环。",
        },
        "device_sync": {
            "device_case_count": len(db.list_device_cases()),
            "sync_mode": "device-import-snapshot",
        },
    }


@app.get(f"{settings.api_prefix}/events")
def events(
    status: str | None = Query(default=None),
    plate: str | None = Query(default=None),
    run_id: str | None = Query(default=None),
    review_status: str | None = Query(default=None),
) -> list[dict[str, Any]]:
    return [present_event_summary(row) for row in db.list_events(status=status, plate=plate, run_id=run_id, review_status=review_status)]


@app.get(f"{settings.api_prefix}/events/{{event_id}}")
def event_detail(request: Request, event_id: str) -> dict[str, Any]:
    row = db.get_event(event_id)
    if not row:
        raise HTTPException(status_code=404, detail="事件不存在")
    evidence_rows = db.get_evidence(event_id)
    return present_event_detail(row, evidence_rows, request, app.state.analysis_service.mode)


@app.get(f"{settings.api_prefix}/cases")
def cases(
    request: Request,
    status: str | None = Query(default=None),
    plate: str | None = Query(default=None),
    run_id: str | None = Query(default=None),
    review_status: str | None = Query(default=None),
) -> list[dict[str, Any]]:
    return [present_case_summary(row, request) for row in db.list_cases(status=status, plate=plate, run_id=run_id, review_status=review_status)]


@app.get(f"{settings.api_prefix}/cases/{{case_id}}")
def case_detail(request: Request, case_id: str) -> dict[str, Any]:
    row = db.get_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="案件不存在")
    event_rows = db.get_case_events(case_id)
    evidence_rows = db.get_case_evidence(case_id)
    return present_case_detail(row, event_rows, evidence_rows, request, app.state.analysis_service.mode)


@app.patch(f"{settings.api_prefix}/cases/{{case_id}}")
def patch_case(request: Request, case_id: str, payload: CaseUpdateRequest) -> dict[str, Any]:
    row = db.get_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="案件不存在")
    fields = derive_case_update(row, payload)
    db.update_case(case_id, **fields)
    if "status" in fields:
        with db.get_conn() as conn:
            conn.execute("UPDATE events SET status = ? WHERE case_id = ?", (fields["status"], case_id))
    updated = db.get_case(case_id)
    event_rows = db.get_case_events(case_id)
    evidence_rows = db.get_case_evidence(case_id)
    return present_case_detail(updated, event_rows, evidence_rows, request, app.state.analysis_service.mode)


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
    result["source"] = source_payload(request, get_source_info(source_name))
    result["run"] = run_summary(db.get_run(result["run_id"]))
    return result


@app.post(f"{settings.api_prefix}/device/cases/import")
def import_device_cases(payload: DeviceCaseImportRequest) -> dict[str, Any]:
    if not payload.items:
        raise HTTPException(status_code=400, detail="至少导入一个端侧案件")

    imported_case_ids: list[str] = []
    imported_evidence_count = 0
    for item in payload.items:
        now = item.updated_at or item.created_at or db.utc_now()
        case_id = f"device-case-{uuid4().hex[:12]}"
        case_payload = {
            "id": case_id,
            "client_case_id": item.client_case_id,
            "device_label": payload.device_label,
            "source_mode": "local-device",
            "plate_number": item.plate_number,
            "corrected_plate_number": item.corrected_plate_number,
            "review_status": item.review_status,
            "status": item.status,
            "operator_note": item.operator_note,
            "summary": item.summary or f"端侧本地检测导入案件 {item.plate_number}",
            "location": item.location,
            "clip_uri": item.clip_uri,
            "report_text": item.report_text,
            "created_at": item.created_at or now,
            "updated_at": item.updated_at or now,
            "last_synced_at": db.utc_now(),
        }
        db.upsert_device_case(case_payload)
        imported_case_ids.append(case_payload["id"])
        for evidence in item.evidence:
            db.insert_device_evidence(
                case_payload["id"],
                {
                    "id": f"device-evidence-{uuid4().hex[:12]}",
                    "label": evidence.label,
                    "image_uri": evidence.image_uri,
                    "captured_at": evidence.captured_at,
                    "note": evidence.note,
                },
            )
            imported_evidence_count += 1

    return {
        "device_label": payload.device_label,
        "imported_cases": len(imported_case_ids),
        "imported_evidence": imported_evidence_count,
        "case_ids": imported_case_ids,
        "message": "端侧案件导入成功",
    }


@app.get(f"{settings.api_prefix}/device/sync-snapshot")
def device_sync_snapshot() -> dict[str, Any]:
    rows = db.list_device_cases()
    return {
        "summary": {
            "device_case_count": len(rows),
            "device_evidence_count": sum(int(row["evidence_count"] or 0) for row in rows),
            "device_labels": sorted({row["device_label"] for row in rows}),
        },
        "cases": [present_device_case_summary(row) for row in rows],
    }


@app.get(f"{settings.api_prefix}/device/cases/{{case_id}}")
def device_case_detail(case_id: str) -> dict[str, Any]:
    row = db.get_device_case(case_id)
    if not row:
        raise HTTPException(status_code=404, detail="端侧案件不存在")
    evidence_rows = db.get_device_evidence(case_id)
    return present_device_case_detail(row, evidence_rows)
