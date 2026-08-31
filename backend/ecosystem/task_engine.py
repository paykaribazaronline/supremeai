"""Task Engine — User Task Engine. ROADMAP §22-§24.

Phase 5: User Task Engine — submit, plan, execute, verify, deliver.
State machine + retry/time-limit enforcement.
"""

from __future__ import annotations

import enum
import uuid
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload


class TaskState(enum.StrEnum):
    RECEIVED = "RECEIVED"
    UNDERSTANDING = "UNDERSTANDING"
    PLANNING = "PLANNING"
    CAPABILITY_CHECK = "CAPABILITY_CHECK"
    RESOURCE_CHECK = "RESOURCE_CHECK"
    PREPARING = "PREPARING"
    EXECUTING = "EXECUTING"
    VERIFYING = "VERIFYING"
    REPAIRING = "REPAIRING"
    DELIVERING = "DELIVERING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    ESCALATED = "ESCALATED"
    CANCELLED = "CANCELLED"


class TaskOwner(enum.StrEnum):
    USER = "USER"
    ADMIN = "ADMIN"
    SYSTEM = "SYSTEM"


_ALLOWED: dict[TaskState, set[TaskState]] = {
    TaskState.RECEIVED: {TaskState.UNDERSTANDING, TaskState.CANCELLED, TaskState.FAILED},
    TaskState.UNDERSTANDING: {
        TaskState.PLANNING,
        TaskState.ESCALATED,
        TaskState.FAILED,
        TaskState.CANCELLED,
    },
    TaskState.PLANNING: {
        TaskState.CAPABILITY_CHECK,
        TaskState.ESCALATED,
        TaskState.FAILED,
        TaskState.CANCELLED,
    },
    TaskState.CAPABILITY_CHECK: {
        TaskState.RESOURCE_CHECK,
        TaskState.ESCALATED,
        TaskState.FAILED,
        TaskState.CANCELLED,
    },
    TaskState.RESOURCE_CHECK: {
        TaskState.PREPARING,
        TaskState.ESCALATED,
        TaskState.FAILED,
        TaskState.CANCELLED,
    },
    TaskState.PREPARING: {
        TaskState.EXECUTING,
        TaskState.ESCALATED,
        TaskState.FAILED,
        TaskState.CANCELLED,
    },
    TaskState.EXECUTING: {
        TaskState.VERIFYING,
        TaskState.FAILED,
        TaskState.ESCALATED,
        TaskState.CANCELLED,
    },
    TaskState.VERIFYING: {
        TaskState.DELIVERING,
        TaskState.REPAIRING,
        TaskState.FAILED,
        TaskState.ESCALATED,
        TaskState.CANCELLED,
    },
    TaskState.REPAIRING: {
        TaskState.EXECUTING,
        TaskState.FAILED,
        TaskState.ESCALATED,
        TaskState.CANCELLED,
    },
    TaskState.DELIVERING: {
        TaskState.COMPLETED,
        TaskState.FAILED,
        TaskState.ESCALATED,
        TaskState.CANCELLED,
    },
    TaskState.COMPLETED: set(),
    TaskState.FAILED: set(),
    TaskState.ESCALATED: set(),
    TaskState.CANCELLED: set(),
}


class TaskStateError(Exception):
    pass


class TaskNotFoundError(Exception):
    pass


class TaskRetryExceeded(Exception):
    pass


class TaskTimeoutError(Exception):
    pass


class TaskRecord(BaseModel):
    task_id: str = Field(default_factory=lambda: f"task-{uuid.uuid4().hex[:16]}")
    goal: str
    owner: TaskOwner = TaskOwner.USER
    scope: dict[str, Any] = Field(default_factory=dict)
    state: TaskState = TaskState.RECEIVED
    plan: dict[str, Any] = Field(default_factory=dict)
    capability_requirements: list[dict[str, Any]] = Field(default_factory=list)
    resource_id: str | None = None
    capability_id: str | None = None
    artifacts: list[dict[str, Any]] = Field(default_factory=list)
    result: dict[str, Any] = Field(default_factory=dict)
    success_criteria: dict[str, Any] = Field(default_factory=dict)
    verification_result: dict[str, Any] = Field(default_factory=dict)
    retry_count: int = 0
    retry_limit: int = 3
    time_limit_seconds: int | None = None
    risk_level: str = "LOW"
    correlation: dict[str, Any] = Field(default_factory=dict)
    created_by: str = "system"
    tenant_id: str | None = None
    audit_id: str | None = None
    error: str | None = None
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    started_at: str | None = None
    completed_at: str | None = None


