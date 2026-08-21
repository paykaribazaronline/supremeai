# backend/learning/hypothesis_engine.py
"""Improvement Hypothesis Engine and ChangeProposal Bridge."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import logging
from typing import Any, Dict, List, Optional
import uuid

from evolution.change_proposal import (
    ChangeProposal,
    ChangeProposalManager,
    ChangeType,
    get_change_manager,
)
from learning.pattern_detector import DetectedPattern, PatternDetector, get_pattern_detector

logger = logging.getLogger("supremeai.learning.hypothesis_engine")


@dataclass
class ImprovementHypothesis:
    """Evidence-backed hypothesis for autonomous system enhancement."""

    category: str
    observation: str
    proposed_change: Dict[str, Any]
    target_module: str
    evidence_ids: List[str] = field(default_factory=list)
    expected_gain: float = 0.05
    expected_cost: float = 0.0
    risk_level: str = "LOW"  # "LOW", "MEDIUM", "HIGH", "CRITICAL"
    confidence: float = 0.85
    hypothesis_id: str = field(default_factory=lambda: f"hyp_{uuid.uuid4().hex[:10]}")
    timestamp: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    def to_dict(self) -> Dict[str, Any]:
        return {
            "hypothesis_id": self.hypothesis_id,
            "category": self.category,
            "observation": self.observation,
            "proposed_change": self.proposed_change,
            "target_module": self.target_module,
            "evidence_ids": self.evidence_ids,
            "expected_gain": self.expected_gain,
            "expected_cost": self.expected_cost,
            "risk_level": self.risk_level,
            "confidence": self.confidence,
            "timestamp": self.timestamp,
        }


class HypothesisEngine:
    """Transforms discovered execution patterns into formal improvement hypotheses and governed ChangeProposals."""

    def __init__(
        self,
        detector: Optional[PatternDetector] = None,
        proposal_manager: Optional[ChangeProposalManager] = None,
    ) -> None:
        self.detector = detector or get_pattern_detector()
        self.proposal_manager = proposal_manager or get_change_manager()

    def generate_hypotheses(self) -> List[ImprovementHypothesis]:
        patterns = self.detector.analyze_patterns()
        hypotheses: List[ImprovementHypothesis] = []

        for pat in patterns:
            if pat.pattern_type == "syntax_error_pattern":
                hypotheses.append(
                    ImprovementHypothesis(
                        category="PROMPT_OPTIMIZATION",
                        observation=f"Detected {pat.occurrence_count} syntax failures in recent tasks.",
                        proposed_change={
                            "system_prompt_addition": "CRITICAL: Return purely valid, parseable syntax. Strip all markdown code fences.",
                            "strip_markdown_enabled": True,
                        },
                        target_module="backend/brain/code_generation_prompt.py",
                        evidence_ids=pat.evidence_task_ids,
                        expected_gain=0.15,
                        risk_level="LOW",
                        confidence=pat.confidence,
                    )
                )
            elif pat.pattern_type == "budget_exhaustion_pattern":
                hypotheses.append(
                    ImprovementHypothesis(
                        category="PARAMETER_TUNING",
                        observation=f"Detected {pat.occurrence_count} budget exhaustion events.",
                        proposed_change={"default_token_multiplier": 1.25},
                        target_module="backend/runtime/budget_guard.py",
                        evidence_ids=pat.evidence_task_ids,
                        expected_gain=0.10,
                        risk_level="LOW",
                        confidence=pat.confidence,
                    )
                )

        return hypotheses

    def convert_hypothesis_to_proposal(self, hypothesis: ImprovementHypothesis) -> Optional[ChangeProposal]:
        """Convert a validated hypothesis with high confidence into a formal ChangeProposal."""
        if hypothesis.confidence < 0.70:
            logger.info(f"⏳ Hypothesis [{hypothesis.hypothesis_id}] confidence too low ({hypothesis.confidence}); skipping proposal.")
            return None

        change_type = ChangeType.PROMPT_OPTIMIZATION if hypothesis.category == "PROMPT_OPTIMIZATION" else ChangeType.PARAMETER_TUNING

        proposal = self.proposal_manager.create_proposal(
            title=f"Self-Improvement: {hypothesis.category} ({hypothesis.hypothesis_id})",
            description=hypothesis.observation,
            change_type=change_type,
            diff_content=hypothesis.proposed_change,
            target_module=hypothesis.target_module,
            current_fitness=0.80,
        )

        logger.info(f"✨ Created Governed ChangeProposal [{proposal.proposal_id}] from Hypothesis [{hypothesis.hypothesis_id}]")
        return proposal


# Global Singleton
_hypothesis_engine: Optional[HypothesisEngine] = None


def get_hypothesis_engine() -> HypothesisEngine:
    global _hypothesis_engine
    if _hypothesis_engine is None:
        _hypothesis_engine = HypothesisEngine()
    return _hypothesis_engine
