# backend/runtime/task_context.py
"""Task Context and Execution Trace Tracker for Canonical Task Runtime."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import hashlib
import time
from typing import Any, Dict, List, Optional
import uuid


def hash_content(content: Any) -> str:
    """Generate SHA-256 fingerprint of input/output data for reproducible tracing."""
    if not content:
        return ""
    data_str = str(content).strip()
    return hashlib.sha256(data_str.encode("utf-8")).hexdigest()[:16]


@dataclass
class TraceEvent:
    """Black-box trace event recording fine-grained actions and causal provenance."""

    phase: str
    component: str
    sequence_number: int = 0
    event_id: str = field(default_factory=lambda: f"evt_{uuid.uuid4().hex[:10]}")
    parent_event_id: Optional[str] = None
    timestamp: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    monotonic_ns: int = field(default_factory=time.perf_counter_ns)
    provider: Optional[str] = None
    model: Optional[str] = None
    input_hash: str = ""
    output_hash: str = ""
    latency_ms: float = 0.0
    cost_usd: float = 0.0
    success: bool = True
    error: Optional[str] = None
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "event_id": self.event_id,
            "sequence_number": self.sequence_number,
            "parent_event_id": self.parent_event_id,
            "phase": self.phase,
            "component": self.component,
            "timestamp": self.timestamp,
            "provider": self.provider,
            "model": self.model,
            "input_hash": self.input_hash,
            "output_hash": self.output_hash,
            "latency_ms": self.latency_ms,
            "cost_usd": self.cost_usd,
            "success": self.success,
            "error": self.error,
            "metadata": self.metadata,
        }


@dataclass
class TaskContext:
    """Execution context, resource consumption monitor, and immutable trace logger."""

    trace_id: str = field(default_factory=lambda: f"trace_{uuid.uuid4().hex[:12]}")
    tenant_id: str = "default_tenant"
    session_id: Optional[str] = None
    client_ip: Optional[str] = None
    user_agent: Optional[str] = None
    started_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    token_usage: Dict[str, int] = field(default_factory=lambda: {"prompt": 0, "completion": 0, "total": 0})
    cost_usd: float = 0.0
    active_provider: Optional[str] = None
    active_model: Optional[str] = None
    metadata: Dict[str, Any] = field(default_factory=dict)
    checkpoints: List[Dict[str, Any]] = field(default_factory=list)
    events: List[TraceEvent] = field(default_factory=list)
    _seq_counter: int = field(default=0, init=False)

    def record_usage(self, prompt_tokens: int, completion_tokens: int, cost: float = 0.0) -> None:
        self.token_usage["prompt"] += prompt_tokens
        self.token_usage["completion"] += completion_tokens
        self.token_usage["total"] += prompt_tokens + completion_tokens
        self.cost_usd += cost

    def checkpoint(self, label: str, details: Optional[Dict[str, Any]] = None) -> None:
        self.checkpoints.append({
            "label": label,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "details": details or {},
        })

    def record_event(
        self,
        phase: str,
        component: str,
        input_data: Optional[Any] = None,
        output_data: Optional[Any] = None,
        parent_event_id: Optional[str] = None,
        latency_ms: float = 0.0,
        cost_usd: float = 0.0,
        success: bool = True,
        error: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> TraceEvent:
        self._seq_counter += 1
        event = TraceEvent(
            sequence_number=self._seq_counter,
            phase=phase,
            component=component,
            parent_event_id=parent_event_id,
            provider=self.active_provider,
            model=self.active_model,
            input_hash=hash_content(input_data),
            output_hash=hash_content(output_data),
            latency_ms=latency_ms,
            cost_usd=cost_usd,
            success=success,
            error=error,
            metadata=metadata or {},
        )
        self.events.append(event)
        return event
