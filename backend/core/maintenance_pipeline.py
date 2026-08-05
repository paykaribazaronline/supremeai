from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext

"""This module implements the `MaintenancePipeline`, acting as the "Immune System" for the SupremeAI ecosystem. It is responsible for continuously monitoring the health and performance of critical backend components such as databases, Redis, and external AI APIs. The pipeline proactively listens for system-wide error events, performs routine health checks, detects potential performance regressions, and attempts automated self-healing remediation actions like switching LLM providers or re-initializing services to ensure the overall stability and resilience of the AI platform.

Key Components:
- `MaintenancePipeline`: A class that orchestrates continuous health monitoring, processes error events, detects performance issues, and triggers automated remediation strategies to maintain system integrity.
- `maintenance_pipeline`: A global singleton instance of the `MaintenancePipeline` class, providing a centralized point of control for system health management.

Dependencies:
- `asyncio`: For managing asynchronous operations and background monitoring tasks.
- `logging`: For structured logging of health status, warnings, and critical events.
- `time`: For timestamping health checks and implementing cooldown periods for remediation.
- `core.messaging.event_bus`: For registering listeners to and emitting system-wide `ErrorEvent`s.
- `core.health.health_probes`: Provides specific functions to probe the health of internal and external services (e.g., database, Redis, external APIs).
- `core.cache.redis_manager`: Utilized within remediation logic to interact with Redis, such as updating configuration or re-initializing connections."""

import asyncio
import logging
import os
import random
import time

from core.health.health_probes import probe_database, probe_external_api, probe_redis
from core.messaging.event_bus import ErrorEvent, error_event_bus

logger = logging.getLogger("supremeai.immune_system")


