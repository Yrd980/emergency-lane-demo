from __future__ import annotations

import json
import sqlite3
from contextlib import contextmanager
from datetime import datetime
from typing import Any, Iterator

from .config import settings

CASE_STATUS_PENDING_REVIEW = "待复核"
CASE_STATUS_READY_TO_REPORT = "待举报"
CASE_STATUS_REPORTED = "已举报"
CASE_REVIEW_PENDING = "待复核"
CASE_REVIEW_APPROVED = "复核通过"
CASE_REVIEW_REJECTED = "复核退回"


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
                source_video TEXT,
                status TEXT NOT NULL,
                mode TEXT NOT NULL,
                started_at TEXT,
                finished_at TEXT,
                progress_percent INTEGER DEFAULT 0,
                message TEXT DEFAULT '',
                error_message TEXT,
                events_created INTEGER DEFAULT 0,
                cases_created INTEGER DEFAULT 0
            );

            CREATE TABLE IF NOT EXISTS captures (
                id TEXT PRIMARY KEY,
                run_id TEXT NOT NULL,
                sample_second REAL NOT NULL,
                image_path TEXT NOT NULL,
                has_violation INTEGER NOT NULL,
                plate_number TEXT NOT NULL,
                confidence REAL NOT NULL,
                reason TEXT NOT NULL,
                created_at TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS cases (
                id TEXT PRIMARY KEY,
                run_id TEXT NOT NULL,
                plate_number TEXT NOT NULL,
                corrected_plate_number TEXT,
                review_status TEXT NOT NULL DEFAULT '待复核',
                operator_note TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL,
                location TEXT NOT NULL,
                confidence REAL NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                first_seen TEXT NOT NULL,
                last_seen TEXT NOT NULL,
                start_second REAL NOT NULL,
                end_second REAL NOT NULL,
                duration_seconds REAL NOT NULL,
                summary TEXT NOT NULL,
                clip_path TEXT,
                report_path TEXT,
                report_content TEXT,
                report_submitted_at TEXT
            );

            CREATE TABLE IF NOT EXISTS events (
                id TEXT PRIMARY KEY,
                run_id TEXT NOT NULL,
                case_id TEXT,
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
                case_id TEXT,
                image_path TEXT NOT NULL,
                second REAL NOT NULL,
                note TEXT NOT NULL,
                raw_result TEXT NOT NULL,
                FOREIGN KEY(event_id) REFERENCES events(id)
            );
            """
        )

        _ensure_columns(
            conn,
            "analysis_runs",
            {
                "source_video": "TEXT",
                "error_message": "TEXT",
                "events_created": "INTEGER DEFAULT 0",
                "cases_created": "INTEGER DEFAULT 0",
            },
        )
        _ensure_columns(
            conn,
            "cases",
            {
                "corrected_plate_number": "TEXT",
                "review_status": f"TEXT NOT NULL DEFAULT '{CASE_REVIEW_PENDING}'",
                "operator_note": "TEXT NOT NULL DEFAULT ''",
            },
        )
        _ensure_columns(conn, "events", {"case_id": "TEXT"})
        _ensure_columns(conn, "evidence", {"case_id": "TEXT"})


def _ensure_columns(conn: sqlite3.Connection, table: str, columns: dict[str, str]) -> None:
    existing = {row["name"] for row in conn.execute(f"PRAGMA table_info({table})").fetchall()}
    for column_name, column_type in columns.items():
        if column_name not in existing:
            conn.execute(f"ALTER TABLE {table} ADD COLUMN {column_name} {column_type}")


def reset_demo_data() -> None:
    with get_conn() as conn:
        conn.execute("DELETE FROM captures")
        conn.execute("DELETE FROM evidence")
        conn.execute("DELETE FROM events")
        conn.execute("DELETE FROM cases")
        conn.execute("DELETE FROM analysis_runs")


def create_run(run_id: str, source_name: str, source_video: str, mode: str) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            INSERT INTO analysis_runs (
                id, source_name, source_video, status, mode, started_at, progress_percent, message,
                events_created, cases_created
            )
            VALUES (?, ?, ?, 'running', ?, ?, 0, '等待视频分析', 0, 0)
            """,
            (run_id, source_name, source_video, mode, utc_now()),
        )


def update_run(run_id: str, **fields: Any) -> None:
    if not fields:
        return
    clauses = ", ".join(f"{key} = ?" for key in fields)
    values = list(fields.values()) + [run_id]
    with get_conn() as conn:
        conn.execute(f"UPDATE analysis_runs SET {clauses} WHERE id = ?", values)


def insert_capture(capture: dict[str, Any]) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            INSERT INTO captures (
                id, run_id, sample_second, image_path, has_violation,
                plate_number, confidence, reason, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                capture["id"],
                capture["run_id"],
                capture["sample_second"],
                capture["image_path"],
                1 if capture["has_violation"] else 0,
                capture["plate_number"],
                capture["confidence"],
                capture["reason"],
                capture["created_at"],
            ),
        )


