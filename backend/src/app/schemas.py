from __future__ import annotations

from typing import Literal

from pydantic import BaseModel


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
    mode: Literal["zhipu", "mock"]
    started_at: str | None = None
    finished_at: str | None = None
    progress_percent: int = 0
    message: str = ""
    error_message: str | None = None
    events_created: int = 0
    cases_created: int = 0
    event_count: int = 0
    case_count: int = 0
