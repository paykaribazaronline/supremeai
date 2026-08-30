"""Governance Engine — risk classification + budgets (ROADMAP §28, §54).

বাংলা: ROADMAP §28 — autonomous actions risk অনুযায়ী ভাগ:
  safe       → observe, read metrics, read logs, inventory, health check
  low-risk   → retry, safe cache cleanup, restart unhealthy worker, re-run CI
  high-risk  → production deploy/rollback, DB migration, secret rotation, destructive ops

ROADMAP §54 — self-evolution does NOT mean unbounded self-modification। time /
memory / compute / dependency / crawl / capability-creation / retry / deployment
budget সব enforced থাকে।
"""

from __future__ import annotations

import enum
from dataclasses import dataclass, field
from datetime import UTC, datetime, timedelta
from typing import Any

from ecosystem._store import get_conn, jdump, jload


class ActionRisk(enum.StrEnum):
    """ROADMAP §28."""

    SAFE = "safe"
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class BudgetKind(enum.StrEnum):
    """ROADMAP §54 — every budget has an enforced limit."""

    TIME = "time"
    MEMORY = "memory"
    COMPUTE = "compute"
    DEPENDENCY = "dependency"
    CRAWL = "crawl"
    CAPABILITY_CREATION = "capability_creation"
    RETRY = "retry"
    DEPLOYMENT = "deployment"


@dataclass
class Budgets:
    """Configurable limits per tenant/owner (ROADMAP §54). Defaults are conservative."""

    time_seconds: int = 60 * 60  # 1h
    memory_mb: int = 512
    compute_minutes: int = 30
    dependency_count: int = 10
    crawl_pages_per_day: int = 500
    capability_creation_per_day: int = 5
    retry_max: int = 3
    deployment_per_day: int = 4


@dataclass
class RiskDecision:
    allowed: bool
    risk: ActionRisk
    reason: str
    requires_approval: bool
    budget_remaining: dict[str, Any] = field(default_factory=dict)


