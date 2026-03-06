from __future__ import annotations

import json
import sqlite3
from contextlib import contextmanager
from datetime import datetime
from typing import Any, Iterator

from .config import settings


def utc_now() -> str:
    return datetime.utcnow().isoformat(timespec="seconds")


def _connect() -> sqlite3.Connection:
    settings.ensure_dirs()
    conn = sqlite3.connect(settings.db_path)
    conn.row_factory = sqlite3.Row
    return conn


@contextmanager
def get_conn() -> Iterator[sqlite3.Connection]:
    conn = _connect()
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


def init_db() -> None:
    with get_conn() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS analysis_runs (
                id TEXT PRIMARY KEY,
                source_name TEXT NOT NULL,
                status TEXT NOT NULL,
                mode TEXT NOT NULL,
                started_at TEXT,
                finished_at TEXT,
                progress_percent INTEGER DEFAULT 0,
                message TEXT DEFAULT ''
            );

            CREATE TABLE IF NOT EXISTS events (
                id TEXT PRIMARY KEY,
                run_id TEXT NOT NULL,
                plate_number TEXT NOT NULL,
                lane_label TEXT NOT NULL,
                status TEXT NOT NULL,
                severity TEXT NOT NULL,
                location TEXT NOT NULL,
                confidence REAL NOT NULL,
                created_at TEXT NOT NULL,
                start_second REAL NOT NULL,
                end_second REAL NOT NULL,
                duration_seconds REAL NOT NULL,
                report_submitted_at TEXT,
                summary TEXT NOT NULL,
                report_content TEXT,
                raw_reason TEXT,
                FOREIGN KEY(run_id) REFERENCES analysis_runs(id)
            );

            CREATE TABLE IF NOT EXISTS evidence (
                id TEXT PRIMARY KEY,
                event_id TEXT NOT NULL,
                image_path TEXT NOT NULL,
                second REAL NOT NULL,
                note TEXT NOT NULL,
                raw_result TEXT NOT NULL,
                FOREIGN KEY(event_id) REFERENCES events(id)
            );
            """
        )


def reset_demo_data() -> None:
    with get_conn() as conn:
        conn.execute("DELETE FROM evidence")
        conn.execute("DELETE FROM events")
        conn.execute("DELETE FROM analysis_runs")


def create_run(run_id: str, source_name: str, mode: str) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            INSERT INTO analysis_runs (id, source_name, status, mode, started_at, progress_percent, message)
            VALUES (?, ?, 'running', ?, ?, 0, '等待视频分析')
            """,
            (run_id, source_name, mode, utc_now()),
        )


def update_run(run_id: str, **fields: Any) -> None:
    if not fields:
        return
    clauses = ", ".join(f"{key} = ?" for key in fields)
    values = list(fields.values()) + [run_id]
    with get_conn() as conn:
        conn.execute(f"UPDATE analysis_runs SET {clauses} WHERE id = ?", values)


def insert_event(event: dict[str, Any]) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            INSERT INTO events (
                id, run_id, plate_number, lane_label, status, severity, location,
                confidence, created_at, start_second, end_second, duration_seconds,
                report_submitted_at, summary, report_content, raw_reason
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                event["id"],
                event["run_id"],
                event["plate_number"],
                event["lane_label"],
                event["status"],
                event["severity"],
                event["location"],
                event["confidence"],
                event["created_at"],
                event["start_second"],
                event["end_second"],
                event["duration_seconds"],
                event.get("report_submitted_at"),
                event["summary"],
                event.get("report_content"),
                event.get("raw_reason"),
            ),
        )


def insert_evidence(event_id: str, evidence: dict[str, Any]) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            INSERT INTO evidence (id, event_id, image_path, second, note, raw_result)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (
                evidence["id"],
                event_id,
                evidence["image_path"],
                evidence["second"],
                evidence["note"],
                json.dumps(evidence["raw_result"], ensure_ascii=False),
            ),
        )


def list_events() -> list[sqlite3.Row]:
    with get_conn() as conn:
        rows = conn.execute(
            """
            SELECT
                e.*,
                COUNT(ev.id) AS evidence_count
            FROM events e
            LEFT JOIN evidence ev ON ev.event_id = e.id
            GROUP BY e.id
            ORDER BY e.created_at DESC
            """
        ).fetchall()
    return rows


def get_event(event_id: str) -> sqlite3.Row | None:
    with get_conn() as conn:
        row = conn.execute(
            """
            SELECT
                e.*,
                COUNT(ev.id) AS evidence_count
            FROM events e
            LEFT JOIN evidence ev ON ev.event_id = e.id
            WHERE e.id = ?
            GROUP BY e.id
            """,
            (event_id,),
        ).fetchone()
    return row


def get_evidence(event_id: str) -> list[sqlite3.Row]:
    with get_conn() as conn:
        rows = conn.execute(
            "SELECT * FROM evidence WHERE event_id = ? ORDER BY second ASC",
            (event_id,),
        ).fetchall()
    return rows


def report_event(event_id: str, content: str) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            UPDATE events
            SET status = '已举报',
                report_submitted_at = ?,
                report_content = ?
            WHERE id = ?
            """,
            (utc_now(), content, event_id),
        )


def latest_run() -> sqlite3.Row | None:
    with get_conn() as conn:
        row = conn.execute(
            "SELECT * FROM analysis_runs ORDER BY started_at DESC LIMIT 1"
        ).fetchone()
    return row
