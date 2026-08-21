# backend/runtime/task_context.py
"""Task Context and Execution Trace Tracker for Canonical Task Runtime."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Dict, List, Optional
import uuid


@dataclass
class TraceEvent:
    """Black-box trace event recording fine-grained actions and performance."""

    phase: str
    component: str
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    provider: str = "Gemini"
    latency_ms: float = 0.0
    cost_usd: float = 0.0
    success: bool = True
    error: Optional[str] = None
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "phase": self.phase,
            "component": self.component,
            "timestamp": self.timestamp,
            "provider": self.provider,
            "latency_ms": self.latency_ms,
            "cost_usd": self.cost_usd,
            "success": self.success,
            "error": self.error,
            "metadata": self.metadata,
        }


@dataclass
class TaskContext:
    """Execution context, resource consumption monitor, and trace logger."""

    trace_id: str = field(default_factory=lambda: f"trace_{uuid.uuid4().hex[:12]}")
    tenant_id: str = "default_tenant"
    session_id: Optional[str] = None
    client_ip: Optional[str] = None
    user_agent: Optional[str] = None
    started_at: datetime = field(default_factory=datetime.now)
    token_usage: Dict[str, int] = field(default_factory=lambda: {"prompt": 0, "completion": 0, "total": 0})
    cost_usd: float = 0.0
    active_provider: str = "Gemini"
    metadata: Dict[str, Any] = field(default_factory=dict)
    checkpoints: List[Dict[str, Any]] = field(default_factory=list)
    events: List[TraceEvent] = field(default_factory=list)

    def record_usage(self, prompt_tokens: int, completion_tokens: int, cost: float = 0.0) -> None:
        self.token_usage["prompt"] += prompt_tokens
        self.token_usage["completion"] += completion_tokens
        self.token_usage["total"] += prompt_tokens + completion_tokens
        self.cost_usd += cost

    def checkpoint(self, label: str, details: Optional[Dict[str, Any]] = None) -> None:
        self.checkpoints.append({
            "label": label,
            "timestamp": datetime.now().isoformat(),
            "details": details or {},
        })

    def record_event(
        self,
        phase: str,
        component: str,
        latency_ms: float = 0.0,
        cost_usd: float = 0.0,
        success: bool = True,
        error: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> None:
        event = TraceEvent(
            phase=phase,
            component=component,
            provider=self.active_provider,
            latency_ms=latency_ms,
            cost_usd=cost_usd,
            success=success,
            error=error,
            metadata=metadata or {},
        )
        self.events.append(event)