class TaskEngine:
    """Phase 5 — User Task Engine. ROADMAP §22."""

    TABLE = "ecosystem_tasks"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.TABLE} (
                task_id TEXT PRIMARY KEY, goal TEXT NOT NULL, owner TEXT NOT NULL DEFAULT 'USER',
                scope TEXT DEFAULT '{{}}', state TEXT NOT NULL DEFAULT 'RECEIVED',
                plan TEXT DEFAULT '{{}}', capability_requirements TEXT DEFAULT '[]',
                resource_id TEXT, capability_id TEXT,
                artifacts TEXT DEFAULT '[]', result TEXT DEFAULT '{{}}',
                success_criteria TEXT DEFAULT '{{}}', verification_result TEXT DEFAULT '{{}}',
                retry_count INTEGER DEFAULT 0, retry_limit INTEGER DEFAULT 3,
                time_limit_seconds INTEGER, risk_level TEXT DEFAULT 'LOW',
                correlation TEXT DEFAULT '{{}}', created_by TEXT DEFAULT 'system', tenant_id TEXT,
                audit_id TEXT, error TEXT,
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL,
                started_at TEXT, completed_at TEXT)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_state ON {self.TABLE}(state)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_owner ON {self.TABLE}(owner)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_tenant ON {self.TABLE}(tenant_id)"
            )
            conn.commit()

    def submit(self, t: TaskRecord) -> TaskRecord:
        now = datetime.now(UTC).isoformat()
        t.created_at = t.created_at or now
        t.updated_at = now
        with get_conn() as conn:
            conn.execute(self._insert_sql(), self._row(t))
            conn.commit()
        return t

    def get(self, tid: str) -> TaskRecord | None:
        with get_conn() as conn:
            r = conn.execute(f"SELECT * FROM {self.TABLE} WHERE task_id=?", (tid,)).fetchone()
        return self._from(r) if r else None

    def list(
        self,
        *,
        state: TaskState | None = None,
        owner: TaskOwner | None = None,
        tenant_id: str | None = None,
        limit: int = 200,
    ) -> list[TaskRecord]:
        clauses, params = [], []
        if state:
            clauses.append("state=?")
            params.append(state)
        if owner:
            clauses.append("owner=?")
            params.append(owner)
        if tenant_id:
            clauses.append("tenant_id=?")
            params.append(tenant_id)
        where = ("WHERE " + " AND ".join(clauses)) if clauses else ""
        params.append(limit)
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.TABLE} {where} ORDER BY created_at DESC LIMIT ?", params
            ).fetchall()
        return [self._from(r) for r in rows]

    def transition(
        self, tid: str, to: TaskState, *, actor: str = "system", error: str | None = None
    ) -> TaskRecord:
        t = self.get(tid)
        if t is None:
            raise TaskNotFoundError(f"Task {tid} not found")
        if to not in _ALLOWED.get(t.state, set()):
            raise TaskStateError(f"Illegal transition {t.state} → {to}")
        # time-limit enforcement (§23)
        if (
            t.time_limit_seconds
            and t.started_at
            and to
            not in {TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELLED, TaskState.ESCALATED}
        ):
            started = datetime.fromisoformat(t.started_at)
            if (datetime.now(UTC) - started).total_seconds() > t.time_limit_seconds:
                raise TaskTimeoutError(f"Task {tid} exceeded time limit {t.time_limit_seconds}s")
        now = datetime.now(UTC).isoformat()
        sets: dict[str, Any] = {"updated_at": now, "state": to}
        if to == TaskState.EXECUTING and t.started_at is None:
            sets["started_at"] = now
        if to in {TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELLED, TaskState.ESCALATED}:
            sets["completed_at"] = now
        # retry-limit enforcement (§23)
        if to == TaskState.REPAIRING:
            new_count = t.retry_count + 1
            if new_count > t.retry_limit:
                raise TaskRetryExceeded(f"Retry limit {t.retry_limit} exceeded for task {tid}")
            sets["retry_count"] = new_count
        if error:
            sets["error"] = error
        sql = ", ".join(f"{k}=?" for k in sets)
        with get_conn() as conn:
            conn.execute(
                f"UPDATE {self.TABLE} SET {sql} WHERE task_id=?", list(sets.values()) + [tid]
            )
            conn.commit()
        return self.get(tid)  # type: ignore[return-value]

    def deliver(
        self,
        tid: str,
        *,
        result: dict[str, Any] | None = None,
        artifacts: list[dict[str, Any]] | None = None,
        actor: str = "system",
    ) -> TaskRecord:
        t = self.get(tid)
        if t is None:
            raise TaskNotFoundError(tid)
        if t.state != TaskState.DELIVERING:
            raise TaskStateError(f"deliver requires DELIVERING state, got {t.state}")
        now = datetime.now(UTC).isoformat()
        sets: dict[str, Any] = {
            "updated_at": now,
            "state": TaskState.COMPLETED,
            "completed_at": now,
        }
        if result is not None:
            sets["result"] = jdump(result)
        if artifacts is not None:
            sets["artifacts"] = jdump(artifacts)
        sql = ", ".join(f"{k}=?" for k in sets)
        with get_conn() as conn:
            conn.execute(
                f"UPDATE {self.TABLE} SET {sql} WHERE task_id=?", list(sets.values()) + [tid]
            )
            conn.commit()
        return self.get(tid)  # type: ignore[return-value]

    def cancel(self, tid: str, *, reason: str | None = None, actor: str = "system") -> TaskRecord:
        return self.transition(tid, TaskState.CANCELLED, actor=actor, error=reason)

    def _insert_sql(self) -> str:
        cols = (
            "task_id,goal,owner,scope,state,plan,capability_requirements,resource_id,capability_id,"
            "artifacts,result,success_criteria,verification_result,retry_count,retry_limit,"
            "time_limit_seconds,risk_level,correlation,created_by,tenant_id,audit_id,error,"
            "created_at,updated_at,started_at,completed_at"
        )
        return f"INSERT INTO {self.TABLE} ({cols}) VALUES ({','.join(['?'] * 26)})"

    def _row(self, t: TaskRecord) -> tuple:
        return (
            t.task_id,
            t.goal,
            t.owner,
            jdump(t.scope),
            t.state,
            jdump(t.plan),
            jdump(t.capability_requirements),
            t.resource_id,
            t.capability_id,
            jdump(t.artifacts),
            jdump(t.result),
            jdump(t.success_criteria),
            jdump(t.verification_result),
            t.retry_count,
            t.retry_limit,
            t.time_limit_seconds,
            t.risk_level,
            jdump(t.correlation),
            t.created_by,
            t.tenant_id,
            t.audit_id,
            t.error,
            t.created_at,
            t.updated_at,
            t.started_at,
            t.completed_at,
        )

    def _from(self, r: Any) -> TaskRecord:
        return TaskRecord(
            task_id=r["task_id"],
            goal=r["goal"],
            owner=TaskOwner(r["owner"]),
            scope=jload(r["scope"], {}),
            state=TaskState(r["state"]),
            plan=jload(r["plan"], {}),
            capability_requirements=jload(r["capability_requirements"], []),
            resource_id=r["resource_id"],
            capability_id=r["capability_id"],
            artifacts=jload(r["artifacts"], []),
            result=jload(r["result"], {}),
            success_criteria=jload(r["success_criteria"], {}),
            verification_result=jload(r["verification_result"], {}),
            retry_count=int(r["retry_count"] or 0),
            retry_limit=int(r["retry_limit"] or 3),
            time_limit_seconds=r["time_limit_seconds"],
            risk_level=r["risk_level"],
            correlation=jload(r["correlation"], {}),
            created_by=r["created_by"],
            tenant_id=r["tenant_id"],
            audit_id=r["audit_id"],
            error=r["error"],
            created_at=r["created_at"],
            updated_at=r["updated_at"],
            started_at=r["started_at"],
            completed_at=r["completed_at"],
        )


_engine: TaskEngine | None = None


def get_task_engine() -> TaskEngine:
    global _engine
    if _engine is None:
        _engine = TaskEngine()
    return _engine


__all__ = [
    "TaskState",
    "TaskOwner",
    "TaskRecord",
    "TaskEngine",
    "get_task_engine",
    "TaskStateError",
    "TaskNotFoundError",
    "TaskRetryExceeded",
    "TaskTimeoutError",
]
