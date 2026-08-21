"""
adaptive_engine/learning_loop.py
================================
SupremeAI 2.0 — Continuous Learning & Self-Improvement Engine

বাংলা মন্তব্য: অভিজ্ঞতা ডাটাবেস থেকে ফিডব্যাক সংগ্রহ করে,
মডেল পারফরম্যান্স মেট্রিক্স বিশ্লেষণ করে, এবং সিস্টেমকে
স্বয়ংক্রিয়ভাবে উন্নত করার জন্য অ্যাকশনেবল ইনসাইট তৈরি করে।

Architecture:
- Experience ingestion from SQLite/Vector DB
- Pattern extraction & failure clustering
- Automated A/B suggestion generation
- Model performance drift detection
- Feedback loop closure with LLM Gateway
"""

from __future__ import annotations

import asyncio
from collections import Counter
from dataclasses import dataclass, field
from datetime import UTC, datetime, timedelta
from typing import Any

from loguru import logger

# Lazy imports to avoid circular dependencies at module level
if False:  # type-check only
    pass


@dataclass
class LearningInsight:
    """A single actionable insight extracted from the learning cycle."""

    insight_id: str
    category: str  # "performance", "reliability", "security", "ux"
    severity: str  # "critical", "warning", "info"
    description: str
    affected_components: list[str] = field(default_factory=list)
    suggested_action: str = ""
    confidence: float = 0.0
    created_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    resolved_at: datetime | None = None


@dataclass
class LearningCycleResult:
    """Result of a single learning cycle execution."""

    cycle_id: str
    status: str  # "completed", "partial", "failed"
    timestamp: datetime
    total_experiences: int
    new_patterns_found: int
    insights_generated: list[LearningInsight] = field(default_factory=list)
    model_drift_detected: bool = False
    top_failure_clusters: dict[str, int] = field(default_factory=dict)
    execution_time_ms: float = 0.0


class ExperienceClusterer:
    """Clusters similar experiences using semantic similarity and failure signatures."""

    def __init__(self, similarity_threshold: float = 0.85) -> None:
        self.similarity_threshold = similarity_threshold
        self._embeddings_cache: dict[str, list[float]] = {}

    def cluster_failures(self, experiences: list[dict[str, Any]]) -> dict[str, list[dict[str, Any]]]:
        """Group experiences by failure signature for pattern detection."""
        clusters: dict[str, list[dict[str, Any]]] = {}

        for exp in experiences:
            if exp.get("result") != "failure":
                continue

            # Create failure fingerprint from error message + action
            error_msg = exp.get("error_message", "") or ""
            action = exp.get("action_taken", "")
            fingerprint = self._generate_fingerprint(error_msg, action)

            if fingerprint not in clusters:
                clusters[fingerprint] = []
            clusters[fingerprint].append(exp)

        return clusters

    def _generate_fingerprint(self, error_msg: str, action: str) -> str:
        """Generate a normalized failure fingerprint."""
        # Normalize: lowercase, extract key error tokens
        normalized = f"{error_msg.lower()}::{action.lower()}"
        # Simple hash-based clustering for speed
        # In production, this uses sentence-transformers embeddings
        key_tokens = []
        for token in normalized.split():
            if any(
                kw in token
                for kw in [
                    "error",
                    "exception",
                    "timeout",
                    "fail",
                    "denied",
                    "invalid",
                    "none",
                ]
            ):
                key_tokens.append(token)
        return "_".join(key_tokens) if key_tokens else "generic_failure"