def insert_case(case: dict[str, Any]) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            INSERT INTO cases (
                id, run_id, plate_number, corrected_plate_number, review_status, operator_note,
                status, location, confidence, created_at, updated_at, first_seen, last_seen,
                start_second, end_second, duration_seconds, summary, clip_path,
                report_path, report_content, report_submitted_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                case["id"],
                case["run_id"],
                case["plate_number"],
                case.get("corrected_plate_number"),
                case.get("review_status", CASE_REVIEW_PENDING),
                case.get("operator_note", ""),
                case["status"],
                case["location"],
                case["confidence"],
                case["created_at"],
                case["updated_at"],
                case["first_seen"],
                case["last_seen"],
                case["start_second"],
                case["end_second"],
                case["duration_seconds"],
                case["summary"],
                case.get("clip_path"),
                case.get("report_path"),
                case.get("report_content"),
                case.get("report_submitted_at"),
            ),
        )


def insert_event(event: dict[str, Any]) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            INSERT INTO events (
                id, run_id, case_id, plate_number, lane_label, status, severity, location,
                confidence, created_at, start_second, end_second, duration_seconds,
                report_submitted_at, summary, report_content, raw_reason
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                event["id"],
                event["run_id"],
                event.get("case_id"),
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
            INSERT INTO evidence (id, event_id, case_id, image_path, second, note, raw_result)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (
                evidence["id"],
                event_id,
                evidence.get("case_id"),
                evidence["image_path"],
                evidence["second"],
                evidence["note"],
                json.dumps(evidence["raw_result"], ensure_ascii=False),
            ),
        )


def list_runs() -> list[sqlite3.Row]:
    with get_conn() as conn:
        rows = conn.execute(
            """
            SELECT
                r.*,
                COUNT(DISTINCT e.id) AS event_count,
                COUNT(DISTINCT c.id) AS case_count
            FROM analysis_runs r
            LEFT JOIN events e ON e.run_id = r.id
            LEFT JOIN cases c ON c.run_id = r.id
            GROUP BY r.id
            ORDER BY r.started_at DESC, r.id DESC
            """
        ).fetchall()
    return rows


def get_run(run_id: str) -> sqlite3.Row | None:
    with get_conn() as conn:
        row = conn.execute(
            """
            SELECT
                r.*,
                COUNT(DISTINCT e.id) AS event_count,
                COUNT(DISTINCT c.id) AS case_count
            FROM analysis_runs r
            LEFT JOIN events e ON e.run_id = r.id
            LEFT JOIN cases c ON c.run_id = r.id
            WHERE r.id = ?
            GROUP BY r.id
            """,
            (run_id,),
        ).fetchone()
    return row



def list_events(
    *,
    status: str | None = None,
    plate: str | None = None,
    run_id: str | None = None,
    review_status: str | None = None,
) -> list[sqlite3.Row]:
    clauses: list[str] = []
    params: list[Any] = []
    if status:
        clauses.append("e.status = ?")
        params.append(status)
    if run_id:
        clauses.append("e.run_id = ?")
        params.append(run_id)
    if plate:
        clauses.append("(e.plate_number LIKE ? OR COALESCE(c.corrected_plate_number, '') LIKE ?)")
        params.extend([f"%{plate}%", f"%{plate}%"])
    if review_status:
        clauses.append("COALESCE(c.review_status, ?) = ?")
        params.extend([CASE_REVIEW_PENDING, review_status])
    where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
    with get_conn() as conn:
        rows = conn.execute(
            f"""
            SELECT
                e.*,
                r.source_name AS source_name,
                c.review_status AS review_status,
                c.corrected_plate_number AS corrected_plate_number,
                COUNT(ev.id) AS evidence_count
            FROM events e
            LEFT JOIN cases c ON c.id = e.case_id
            LEFT JOIN analysis_runs r ON r.id = e.run_id
            LEFT JOIN evidence ev ON ev.event_id = e.id
            {where}
            GROUP BY e.id
            ORDER BY e.created_at DESC, e.id DESC
            """,
            params,
        ).fetchall()
    return rows


def list_cases(
    *,
    status: str | None = None,
    plate: str | None = None,
    run_id: str | None = None,
    review_status: str | None = None,
) -> list[sqlite3.Row]:
    clauses: list[str] = []
    params: list[Any] = []
    if status:
        clauses.append("c.status = ?")
        params.append(status)
    if run_id:
        clauses.append("c.run_id = ?")
        params.append(run_id)
    if plate:
        clauses.append("(c.plate_number LIKE ? OR COALESCE(c.corrected_plate_number, '') LIKE ?)")
        params.extend([f"%{plate}%", f"%{plate}%"])
    if review_status:
        clauses.append("c.review_status = ?")
        params.append(review_status)
    where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
    with get_conn() as conn:
        rows = conn.execute(
            f"""
            SELECT
                c.*,
                r.source_name AS source_name,
                COUNT(DISTINCT e.id) AS event_count,
                COUNT(DISTINCT ev.id) AS evidence_count
            FROM cases c
            LEFT JOIN analysis_runs r ON r.id = c.run_id
            LEFT JOIN events e ON e.case_id = c.id
            LEFT JOIN evidence ev ON ev.case_id = c.id
            {where}
            GROUP BY c.id
            ORDER BY c.updated_at DESC, c.created_at DESC, c.id DESC
            """,
            params,
        ).fetchall()
    return rows


