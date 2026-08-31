"""Deployment Tracker — Central Control Plane. ROADMAP §40, §44.

Phase 13: Deployment lifecycle + correlation trace.
"""

from __future__ import annotations

import enum
import uuid
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload


class DeploymentStatus(enum.StrEnum):
    PENDING = "PENDING"
    IN_PROGRESS = "IN_PROGRESS"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    ROLLED_BACK = "ROLLED_BACK"
    SUPERSEDED = "SUPERSEDED"


_ALLOWED: dict[DeploymentStatus, set[DeploymentStatus]] = {
    DeploymentStatus.PENDING: {
        DeploymentStatus.IN_PROGRESS,
        DeploymentStatus.FAILED,
        DeploymentStatus.SUPERSEDED,
    },
    DeploymentStatus.IN_PROGRESS: {
        DeploymentStatus.SUCCEEDED,
        DeploymentStatus.FAILED,
        DeploymentStatus.ROLLED_BACK,
    },
    DeploymentStatus.SUCCEEDED: {DeploymentStatus.ROLLED_BACK, DeploymentStatus.SUPERSEDED},
    DeploymentStatus.FAILED: {DeploymentStatus.PENDING, DeploymentStatus.SUPERSEDED},
    DeploymentStatus.ROLLED_BACK: set(),
    DeploymentStatus.SUPERSEDED: set(),
}


class DeploymentStateError(Exception):
    pass


class DeploymentNotFoundError(Exception):
    pass


class DeploymentRecord(BaseModel):
    deployment_id: str = Field(default_factory=lambda: f"dep-{uuid.uuid4().hex[:16]}")
    resource_id: str
    commit_sha: str | None = None
    branch: str = "main"
    version: str = "0.0.0"
    status: DeploymentStatus = DeploymentStatus.PENDING
    started_by: str = "system"
    correlation: dict[str, Any] = Field(default_factory=dict)
    started_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    finished_at: str | None = None
    log_url: str | None = None
    rollback_of: str | None = None
    notes: str = ""
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    updated_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())


class DeploymentTracker:
    """Phase 13 — Deployment Tracker. ROADMAP §40."""

    TABLE = "ecosystem_deployments"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.TABLE} (
                deployment_id TEXT PRIMARY KEY, resource_id TEXT NOT NULL, commit_sha TEXT,
                branch TEXT DEFAULT 'main', version TEXT DEFAULT '0.0.0',
                status TEXT NOT NULL DEFAULT 'PENDING', started_by TEXT DEFAULT 'system',
                correlation TEXT DEFAULT '{{}}',
                started_at TEXT NOT NULL, finished_at TEXT,
                log_url TEXT, rollback_of TEXT, notes TEXT DEFAULT '',
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_resource ON {self.TABLE}(resource_id, started_at)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_commit ON {self.TABLE}(commit_sha)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_status ON {self.TABLE}(status)"
            )
            conn.commit()

    def start(self, d: DeploymentRecord) -> DeploymentRecord:
        with get_conn() as conn:
            conn.execute(self._insert_sql(), self._row(d))
            # supersede any prior IN_PROGRESS for same resource
            conn.execute(
                f"UPDATE {self.TABLE} SET status=?, updated_at=? "
                "WHERE resource_id=? AND status=? AND deployment_id != ?",
                (
                    DeploymentStatus.SUPERSEDED,
                    datetime.now(UTC).isoformat(),
                    d.resource_id,
                    DeploymentStatus.IN_PROGRESS,
                    d.deployment_id,
                ),
            )
            conn.commit()
        return d

    def finish(
        self,
        deployment_id: str,
        status: DeploymentStatus,
        *,
        notes: str = "",
        log_url: str | None = None,
    ) -> DeploymentRecord:
        d = self.get(deployment_id)
        if d is None:
            raise DeploymentNotFoundError(deployment_id)
        if status not in _ALLOWED.get(d.status, set()):
            raise DeploymentStateError(f"Illegal transition {d.status} → {status}")
        now = datetime.now(UTC).isoformat()
        sets: dict[str, Any] = {"updated_at": now, "status": status, "finished_at": now}
        if notes:
            sets["notes"] = notes
        if log_url:
            sets["log_url"] = log_url
        sql = ", ".join(f"{k}=?" for k in sets)
        with get_conn() as conn:
            conn.execute(
                f"UPDATE {self.TABLE} SET {sql} WHERE deployment_id=?",
                list(sets.values()) + [deployment_id],
            )
            conn.commit()
        return self.get(deployment_id)  # type: ignore[return-value]

    def get(self, did: str) -> DeploymentRecord | None:
        with get_conn() as conn:
            r = conn.execute(f"SELECT * FROM {self.TABLE} WHERE deployment_id=?", (did,)).fetchone()
        return self._from(r) if r else None

    def list_by_resource(self, resource_id: str, *, limit: int = 50) -> list[DeploymentRecord]:
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.TABLE} WHERE resource_id=? ORDER BY started_at DESC LIMIT ?",
                (resource_id, limit),
            ).fetchall()
        return [self._from(r) for r in rows]

    def list_by_commit(self, commit_sha: str, *, limit: int = 50) -> list[DeploymentRecord]:
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.TABLE} WHERE commit_sha=? ORDER BY started_at DESC LIMIT ?",
                (commit_sha, limit),
            ).fetchall()
        return [self._from(r) for r in rows]

    def trace(self, correlation_key: str, value: str) -> list[DeploymentRecord]:
        """Find deployments by correlation key/value (e.g. task_id, request_id)."""
        with get_conn() as conn:
            rows = conn.execute(
                f"SELECT * FROM {self.TABLE} WHERE json_extract(correlation, ?) = ? ORDER BY started_at DESC",
                (f"$.{correlation_key}", value),
            ).fetchall()
        return [self._from(r) for r in rows]

    def _insert_sql(self) -> str:
        cols = (
            "deployment_id,resource_id,commit_sha,branch,version,status,started_by,correlation,"
            "started_at,finished_at,log_url,rollback_of,notes,created_at,updated_at"
        )
        return f"INSERT INTO {self.TABLE} ({cols}) VALUES ({','.join(['?'] * 15)})"

    def _row(self, d: DeploymentRecord) -> tuple:
        return (
            d.deployment_id,
            d.resource_id,
            d.commit_sha,
            d.branch,
            d.version,
            d.status,
            d.started_by,
            jdump(d.correlation),
            d.started_at,
            d.finished_at,
            d.log_url,
            d.rollback_of,
            d.notes,
            d.created_at,
            d.updated_at,
        )

    def _from(self, r: Any) -> DeploymentRecord:
        return DeploymentRecord(
            deployment_id=r["deployment_id"],
            resource_id=r["resource_id"],
            commit_sha=r["commit_sha"],
            branch=r["branch"],
            version=r["version"],
            status=DeploymentStatus(r["status"]),
            started_by=r["started_by"],
            correlation=jload(r["correlation"], {}),
            started_at=r["started_at"],
            finished_at=r["finished_at"],
            log_url=r["log_url"],
            rollback_of=r["rollback_of"],
            notes=r["notes"],
            created_at=r["created_at"],
            updated_at=r["updated_at"],
        )


_tracker: DeploymentTracker | None = None


def get_deployment_tracker() -> DeploymentTracker:
    global _tracker
    if _tracker is None:
        _tracker = DeploymentTracker()
    return _tracker


__all__ = [
    "DeploymentStatus",
    "DeploymentRecord",
    "DeploymentTracker",
    "get_deployment_tracker",
    "DeploymentStateError",
    "DeploymentNotFoundError",
]
