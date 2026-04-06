from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class TaskStatus(BaseModel):
    id: str
    source_name: str
    status: Literal["idle", "running", "done", "failed"]
    mode: Literal["zhipu", "mock"]
    started_at: datetime | None
    finished_at: datetime | None
    progress_percent: int = 0
    message: str = ""


class OverviewStats(BaseModel):
    total_events: int = 0
    pending_events: int = 0
    reported_events: int = 0
    average_confidence: float = 0.0
    highest_risk_plate: str | None = None


class TrendPoint(BaseModel):
    label: str
    value: int


class RankingItem(BaseModel):
    label: str
    value: int


class EvidenceItem(BaseModel):
    id: str
    second: float
    image_url: str
    note: str


class EventListItem(BaseModel):
    id: str
    case_id: str | None = None
    plate_number: str
    lane_label: str
    status: str
    severity: str
    location: str
    confidence: float
    created_at: datetime
    duration_seconds: float
    report_submitted_at: datetime | None = None
    summary: str
    evidence_count: int = 0


class EventDetail(EventListItem):
    start_second: float
    end_second: float
    report_content: str | None = None
    raw_reason: str | None = None
    evidence: list[EvidenceItem] = Field(default_factory=list)


class ReportRequest(BaseModel):
    reporter_name: str = "演示用户"
    reporter_phone: str = "13800000000"
    note: str = "系统辅助生成并提交至模拟平台。"


class OverviewResponse(BaseModel):
    project_name: str
    mode: Literal["zhipu", "mock"]
    stats: OverviewStats
    current_task: TaskStatus | None = None
    hourly_trend: list[TrendPoint] = Field(default_factory=list)
    top_plates: list[RankingItem] = Field(default_factory=list)
    recent_alerts: list[EventListItem] = Field(default_factory=list)
    demo_video_url: str
