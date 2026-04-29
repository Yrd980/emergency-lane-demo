from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class ReportRequest(BaseModel):
    reporter_name: str = "演示用户"
    reporter_phone: str = "13800000000"
    note: str = "系统辅助生成并提交至模拟平台。"


class AnalyzeDemoRequest(BaseModel):
    source_name: str | None = None


class CaseUpdateRequest(BaseModel):
    corrected_plate_number: str | None = None
    operator_note: str | None = None
    review_status: Literal["待复核", "复核通过", "复核退回"] | None = None
    status: Literal["待复核", "待举报"] | None = None


class RunStatusResponse(BaseModel):
    id: str
    source_name: str
    status: Literal["running", "done", "failed"]
    mode: Literal["mock"]
    started_at: str | None = None
    finished_at: str | None = None
    progress_percent: int = 0
    message: str = ""
    error_message: str | None = None
    events_created: int = 0
    cases_created: int = 0
    event_count: int = 0
    case_count: int = 0


class DeviceEvidenceImport(BaseModel):
    label: str
    image_uri: str
    captured_at: str | None = None
    note: str = ""


class DeviceCaseImport(BaseModel):
    client_case_id: str = Field(min_length=1)
    plate_number: str = Field(min_length=1)
    corrected_plate_number: str | None = None
    review_status: Literal["待复核", "复核通过", "复核退回"] = "待复核"
    status: Literal["待复核", "待举报", "已举报"] = "待复核"
    operator_note: str = ""
    summary: str = ""
    location: str = ""
    clip_uri: str | None = None
    report_text: str | None = None
    created_at: str | None = None
    updated_at: str | None = None
    evidence: list[DeviceEvidenceImport] = []


class DeviceCaseImportRequest(BaseModel):
    device_label: str = "android-local-mode"
    items: list[DeviceCaseImport]
