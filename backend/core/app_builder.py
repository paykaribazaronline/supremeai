# backend/core/app_builder.py
"""SupremeAI 2.0 — FastAPI Application Builder.

বাংলা মন্তব্য: এই মডিউলটি কোর FastAPI অ্যাপ্লিকেশনের গঠন ও বিল্ডার লজিক ধারণ করে।
এটি app.py থেকে আলাদা করা হয়েছে যাতে এডমিন এপিআই এবং ইউজার এপিআই আলাদাভাবে
রোল অনুযায়ী লোড হতে পারে এবং কোনো সাইড ইফেক্ট ছাড়াই শুধু প্রয়োজনীয় মডিউলগুলো
ইম্পোর্ট করে বুটস্ট্যাপ হতে পারে।
"""

from __future__ import annotations

import base64
import logging
import os
import secrets
import sys
from typing import Any

import sentry_sdk
from api.errors import api_error_handler
from api.middleware import (ChaosInjectorMiddleware, IdempotencyMiddleware,
                            RequestIdMiddleware,
                            ResponseStandardizationMiddleware,
                            SupremeContextMiddleware,
                            TenantExtractionMiddleware)
from core import lifespan, services
from core.config import settings
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from core.observability.observability_middleware import ObservabilityMiddleware
from core.reliability_controller import ReliabilityController
from core.request_context import RequestContextMiddleware
from core.security.api_key_middleware import APIKeyAuthMiddleware
from core.security.auth_middleware import AuthMiddleware
from core.security.autonoguard_middleware import AutonoGuardMiddleware
from core.security.honeypot_middleware import HoneypotMiddleware
from core.security.origin_validator import TrustedOriginMiddleware
from core.startup_validator import StartupValidator
from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.responses import JSONResponse
from fastapi.security import HTTPBasic, HTTPBasicCredentials
from loguru import logger


class InterceptHandler(logging.Handler):
    """Redirect stdlib logging to Loguru."""

    def emit(self, record: logging.LogRecord) -> None:
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = str(record.levelno)
        frame, depth = logging.currentframe(), 2
        while frame and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1
        logger.opt(depth=depth, exception=record.exc_info).log(
            level, record.getMessage()
        )


logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)

security = HTTPBasic()

if settings.sentry_dsn and settings.sentry_dsn.strip():
    try:
        sentry_sdk.init(
            dsn=settings.sentry_dsn.strip(),
            traces_sample_rate=0.2 if settings.env.lower() == "production" else 1.0,
            environment=settings.env,
        )
        logger.info("✅ Sentry SDK initialized successfully.")
    except Exception:  # noqa: BLE001
        logger.warning("Sentry SDK initialization failed — continuing without Sentry.")
else:
    logger.info("ℹ️ Sentry DSN not configured — error tracking disabled.")


def _docs_auth(credentials: HTTPBasicCredentials = Depends(security)) -> str:
    """Authenticate docs access via HTTP Basic."""
    correct = secrets.compare_digest(
        credentials.username, settings.docs_username
    ) and secrets.compare_digest(credentials.password, settings.docs_password)
    if not correct:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Basic"},
        )
    return credentials.username


def _maybe_docs_auth() -> list[Depends]:
    if settings.docs_auth_enabled and not settings.debug:
        return [Depends(_docs_auth)]
    return []


docs_auth_dep = _maybe_docs_auth()

is_prod = settings.env.lower() == "production"
docs_enabled = settings.debug or not is_prod or settings.docs_auth_enabled

tags_metadata = [
    {"name": "admin", "description": "God-mode admin operations."},
    {"name": "agent", "description": "Autonomous agents execution and planning."},
    {"name": "marketplace", "description": "Discover and manage AI skills and tools."},
    {"name": "tools", "description": "Registry and management of integrated tools."},
]


# JWT role অনুযায়ী Admin (100 RPM) vs Standard User (20 RPM) থ্রেশহোল্ড নির্ধারণ
def supremeai_dynamic_rate_evaluator(request: Request) -> str:
    """ডাইনামিক rate key: JWT role বা IP fallback অনুযায়ী limiter বাউন্ডারি বাছাই করে।"""
    user = getattr(request.state, "user", None)
    user_role = (
        user.get("role", "Standard_User") if isinstance(user, dict) else "Standard_User"
    )
    client_ip = request.client.host if request.client else "unknown"
    if user_role in {"Admin", "admin"}:
        return f"admin:{client_ip}"
    return f"user:{client_ip}"


# বাংলা মন্তব্য: নেটিভ রেডিস স্লাইডিং-উইন্ডো রেট লিমিটার — slowapi প্রতিস্থাপন।
# জিরো-কস্ট কমপ্লায়েন্স: কোনো পেইড থার্ড-পার্টি গেটওয়ে নয়, সরাসরি Upstash Redis।
class RateLimitExceeded(Exception):
    """Rate limit exceeded — ক্লায়েন্টকে 429 রিটার্ন করতে।"""


async def _rate_limit_exceeded_handler(
    request: Request, exc: RateLimitExceeded
) -> JSONResponse:
    return JSONResponse(status_code=429, content={"detail": "Rate limit exceeded"})