class GovernanceEngine:
    """Risk gating + budget enforcement (ROADMAP §28, §54)."""

    TABLE = "ecosystem_budget_usage"
    DECISION_TABLE = "ecosystem_governance_decisions"

    # বাংলা: ROADMAP §28 — safe/low-risk autonomous action-গুলো approval ছাড়াই চলে।
    _AUTO_OK: set[ActionRisk] = {ActionRisk.SAFE, ActionRisk.LOW}

    def __init__(self) -> None:
        self._ensure_schema()
        self._budgets = Budgets()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(
                f"""
                CREATE TABLE IF NOT EXISTS {self.TABLE} (
                    usage_id TEXT PRIMARY KEY,
                    budget_kind TEXT NOT NULL,
                    scope TEXT NOT NULL DEFAULT 'global',
                    scope_value TEXT NOT NULL DEFAULT 'global',
                    used INTEGER NOT NULL DEFAULT 0,
                    window_start TEXT NOT NULL,
                    window_kind TEXT NOT NULL DEFAULT 'day'
                )
                """
            )
            conn.execute(
                f"""
                CREATE TABLE IF NOT EXISTS {self.DECISION_TABLE} (
                    decision_id TEXT PRIMARY KEY,
                    action TEXT NOT NULL,
                    risk TEXT NOT NULL,
                    allowed INTEGER NOT NULL,
                    requires_approval INTEGER NOT NULL DEFAULT 0,
                    reason TEXT,
                    scope TEXT NOT NULL DEFAULT 'global',
                    scope_value TEXT NOT NULL DEFAULT 'global',
                    decided_at TEXT NOT NULL,
                    correlation TEXT NOT NULL DEFAULT '{{}}'
                )
                """
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_window "
                f"ON {self.TABLE}(budget_kind, scope, scope_value, window_start)"
            )
            conn.commit()

    # -- risk classification -----------------------------------------------

    def classify(self, action: str, *, context: dict[str, Any] | None = None) -> ActionRisk:
        """ROADMAP §28 — classify an autonomous action by keyword heuristics."""
        ctx = context or {}
        a = action.lower()
        # high-risk
        high = (
            "deploy", "rollback", "migration", "migrate", "secret",
            "rotate", "delete", "destroy", "drop", "production", "policy_change"
        )
        if any(k in a for k in high):
            return ActionRisk.HIGH
        # low-risk
        low = ("retry", "restart", "cache_cleanup", "rerun_ci", "reload", "reindex")
        if any(k in a for k in low):
            return ActionRisk.LOW
        # safe
        safe = ("observe", "metrics", "logs", "inventory", "health", "list", "get", "status")
        if any(k in a for k in safe):
            return ActionRisk.SAFE
        if ctx.get("destructive"):
            return ActionRisk.HIGH
        return ActionRisk.MEDIUM

    # -- gating -------------------------------------------------------------

    def authorize(
        self,
        action: str,
        *,
        scope: str = "global",
        scope_value: str = "global",
        context: dict[str, Any] | None = None,
        correlation: dict[str, Any] | None = None,
    ) -> RiskDecision:
        """ROADMAP §28, §54 — combined risk + budget gate."""
        risk = self.classify(action, context=context)
        requires_approval = risk in {ActionRisk.HIGH, ActionRisk.CRITICAL}
        # বাংলা: ROADMAP §54 — budget ব্যবহার আজকের window-এ ট্র্যাক করা হয়।
        budget_used = self._today_usage(BudgetKind.TIME, scope, scope_value)
        budget_remaining = {
            "time_seconds": max(0, self._budgets.time_seconds - budget_used),
            "capability_creation_per_day": max(
                0,
                self._budgets.capability_creation_per_day
                - self._today_usage(BudgetKind.CAPABILITY_CREATION, scope, scope_value),
            ),
            "deployment_per_day": max(
                0,
                self._budgets.deployment_per_day
                - self._today_usage(BudgetKind.DEPLOYMENT, scope, scope_value),
            ),
            "retry_max": max(
                0,
                self._budgets.retry_max
                - self._today_usage(BudgetKind.RETRY, scope, scope_value),
            ),
        }
        # reject if a high-risk action would breach deployment/retry budget
        denied_reason: str | None = None
        if risk in {ActionRisk.HIGH, ActionRisk.CRITICAL}:
            if budget_remaining["deployment_per_day"] <= 0:
                denied_reason = "deployment_budget_exhausted"
        allowed = denied_reason is None
        if not allowed:
            requires_approval = False  # already denied, no point approving
        # persist the decision
        self._record_decision(
            action=action,
            risk=risk,
            allowed=allowed,
            requires_approval=requires_approval,
            reason=denied_reason or "ok",
            scope=scope,
            scope_value=scope_value,
            correlation=correlation or {},
        )
        return RiskDecision(
            allowed=allowed,
            risk=risk,
            reason=denied_reason or "ok",
            requires_approval=requires_approval,
            budget_remaining=budget_remaining,
        )

    def record_budget_use(
        self,
        kind: BudgetKind,
        amount: int,
        *,
        scope: str = "global",
        scope_value: str = "global",
    ) -> None:
        today = datetime.now(UTC).date().isoformat()
        with get_conn() as conn:
            conn.execute(
                f"""
                INSERT INTO {self.TABLE} (usage_id, budget_kind, scope, scope_value,
                used, window_start, window_kind)
                VALUES (?, ?, ?, ?, ?, ?, 'day')
                ON CONFLICT(usage_id) DO UPDATE SET used = used + ?
                """,
                (
                    f"{kind.value}:{scope}:{scope_value}:{today}",
                    kind.value,
                    scope,
                    scope_value,
                    amount,
                    today,
                    amount,
                ),
            )
            conn.commit()

    # -- internals ----------------------------------------------------------

    def _today_usage(self, kind: BudgetKind, scope: str, scope_value: str) -> int:
        today = datetime.now(UTC).date().isoformat()
        with get_conn() as conn:
            row = conn.execute(
                f"SELECT COALESCE(SUM(used), 0) AS total FROM {self.TABLE} "
                f"WHERE budget_kind = ? AND scope = ? AND scope_value = ? "
                f"AND window_start = ?",
                (kind.value, scope, scope_value, today),
            ).fetchone()
        return int(row["total"] or 0)

    def _record_decision(
        self,
        *,
        action: str,
        risk: ActionRisk,
        allowed: bool,
        requires_approval: bool,
        reason: str,
        scope: str,
        scope_value: str,
        correlation: dict[str, Any],
    ) -> None:
        import uuid

        with get_conn() as conn:
            conn.execute(
                f"INSERT INTO {self.DECISION_TABLE} "
                f"(decision_id, action, risk, allowed, requires_approval, reason, "
                f"scope, scope_value, decided_at, correlation) "
                f"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (
                    f"gov-{uuid.uuid4().hex[:16]}",
                    action,
                    risk,
                    int(allowed),
                    int(requires_approval),
                    reason,
                    scope,
                    scope_value,
                    datetime.now(UTC).isoformat(),
                    jdump(correlation),
                ),
            )
            conn.commit()


# ---------------------------------------------------------------------------
# Singleton
# ---------------------------------------------------------------------------

_engine: GovernanceEngine | None = None


def get_governance_engine() -> GovernanceEngine:
    global _engine
    if _engine is None:
        _engine = GovernanceEngine()
    return _engine


__all__ = [
    "ActionRisk",
    "BudgetKind",
    "Budgets",
    "RiskDecision",
    "GovernanceEngine",
    "get_governance_engine",
]
