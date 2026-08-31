"""Health Model — Central Control Plane. ROADMAP §41-§42.

Phase 13: Unified health aggregation across providers.
Tracks memory, CPU, disk, latency, error_rate, custom_metrics.
"""

from __future__ import annotations

import enum
import uuid
from datetime import UTC, datetime, timedelta
from typing import Any

from pydantic import BaseModel, Field

from ecosystem._store import get_conn, jdump, jload


class HealthStatus(enum.StrEnum):
    HEALTHY = "HEALTHY"
    DEGRADED = "DEGRADED"
    WARNING = "WARNING"
    CRITICAL = "CRITICAL"
    UNKNOWN = "UNKNOWN"
    MAINTENANCE = "MAINTENANCE"


class MemoryInfo(BaseModel):
    current_mb: float = 0.0
    peak_mb: float = 0.0
    limit_mb: float = 0.0
    percent: float = 0.0
    trend: str = "stable"  # increasing | decreasing | stable


class UnifiedHealth(BaseModel):
    record_id: str = Field(default_factory=lambda: f"hlth-{uuid.uuid4().hex[:16]}")
    source: str  # e.g. "provider:resource_id"
    status: HealthStatus = HealthStatus.UNKNOWN
    memory: MemoryInfo = Field(default_factory=MemoryInfo)
    cpu_percent: float = 0.0
    disk_percent: float = 0.0
    latency_ms: float = 0.0
    error_rate: float = 0.0
    custom_metrics: dict[str, Any] = Field(default_factory=dict)
    timestamp: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())
    created_at: str = Field(default_factory=lambda: datetime.now(UTC).isoformat())


def _status_rank(s: HealthStatus) -> int:
    return {
        HealthStatus.HEALTHY: 0,
        HealthStatus.MAINTENANCE: 1,
        HealthStatus.UNKNOWN: 2,
        HealthStatus.WARNING: 3,
        HealthStatus.DEGRADED: 4,
        HealthStatus.CRITICAL: 5,
    }.get(s, 2)


class HealthAggregator:
    """Phase 13 — Central Control Plane. ROADMAP §41."""

    TABLE = "ecosystem_health"

    def __init__(self) -> None:
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.TABLE} (
                record_id TEXT PRIMARY KEY, source TEXT NOT NULL, status TEXT NOT NULL,
                memory TEXT DEFAULT '{{}}', cpu_percent REAL DEFAULT 0,
                disk_percent REAL DEFAULT 0, latency_ms REAL DEFAULT 0,
                error_rate REAL DEFAULT 0, custom_metrics TEXT DEFAULT '{{}}',
                timestamp TEXT NOT NULL, created_at TEXT NOT NULL)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_source ON {self.TABLE}(source, created_at)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_status ON {self.TABLE}(status)"
            )
            conn.commit()

    def record(self, h: UnifiedHealth) -> UnifiedHealth:
        with get_conn() as conn:
            conn.execute(self._insert_sql(), self._row(h))
            conn.commit()
        return h

    def latest(self, source: str) -> UnifiedHealth | None:
        with get_conn() as conn:
            r = conn.execute(
                f"SELECT * FROM {self.TABLE} WHERE source=? ORDER BY created_at DESC LIMIT 1",
                (source,),
            ).fetchone()
        return self._from(r) if r else None

    def all_latest(self, *, since_minutes: int = 60) -> list[UnifiedHealth]:
        cutoff = (datetime.now(UTC) - timedelta(minutes=since_minutes)).isoformat()
        with get_conn() as conn:
            rows = conn.execute(
                f"""SELECT * FROM {self.TABLE} t1
                WHERE created_at >= ? AND created_at = (
                    SELECT MAX(created_at) FROM {self.TABLE} t2 WHERE t2.source = t1.source
                ) ORDER BY source ASC""",
                (cutoff,),
            ).fetchall()
        return [self._from(r) for r in rows]

    def composite_status(self) -> HealthStatus:
        latest = self.all_latest(since_minutes=10)
        if not latest:
            return HealthStatus.UNKNOWN
        worst = max(latest, key=lambda h: _status_rank(h.status))
        return worst.status

    def top_memory_consumers(self, *, limit: int = 10) -> list[UnifiedHealth]:
        latest = self.all_latest(since_minutes=30)
        return sorted(latest, key=lambda h: h.memory.current_mb, reverse=True)[:limit]

    def detect_memory_regression(
        self, *, hours: int = 24, threshold_percent: float = 20.0
    ) -> list[dict[str, Any]]:
        cutoff = (datetime.now(UTC) - timedelta(hours=hours)).isoformat()
        out: list[dict[str, Any]] = []
        with get_conn() as conn:
            sources = [
                r["source"]
                for r in conn.execute(
                    f"SELECT DISTINCT source FROM {self.TABLE} WHERE created_at >= ?", (cutoff,)
                ).fetchall()
            ]
            for s in sources:
                rows = conn.execute(
                    f"SELECT * FROM {self.TABLE} WHERE source=? AND created_at >= ? ORDER BY created_at ASC",
                    (s, cutoff),
                ).fetchall()
                if len(rows) < 2:
                    continue
                first = self._from(rows[0])
                last = self._from(rows[-1])
                delta = last.memory.current_mb - first.memory.current_mb
                pct = (
                    (delta / first.memory.current_mb * 100) if first.memory.current_mb > 0 else 0.0
                )
                if pct > threshold_percent:
                    out.append(
                        {
                            "source": s,
                            "first_mb": first.memory.current_mb,
                            "last_mb": last.memory.current_mb,
                            "delta_mb": delta,
                            "percent_change": pct,
                            "trend": last.memory.trend,
                        }
                    )
        return out

    def _insert_sql(self) -> str:
        cols = (
            "record_id,source,status,memory,cpu_percent,disk_percent,latency_ms,"
            "error_rate,custom_metrics,timestamp,created_at"
        )
        return f"INSERT INTO {self.TABLE} ({cols}) VALUES ({','.join(['?'] * 11)})"

    def _row(self, h: UnifiedHealth) -> tuple:
        return (
            h.record_id,
            h.source,
            h.status,
            jdump(h.memory.model_dump()),
            h.cpu_percent,
            h.disk_percent,
            h.latency_ms,
            h.error_rate,
            jdump(h.custom_metrics),
            h.timestamp,
            h.created_at,
        )

    def _from(self, r: Any) -> UnifiedHealth:
        mem_raw = jload(
            r["memory"],
            {"current_mb": 0.0, "peak_mb": 0.0, "limit_mb": 0.0, "percent": 0.0, "trend": "stable"},
        )
        if not isinstance(mem_raw, dict):
            mem_raw = {}
        return UnifiedHealth(
            record_id=r["record_id"],
            source=r["source"],
            status=HealthStatus(r["status"]),
            memory=MemoryInfo(**mem_raw),
            cpu_percent=float(r["cpu_percent"] or 0),
            disk_percent=float(r["disk_percent"] or 0),
            latency_ms=float(r["latency_ms"] or 0),
            error_rate=float(r["error_rate"] or 0),
            custom_metrics=jload(r["custom_metrics"], {}),
            timestamp=r["timestamp"],
            created_at=r["created_at"],
        )


_aggregator: HealthAggregator | None = None


def get_health_aggregator() -> HealthAggregator:
    global _aggregator
    if _aggregator is None:
        _aggregator = HealthAggregator()
    return _aggregator


__all__ = [
    "HealthStatus",
    "MemoryInfo",
    "UnifiedHealth",
    "HealthAggregator",
    "get_health_aggregator",
]