async def check_native_rate_limit(
    request: Request,
    max_requests: int = 60,
    window_seconds: int = 60,
) -> bool:
    """বাংলা মন্তব্য: Redis sorted set ব্যবহার করে অ্যাটমিক স্লাইডিং-উইন্ডো রেট লিমিট চেক।
    Redis ডাউন থাকলে fail-closed — রিকোয়েস্ট ব্লক করে সিকিউরিটি রিস্ক এড়ায়।
    """
    from core.cache.redis_manager import redis_manager

    if not redis_manager.client:
        logger.warning("Rate limit check skipped — Redis unavailable (fail-closed)")
        return False

    import time

    client_ip = request.client.host if request.client else "127.0.0.1"
    key = f"ratelimit:{client_ip}"
    now = time.time()
    window_start = now - window_seconds

    try:
        pipe = redis_manager.client.pipeline()
        pipe.zremrangebyscore(key, 0, window_start)
        pipe.zcard(key)
        pipe.expire(key, window_seconds)
        _, count, _ = await pipe.execute()

        if count >= max_requests:
            raise RateLimitExceeded(
                f"Rate limit exceeded for {client_ip}: {count} requests in {window_seconds}s"
            )

        await redis_manager.client.zadd(key, {str(now): now})
        return True
    except RateLimitExceeded:
        raise
    except Exception as exc:  # noqa: BLE001
        logger.error(f"Rate limit check failed: {exc} — fail-closed")
        return False


