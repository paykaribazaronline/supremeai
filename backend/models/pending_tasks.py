import json
import sqlite3
import uuid
from datetime import datetime
from datetime import timezone
from enum import Enum
from pathlib import Path

from pydantic import BaseModel


class TaskType(str, Enum):
    CODE_PUSH = "CODE_PUSH"
    NEW_SITE_VISIT = "NEW_SITE_VISIT"
    SKILL_GENERATION = "SKILL_GENERATION"
    VPN_SWITCH = "VPN_SWITCH"
    AUTO_EVOLUTION_PATCH = "AUTO_EVOLUTION_PATCH"


class TaskStatus(str, Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    EXECUTED = "EXECUTED"
    CANCELLED = "CANCELLED"


class PendingTask(BaseModel):
    task_id: str
    task_type: TaskType
    payload: dict
    status: TaskStatus
    created_at: str
    resolved_by: str | None = None
    resolved_at: str | None = None
    reason: str | None = None


DB_PATH = Path(__file__).resolve().parent.parent / "data" / "pending_tasks.db"


def _get_conn():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = _get_conn()
    cursor = conn.cursor()
    cursor.execute(
        """
        CREATE TABLE IF NOT EXISTS pending_tasks (
            task_id TEXT PRIMARY KEY,
            task_type TEXT NOT NULL,
            payload TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at TEXT NOT NULL,
            resolved_by TEXT,
            resolved_at TEXT,
            reason TEXT
        )
        """
    )
    conn.commit()
    conn.close()


def create_pending_task(
    task_type: TaskType, payload: dict, created_by: str = "system"
) -> PendingTask:
    task = PendingTask(
        task_id=str(uuid.uuid4()),
        task_type=task_type,
        payload=payload,
        status=TaskStatus.PENDING,
        created_at=datetime.now(timezone.utc).isoformat(),
    )
    conn = _get_conn()
    cursor = conn.cursor()
    cursor.execute(
        """
        INSERT INTO pending_tasks (task_id, task_type, payload, status, created_at, resolved_by, resolved_at, reason)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            task.task_id,
            task.task_type,
            json.dumps(task.payload),
            task.status,
            task.created_at,
            task.resolved_by,
            task.resolved_at,
            task.reason,
        ),
    )
    conn.commit()
    conn.close()
    return task


def list_pending() -> list[PendingTask]:
    conn = _get_conn()
    cursor = conn.cursor()
    cursor.execute(
        "SELECT * FROM pending_tasks WHERE status = ?", (TaskStatus.PENDING,)
    )
    rows = cursor.fetchall()
    conn.close()
    return [row_to_task(row) for row in rows]


def update_task_status(
    task_id: str, status: TaskStatus, resolved_by: str, reason: str | None = None
) -> PendingTask | None:
    conn = _get_conn()
    cursor = conn.cursor()
    resolved_at = (
        datetime.now(timezone.utc).isoformat() if status != TaskStatus.PENDING else None
    )
    cursor.execute(
        """
        UPDATE pending_tasks SET status = ?, resolved_by = ?, resolved_at = ?, reason = ?
        WHERE task_id = ?
        """,
        (status, resolved_by, resolved_at, reason, task_id),
    )
    conn.commit()
    conn.close()
    cursor.execute("SELECT * FROM pending_tasks WHERE task_id = ?", (task_id,))
    row = cursor.fetchone()
    conn.close()
    return row_to_task(row) if row else None


def row_to_task(row: sqlite3.Row) -> PendingTask:
    return PendingTask(
        task_id=row["task_id"],
        task_type=TaskType(row["task_type"]),
        payload=json.loads(row["payload"]),
        status=TaskStatus(row["status"]),
        created_at=row["created_at"],
        resolved_by=row["resolved_by"],
        resolved_at=row["resolved_at"],
        reason=row["reason"],
    )
