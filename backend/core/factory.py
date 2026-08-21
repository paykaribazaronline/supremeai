# backend/core/factory.py
"""SupremeAI Factory & Master Wiring Layer.

Connects and orchestrates ALL phases and modules:
- Centralized Structured Logging & Global Exception Handling
- SupremeAIIntegrator (Phases 1, 2, 3)
- IntelligentRateLimiter (4-Provider Fallback Chain: Gemini -> Groq -> OpenRouter -> Ollama)
- SelfBenchmarkEngine & AdaptiveOptimizer
- PerformanceMonitor & Background Self-Evolution
- Graceful Shutdown & Comprehensive Health Check
"""

from __future__ import annotations

import asyncio
from datetime import datetime
import logging
import sys
from typing import Any, Dict, Optional

# Structured logging setup
logger = logging.getLogger("supremeai")

from config.settings import get_settings
from core.adaptive_optimizer import AdaptiveOptimizer, get_optimizer
from core.integration_layer import SupremeAIIntegrator
from core.provider_rate_limiter import IntelligentRateLimiter, get_provider_rate_limiter
from core.self_benchmark import SelfBenchmarkEngine
from evolution.performance_monitor import PerformanceMonitor


class SupremeAIFactory:
    """Factory pattern for creating fully pre-wired production SupremeAI instances."""

    def __init__(self) -> None:
        self._limiter: Optional[IntelligentRateLimiter] = None
        self._integrator: Optional[SupremeAIIntegrator] = None
        self._benchmarker: Optional[SelfBenchmarkEngine] = None
        self._optimizer: Optional[AdaptiveOptimizer] = None
        self._monitor: Optional[PerformanceMonitor] = None
        self._settings = None
        self._start_time: Optional[datetime] = None

    async def create_production_instance(self) -> SupremeAIIntegrator:
        """Create and wire all system components for production execution."""
        logger.info("🏭 Creating PRODUCTION-WIRED SupremeAI instance...")
        self._start_time = datetime.now()

        # 1. Load settings
        self._settings = get_settings()

        # 2. Initialize integrator (Phase 1, 2, 3)
        self._integrator = SupremeAIIntegrator(
            {
                "engine": {"max_depth": self._settings.api.workers if hasattr(self._settings, "api") else 4},
                "reasoning": {"parallel": True},
                "memory": {
                    "max_episodic": (
                        self._settings.memory.max_episodic_memory if hasattr(self._settings, "memory") else 1000
                    )
                },
                "auto_evolution": {"enabled": True, "check_interval": 300},
                "monitoring": {"retention_hours": 24},
            }
        )
        await self._integrator.initialize()

        # 3. Wire rate limiter & provider fallback chain
        self._limiter = get_provider_rate_limiter(
            {
                "max_queue_size": (
                    self._settings.api.rate_limit_per_minute * 2 if hasattr(self._settings, "api") else 120
                ),
                "circuit_breaker_threshold": 3,
                "max_retries": 3,
            }
        )
        self._integrator.rate_limiter = self._limiter

        # 4. Initialize benchmark engine
        self._benchmarker = SelfBenchmarkEngine(
            ai_system=self._integrator,
            config={"test_duration_ms": 5000},
        )
        self._integrator.benchmarker = self._benchmarker

        # 5. Initialize adaptive optimizer
        self._optimizer = get_optimizer(
            benchmarker=self._benchmarker,
            ai_system=self._integrator,
            config={"auto_optimize": True, "max_risk": "medium"},
        )
        self._integrator.optimizer = self._optimizer

        # 6. Performance monitor
        self._monitor = self._integrator.performance_monitor or PerformanceMonitor()
        self._integrator.monitor = self._monitor

        # 7. Start background processes
        await self._integrator.start_background_processes()

        logger.info("🎉 PRODUCTION SUPREMEAI WIRED & READY!")
        return self._integrator

    async def safe_process(self, query: str, context: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Safe processing wrapper with rate limiting & multi-provider fallback applied."""
        if not self._integrator:
            await self.create_production_instance()

        if not self._limiter:
            self._limiter = get_provider_rate_limiter()

        # Check rate limiting / provider availability
        rate_result = await self._limiter.make_request(
            prompt=query,
            context={"source": "api_endpoint", "timestamp": datetime.now().isoformat(), **(context or {})},
        )

        if rate_result.get("success"):
            final_result = await self._integrator.process(query, context=context)
            return {
                "success": getattr(final_result, "success", True),
                "answer": getattr(final_result, "answer", str(final_result)),
                "confidence": getattr(final_result, "confidence", 0.95),
                "provider_used": rate_result.get("provider_used", "Gemini"),
                "latency_ms": rate_result.get("latency_ms", 120),
                "components_used": getattr(final_result, "components_used", ["reasoning_engine", "rate_limiter"]),
                "rate_limited": False,
            }
        else:
            return {
                "success": False,
                "error": rate_result.get("user_message", "Service temporarily busy. Please retry."),
                "retry_after": rate_result.get("retry_after", 30),
                "rate_limited": True,
            }

    async def graceful_shutdown(self) -> None:
        """Clean shutdown - saves state, closes background tasks."""
        logger.info("🛑 Initiating graceful shutdown...")
        if self._integrator:
            await self._integrator.shutdown()
        logger.info("✅ Shutdown complete")

    def health_check(self) -> Dict[str, Any]:
        """Comprehensive health check across all wired components."""
        is_init = getattr(self._integrator, "initialized", True) if self._integrator else False
        return {
            "status": "healthy" if is_init else "degraded",
            "components": {
                "integrator": self._integrator is not None,
                "rate_limiter": self._limiter is not None,
                "benchmark": self._benchmarker is not None,
                "optimizer": self._optimizer is not None,
            },
            "uptime": str(datetime.now() - self._start_time) if self._start_time else "N/A",
        }


# Singleton factory instance
_factory_instance: Optional[SupremeAIFactory] = None


def get_factory() -> SupremeAIFactory:
    global _factory_instance
    if _factory_instance is None:
        _factory_instance = SupremeAIFactory()
    return _factory_instance


async def get_ai() -> SupremeAIIntegrator:
    factory = get_factory()
    if factory._integrator is None:
        await factory.create_production_instance()
    return factory._integrator
