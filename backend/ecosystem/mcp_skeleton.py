"""SupremeAI MCP Skeleton. ROADMAP §45-§46.

Phase 14: Unified MCP-style operation interface.
OBSERVE / ANALYZE / ACT — ACT operations are governance-gated.
"""

from __future__ import annotations

import enum
import uuid
from collections.abc import Callable
from datetime import UTC, datetime
from typing import Any

from ecosystem._store import get_conn, jdump
from ecosystem.governance import get_governance_engine


class MCPOperationCategory(enum.StrEnum):
    OBSERVE = "OBSERVE"
    ANALYZE = "ANALYZE"
    ACT = "ACT"


class MCPOperationError(Exception):
    pass


class MCPOperationNotRegisteredError(Exception):
    pass


class MCPActionDenied(Exception):
    pass


_OBSERVE_OPS = {
    "list_resources",
    "get_resource",
    "get_health",
    "get_metrics",
    "get_logs",
    "get_deployment",
    "get_task_status",
    "get_capabilities",
    "get_kaggle_quota",
}
_ANALYZE_OPS = {
    "find_capability",
    "detect_capability_gap",
    "forecast_capability",
    "trace_dependency",
    "correlate_error",
    "analyze_resource_usage",
}
_ACT_OPS = {
    "create_capability",
    "activate_capability",
    "archive_capability",
    "restart",
    "deploy",
    "rollback",
    "trigger_job",
    "trigger_kaggle",
    "create_github_pr",
}


def _to_serializable(value: Any) -> Any:
    if isinstance(value, (dict, list, str, int, float, bool, type(None))):
        return value
    return {"value": str(value)}


class MCPSkeleton:
    """Phase 14 — SupremeAI MCP. ROADMAP §45."""

    TABLE = "ecosystem_mcp_calls"

    def __init__(self) -> None:
        self._handlers: dict[str, tuple[MCPOperationCategory, Callable[..., Any]]] = {}
        self._ensure_schema()
        self._register_defaults()

    def _ensure_schema(self) -> None:
        with get_conn() as conn:
            conn.execute(f"""CREATE TABLE IF NOT EXISTS {self.TABLE} (
                call_id TEXT PRIMARY KEY, operation TEXT NOT NULL, category TEXT NOT NULL,
                allowed INTEGER NOT NULL, params TEXT DEFAULT '{{}}',
                result TEXT DEFAULT '{{}}', error TEXT, reason TEXT,
                correlation TEXT DEFAULT '{{}}', created_at TEXT NOT NULL)""")
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_op ON {self.TABLE}(operation)"
            )
            conn.execute(
                f"CREATE INDEX IF NOT EXISTS idx_{self.TABLE}_cat ON {self.TABLE}(category)"
            )
            conn.commit()

    def register(
        self, operation: str, category: MCPOperationCategory, handler: Callable[..., Any]
    ) -> None:
        self._handlers[operation] = (category, handler)

    def call(
        self,
        operation: str,
        *,
        params: dict[str, Any] | None = None,
        correlation: dict[str, Any] | None = None,
        actor: str = "system",
    ) -> dict[str, Any]:
        params = params or {}
        corr = correlation or {}
        if operation not in self._handlers:
            raise MCPOperationNotRegisteredError(f"Operation '{operation}' not registered")
        category, handler = self._handlers[operation]
        # governance gate for ACT operations (§46)
        allowed = True
        reason = "auto-allowed"
        if category == MCPOperationCategory.ACT:
            gov = get_governance_engine()
            decision = gov.authorize(operation, context={"actor": actor, **params})
            allowed = decision.allowed
            reason = decision.reason
            if not allowed:
                self._record(
                    operation, category, False, params, None, "denied_by_governance", reason, corr
                )
                raise MCPActionDenied(f"ACT '{operation}' denied by governance: {reason}")
        try:
            result = handler(**params) if isinstance(params, dict) else handler(params)
            self._record(operation, category, True, params, result, None, reason, corr)
            return {
                "operation": operation,
                "category": str(category),
                "allowed": True,
                "result": result,
            }
        except Exception as e:
            self._record(operation, category, False, params, None, str(e), reason, corr)
            raise

    def list_operations(
        self, *, category: MCPOperationCategory | None = None
    ) -> list[dict[str, str]]:
        out = []
        for op, (cat, _) in self._handlers.items():
            if category is None or cat == category:
                out.append({"operation": op, "category": str(cat)})
        return sorted(out, key=lambda x: (x["category"], x["operation"]))

    def _record(
        self,
        operation: str,
        category: MCPOperationCategory,
        allowed: bool,
        params: dict[str, Any],
        result: Any,
        error: str | None,
        reason: str,
        correlation: dict[str, Any],
    ) -> None:
        with get_conn() as conn:
            conn.execute(
                f"INSERT INTO {self.TABLE} (call_id, operation, category, allowed, params, "
                "result, error, reason, correlation, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (
                    f"mcp-{uuid.uuid4().hex[:16]}",
                    operation,
                    str(category),
                    int(allowed),
                    jdump(params),
                    jdump(_to_serializable(result)),
                    error,
                    reason,
                    jdump(correlation),
                    datetime.now(UTC).isoformat(),
                ),
            )
            conn.commit()

    def _register_defaults(self) -> None:
        # Default no-op stubs — real adapters override via register()
        for op in _OBSERVE_OPS:
            self.register(op, MCPOperationCategory.OBSERVE, self._noop)
        for op in _ANALYZE_OPS:
            self.register(op, MCPOperationCategory.ANALYZE, self._noop)
        for op in _ACT_OPS:
            self.register(op, MCPOperationCategory.ACT, self._noop)

    @staticmethod
    def _noop(**kwargs: Any) -> dict[str, Any]:
        return {"status": "noop", "kwargs": kwargs}


_skeleton: MCPSkeleton | None = None


def get_mcp_skeleton() -> MCPSkeleton:
    global _skeleton
    if _skeleton is None:
        _skeleton = MCPSkeleton()
    return _skeleton


__all__ = [
    "MCPOperationCategory",
    "MCPSkeleton",
    "get_mcp_skeleton",
    "MCPOperationError",
    "MCPOperationNotRegisteredError",
    "MCPActionDenied",
]
