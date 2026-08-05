"""
SupremeAI — Meta-Learning Agent
================================
Learns how to learn, optimizing the learning process itself.
Analyzes past learning outcomes and optimizes future learning strategies.
"""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any

from core.cache import get_cache
from core.error_bus import with_error_bus
from core.llm_router import LLMRouter

logger = logging.getLogger("supremeai.meta_learning")

META_LEARNING_CACHE_TTL = 3600


@dataclass(frozen=True)
class LearningOutcome:
    """Immutable learning outcome record."""

    task_type: str
    strategy_used: str
    success_rate: float
    iterations: int
    time_taken_seconds: float
    feedback_score: float


@dataclass(frozen=True)
class LearningStrategy:
    """Immutable learning strategy recommendation."""

    task_type: str
    recommended_strategy: str
    expected_improvement: float
    confidence: float
    reasoning: str


class MetaLearningAgent:
    """
    Learns how to learn, optimizing the learning process itself.
    Analyzes past outcomes and optimizes future strategies.
    """

    def __init__(self, llm_router: LLMRouter | None = None) -> None:
        self.llm = llm_router or LLMRouter()
        self.cache = get_cache()
        self._outcomes: list[LearningOutcome] = []

    def _cache_key(self, prefix: str, identifier: str) -> str:
        raw = f"metalearning:{prefix}:{identifier}:{datetime.now(UTC).strftime('%Y%m%d')}"
        return f"metalearning:{hashlib.sha256(raw.encode()).hexdigest()[:16]}"

    def record_outcome(self, outcome: LearningOutcome) -> None:
        """Record a learning outcome for analysis."""
        self._outcomes.append(outcome)
        logger.info(
            "Recorded learning outcome: %s with %s (success: %.2f)",
            outcome.task_type,
            outcome.strategy_used,
            outcome.success_rate,
        )

    @with_error_bus("recommend_strategy")
    async def recommend_strategy(self, task_type: str) -> LearningStrategy:
        """Recommend the best learning strategy for a task type."""
        # Analyze historical outcomes for this task type
        relevant = [o for o in self._outcomes if o.task_type == task_type]

        if not relevant:
            # No history - use LLM to suggest initial strategy
            prompt = (
                f"Recommend a learning strategy for task type: {task_type}\n"
                f"Consider: chain-of-thought, few-shot, zero-shot, self-consistency, tree-of-thought.\n"
                f"Return as JSON with: recommended_strategy, expected_improvement (0-1), confidence (0-1), reasoning."
            )
            try:
                result = await self.llm.route(prompt=prompt, task_type="reasoning", max_tokens=300)
                import json

                content = result.get("content", "{}")
                data = json.loads(content) if isinstance(content, str) else content
                return LearningStrategy(
                    task_type=task_type,
                    recommended_strategy=data.get("recommended_strategy", "chain-of-thought"),
                    expected_improvement=float(data.get("expected_improvement", 0.3)),
                    confidence=float(data.get("confidence", 0.5)),
                    reasoning=data.get("reasoning", "Initial recommendation based on task type"),
                )
            except Exception:
                return LearningStrategy(
                    task_type=task_type,
                    recommended_strategy="chain-of-thought",
                    expected_improvement=0.3,
                    confidence=0.5,
                    reasoning="Default strategy for new task type",
                )

        # Find best performing strategy
        strategy_scores: dict[str, list[float]] = {}
        for o in relevant:
            strategy_scores.setdefault(o.strategy_used, []).append(o.success_rate)

        best_strategy = max(strategy_scores.items(), key=lambda x: sum(x[1]) / len(x[1]))
        avg_success = sum(best_strategy[1]) / len(best_strategy[1])

        # Calculate improvement over average
        all_successes = [o.success_rate for o in relevant]
        overall_avg = sum(all_successes) / len(all_successes) if all_successes else 0
        improvement = avg_success - overall_avg

        return LearningStrategy(
            task_type=task_type,
            recommended_strategy=best_strategy[0],
            expected_improvement=round(max(0, improvement), 2),
            confidence=round(min(0.95, len(best_strategy[1]) * 0.1), 2),
            reasoning=f"Best historical performance: {best_strategy[0]} with {avg_success:.0%} success rate",
        )

    def get_performance_summary(self) -> dict[str, Any]:
        """Get summary of learning performance across task types."""
        summary = {}
        for outcome in self._outcomes:
            if outcome.task_type not in summary:
                summary[outcome.task_type] = {
                    "total_attempts": 0,
                    "avg_success_rate": 0.0,
                    "best_strategy": "",
                    "strategies_tried": set(),
                }
            s = summary[outcome.task_type]
            s["total_attempts"] += 1
            s["strategies_tried"].add(outcome.strategy_used)

        # Calculate averages
        for task_type, data in summary.items():
            relevant = [o for o in self._outcomes if o.task_type == task_type]
            data["avg_success_rate"] = sum(o.success_rate for o in relevant) / len(relevant)
            data["strategies_tried"] = list(data["strategies_tried"])

            # Find best strategy
            strategy_scores: dict[str, list[float]] = {}
            for o in relevant:
                strategy_scores.setdefault(o.strategy_used, []).append(o.success_rate)
            if strategy_scores:
                best = max(strategy_scores.items(), key=lambda x: sum(x[1]) / len(x[1]))
                data["best_strategy"] = best[0]

        return summary


# Singleton
_meta_learning_instance: MetaLearningAgent | None = None


def get_meta_learning() -> MetaLearningAgent:
    """Get or create the singleton MetaLearningAgent."""
    global _meta_learning_instance
    if _meta_learning_instance is None:
        _meta_learning_instance = MetaLearningAgent()
    return _meta_learning_instance
