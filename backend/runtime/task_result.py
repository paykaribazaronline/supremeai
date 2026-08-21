# backend/runtime/task_result.py
"""Task Result Object for Canonical Task Runtime."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Dict, List, Optional


@dataclass
class CriterionResult:
    criterion: str
    passed: bool
    evidence: str = ""
    is_required: bool = True

    def to_dict(self) -> Dict[str, Any]:
        return {
            "criterion": self.criterion,
            "passed": self.passed,
            "evidence": self.evidence,
            "is_required": self.is_required,
        }


@dataclass
class VerificationSummary:
    """Detailed, objective verification verdict separating evidence from AI self-confidence."""

    verified: bool = False
    policy_used: str = "standard"
    score: float = 0.0  # Objective evidence score 0.0 - 1.0
    criteria_results: List[CriterionResult] = field(default_factory=list)
    failures: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)
    evidence: List[str] = field(default_factory=list)
    recommendation: str = "PASS"
    verification_time_ms: float = 0.0

    @property
    def criteria_passed(self) -> List[str]:
        return [c.criterion for c in self.criteria_results if c.passed]

    @property
    def criteria_failed(self) -> List[str]:
        return [c.criterion for c in self.criteria_results if not c.passed] + self.failures

    def to_dict(self) -> Dict[str, Any]:
        return {
            "verified": self.verified,
            "policy_used": self.policy_used,
            "score": self.score,
            "criteria_results": [c.to_dict() for c in self.criteria_results],
            "criteria_passed": self.criteria_passed,
            "criteria_failed": self.criteria_failed,
            "failures": self.failures,
            "warnings": self.warnings,
            "evidence": self.evidence,
            "recommendation": self.recommendation,
            "verification_time_ms": self.verification_time_ms,
        }


@dataclass
class TaskResult:
    """Standardized output produced by Canonical Task Runtime."""

    task_id: str
    success: bool
    answer: Any
    confidence: float  # AI model confidence belief
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
            "verification_score": self.verification.score,
            "components_used": self.components_used,
            "metadata": self.metadata,
            "error": self.error,
            "timestamp": self.timestamp,
        }
