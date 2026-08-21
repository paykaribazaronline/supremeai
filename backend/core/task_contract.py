# backend/core/task_contract.py
"""Universal Task Contract (P0 Canonical Intelligence Abstraction).

Provides a single unified contract for all agents, planners, executors,
evaluators, and evolution engines across SupremeAI.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional
import uuid


class TaskStatus(str, Enum):
    PENDING = "pending"
    PLANNING = "planning"
    EXECUTING = "executing"
    VERIFYING = "verifying"
    COMPLETED = "completed"
    FAILED = "failed"
    ROLLED_BACK = "rolled_back"


class RiskLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class VerificationPolicy(str, Enum):
    STRICT = "strict"        # Must pass tests, AST scan & deterministic benchmark
    STANDARD = "standard"    # Must pass schema validation & confidence check
    PERMISSIVE = "permissive"# Fast path for read-only / low-risk queries


@dataclass
class TaskContract:
    """Universal Task Object connecting Planner -> Reasoner -> Executor -> Evaluator -> Memory."""

    goal: str
    task_id: str = field(default_factory=lambda: f"task_{uuid.uuid4().hex[:12]}")
    constraints: List[str] = field(default_factory=list)
    context: Dict[str, Any] = field(default_factory=dict)
    risk_level: RiskLevel = RiskLevel.MEDIUM
    budget: float = 1.0  # Max compute/token budget in USD or units
    deadline: Optional[datetime] = None
    required_capabilities: List[str] = field(default_factory=list)
    success_criteria: List[str] = field(default_factory=list)
    verification_policy: VerificationPolicy = VerificationPolicy.STANDARD
    allowed_tools: List[str] = field(default_factory=list)
    memory_scope: str = "session"  # "global" | "session" | "ephemeral"
    status: TaskStatus = TaskStatus.PENDING
    plan_steps: List[Dict[str, Any]] = field(default_factory=list)
    execution_history: List[Dict[str, Any]] = field(default_factory=list)
    result: Optional[Any] = None
    confidence: float = 0.0
    error: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)
    completed_at: Optional[datetime] = None
    metrics: Dict[str, Any] = field(default_factory=dict)

    def mark_in_progress(self, stage: TaskStatus) -> None:
        self.status = stage
        self.execution_history.append({
            "stage": stage.value,
            "timestamp": datetime.now().isoformat()
        })

    def complete(self, result: Any, confidence: float = 0.95) -> None:
        self.status = TaskStatus.COMPLETED
        self.result = result
        self.confidence = confidence
        self.completed_at = datetime.now()
        self.execution_history.append({
            "stage": TaskStatus.COMPLETED.value,
            "timestamp": self.completed_at.isoformat(),
            "confidence": confidence
        })

    def fail(self, error_message: str) -> None:
        self.status = TaskStatus.FAILED
        self.error = error_message
        self.completed_at = datetime.now()
        self.execution_history.append({
            "stage": TaskStatus.FAILED.value,
            "error": error_message,
            "timestamp": self.completed_at.isoformat()
        })

    def to_dict(self) -> Dict[str, Any]:
        return {
            "task_id": self.task_id,
            "goal": self.goal,
            "constraints": self.constraints,
            "risk_level": self.risk_level.value,
            "budget": self.budget,
            "required_capabilities": self.required_capabilities,
            "success_criteria": self.success_criteria,
            "verification_policy": self.verification_policy.value,
            "allowed_tools": self.allowed_tools,
            "memory_scope": self.memory_scope,
            "status": self.status.value,
            "plan_steps_count": len(self.plan_steps),
            "confidence": self.confidence,
            "error": self.error,
            "created_at": self.created_at.isoformat(),
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "metrics": self.metrics,
        }
