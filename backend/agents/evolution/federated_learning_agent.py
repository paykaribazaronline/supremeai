"""
SupremeAI — Federated Learning Agent
=====================================
Enables distributed learning while preserving privacy.
Coordinates model training across decentralized clients with differential privacy.
"""

from __future__ import annotations

import hashlib
import logging
import random
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any

from core.cache import get_cache

logger = logging.getLogger("supremeai.federated_learning")

FEDERATED_CACHE_TTL = 3600


@dataclass(frozen=True)
class ModelUpdate:
    """Immutable model update from a client."""

    client_id: str
    round_number: int
    parameters: dict[str, list[float]]
    metrics: dict[str, float]
    num_samples: int
    timestamp: datetime


@dataclass(frozen=True)
class PrivacyBudget:
    """Immutable privacy budget tracking."""

    epsilon: float
    delta: float
    spent_epsilon: float
    remaining_epsilon: float
    max_rounds: int


@dataclass(frozen=True)
class FederatedRound:
    """Immutable federated learning round."""

    round_number: int
    participants: list[str]
    global_parameters: dict[str, list[float]]
    aggregated_metrics: dict[str, float]
    privacy_budget_used: float
    completed_at: datetime


class FederatedLearningAgent:
    """
    Enables distributed learning while preserving privacy.
    """

    def __init__(self) -> None:
        self.cache = get_cache()
        self._rounds: list[FederatedRound] = []
        self._updates: list[ModelUpdate] = []
        self._current_round = 0
        self._privacy_budget = PrivacyBudget(
            epsilon=1.0,
            delta=1e-5,
            spent_epsilon=0.0,
            remaining_epsilon=1.0,
            max_rounds=100,
        )

    def _cache_key(self, prefix: str, identifier: str) -> str:
        raw = f"federated:{prefix}:{identifier}:{datetime.now(UTC).strftime('%Y%m%d')}"
        return f"federated:{hashlib.sha256(raw.encode()).hexdigest()[:16]}"

    def start_round(self, participants: list[str]) -> int:
        """Start a new federated learning round."""
        self._current_round += 1
        logger.info(
            "Starting federated round %d with %d participants",
            self._current_round,
            len(participants),
        )
        return self._current_round

    def submit_update(self, update: ModelUpdate) -> bool:
        """Submit a model update from a client."""
        if update.round_number != self._current_round:
            logger.warning(
                "Update round mismatch: got %d, expected %d",
                update.round_number,
                self._current_round,
            )
            return False
        self._updates.append(update)
        return True

    def apply_differential_privacy(
        self, parameters: dict[str, list[float]], epsilon: float = 0.1
    ) -> dict[str, list[float]]:
        """Apply differential privacy noise to parameters."""
        if self._privacy_budget.remaining_epsilon < epsilon:
            logger.warning(
                "Insufficient privacy budget: remaining %.4f, requested %.4f",
                self._privacy_budget.remaining_epsilon,
                epsilon,
            )
            return parameters

        noisy_params = {}
        for key, values in parameters.items():
            sensitivity = 1.0
            scale = sensitivity / epsilon
            noisy_params[key] = [v + random.gauss(0, scale) for v in values]

        return noisy_params

    def aggregate_updates(self, round_number: int) -> FederatedRound | None:
        """Aggregate all updates for a round using FedAvg."""
        round_updates = [u for u in self._updates if u.round_number == round_number]

        if not round_updates:
            logger.warning("No updates to aggregate for round %d", round_number)
            return None

        # FedAvg: weighted average by num_samples
        total_samples = sum(u.num_samples for u in round_updates)
        participants = list(set(u.client_id for u in round_updates))

        # Aggregate parameters
        aggregated_params: dict[str, list[float]] = {}
        for key in round_updates[0].parameters:
            weighted_sum = [0.0] * len(round_updates[0].parameters[key])
            for update in round_updates:
                weight = update.num_samples / total_samples
                for i, val in enumerate(update.parameters[key]):
                    weighted_sum[i] += val * weight
            aggregated_params[key] = weighted_sum

        # Apply differential privacy
        privacy_epsilon = 0.1
        private_params = self.apply_differential_privacy(
            aggregated_params, privacy_epsilon
        )

        # Aggregate metrics
        aggregated_metrics = {}
        for key in round_updates[0].metrics:
            aggregated_metrics[key] = sum(
                u.metrics[key] * u.num_samples / total_samples for u in round_updates
            )

        # Update privacy budget
        self._privacy_budget = PrivacyBudget(
            epsilon=self._privacy_budget.epsilon,
            delta=self._privacy_budget.delta,
            spent_epsilon=self._privacy_budget.spent_epsilon + privacy_epsilon,
            remaining_epsilon=self._privacy_budget.remaining_epsilon - privacy_epsilon,
            max_rounds=self._privacy_budget.max_rounds,
        )

        round_result = FederatedRound(
            round_number=round_number,
            participants=participants,
            global_parameters=private_params,
            aggregated_metrics=aggregated_metrics,
            privacy_budget_used=privacy_epsilon,
            completed_at=datetime.now(UTC),
        )

        self._rounds.append(round_result)
        return round_result

    def get_privacy_spent(self) -> dict[str, float]:
        """Get current privacy budget usage."""
        return {
            "epsilon_used": self._privacy_budget.spent_epsilon,
            "epsilon_remaining": self._privacy_budget.remaining_epsilon,
            "epsilon_total": self._privacy_budget.epsilon,
            "usage_percent": (
                self._privacy_budget.spent_epsilon / self._privacy_budget.epsilon
            )
            * 100,
        }

    def get_round_history(self) -> list[dict[str, Any]]:
        """Get history of all completed rounds."""
        return [
            {
                "round": r.round_number,
                "participants": len(r.participants),
                "metrics": r.aggregated_metrics,
                "privacy_cost": r.privacy_budget_used,
                "completed_at": r.completed_at.isoformat(),
            }
            for r in self._rounds
        ]


# Singleton
_federated_instance: FederatedLearningAgent | None = None


def get_federated_learning() -> FederatedLearningAgent:
    """Get or create the singleton FederatedLearningAgent."""
    global _federated_instance
    if _federated_instance is None:
        _federated_instance = FederatedLearningAgent()
    return _federated_instance
