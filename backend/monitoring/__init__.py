"""Real-Time Zero-Cost Observability & Error Collector Engine.

বাংলা মন্তব্য: এই মডিউলটি প্রোডাকশনের ব্যাকএন্ড এরর, পারফরম্যান্স লেটেন্সি এবং সিস্টেম হেলথ মেট্রিক্স
Sentry (Free Tier) এবং OpenTelemetry/Loguru দিয়ে সেন্ট্রালি ট্র্যাক করে। Zero silent failure নিশ্চিত করে।
"""

import os
import time
from typing import Any

from loguru import logger

_sentry_initialized = False


def init_observability():
    """
    বাংলা মন্তব্য: Sentry SDK ইনিশিয়ালাইজ করে যদি SENTRY_DSN এনভায়রনমেন্ট ভ্যারিয়েবলে উপলব্ধ থাকে।
    """
    global _sentry_initialized
    dsn = os.getenv("SENTRY_DSN")
    if dsn and not _sentry_initialized:
        try:
            import sentry_sdk
            from sentry_sdk.integrations.fastapi import FastApiIntegration
            from sentry_sdk.integrations.logging import LoggingIntegration

            sentry_sdk.init(
                dsn=dsn,
                traces_sample_rate=0.2,  # 20% performance tracing to stay in free tier limits
                profiles_sample_rate=0.1,
                integrations=[
                    FastApiIntegration(transaction_style="url"),
                    LoggingIntegration(level=None, event_level=None),
                ],
                environment=os.getenv("ENV", "production"),
            )
            _sentry_initialized = True
            logger.info(
                "📡 [Observability] Sentry Real-Time Error Tracking Initialized."
            )
        except Exception as e:
            logger.warning(f"⚠️ [Observability] Failed to initialize Sentry: {e}")
    else:
        logger.info(
            "ℹ️ [Observability] Running with Loguru central logging (SENTRY_DSN not configured or already active)."
        )


def track_exception(error: Exception, context: dict[str, Any] | None = None):
    """
    বাংলা মন্তব্য: যেকোনো ব্যাকএন্ড এক্সেপশন রিয়েল-টাইমে সেনট্রি বা লগ মডিউলে রেজিস্টার করে।
    """
    logger.error(f"❌ [Error Tracked] {error} | Context: {context or {}}")
    if _sentry_initialized:
        try:
            import sentry_sdk

            with sentry_sdk.push_scope() as scope:
                if context:
                    for k, v in context.items():
                        scope.set_extra(k, v)
                sentry_sdk.capture_exception(error)
        except Exception as e:
            logger.warning(f"⚠️ Error forwarding exception to Sentry: {e}")


class PerformanceTimer:
    """
    বাংলা মন্তব্য: লেটেন্সি এবং এক্সিকিউশন টাইম মাপার জন্য পারফরম্যান্স টাইমার মডিউল।
    """

    def __init__(self, operation_name: str):
        self.operation_name = operation_name
        self.start_time = 0.0

    def __enter__(self):
        self.start_time = time.time()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        duration = round((time.time() - self.start_time) * 1000, 2)
        if exc_type:
            logger.error(
                f"⏱️ [Metrics] {self.operation_name} FAILED after {duration}ms: {exc_val}"
            )
            track_exception(
                exc_val, {"operation": self.operation_name, "duration_ms": duration}
            )
        else:
            logger.info(f"⏱️ [Metrics] {self.operation_name} completed in {duration}ms")
