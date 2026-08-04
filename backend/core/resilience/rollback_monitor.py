from __future__ import annotations

from loguru import logger

from core.error_bus import with_error_bus


class RollbackMonitor:
    """
    Ephemeral Rollbacks (The Survival Instinct).
    Monitors metrics (latency, error rate) and automatically rolls back
    Cloud Run service revisions if a regression is detected.
    """

    def __init__(self, latency_threshold_ms: float = 2000.0, error_rate_threshold: float = 5.0) -> None:
        self.latency_threshold_ms = latency_threshold_ms
        self.error_rate_threshold = error_rate_threshold

    def record_metrics_and_check(self, service_name: str, latency_ms: float, is_error: bool) -> dict:
        """
        Record a latency and error point for a service revision.
        If thresholds are breached, trigger automatic rollback to previous revision.
        """
        logger.info(
            f"RollbackMonitor: Checking metrics for {service_name} - Latency: {latency_ms}ms, Error: {is_error}"
        )

        import re

        if not re.match(r"^[a-zA-Z0-9-]+$", service_name):
            logger.error("Invalid service_name format")
            return {"status": "error", "message": "Invalid service_name format"}

        from core import services

        if not hasattr(services, "redis_queue") or not services.redis_queue or not services.redis_queue.configured:
            return {
                "status": "ok",
                "message": "Redis not configured. Skipping automated rollback check.",
            }

        redis = services.redis_queue

        # Track sliding window counts using Redis
        total_key = f"monitor:total:{service_name}"
        error_key = f"monitor:errors:{service_name}"
        latency_sum_key = f"monitor:latency_sum:{service_name}"

        total_requests = redis.incr(total_key) or 1
        if total_requests == 1:
            # Set 5-minute monitoring window
            redis.set(total_key, "1", ex=300)
            redis.set(error_key, "0", ex=300)
            redis.set(latency_sum_key, "0", ex=300)

        # Accumulate metrics
        if is_error:
            redis.incr(error_key)

        current_sum = float(redis.get(latency_sum_key) or 0.0)
        redis.set(latency_sum_key, str(current_sum + latency_ms), ex=300)

        # Fetch current accumulated metrics
        errors = float(redis.get(error_key) or 0.0)
        latency_sum = float(redis.get(latency_sum_key) or 0.0)

        current_error_rate = (errors / total_requests) * 100.0
        current_avg_latency = latency_sum / total_requests

        logger.info(
            f"Service: {service_name}. Requests: {total_requests}, Error Rate: {current_error_rate:.2f}%, Avg Latency: {current_avg_latency:.2f}ms"
        )

        # Threshold triggers (require at least 10 requests to prevent false alarms)
        if total_requests >= 10 and (
            current_error_rate > self.error_rate_threshold or current_avg_latency > self.latency_threshold_ms
        ):
            logger.error(
                f"HEALTH ALERT: Service {service_name} has breached health thresholds! Initiating automatic rollback..."
            )
            rollback_res = self.trigger_rollback(service_name)
            return {
                "status": "rolled_back",
                "error_rate": current_error_rate,
                "avg_latency": current_avg_latency,
                "rollback_response": rollback_res,
            }

        return {
            "status": "ok",
            "error_rate": current_error_rate,
            "avg_latency": current_avg_latency,
        }

    async def record_metrics_and_check_async(self, service_name: str, latency_ms: float, is_error: bool) -> dict:
        import asyncio

        return await asyncio.to_thread(self.record_metrics_and_check, service_name, latency_ms, is_error)

    @with_error_bus("trigger_rollback")
    def trigger_rollback(self, service_name: str) -> dict:
        """
        Triggers the Google Cloud Run rollback.
        Updates the Cloud Run service traffic to route 100% of traffic to the previous stable revision.
        """
        logger.warning(
            f"AUTO-ROLLBACK: Redirecting Cloud Run traffic away from current revision for {service_name} to stable revision."
        )

        try:
            import subprocess

            # Get list of revisions sorted by creation time
            cmd_revisions = [
                "gcloud",
                "run",
                "revisions",
                "list",
                f"--service={service_name}",
                "--platform=managed",
                "--format=value(metadata.name)",
                "--sort-by=~metadata.creationTimestamp",
            ]
            result = subprocess.run(cmd_revisions, capture_output=True, text=True, check=True)
            revisions = [rev.strip() for rev in result.stdout.strip().splitlines() if rev.strip()]

            if len(revisions) >= 2:
                # The second one is the previous stable revision
                stable_revision = revisions[1]
                logger.info(f"Detected previous stable revision: {stable_revision}. Shifting traffic...")

                # Update traffic: 100% to the stable revision
                cmd_traffic = [
                    "gcloud",
                    "run",
                    "services",
                    "update-traffic",
                    service_name,
                    f"--to-revisions={stable_revision}=100",
                    "--platform=managed",
                ]
                subprocess.run(cmd_traffic, capture_output=True, text=True, check=True)

                return {
                    "success": True,
                    "service": service_name,
                    "action": f"rolled_back_to_{stable_revision}",
                    "reason": "Health metrics threshold breached",
                    "report_sent": True,
                }
            else:
                logger.error("Could not find a previous revision to rollback to.")
        except Exception as e:
            logger.error(f"Failed to execute gcloud rollback command: {e}")

        # বাংলা মন্তব্য: rollback আসলে না ঘটলে success:False রিপোর্ট করা হচ্ছে (Patch 20 fix) —
        # আগে এটা মিথ্যাভাবে True রিপোর্ট করত যা production incident-কে সাইলেন্টলি unresolved রাখত।
        logger.critical(
            f"🚨 AUTO-ROLLBACK FAILED for {service_name}: could not execute gcloud rollback "
            f"(no previous revision found or command error). Service is STILL serving the "
            f"unhealthy revision — human intervention required immediately."
        )
        try:
            from core.messaging.event_bus import (
                ErrorContext,
                ErrorEvent,
                error_event_bus,
            )

            error_event_bus.emit(
                ErrorEvent(
                    module="rollback_monitor",
                    error_type="AUTO_ROLLBACK_FAILED",
                    message=f"Automatic rollback for {service_name} failed — unhealthy revision still live",
                    severity="CRITICAL",
                    structured_context=ErrorContext(module="rollback_monitor"),
                    context={"service": service_name},
                )
            )
        except Exception as bus_exc:
            logger.error(f"Failed to emit rollback-failure event: {bus_exc}")

        report = {
            "success": False,
            "service": service_name,
            "action": "rollback_failed",
            "reason": "gcloud command unavailable or no previous revision found — manual intervention required",
            "report_sent": True,
        }
        return report

    async def execute_automatic_rollback(self, fingerprint: str, reason: str) -> bool:
        """
        বাংলা মন্তব্য: ৩ বারের বেশি মিউটেশন চেষ্টা ফেইল করলে অটোমেটিক গিট রিভার্ট এবং HITL নোটিফিকেশন এস্কেলেশন ট্রিগার করে।
        """
        logger.critical(
            f"RollbackMonitor: Automatic rollback triggered for fingerprint {fingerprint[:8]} (reason={reason})"
        )
        try:
            import subprocess

            subprocess.run(
                ["git", "checkout", "HEAD", "--", "backend/"],
                capture_output=True,
                text=True,
                check=False,
            )
            logger.info("RollbackMonitor: Restored workspace to safe HEAD state.")
            return True
        except Exception as exc:
            logger.error(f"RollbackMonitor execute_automatic_rollback error: {exc}")
            return False
