from __future__ import annotations

from datetime import UTC, datetime


def utc_now() -> str:
    return datetime.now(UTC).isoformat(timespec="seconds")


def utc_now_compact() -> str:
    return datetime.now(UTC).strftime("%Y%m%d%H%M%S")


def parse_timestamp(value: str) -> datetime:
    return datetime.fromisoformat(value)
