# backend/runtime/task_result.py
"""Task Result Object for Canonical Task Runtime."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Dict, List, Optional


@dataclass
class VerificationSummary:
    verified: bool = False
    policy_used: str = "standard"
    criteria_passed: List[str] = field(default_factory=list)
    criteria_failed: List[str] = field(default_factory=list)
    confidence: float = 0.0
    verification_time_ms: float = 0.0


@dataclass
class TaskResult:
    """Standardized output produced by Canonical Task Runtime."""

    task_id: str
    success: bool
    answer: Any
    confidence: float
    execution_time_ms: float
    provider_used: str = "Gemini"
    verification: VerificationSummary = field(default_factory=VerificationSummary)
    components_used: List[str] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())

    def to_dict(self) -> Dict[str, Any]:
        return {
            "task_id": self.task_id,
            "success": self.success,
            "answer": self.answer,
            "confidence": self.confidence,
            "execution_time_ms": self.execution_time_ms,
            "provider_used": self.provider_used,
            "verified": self.verification.verified,
            "components_used": self.components_used,
            "metadata": self.metadata,
            "error": self.error,
            "timestamp": self.timestamp,
        }