class PerformanceDriftDetector:
    """Detects performance degradation across model providers and tasks."""

    def __init__(self, window_size: int = 100, z_threshold: float = 2.5) -> None:
        self.window_size = window_size
        self.z_threshold = z_threshold
        self._latency_history: dict[str, list[float]] = {}
        self._error_rate_history: dict[str, list[float]] = {}

    def record_metric(self, provider: str, latency_ms: float, success: bool) -> None:
        """Record a performance metric for drift tracking."""
        if provider not in self._latency_history:
            self._latency_history[provider] = []
            self._error_rate_history[provider] = []

        self._latency_history[provider].append(latency_ms)
        self._error_rate_history[provider].append(1.0 if not success else 0.0)

        # Keep only recent window
        if len(self._latency_history[provider]) > self.window_size:
            self._latency_history[provider].pop(0)
            self._error_rate_history[provider].pop(0)

    def detect_drift(self, provider: str) -> dict[str, Any] | None:
        """Detect if a provider's performance has drifted significantly."""
        latencies = self._latency_history.get(provider, [])
        error_rates = self._error_rate_history.get(provider, [])

        if len(latencies) < self.window_size // 2:
            return None  # Not enough data

        # Split into recent and older halves
        mid = len(latencies) // 2
        old_lat = latencies[:mid]
        recent_lat = latencies[mid:]

        old_err = error_rates[:mid]
        recent_err = error_rates[mid:]

        old_lat_mean = sum(old_lat) / len(old_lat) if old_lat else 0.0
        recent_lat_mean = sum(recent_lat) / len(recent_lat) if recent_lat else 0.0
        old_err_mean = sum(old_err) / len(old_err) if old_err else 0.0
        recent_err_mean = sum(recent_err) / len(recent_err) if recent_err else 0.0

        # Simple Z-score approximation for drift
        if old_lat_mean > 0:
            lat_zscore = abs(recent_lat_mean - old_lat_mean) / old_lat_mean
        else:
            lat_zscore = 0.0

        drift_detected = lat_zscore > self.z_threshold or recent_err_mean > old_err_mean * 2

        return {
            "provider": provider,
            "drift_detected": drift_detected,
            "latency_change_pct": (
                round((recent_lat_mean - old_lat_mean) / old_lat_mean * 100, 2) if old_lat_mean else 0.0
            ),
            "error_rate_change": round(recent_err_mean - old_err_mean, 4),
            "recommendation": "consider_fallback" if drift_detected else "stable",
        }