def get_event(event_id: str) -> sqlite3.Row | None:
    with get_conn() as conn:
        row = conn.execute(
            """
            SELECT
                e.*,
                r.source_name AS source_name,
                c.review_status AS review_status,
                c.corrected_plate_number AS corrected_plate_number,
                COUNT(ev.id) AS evidence_count
            FROM events e
            LEFT JOIN cases c ON c.id = e.case_id
            LEFT JOIN analysis_runs r ON r.id = e.run_id
            LEFT JOIN evidence ev ON ev.event_id = e.id
            WHERE e.id = ?
            GROUP BY e.id
            """,
            (event_id,),
        ).fetchone()
    return row


def get_case(case_id: str) -> sqlite3.Row | None:
    with get_conn() as conn:
        row = conn.execute(
            """
            SELECT
                c.*,
                r.source_name AS source_name,
                COUNT(DISTINCT e.id) AS event_count,
                COUNT(DISTINCT ev.id) AS evidence_count
            FROM cases c
            LEFT JOIN analysis_runs r ON r.id = c.run_id
            LEFT JOIN events e ON e.case_id = c.id
            LEFT JOIN evidence ev ON ev.case_id = c.id
            WHERE c.id = ?
            GROUP BY c.id
            """,
            (case_id,),
        ).fetchone()
    return row


def get_evidence(event_id: str) -> list[sqlite3.Row]:
    with get_conn() as conn:
        rows = conn.execute(
            "SELECT * FROM evidence WHERE event_id = ? ORDER BY second ASC",
            (event_id,),
        ).fetchall()
    return rows


def get_case_evidence(case_id: str) -> list[sqlite3.Row]:
    with get_conn() as conn:
        rows = conn.execute(
            """
            SELECT * FROM evidence
            WHERE case_id = ?
            ORDER BY second ASC, id ASC
            """,
            (case_id,),
        ).fetchall()
    return rows


def get_case_events(case_id: str) -> list[sqlite3.Row]:
    with get_conn() as conn:
        rows = conn.execute(
            """
            SELECT
                e.*,
                r.source_name AS source_name,
                c.review_status AS review_status,
                c.corrected_plate_number AS corrected_plate_number,
                COUNT(ev.id) AS evidence_count
            FROM events e
            LEFT JOIN cases c ON c.id = e.case_id
            LEFT JOIN analysis_runs r ON r.id = e.run_id
            LEFT JOIN evidence ev ON ev.event_id = e.id
            WHERE e.case_id = ?
            GROUP BY e.id
            ORDER BY e.start_second ASC
            """,
            (case_id,),
        ).fetchall()
    return rows


def update_case(case_id: str, **fields: Any) -> None:
    if not fields:
        return
    fields["updated_at"] = utc_now()
    clauses = ", ".join(f"{key} = ?" for key in fields)
    values = list(fields.values()) + [case_id]
    with get_conn() as conn:
        conn.execute(f"UPDATE cases SET {clauses} WHERE id = ?", values)


def update_case_status(case_id: str, status: str) -> None:
    update_case(case_id, status=status)
    with get_conn() as conn:
        conn.execute("UPDATE events SET status = ? WHERE case_id = ?", (status, case_id))


def report_event(event_id: str, content: str) -> None:
    with get_conn() as conn:
        conn.execute(
            """
            UPDATE events
            SET status = ?,
                report_submitted_at = ?,
                report_content = ?
            WHERE id = ?
            """,
            (CASE_STATUS_REPORTED, utc_now(), content, event_id),
        )


def report_case(case_id: str, content: str) -> None:
    reported_at = utc_now()
    with get_conn() as conn:
        conn.execute(
            """
            UPDATE cases
            SET status = ?,
                updated_at = ?,
                report_submitted_at = ?,
                report_content = ?
            WHERE id = ?
            """,
            (CASE_STATUS_REPORTED, reported_at, reported_at, content, case_id),
        )
        conn.execute(
            """
            UPDATE events
            SET status = ?,
                report_submitted_at = ?,
                report_content = ?
            WHERE case_id = ?
            """,
            (CASE_STATUS_REPORTED, reported_at, content, case_id),
        )


def latest_run() -> sqlite3.Row | None:
    with get_conn() as conn:
        row = conn.execute(
            "SELECT * FROM analysis_runs ORDER BY started_at DESC, id DESC LIMIT 1"
        ).fetchone()
    return row
