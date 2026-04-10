from __future__ import annotations

import json
from datetime import timedelta
from typing import Any

from fastapi import HTTPException, Request

from .. import db
from ..time_utils import parse_timestamp
from ..config import settings


def absolute_media_url(request: Request, relative_path: str) -> str:
    return str(request.base_url).rstrip("/") + relative_path


def source_payload(request: Request, source: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": source["name"],
        "title": source.get("title") or source["name"],
        "video_url": absolute_media_url(request, f"/media/data/{source['video_filename']}"),
        "preview_url": absolute_media_url(request, f"/media/data/{source['preview_filename']}") if source.get("preview_filename") else None,
        "fps": source.get("fps", 0),
        "frame_count": source.get("frame_count", 0),
        "duration_seconds": source.get("duration_seconds", 0),
        "sample_interval_seconds": source.get("sample_interval_seconds", settings.frame_interval_seconds),
        "case_clip_seconds": source.get("case_clip_seconds", settings.case_clip_seconds),
        "reference_mode": source.get("reference_mode", "网页大屏 + Android 移动端"),
        "location": source.get("location", settings.location_label),
        "lane_label": source.get("lane_label", "应急车道"),
    }


def event_time_window(row) -> tuple[str, str]:
    created_at = parse_timestamp(row["created_at"])
    first_seen = created_at + timedelta(seconds=float(row["start_second"]))
    last_seen = created_at + timedelta(seconds=float(row["end_second"]))
    return first_seen.isoformat(), last_seen.isoformat()


def event_summary(row) -> dict[str, Any]:
    first_seen, last_seen = event_time_window(row)
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


def event_detail(row, evidence_rows, request: Request, analysis_mode: str) -> dict[str, Any]:
    evidence = []
    timeline = []
    created_at = parse_timestamp(row["created_at"])
    for item in evidence_rows:
        parsed = json.loads(item["raw_result"])
        captured_at = created_at + timedelta(seconds=float(item["second"]))
        evidence.append(
            {
                "label": item["note"],
                "image_url": absolute_media_url(request, f"/media/evidence/{item['image_path']}"),
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

    summary = event_summary(row)
    return {
        **summary,
        "description": row["raw_reason"] or row["summary"],
        "vehicle_count": 1,
        "reported_at": row["report_submitted_at"],
        "report_content": row["report_content"],
        "evidence": evidence,
        "raw_analysis": {
            "source_mode": [analysis_mode],
            "timeline": timeline,
        },
    }


def case_summary(row, request: Request) -> dict[str, Any]:
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
        "clip_url": absolute_media_url(request, f"/media/clips/{row['clip_path']}") if row["clip_path"] else None,
        "report_number": report_number if row["status"] == db.CASE_STATUS_REPORTED else None,
    }


def case_detail(row, event_rows, evidence_rows, request: Request, analysis_mode: str) -> dict[str, Any]:
    evidence = []
    timeline = []
    created_at = parse_timestamp(row["created_at"])
    for item in evidence_rows:
        parsed = json.loads(item["raw_result"])
        evidence.append(
            {
                "label": item["note"],
                "image_url": absolute_media_url(request, f"/media/evidence/{item['image_path']}"),
                "captured_at": (created_at + timedelta(seconds=float(item["second"]))).isoformat(),
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
        **case_summary(row, request),
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
        "report_content": row["report_content"],
        "report_file_url": absolute_media_url(request, f"/media/reports/{row['report_path']}") if row["report_path"] else None,
        "reported_at": row["report_submitted_at"],
        "events": [event_summary(event_row) for event_row in event_rows],
        "evidence": evidence,
        "raw_analysis": {
            "timeline": timeline,
            "source_mode": [analysis_mode],
        },
    }


def run_summary(row) -> dict[str, Any]:
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


def pipeline_payload() -> list[dict[str, Any]]:
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


def derive_case_update(existing_row, payload) -> dict[str, Any]:
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


def device_case_summary(row) -> dict[str, Any]:
    return {
        "id": row["id"],
        "client_case_id": row["client_case_id"],
        "device_label": row["device_label"],
        "source_mode": row["source_mode"],
        "plate_number": row["plate_number"],
        "corrected_plate_number": row["corrected_plate_number"],
        "review_status": row["review_status"],
        "status": row["status"],
        "operator_note": row["operator_note"],
        "summary": row["summary"],
        "location": row["location"],
        "clip_uri": row["clip_uri"],
        "report_text": row["report_text"],
        "created_at": row["created_at"],
        "updated_at": row["updated_at"],
        "last_synced_at": row["last_synced_at"],
        "evidence_count": int(row["evidence_count"] or 0),
    }


def device_case_detail(row, evidence_rows) -> dict[str, Any]:
    return {
        **device_case_summary(row),
        "evidence": [
            {
                "id": item["id"],
                "label": item["label"],
                "image_uri": item["image_uri"],
                "captured_at": item["captured_at"],
                "note": item["note"],
            }
            for item in evidence_rows
        ],
    }
