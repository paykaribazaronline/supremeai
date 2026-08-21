# backend/runtime/task_context.py
"""Task Context Tracker for Canonical Task Runtime."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Dict, List, Optional
import uuid


@dataclass
class TaskContext:
    """Execution context and resource consumption monitor for active tasks."""

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
