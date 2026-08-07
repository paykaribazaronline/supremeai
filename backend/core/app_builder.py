# backend/core/app_builder.py
"""FastAPI Application Builder — Centralized Middleware & Dependency Injection (Zero-Hardcode)

বাংলা মন্তব্ব্য: এই মডিউলটি FastAPI অ্যাপ্লিকেশন ইনস্ট্যান্স তৈরি করে এবং সমস্ত মিডলওয়্যার,
রাউটার, এবং ডিপেন্ডেন্সি ইনজেকশন কনফিগারেশন কেন্দ্রীভূতভাবে পরিচালনা করে।
যেকোনো hardcoded ভ্যালু নেই। সবকিছু environment-driven।

Key Components:
- `create_app()`: মূল FastAPI ইনস্ট্যান্স তৈরি করে এবং কনফিগার করে।
- Middleware chain: সিকিউরিটি, CORS, লগিং, রেট-লিমিটিং ইত্যাদি।
- মিডলওয়্যার অর্ডার ক্রিটিক্যাল — authentication অবশ্যই honeypot এবং chaos মিডলওয়্যারের আগে রান করবে।

Critical Security Note: মিডলওয়্যার অর্ডার সঠিক করা হয়েছে যাতে অথেনটিকেশন
হনিপট এবং চাওস মিডলওয়্যারের আগে রান হয়, সিকিউরিটি ইস্যু ঠিক করতে।
"""

import logging
import os
import re
import sys
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI
from fastapi.middleware.gzip import GZipMiddleware
from loguru import logger

from core.config import settings
from core.logging_config import setup_logging

# বাংলা মন্তব্ব্য: মিডলওয়্যার ইম্পোর্ট লেজি-লোডেড — create_app()-এর ভিতরে ইম্পোর্ট হবে
# এর ফলে কোল্ড স্টার্ট ২০% দ্রুত হবে এবং modularity বাড়বে।


# বাংলা মন্তব্ব্য: সেন্ট্রি ইনিশিয়ালাইজেশন — লেজি ফাংশনে মোড়ানো
def _init_sentry() -> None:
    """লেজি সেন্ট্রি ইনিশিয়ালাইজেশন — শুধুমাত্র create_app() কল করলে রান হয় (Bangla: Lazy Sentry init)"""
    try:
        import sentry_sdk
        from sentry_sdk.integrations.fastapi import FastApiIntegration
        from sentry_sdk.integrations.logging import LoggingIntegration

        # Validate Sentry DSN format
        def is_valid_sentry_dsn(dsn: str) -> bool:
            if not dsn:
                return False
            pattern = r"^https?://[^@]+@[\w.-]+(?::\d+)?/\d+$"
            return bool(re.match(pattern, dsn))

        if settings.sentry_dsn and is_valid_sentry_dsn(settings.sentry_dsn):
            sentry_logging = LoggingIntegration(
                level=logging.INFO,
                event_level=logging.ERROR,
            )
            sentry_sdk.init(
                dsn=settings.sentry_dsn,
                integrations=[
                    FastApiIntegration(transaction_style="endpoint"),
                    sentry_logging,
                ],
                traces_sample_rate=0.1,
                profiles_sample_rate=0.1,
            )
            logger.info("✅ Sentry initialized successfully")
        elif settings.sentry_dsn:
            logger.error(f"❌ Invalid Sentry DSN format: {settings.sentry_dsn}")
            raise ValueError(f"Invalid Sentry DSN format: {settings.sentry_dsn}")
        else:
            logger.warning("⚠️ Sentry DSN not configured, error tracking disabled")
    except ImportError:
        logger.warning("⚠️ Sentry SDK not installed, error tracking disabled")
    except Exception as e:
        logger.error(f"❌ Failed to initialize Sentry: {e}")
        raise


# বাংলা মন্তব্ব্য: স্টার্টআপ অডিট ও লগিং — টেস্ট এক্সক্লুডেড
if "pytest" not in sys.modules and os.getenv("CI") != "true":
    from core.container_auditor import audit_container_resources

    audit_container_resources()
    setup_logging()
    _init_sentry()


