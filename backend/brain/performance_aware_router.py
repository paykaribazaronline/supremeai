from typing import Any
from typing import TypedDict

from loguru import logger


class ProviderHealth(TypedDict):
    status: str
    latency_ms: int


# Health checker simulation - in production this would come from actual health checks
PROVIDER_HEALTH: dict[str, ProviderHealth] = {
    "groq": {"status": "ok", "latency_ms": 50},
    "google": {"status": "ok", "latency_ms": 250},
    "openrouter": {"status": "degraded", "latency_ms": 1200},
    "together": {"status": "ok", "latency_ms": 300},
    "nvidia": {"status": "ok", "latency_ms": 180},
    "huggingface": {"status": "ok", "latency_ms": 800},
}


class PerformanceAwareRouter:
    """
    Performance-Aware Router for SupremeAI 2.0
    Routes requests based on cost, latency, and provider health status
    """

    def __init__(self):
        self.providers = [
            {"name": "groq", "cost_per_1k": 0.0001, "quality": 9},
            {"name": "google", "cost_per_1k": 0.00025, "quality": 10},
            {"name": "together", "cost_per_1k": 0.0002, "quality": 8},
            {"name": "openrouter", "cost_per_1k": 0.001, "quality": 9},
            {"name": "nvidia", "cost_per_1k": 0.0005, "quality": 8},
            {
                "name": "huggingface",
                "cost_per_1k": 0.00005,
                "quality": 6,
            },  # Free but slower
        ]

        # Weights for scoring algorithm (can be tuned)
        self.latency_weight = 0.5
        self.cost_weight = 0.3
        self.quality_weight = 0.2

    def _is_provider_healthy(self, provider_name: str) -> bool:
        """Check if a provider is healthy based on health checks"""
        health = PROVIDER_HEALTH.get(provider_name, {"status": "down"})
        return health.get("status") == "ok"

    def _get_provider_latency(self, provider_name: str) -> int:
        """Get current latency for a provider from health checks"""
        health = PROVIDER_HEALTH.get(provider_name)
        if health is None:
            return 9999
        return health["latency_ms"]

    def _calculate_score(self, provider_info: dict, latency_ms: int) -> float:
        """
        Calculate composite score for a provider
        Lower score = better performance
        """
        # If provider is down, return infinity to avoid selecting it
        if not self._is_provider_healthy(provider_info["name"]):
            return float("inf")

        # Normalize metrics to 0-1 scale (0 = best, 1 = worst)
        max_latency = 2000  # ms - assume anything slower is unusable
        max_cost = 0.01  # $0.01 per 1k tokens - reasonable upper bound
        min_quality, max_quality = 1, 10  # Assume quality scores 1-10

        # Normalize latency (0 = best, 1 = worst)
        normalized_latency = min(latency_ms / max_latency, 1.0)

        # Normalize cost (0 = best, 1 = worst)
        normalized_cost = min(provider_info["cost_per_1k"] / max_cost, 1.0)

        # Normalize quality (0 = worst, 1 = best) then invert for scoring
        normalized_quality = (provider_info["quality"] - min_quality) / (
            max_quality - min_quality
        )
        normalized_quality_inverse = (
            1.0 - normalized_quality
        )  # So higher quality = lower score

        # Calculate weighted score
        score = (
            (normalized_latency * self.latency_weight)
            + (normalized_cost * self.cost_weight)
            + (normalized_quality_inverse * self.quality_weight)
        )

        return score

    async def route(self, prompt: str, task_type: str = "general") -> dict[str, Any]:
        """
        Select the best provider based on performance metrics

        Returns:
            Dict containing provider info and selection metadata
        """
        scored_providers = []

        # Score all providers based on current performance
        for provider in self.providers:
            # Get real-time latency from health checks
            latency = self._get_provider_latency(provider["name"])

            # Calculate score
            score = self._calculate_score(provider, latency)
            scored_providers.append((provider, score))

        # Filter out unhealthy providers (score = inf)
        healthy_providers = [(p, s) for p, s in scored_providers if s != float("inf")]

        if not healthy_providers:
            # Fallback to any available provider if all are unhealthy
            healthy_providers = scored_providers
            if not healthy_providers or all(
                s == float("inf") for _, s in healthy_providers
            ):
                raise Exception("No healthy providers available")

        # Sort by score (ascending - lower is better)
        healthy_providers.sort(key=lambda x: x[1])
        best_provider, best_score = healthy_providers[0]

        # Log the decision for monitoring/debugging
        logger.info(
            f"🚀 PERFORMANCE ROUTING: Selected '{best_provider['name']}' "
            f"(score: {best_score:.3f}, latency: {self._get_provider_latency(best_provider['name'])}ms, "
            f"cost: ${best_provider['cost_per_1k']:.5f}/1k, quality: {best_provider['quality']}/10)"
        )

        # Log alternatives for debugging
        if len(healthy_providers) > 1:
            alternatives = [
                f"{p['name']}({s:.3f})"
                for p, s in healthy_providers[1:4]  # Top 3 alternatives
            ]
            logger.debug(f"🔄 Alternatives: {', '.join(alternatives)}")

        return {
            "provider": best_provider["name"],
            "score": best_score,
            "latency_ms": self._get_provider_latency(best_provider["name"]),
            "cost_per_1k": best_provider["cost_per_1k"],
            "quality": best_provider["quality"],
            "alternatives_considered": len(healthy_providers),
        }


# Global instance
performance_router = PerformanceAwareRouter()
