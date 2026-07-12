import asyncio
import logging
import time

from core.messaging.event_bus import ErrorEvent
from core.messaging.event_bus import error_event_bus
from core.health.health_probes import probe_database
from core.health.health_probes import probe_external_api
from core.health.health_probes import probe_redis


logger = logging.getLogger("supremeai.immune_system")


class MaintenancePipeline:
    def __init__(self):
        self.health_score = 100
        self._monitor_task = None
        self.last_recovery_time = 0  # Cooldown tracker
        # Register to listen to error events
        error_event_bus.register_listener(self._handle_error_event)

    def start_monitoring(self):
        """Starts the background probing task."""
        if self._monitor_task is None:
            self._monitor_task = asyncio.create_task(self._monitoring_loop())

    async def _monitoring_loop(self):
        """Background loop that runs zero-cost health probes every 60 seconds."""
        logger.info("🛡️ Immune System: Background monitoring started.")
        while True:
            await asyncio.sleep(60)
            await self.run_health_check()

    async def _handle_error_event(self, event):
        logger.warning(f"🛡️ Immune System received error event: {event.error_type} in {event.module}")
        if event.severity in ("ERROR", "CRITICAL"):
            self.health_score = max(0, self.health_score - 5)
            await self.auto_remediate(event)

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
            logger.warning(f"🛡️ Immune System: Health degraded (Score: {self.health_score}). Triggering circuit breaker event.")
            error_event_bus.emit(
                ErrorEvent(
                    module="maintenance_pipeline",
                    error_type="system.health.degraded",
                    message="System health score dropped below safe threshold.",
                    severity="CRITICAL",
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
            # This would be a real call to a monitoring service like Prometheus/Datadog
            # For demonstration, we use a placeholder function.
            # p95_latency = await monitoring_service.get_p95_latency("api_main_router")
            p95_latency = 150  # Simulated latency in ms

            LATENCY_THRESHOLD_MS = 100

            if p95_latency > LATENCY_THRESHOLD_MS:
                logger.critical(f"🚨 Performance Regression Detected! p95 latency ({p95_latency}ms) exceeds threshold ({LATENCY_THRESHOLD_MS}ms).")
                # In a real scenario, this would trigger a GitHub Actions workflow to rollback the deployment.
                # For example: call_github_action_webhook("rollback_deployment")

        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to run performance regression check: {e}")

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
                            )
                        )
                        self.last_recovery_time = current_time
                        self.health_score = min(100, self.health_score + 30)
                    except Exception as e:  # noqa: BLE001
                        logger.error(f"Failed to switch provider: {e}")

            if "redis" in str(event.context).lower() or event.error_type == "redis_connection_lost":
                logger.info("🚑 Auto-Recovery: Redis degraded. Attempting to re-initialize pool.")
                try:
                    await redis_manager.close()
                    # Re-init would happen here depending on the manager implementation
                    # e.g., await redis_manager.connect()
                    self.last_recovery_time = current_time
                    self.health_score = min(100, self.health_score + 20)
                except Exception as e:  # noqa: BLE001
                    logger.error(f"Failed to recover Redis: {e}")

        logger.info("🚑 Remediation cycle completed.")


# Global instance for easy import and singleton usage
maintenance_pipeline = MaintenancePipeline()