def build_app_shell(
    title: str = "SupremeAI API", docs_url: str | None = "/docs"
) -> FastAPI:
    """Builds the base FastAPI shell with shared configuration, middleware, and exception handlers.

    বাংলা মন্তব্য: কোর FastAPI অ্যাপ সেল যা মিডলওয়্যার এবং এক্সেপশন হ্যান্ডলারগুলো ইনিশিয়ালাইজ করে।
    """
    is_prod = settings.env.lower() == "production"
    docs_enabled = settings.debug or not is_prod or settings.docs_auth_enabled

    fastapi_app = FastAPI(
        title=title,
        description="Multi-cloud AI orchestration platform with zero-cost edge computing.",
        version="2.0.0",
        openapi_tags=tags_metadata,
        debug=settings.debug,
        docs_url=docs_url if docs_enabled else None,
        redoc_url=("/redoc" if docs_url else None) if docs_enabled else None,
        openapi_url=("/openapi.json" if docs_url else None) if docs_enabled else None,
    )

    @fastapi_app.middleware("http")
    async def basic_auth_for_docs_middleware(
        request: Request, call_next: Any
    ) -> JSONResponse:  # noqa: ANN401
        """Protect docs with Basic Auth if enabled."""
        if settings.docs_auth_enabled and not settings.debug:
            path = request.url.path
            if path in {"/docs", "/redoc", "/openapi.json"}:
                auth = request.headers.get("Authorization")
                if not auth or not auth.startswith("Basic "):
                    return JSONResponse(
                        status_code=401,
                        content={"detail": "Invalid credentials"},
                        headers={"WWW-Authenticate": "Basic"},
                    )
                try:
                    decoded = base64.b64decode(auth[6:]).decode("utf-8")
                    username, password = decoded.split(":", 1)
                    if (
                        username != settings.docs_username
                        or password != settings.docs_password
                    ):
                        raise ValueError("Mismatch")
                except (ValueError, UnicodeDecodeError):
                    return JSONResponse(
                        status_code=401,
                        content={"detail": "Invalid credentials"},
                        headers={"WWW-Authenticate": "Basic"},
                    )
        return await call_next(request)

    # বাংলা মন্তব্য: রিকোয়েস্ট ট্রেসিংয়ের সুবিধার্থে কোরিলেশন আইডি জেনারেট করার মিডলওয়্যার যোগ করা হলো।
    fastapi_app.add_middleware(RequestContextMiddleware)  # 1 - Always first
    fastapi_app.add_middleware(
        GZipMiddleware, minimum_size=1000
    )  # 2 - Decode body early
    fastapi_app.add_middleware(RequestIdMiddleware)  # 3
    fastapi_app.add_middleware(TrustedOriginMiddleware)  # 4
    fastapi_app.add_middleware(SupremeContextMiddleware)  # 5
    fastapi_app.add_middleware(TenantExtractionMiddleware)  # 6
    fastapi_app.add_middleware(ObservabilityMiddleware)  # 7
    fastapi_app.add_middleware(AuthMiddleware)  # 8 - AUTH FIRST
    fastapi_app.add_middleware(APIKeyAuthMiddleware)  # 9
    fastapi_app.add_middleware(AutonoGuardMiddleware)  # 10 - Security BEFORE internals
    fastapi_app.add_middleware(HoneypotMiddleware)  # 11 - Now authenticated
    fastapi_app.add_middleware(ChaosInjectorMiddleware)  # 12 - Now authenticated
    fastapi_app.add_middleware(IdempotencyMiddleware)  # 13
    fastapi_app.add_middleware(ResponseStandardizationMiddleware)  # 14 - Last

    # বাংলা মন্তব্য: api/errors.py-তে সংজ্ঞায়িত api_error_handler রেজিস্টার করা হলো
    # যাতে ErrorResponse schema টি globally এনফোর্স করা যায় এবং ডুপ্লিকেট হ্যান্ডলার অপসারণ করা হয়।
    fastapi_app.add_exception_handler(Exception, api_error_handler)
    fastapi_app.add_exception_handler(HTTPException, api_error_handler)

    if isinstance(RateLimitExceeded, type) and issubclass(RateLimitExceeded, Exception):
        fastapi_app.add_exception_handler(
            RateLimitExceeded, _rate_limit_exceeded_handler
        )

    @fastapi_app.get("/")
    async def root() -> dict[str, Any]:
        return {
            "name": settings.app_name,
            "version": "2.0.0",
            "status": "online",
            "docs": "/docs",
            "health": "/api/v1/health",
            "description": "Multi-cloud AI orchestration platform.",
        }

    @fastapi_app.get("/health")
    async def health() -> dict[str, Any]:
        redis_ok = False
        if hasattr(services, "redis_queue") and services.redis_queue.configured:
            try:
                services.redis_queue.set("health", "ok", ex=5)
                redis_ok = services.redis_queue.get("health") == "ok"
            except Exception:  # noqa: BLE001
                logger.exception("Health check failed on redis connection")
                error_event_bus.emit(
                    ErrorEvent(
                        module="app.health",
                        error_type="REDIS_HEALTH_FAIL",
                        message="Redis health error",
                        severity="ERROR",
                        structured_context=ErrorContext(module="auto_fixed"),
                    )
                )
                redis_ok = False
        else:
            redis_ok = True

        api_keys_ok = bool(
            settings.openrouter_api_key
            or settings.gemini_api_key
            or settings.deepseek_api_key
            or settings.groq_api_key
            or settings.nvidia_api_key
        )
        # বাংলা মন্তব্য: নির্ভরযোগ্যতা এবং স্টার্টআপ ভ্যালিডেশন মেট্রিক্স হেলথ চেকে যুক্ত করা হলো।
        startup_status = StartupValidator.last_status()
        validation_summary = StartupValidator.get_validation_summary()
        checks = {
            "redis": redis_ok,
            "api_keys_configured": api_keys_ok,
            "reliability_controller": ReliabilityController.health(),
            "startup_validation": startup_status,
        }
        all_ok = redis_ok and api_keys_ok and startup_status.get("success", True)
        return {
            "status": "ok" if all_ok else "degraded",
            "orchestrator": "online",
            "startup_duration_ms": validation_summary.get("duration_ms", 0),
            "cors_origins_configured": len(settings.cors_origins),
            "security": {
                "jwt_configured": bool(settings.jwt_secret),
                "jit_otp_enabled": True,
                "token_revocation_active": True,
            },
            "checks": checks,
        }

    @fastapi_app.get("/actuator/health")
    def actuator_health() -> dict[str, str]:
        return {"status": "UP", "orchestrator": "online"}

    @fastapi_app.get("/health/aggregated")
    async def aggregated_health() -> dict[str, Any]:
        """Aggregated health endpoint showing all subsystem statuses."""
        import time as _time

        redis_ok = False
        if hasattr(services, "redis_queue") and services.redis_queue.configured:
            try:
                services.redis_queue.set("health", "ok", ex=5)
                redis_ok = services.redis_queue.get("health") == "ok"
            except Exception:
                redis_ok = False
        else:
            redis_ok = True

        api_keys_ok = bool(
            settings.openrouter_api_key
            or settings.gemini_api_key
            or settings.deepseek_api_key
            or settings.groq_api_key
            or settings.nvidia_api_key
        )

        subsystems = {
            "redis": {"status": "up" if redis_ok else "down"},
            "api_keys": {"status": "configured" if api_keys_ok else "missing"},
            "config": {"status": "loaded", "env": settings.env},
            "cors": {"origins_configured": len(settings.cors_origins)},
            "jwt": {"configured": bool(settings.jwt_secret)},
        }

        all_ok = redis_ok and api_keys_ok
        return {
            "status": "ok" if all_ok else "degraded",
            "version": "2.0.0",
            "uptime_seconds": _time.time()
            - _time.time(),  # placeholder — track actual startup time
            "subsystems": subsystems,
        }

    fastapi_app.router.lifespan_context = lifespan.app_lifespan
    return fastapi_app


def router_health_check(
    fastapi_app: FastAPI, expected_count: int | None = None
) -> None:
    """Fail-fast if fewer than minimum routes loaded.

    বাংলা মন্তব্য: স্টার্টআপে রাউটার লোডিং ভ্যালিডেশন। মিনিমাম রুট চেক করে ফেইল-ফাস্ট নিশ্চিত করে।
    """
    if expected_count is None:
        expected_count = int(os.getenv("MIN_EXPECTED_ROUTES", "20"))
    if len(fastapi_app.routes) < expected_count:
        logger.critical(
            f"🔥 CRITICAL: Only {len(fastapi_app.routes)} routes loaded. Expected at least {expected_count}. Some routers failed to load!"
        )
        sys.exit(1)