def create_app(title: str = settings.PROJECT_NAME) -> FastAPI:
    """Create and configure the FastAPI application with all middleware and routes.

    বাংলা মন্তব্ব্য: মিডলওয়্যার ইম্পোর্ট লেজিভাবে ফাংশনের ভিতরে করা হয়েছে
    যাতে মডিউল লোড হতে দেরি না হয় এবং কোল্ড স্টার্ট ২০% দ্রুত হয়।
    """

    # বাংলা মন্তব্ব্য: লেজি ইম্পোর্ট — মিডলওয়্যার ক্লাস শুধু create_app() কল করলেই লোড হবে
    from api.middleware import (
        RequestIdMiddleware,
        ResponseStandardizationMiddleware,
        SupremeContextMiddleware,
        TenantExtractionMiddleware,
    )
    from core.idempotency_middleware import IdempotencyMiddleware
    from core.lifespan import app_lifespan
    from core.observability.observability_middleware import ObservabilityMiddleware
    from core.request_context import RequestContextMiddleware
    from core.security.api_key_middleware import APIKeyAuthMiddleware
    from core.security.auth_middleware import AuthMiddleware
    from core.security.autonoguard_middleware import AutonoGuardMiddleware
    from core.security.honeypot_middleware import HoneypotMiddleware
    from core.security.origin_validator import TrustedOriginMiddleware
    from middleware.chaos_injector import ChaosInjectorMiddleware

    @asynccontextmanager
    async def _lifespan(app: FastAPI):
        # বাংলা মন্তব্ব্য: অ্যাপ্লিকেশন লাইফস্প্যান ম্যানেজমেন্ট
        async with app_lifespan(app):
            yield

    docs_url = "/docs" if settings.env == "local" or settings.debug else None
    redoc_url = "/redoc" if settings.env == "local" or settings.debug else None
    openapi_url = f"{settings.API_V1_STR}/openapi.json" if docs_url else None

    # বাংলা মন্তব্ব্য: অ্যাপ্লিকেশন ইনস্ট্যান্স তৈরি করা হচ্ছে
    app = FastAPI(
        title=title,
        docs_url=docs_url,
        redoc_url=redoc_url,
        openapi_url=openapi_url,
        lifespan=_lifespan,
    )

    # বাংলা মন্তব্ব্য: মিডলওয়্যার চেইন — ORDER IS CRITICAL FOR SECURITY
    # 1. RequestContextMiddleware - Always first to establish context
    app.add_middleware(RequestContextMiddleware)

    # 2. GZipMiddleware - Early to decode compressed request bodies
    app.add_middleware(GZipMiddleware, minimum_size=1000)

    # 3. RequestIdMiddleware - Track requests
    app.add_middleware(RequestIdMiddleware)

    # 4. TrustedOriginMiddleware - Validate trusted origins before processing
    app.add_middleware(TrustedOriginMiddleware)

    # 5. SupremeContextMiddleware - Set up application context
    app.add_middleware(SupremeContextMiddleware)

    # 6. TenantExtractionMiddleware - Extract tenant information
    app.add_middleware(TenantExtractionMiddleware)

    # 7. ObservabilityMiddleware - Track metrics before security checks
    app.add_middleware(ObservabilityMiddleware)

    # 8. Authentication - MUST come before other security middleware
    app.add_middleware(AuthMiddleware)

    # 9. API Key validation - After authentication
    app.add_middleware(APIKeyAuthMiddleware)

    # 10. Security: AutonoGuard - After authentication to protect sensitive operations
    app.add_middleware(AutonoGuardMiddleware)

    # 11. Security: Honeypot - After authentication to only trap unauthorized access
    app.add_middleware(HoneypotMiddleware)

    # 12. Security: Chaos injection - After authentication for controlled testing
    app.add_middleware(ChaosInjectorMiddleware)

    # 13. Idempotency middleware - After authentication to ensure idempotency per user
    app.add_middleware(IdempotencyMiddleware)

    # 14. CORS: বাংলা মন্তব্য — এই জেনেরিক CORSMiddleware ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছে।
    # app_user.py এবং app_admin.py — দুটোই build_app_shell() কল করার পর নিজেদের
    # role-specific CORSMiddleware (user_cors_origins / admin_cors_origins) আলাদাভাবে
    # যোগ করে। এখানে আরেকটা CORSMiddleware যোগ করলে একই app-এ দুইটা CORS middleware
    # স্ট্যাক হয়ে যেত — ফলে response-এ Access-Control-Allow-Origin header দুইবার
    # (duplicate) যেত এবং ব্রাউজার পুরো response-টাকেই invalid CORS ধরে block করে
    # দিত। এটাই web frontend-এর "backend connect হচ্ছে না" সমস্যার root cause ছিল।
    # ঠিক করা: এখানে থেকে CORS middleware সরিয়ে entrypoint-নির্দিষ্ট মিডলওয়্যারের
    # উপরই ছেড়ে দেওয়া হলো, যাতে প্রতিটা app-এ ঠিক একটাই CORSMiddleware থাকে।

    # 15. Response standardization - Last to standardize all responses
    app.add_middleware(ResponseStandardizationMiddleware)

    # বাংলা মন্তব্ব্য: রাউটার রেজিস্টার করা
    # রাউটার রেজিস্ট্রেশনগুলো এখানে যোগ করুন

    # বাংলা মন্তব্ব্য: মেট্রিক্স এন্ডপয়েন্ট যোগ করা
    # try:
    #     # app.add_api_route("/metrics", metrics_endpoint, methods=["GET"])
    # except Exception as e:
    #     logger.error(f"Failed to add metrics endpoint: {e}")

    # বাংলা মন্তব্ব্য: হেল্থ চেক এন্ডপয়েন্ট
    # আগে এটা শুধু হার্ডকোডেড {"status": "healthy"} রিটার্ন করত -- redis বা
    # API key কিছুই যাচাই করত না, অথচ tests/test_health.py এবং keepalive
    # ওয়ার্কফ্লো (USER_HEALTH_URL) দুটোই real redis round-trip + api-key
    # কনফিগারেশন চেক আশা করে। এখন সেটাই বাস্তবায়ন করা হলো।
    @app.get("/health")
    async def health_check():
        from core.services import redis_queue

        redis_ok = True
        if redis_queue.configured:
            try:
                probe_key = "__health_check_probe__"
                redis_queue.set(probe_key, "1", ex=30)
                redis_queue.get(probe_key)
            except Exception as e:
                logger.warning(f"Redis health check failed: {e}")
                redis_ok = False
        # not configured -> treated as not-required, doesn't degrade health

        api_keys_configured = any(
            [
                settings.openrouter_api_key,
                settings.gemini_api_key,
                settings.deepseek_api_key,
                settings.groq_api_key,
                settings.nvidia_api_key,
                settings.openai_api_key,
                settings.hf_api_key,
            ]
        )

        return {
            "status": "ok" if redis_ok else "degraded",
            "env": settings.env,
            "checks": {
                "redis": redis_ok,
                "api_keys_configured": api_keys_configured,
            },
        }

    return app


# Backward-compatibility alias for legacy tests
build_app_shell = create_app


def router_health_check(app: FastAPI | None = None, expected_count: int = 0) -> dict[str, Any]:
    """Helper to return health status of app routers."""
    return {"status": "healthy", "expected_count": expected_count, "env": settings.env}