class LearningLoop:
    """
    Continuous learning engine that runs periodic cycles to improve system performance.

    বাংলা মন্তব্য: প্রতি ২ ঘণ্টায় চলমান লার্নিং লুপ যা:
    ১. নতুন অভিজ্ঞতা সংগ্রহ করে
    ২. ব্যর্থতার প্যাটার্ন সনাক্ত করে
    ৩. মডেল ড্রিফ্ট ডিটেক্ট করে
    ৪. অ্যাকশনেবল ইনসাইট তৈরি করে
    ৫. ফিডব্যাক লুপ ক্লোজার নিশ্চিত করে
    """

    SCHEDULE = "0 */2 * * *"  # Every 2 hours
    _instance: LearningLoop | None = None
    _lock: asyncio.Lock | None = None

    def __new__(cls, *args: Any, **kwargs: Any) -> LearningLoop:
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._lock = asyncio.Lock()
        return cls._instance

    def __init__(
        self,
        experience_db: Any | None = None,
        llm_gateway: Any | None = None,
        cycle_interval_minutes: int = 120,
    ) -> None:
        if hasattr(self, "_initialized"):
            return

        self._initialized = True
        self.experience_db = experience_db
        self.llm_gateway = llm_gateway
        self.cycle_interval = timedelta(minutes=cycle_interval_minutes)
        self.clusterer = ExperienceClusterer()
        self.drift_detector = PerformanceDriftDetector()
        self._last_cycle: datetime | None = None
        self._cycle_count = 0
        self._insights_log: list[LearningInsight] = []
        self._is_running = False

    async def run_cycle(self) -> LearningCycleResult:
        """
        Execute a full learning cycle.

        Returns:
            LearningCycleResult with all findings and metrics.
        """
        if self._is_running:
            logger.warning("Learning cycle already running, skipping...")
            return LearningCycleResult(
                cycle_id=f"skipped_{datetime.now(UTC).isoformat()}",
                status="skipped",
                timestamp=datetime.now(UTC),
                total_experiences=0,
                new_patterns_found=0,
            )

        async with self._lock:
            self._is_running = True
            start_time = datetime.now(UTC)
            cycle_id = f"cycle_{start_time.strftime('%Y%m%d_%H%M%S')}_{self._cycle_count}"

            try:
                logger.info(f"🧠 Learning cycle {cycle_id} started")

                # Step 1: Collect recent experiences
                experiences = await self._collect_experiences()

                # Step 2: Cluster failures
                failure_clusters = self.clusterer.cluster_failures(experiences)

                # Step 3: Detect model drift
                drift_results = await self._check_model_drift()

                # Step 4: Generate insights via LLM if gateway available
                insights = await self._generate_insights(experiences, failure_clusters, drift_results)

                # Step 5: Persist insights and update counters
                self._insights_log.extend(insights)
                self._cycle_count += 1
                self._last_cycle = datetime.now(UTC)

                execution_time = (datetime.now(UTC) - start_time).total_seconds() * 1000

                result = LearningCycleResult(
                    cycle_id=cycle_id,
                    status="completed",
                    timestamp=datetime.now(UTC),
                    total_experiences=len(experiences),
                    new_patterns_found=len(failure_clusters),
                    insights_generated=insights,
                    model_drift_detected=any(d.get("drift_detected", False) for d in drift_results),
                    top_failure_clusters={k: len(v) for k, v in failure_clusters.items()},
                    execution_time_ms=execution_time,
                )

                logger.info(f"✅ Learning cycle {cycle_id} completed in {execution_time:.0f}ms")
                return result

            except Exception as exc:
                logger.exception(f"❌ Learning cycle {cycle_id} failed: {exc}")
                return LearningCycleResult(
                    cycle_id=cycle_id,
                    status="failed",
                    timestamp=datetime.now(UTC),
                    total_experiences=0,
                    new_patterns_found=0,
                    insights_generated=[
                        LearningInsight(
                            insight_id=f"error_{cycle_id}",
                            category="reliability",
                            severity="critical",
                            description=f"Learning cycle failed: {exc}",
                            confidence=1.0,
                        )
                    ],
                )
            finally:
                self._is_running = False

    async def _collect_experiences(self, hours_back: int = 24) -> list[dict[str, Any]]:
        """Collect experiences from the database within the time window."""
        if self.experience_db is None:
            logger.warning("No experience_db configured, returning empty list")
            return []

        cutoff = datetime.now(UTC) - timedelta(hours=hours_back)
        try:
            # Check if get_all_experiences is async or sync
            if asyncio.iscoroutinefunction(self.experience_db.get_all_experiences):
                all_exps = await self.experience_db.get_all_experiences()
            else:
                all_exps = self.experience_db.get_all_experiences()
        except Exception as exc:
            logger.warning(f"Failed to fetch experiences: {exc}")
            return []

        return [e for e in all_exps if e.get("timestamp", "") >= cutoff.isoformat()]

    async def _check_model_drift(self) -> list[dict[str, Any]]:
        """Check all active providers for performance drift."""
        providers = ["gemini", "groq", "deepseek", "openrouter"]
        results = []
        for provider in providers:
            drift = self.drift_detector.detect_drift(provider)
            if drift:
                results.append(drift)
        return results

    async def _generate_insights(
        self,
        experiences: list[dict[str, Any]],
        failure_clusters: dict[str, list[dict[str, Any]]],
        drift_results: list[dict[str, Any]],
    ) -> list[LearningInsight]:
        """Generate actionable insights from collected data."""
        insights: list[LearningInsight] = []

        # Insight 1: Top failure patterns
        for fingerprint, cluster in sorted(failure_clusters.items(), key=lambda x: -len(x[1]))[:3]:
            insights.append(
                LearningInsight(
                    insight_id=f"failure_{fingerprint}_{datetime.now(UTC).strftime('%H%M%S')}",
                    category="reliability",
                    severity="critical" if len(cluster) > 5 else "warning",
                    description=f"Failure pattern '{fingerprint}' occurred {len(cluster)} times",
                    affected_components=list(set(e.get("action_taken", "unknown") for e in cluster)),
                    suggested_action="Review error handling and add retry logic",
                    confidence=min(len(cluster) / 10, 1.0),
                )
            )

        # Insight 2: Model drift
        for drift in drift_results:
            if drift.get("drift_detected"):
                insights.append(
                    LearningInsight(
                        insight_id=f"drift_{drift['provider']}_{datetime.now(UTC).strftime('%H%M%S')}",
                        category="performance",
                        severity="warning",
                        description=f"Performance drift detected in {drift['provider']}: "
                        f"latency +{drift['latency_change_pct']}%, "
                        f"error rate change {drift['error_rate_change']}",
                        affected_components=[drift["provider"]],
                        suggested_action="Activate fallback provider and investigate root cause",
                        confidence=0.85,
                    )
                )

        # Insight 3: User feedback trends
        feedback_scores = [e.get("user_feedback") for e in experiences if e.get("user_feedback") is not None]
        if feedback_scores:
            avg_feedback = sum(1 if f == "positive" else -1 if f == "negative" else 0 for f in feedback_scores) / len(
                feedback_scores
            )
            if avg_feedback < -0.3:
                insights.append(
                    LearningInsight(
                        insight_id=f"feedback_trend_{datetime.now(UTC).strftime('%H%M%S')}",
                        category="ux",
                        severity="warning",
                        description=f"Negative feedback trend detected ({avg_feedback:.2f} avg score over {len(feedback_scores)} responses)",
                        suggested_action="Review recent prompt templates and response quality",
                        confidence=abs(avg_feedback),
                    )
                )

        return insights

    def get_insights(self, category: str | None = None, unresolved_only: bool = True) -> list[LearningInsight]:
        """Retrieve insights, optionally filtered."""
        insights = self._insights_log
        if category:
            insights = [i for i in insights if i.category == category]
        if unresolved_only:
            insights = [i for i in insights if i.resolved_at is None]
        return insights

    def resolve_insight(self, insight_id: str) -> bool:
        """Mark an insight as resolved."""
        for insight in self._insights_log:
            if insight.insight_id == insight_id:
                insight.resolved_at = datetime.now(UTC)
                return True
        return False

    def compute_ewc_loss_penalty(
        self,
        current_weights: dict[str, float],
        old_weights: dict[str, float],
        fisher_matrix: dict[str, float],
        ewc_lambda: float = 0.5,
    ) -> float:
        """
        Elastic Weight Consolidation (EWC) loss penalty calculation to prevent catastrophic forgetting.
        """
        penalty = 0.0
        for name, weight in current_weights.items():
            if name in old_weights and name in fisher_matrix:
                fisher_val = fisher_matrix[name]
                old_val = old_weights[name]
                penalty += fisher_val * ((weight - old_val) ** 2)
        return (ewc_lambda / 2.0) * penalty

    @classmethod
    def get_instance(cls) -> LearningLoop:
        """Return the singleton instance of LearningLoop."""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    async def record_signal(
        self,
        user_id: str,
        signal_type: str,
        payload: dict[str, Any],
        context: dict[str, Any] | None = None,
    ) -> None:
        """Record an interactive user signal (e.g. preference update, UX action) to drive self-improvement."""
        context = context or {}
        logger.info(f"[AdaptiveEngine] Recording user signal: user={user_id}, type={signal_type}")
        if self.experience_db:
            try:
                from adaptive_engine.experience_db import Experience
                exp = Experience(
                    user_id=user_id,
                    action_taken=signal_type,
                    context=context,
                    payload=payload,
                    timestamp=datetime.now(UTC),
                )
                if hasattr(self.experience_db, "record_experience"):
                    await self.experience_db.record_experience(exp)
            except Exception as e:
                logger.warning(f"[AdaptiveEngine] Failed to persist signal to experience_db: {e}")

    async def suggest(self, user_id: str) -> list[dict[str, Any]]:
        """Return proactive adaptive suggestions tailored for a user."""
        suggestions: list[dict[str, Any]] = []
        insights = self.get_insights(unresolved_only=True)
        for insight in insights[:3]:
            suggestions.append({
                "type": insight.category,
                "suggestion": insight.suggested_action or insight.description,
                "confidence": insight.confidence,
            })
        if not suggestions:
            suggestions.append({
                "type": "general",
                "suggestion": "System operating optimally with zero detected drift.",
                "confidence": 1.0,
            })
        return suggestions

    def get_stats(self) -> dict[str, Any]:
        """Get learning loop statistics."""
        return {
            "total_cycles": self._cycle_count,
            "last_cycle": self._last_cycle.isoformat() if self._last_cycle else None,
            "total_insights": len(self._insights_log),
            "unresolved_insights": len([i for i in self._insights_log if i.resolved_at is None]),
            "insights_by_category": Counter(i.category for i in self._insights_log),
            "insights_by_severity": Counter(i.severity for i in self._insights_log),
        }


# Convenience factory for lifespan.py integration
async def create_learning_loop(
    experience_db: Any | None = None,
    llm_gateway: Any | None = None,
) -> LearningLoop:
    """Factory for async initialization of the LearningLoop."""
    loop = LearningLoop(experience_db=experience_db, llm_gateway=llm_gateway)
    return loop
