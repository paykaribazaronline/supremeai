# backend/learning/pattern_detector.py
"""Pattern Detector for Continuous Learning from Task Experiences."""

from __future__ import annotations

from collections import Counter
from dataclasses import dataclass, field
from datetime import datetime, timezone
import logging
from typing import Any, Dict, List, Optional

from learning.experience import ExperienceRecord, ExperienceStore, get_experience_store

logger = logging.getLogger("supremeai.learning.pattern_detector")


@dataclass
class DetectedPattern:
    """Recurring pattern identified from past execution experiences."""

    pattern_type: str  # "syntax_error", "timeout", "budget_exhausted", "provider_performance"
    occurrence_count: int
    confidence: float
    description: str
    evidence_task_ids: List[str] = field(default_factory=list)
    suggested_action: str = ""
    timestamp: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    def to_dict(self) -> Dict[str, Any]:
        return {
            "pattern_type": self.pattern_type,
            "occurrence_count": self.occurrence_count,
            "confidence": self.confidence,
            "description": self.description,
            "evidence_task_ids": self.evidence_task_ids,
            "suggested_action": self.suggested_action,
            "timestamp": self.timestamp,
        }


class PatternDetector:
    """Scans experience ledger to identify systematic failures and optimization opportunities."""

    def __init__(self, store: Optional[ExperienceStore] = None) -> None:
        self.store = store or get_experience_store()

    def analyze_patterns(self, min_support: int = 2) -> List[DetectedPattern]:
        records = self.store.get_recent(limit=100)
        if not records:
            return []

        patterns: List[DetectedPattern] = []

        # 1. Failure Analysis
        syntax_fails: List[str] = []
        budget_fails: List[str] = []
        timeout_fails: List[str] = []

        for rec in records:
            for failure in rec.failures:
                failure_lower = failure.lower()
                if "syntax" in failure_lower or "ast" in failure_lower:
                    syntax_fails.append(rec.task_id)
                elif "budget" in failure_lower or "cost" in failure_lower or "token" in failure_lower:
                    budget_fails.append(rec.task_id)
                elif "timeout" in failure_lower or "latency" in failure_lower:
                    timeout_fails.append(rec.task_id)

        if len(syntax_fails) >= min_support:
            patterns.append(
                DetectedPattern(
                    pattern_type="syntax_error_pattern",
                    occurrence_count=len(syntax_fails),
                    confidence=round(min(0.98, 0.70 + (len(syntax_fails) * 0.05)), 2),
                    description="Repeated syntax/parsing errors during code generation.",
                    evidence_task_ids=syntax_fails,
                    suggested_action="Optimize code generation prompt to enforce strict AST compliance and clean markdown stripping.",
                )
            )

        if len(budget_fails) >= min_support:
            patterns.append(
                DetectedPattern(
                    pattern_type="budget_exhaustion_pattern",
                    occurrence_count=len(budget_fails),
                    confidence=round(min(0.95, 0.75 + (len(budget_fails) * 0.05)), 2),
                    description="Multiple tasks exceeded compute or token budget constraints.",
                    evidence_task_ids=budget_fails,
                    suggested_action="Scale initial token budget allocation or optimize plan step decomposition.",
                )
            )

        return patterns


# Global Singleton
_detector: Optional[PatternDetector] = None


def get_pattern_detector() -> PatternDetector:
    global _detector
    if _detector is None:
        _detector = PatternDetector()
    return _detector