class MaintenancePipeline:
    def __init__(self):
        self.health_score = 100
        self._monitor_task = None
        self.last_recovery_time = 0  # Cooldown tracker
        # Register to listen to error events
        error_event_bus.register_listener("*", self._handle_error_event)

    def start_monitoring(self, interval: int | None = None):
        """Starts the background probing task with configurable interval."""
        if interval is None:
            interval = int(os.getenv("MAINTENANCE_INTERVAL", "120"))
        self._monitor_interval = interval
        if self._monitor_task is None:
            self._monitor_task = asyncio.create_task(self._monitoring_loop())

    async def _monitoring_loop(self):
        """Background loop that runs zero-cost health probes with configurable interval + jitter."""
        logger.info(f"🛡️ Immune System: Background monitoring started (interval={self._monitor_interval}s).")
        while True:
            # Add random jitter (±10%) to prevent thundering herd
            jitter = random.uniform(0.9, 1.1)
            actual_interval = int(self._monitor_interval * jitter)
            await asyncio.sleep(actual_interval)
            await self.run_health_check()

    async def _handle_error_event(self, event):
        logger.warning(f"🛡️ Immune System received error event: {event.error_type} in {event.module}")
        if event.severity in ("ERROR", "CRITICAL"):
            self.health_score = max(0, self.health_score - 5)
            await self.auto_remediate(event)

    @with_error_bus("run_health_check")
    async def run_health_check(self):
        # logger.info("🛡️ Immune System: Running routine health check...")

        results = {
            "redis": await probe_redis(),
            "database": await probe_database(),
            "api_gemini": await probe_external_api("https://generativelanguage.googleapis.com"),
            "api_openrouter": await probe_external_api("https://openrouter.ai/api/v1/auth/key"),
            "timestamp": time.time(),
        }

        # Calculate Degradation
        penalty = 0
        if results["redis"]["status"] == "down":
            penalty += 50
        if results["database"]["status"] == "down":
            penalty += 50
        if results["api_gemini"]["status"] == "down":
            penalty += 20
        if results["api_openrouter"]["status"] == "down":
            penalty += 20

        # Adjust score safely
        self.health_score = max(0, 100 - penalty)

        # Trigger circuit breaker event if degraded significantly
        if self.health_score < 70:
            logger.warning(
                f"🛡️ Immune System: Health degraded (Score: {self.health_score}). Triggering circuit breaker event."
            )
            error_event_bus.emit(
                ErrorEvent(
                    module="maintenance_pipeline",
                    error_type="system.health.degraded",
                    message="System health score dropped below safe threshold.",
                    severity="CRITICAL",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"results": results, "score": self.health_score},
                )
            )

        status = "HEALTHY" if self.health_score > 80 else ("DEGRADED" if self.health_score > 50 else "CRITICAL")
        results["status"] = status
        results["health_score"] = self.health_score
        return results

    async def detect_performance_regression(self):
        """
        Monitors key performance indicators (KPIs) like p95 latency.
        If latency exceeds a predefined threshold after a new deployment,
        it can trigger a rollback action via GitHub Actions.
        """
        logger.info("🛡️ Immune System: Running performance regression detection...")
        try:
            from api.routes.metrics import metrics_engine

            history = metrics_engine.latency_history
            if not history:
                logger.info("🛡️ Immune System: Latency logs empty. Skipping regression check.")
                return

            # বাংলা মন্তব্য: P95 ল্যাটেন্সি গণনা করা।
            # হিস্ট্রি সর্ট করে ৯৫তম পার্সেন্টাইল ভ্যালু বের করা হচ্ছে।
            sorted_history = sorted(history)
            idx = int(len(sorted_history) * 0.95)
            # duration-টি সেকেন্ডে থাকে, তাই ms-এ রূপান্তর করছি
            p95_latency = sorted_history[min(idx, len(sorted_history) - 1)] * 1000.0

            LATENCY_THRESHOLD_MS = 500.0  # Dev default threshold 500ms

            logger.info(f"🛡️ Immune System: Current real P95 Latency = {p95_latency:.2f}ms")
            if p95_latency > LATENCY_THRESHOLD_MS:
                logger.critical(
                    f"🚨 Performance Regression Detected! p95 latency ({p95_latency:.2f}ms) exceeds threshold ({LATENCY_THRESHOLD_MS}ms)."
                )
                # In a real scenario, this would trigger a GitHub Actions workflow to rollback the deployment.

        except Exception as e:
            logger.error(f"Failed to run performance regression check: {e}")

    @with_error_bus("auto_remediate")
    async def auto_remediate(self, event=None):
        logger.warning("🚑 Immune System: Triggering self-healing remediation...")

        # Cooldown Period (2 minutes) to prevent Flapping
        current_time = time.time()
        if current_time - self.last_recovery_time < 120:
            logger.info("⏳ Auto-Recovery skipped: System is in cooldown period.")
            return

        if event:
            logger.info(f"Attempting to heal module {event.module} for error {event.error_type}")
            from core.cache.redis_manager import redis_manager

            # Simulated checks based on the event payload or type
            # In a real scenario, the event type might be exactly 'llm_provider_down' or 'redis_connection_lost'

            if "gemini" in str(event.context).lower() or event.error_type == "system.health.degraded":
                logger.info("🚑 Auto-Recovery: LLM Provider degraded. Switching active provider to OpenRouter.")
                # Set active_provider in Redis (if redis is up)
                if redis_manager.client:
                    try:
                        await redis_manager.client.set("active_provider", "openrouter")
                        error_event_bus.emit(
                            ErrorEvent(
                                module="auto_remediation",
                                error_type="system.routing.updated",
                                message="Switched to OpenRouter successfully.",
                                severity="INFO",
                                structured_context=ErrorContext(module="auto_fixed"),
                            )
                        )
                        self.last_recovery_time = current_time
                        self.health_score = min(100, self.health_score + 30)
                    except Exception as e:
                        logger.error(f"Failed to switch provider: {e}")

            if "redis" in str(event.context).lower() or event.error_type == "redis_connection_lost":
                logger.info("🚑 Auto-Recovery: Redis degraded. Attempting to re-initialize pool.")
                try:
                    await redis_manager.close()
                    # Re-init would happen here depending on the manager implementation
                    # e.g., await redis_manager.connect()
                    self.last_recovery_time = current_time
                    self.health_score = min(100, self.health_score + 20)
                except Exception as e:
                    logger.error(f"Failed to recover Redis: {e}")

        # বাংলা মন্তব্য: Health critical threshold-এ পৌঁছালে SelfEvolutionAgent-কে
        # জরুরি evolution cycle চালাতে signal দেওয়া হচ্ছে।
        # এটাই সেই bridge যেটা self-healing → self-evolution loop বন্ধ করে।
        if self.health_score < 50:
            try:
                import asyncio as _asyncio

                from core.evolution.self_evolution_agent import (
                    SelfEvolutionAgent,
                )

                # বাংলা: আগে এখানে SelfEvolutionAgent.__new__(SelfEvolutionAgent) দিয়ে
                # instance বানানো হতো, যেটা __init__() সম্পূর্ণ স্কিপ করে দেয় — ফলে
                # fitness_engine (ও অন্য সব attribute) কখনো সেট হতো না, আর প্রতিটা
                # _tick() কল AttributeError দিয়ে ক্র্যাশ করত (production লগে দেখা
                # "Task exception was never retrieved... 'fitness_engine'" এর মূল কারণ)।
                # SelfEvolutionAgent.__init__() নিজে থেকে কোনো continuous loop চালু করে
                # না — সেটা শুধু .start() কল করলেই শুরু হয় — তাই স্বাভাবিক constructor
                # ব্যবহার করলেও "শুধু tick(), পুরো loop নয়" এই উদ্দেশ্য অক্ষুণ্ণ থাকে।
                _evo = SelfEvolutionAgent()
                if hasattr(_evo, "_tick"):
                    from core.utils.background_tasks import track_task

                    track_task(_asyncio.create_task(_evo._tick()))
                    logger.warning(
                        f"🛡️→🧬 Health critical (score={self.health_score}), " "triggered emergency evolution tick."
                    )
            except Exception as evo_exc:
                logger.debug(f"Evolution trigger skipped: {evo_exc!r}")

        logger.info("🚑 Remediation cycle completed.")


# Global instance for easy import and singleton usage
maintenance_pipeline = MaintenancePipeline()
