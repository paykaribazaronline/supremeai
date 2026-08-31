"""Correlation IDs — distributed tracing (ROADMAP §44)."""

from __future__ import annotations

import uuid
from contextvars import ContextVar
from dataclasses import dataclass, field
from typing import Any

_current: ContextVar[CorrelationContext | None] = ContextVar("ecosystem_correlation", default=None)


@dataclass
class CorrelationContext:
    request_id: str = field(default_factory=lambda: f"req-{uuid.uuid4().hex[:16]}")
    task_id: str | None = None
    job_id: str | None = None
    deployment_id: str | None = None
    resource_id: str | None = None
    capability_id: str | None = None
    audit_id: str | None = None
    user_id: str | None = None
    tenant_id: str | None = None
    extra: dict[str, Any] = field(default_factory=dict)

    def as_headers(self) -> dict[str, str]:
        h = {"x-correlation-request-id": self.request_id}
        if self.task_id:
            h["x-correlation-task-id"] = self.task_id
        if self.job_id:
            h["x-correlation-job-id"] = self.job_id
        if self.deployment_id:
            h["x-correlation-deployment-id"] = self.deployment_id
        if self.resource_id:
            h["x-correlation-resource-id"] = self.resource_id
        if self.capability_id:
            h["x-correlation-capability-id"] = self.capability_id
        return h

    def child(self, **overrides: Any) -> CorrelationContext:
        base = {k: v for k, v in self.__dict__.items()}
        base.update(overrides)
        return CorrelationContext(**base)


def new_correlation_context(**kwargs: Any) -> CorrelationContext:
    ctx = CorrelationContext(**kwargs)
    _current.set(ctx)
    return ctx


def current_correlation() -> CorrelationContext:
    ctx = _current.get()
    if ctx is None:
        ctx = CorrelationContext()
        _current.set(ctx)
    return ctx


__all__ = ["CorrelationContext", "new_correlation_context", "current_correlation"]
