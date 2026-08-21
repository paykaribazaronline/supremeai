# backend/evolution/canary_manager.py
"""Real Canary Rollout Controller and Automatic Rollback Gate."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
import logging
from typing import Any, Dict, List, Optional

from evolution.change_proposal import (
    ChangeProposal,
    ChangeProposalManager,
    ProposalState,
    get_change_manager,
)

logger = logging.getLogger("supremeai.evolution.canary")


@dataclass
class CanaryTrial:
    proposal_id: str
    sample_ratio: float = 0.10
    total_trials: int = 0
    successful_trials: int = 0
    failed_trials: int = 0
    total_latency_ms: float = 0.0
    started_at: datetime = field(default_factory=datetime.now)

    @property
    def success_rate(self) -> float:
        return (self.successful_trials / self.total_trials) if self.total_trials > 0 else 0.0

    @property
    def avg_latency_ms(self) -> float:
        return (self.total_latency_ms / self.total_trials) if self.total_trials > 0 else 0.0


class CanaryRolloutController:
    """Manages progressive canary deployments of AI ChangeProposals with automatic rollback."""

    def __init__(self, proposal_manager: Optional[ChangeProposalManager] = None) -> None:
        self.proposal_manager = proposal_manager or get_change_manager()
        self.active_canaries: Dict[str, CanaryTrial] = {}

    def deploy_canary(self, proposal_id: str, sample_ratio: float = 0.10) -> bool:
        proposal = self.proposal_manager.proposals.get(proposal_id)
        if not proposal:
            return False

        trial = CanaryTrial(proposal_id=proposal_id, sample_ratio=sample_ratio)
        self.active_canaries[proposal_id] = trial
        proposal.advance_state(ProposalState.CANARY_ACTIVE)
        logger.info(f"🐤 Canary active for [{proposal_id}] with traffic ratio: {sample_ratio * 100}%")
        return True

    def record_observation(self, proposal_id: str, success: bool, latency_ms: float = 0.0) -> None:
        trial = self.active_canaries.get(proposal_id)
        if not trial:
            return

        trial.total_trials += 1
        trial.total_latency_ms += latency_ms
        if success:
            trial.successful_trials += 1
        else:
            trial.failed_trials += 1

        # Check for immediate regression trigger
        if trial.total_trials >= 3 and trial.success_rate < 0.60:
            self.trigger_rollback(proposal_id, reason=f"High failure rate in canary ({trial.success_rate * 100:.1f}%)")

    def evaluate_and_promote(
        self,
        proposal_id: str,
        min_trials: int = 5,
        min_success_rate: float = 0.85,
    ) -> bool:
        trial = self.active_canaries.get(proposal_id)
        proposal = self.proposal_manager.proposals.get(proposal_id)
        if not trial or not proposal:
            return False

        if trial.total_trials < min_trials:
            logger.info(f"⏳ Canary for [{proposal_id}] still accumulating samples ({trial.total_trials}/{min_trials})")
            return False

        if trial.success_rate >= min_success_rate:
            proposal.canary_success_rate = trial.success_rate
            proposal.advance_state(ProposalState.PROMOTED)
            self.active_canaries.pop(proposal_id, None)
            logger.info(f"🎉 Canary PASSED! Proposal [{proposal_id}] promoted to production ({trial.success_rate * 100:.1f}%)")
            return True
        else:
            self.trigger_rollback(
                proposal_id,
                reason=f"Failed minimum canary success threshold ({trial.success_rate * 100:.1f}% < {min_success_rate * 100}%)",
            )
            return False

    def trigger_rollback(self, proposal_id: str, reason: str) -> None:
        self.active_canaries.pop(proposal_id, None)
        self.proposal_manager.rollback(proposal_id, reason)
        logger.warning(f"🚨 ROLLBACK TRIGGERED on [{proposal_id}]: {reason}")


# Global Singleton
_canary_controller: Optional[CanaryRolloutController] = None


def get_canary_controller() -> CanaryRolloutController:
    global _canary_controller
    if _canary_controller is None:
        _canary_controller = CanaryRolloutController()
    return _canary_controller
