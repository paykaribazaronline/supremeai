# backend/core/app_builder.py
"""SupremeAI 2.0 — FastAPI Application Builder.

বাংলা মন্তব্য: এই মডিউলটি কোর FastAPI অ্যাপ্লিকেশনের গঠন ও বিল্ডার লজিক ধারণ করে।
এটি app.py থেকে আলাদা করা হয়েছে যাতে এডমিন এপিআই এবং ইউজার এপিআই আলাদাভাবে
রোল অনুযায়ী লোড হতে পারে এবং কোনো সাইড ইফেক্ট ছাড়াই শুধু প্রয়োজনীয় মডিউলগুলো
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
from api.middleware import (ChaosInjectorMiddleware,
                            ResponseStandardizationMiddleware,
                            SupremeContextMiddleware)
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

if settings.sentry_dsn:
    try:
        sentry_sdk.init(
            dsn=settings.sentry_dsn,
            traces_sample_rate=0.2 if settings.env.lower() == "production" else 1.0,
            environment=settings.env,
        )
    except Exception:  # noqa: BLE001
        logger.critical("Sentry SDK initialization failed. Configuration error.")
        if os.getenv("ENV", "development").lower() != "test":
            sys.exit(1)


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


# slowapi টেস্টে মক করা হলেও RateLimitExceeded যেন সত্যিকারের Exception ক্লাস থাকে
try:
    from slowapi import Limiter
    from slowapi import \
        _rate_limit_exceeded_handler as _slowapi_rate_limit_handler
    from slowapi.errors import RateLimitExceeded as _SlowAPIRateLimitExceeded
    from slowapi.util import get_remote_address as _slowapi_get_remote_address

    if not isinstance(_SlowAPIRateLimitExceeded, type) or not issubclass(
        _SlowAPIRateLimitExceeded, Exception
    ):

        class RateLimitExceeded(Exception):  # type: ignore[no-redef]
            """Fallback RateLimitExceeded for test environments where slowapi is mocked."""

        def _rate_limit_exceeded_handler(request: Any, exc: Any) -> JSONResponse:  # type: ignore[misc]
            return JSONResponse(
                status_code=429, content={"detail": "Rate limit exceeded"}
            )

        def get_remote_address(request: Any) -> str:  # type: ignore[misc]
            return request.client.host if request.client else "127.0.0.1"

        limiter = None
    else:
        RateLimitExceeded = _SlowAPIRateLimitExceeded  # type: ignore[misc,assignment]
        _rate_limit_exceeded_handler = _slowapi_rate_limit_handler
        get_remote_address = _slowapi_get_remote_address
        limiter = Limiter(key_func=get_remote_address)
except Exception:  # noqa: BLE001

    class RateLimitExceeded(Exception):  # type: ignore[no-redef]
        """Fallback RateLimitExceeded for test environments."""

    def _rate_limit_exceeded_handler(request: Any, exc: Any) -> JSONResponse:  # type: ignore[misc]
        return JSONResponse(status_code=429, content={"detail": "Rate limit exceeded"})

    limiter = None


def build_app_shell(
    title: str = "SupremeAI API", docs_url: str | None = "/docs"
) -> FastAPI:
    """Builds the base FastAPI shell with shared configuration, middleware, and exception handlers.

    বাংলা মন্তব্য: কোর FastAPI অ্যাপ সেল যা মিডলওয়্যার এবং এক্সেপশন হ্যান্ডলারগুলো ইনিশিয়ালাইজ করে।
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

    # বাংলা মন্তব্য: রিকোয়েস্ট ট্রেসিংয়ের সুবিধার্থে কোরিলেশন আইডি জেনারেট করার মিডলওয়্যার যোগ করা হলো।
    fastapi_app.add_middleware(RequestContextMiddleware)
    fastapi_app.add_middleware(SupremeContextMiddleware)
    fastapi_app.add_middleware(TrustedOriginMiddleware)
    fastapi_app.add_middleware(ChaosInjectorMiddleware)
    fastapi_app.add_middleware(ObservabilityMiddleware)
    fastapi_app.add_middleware(HoneypotMiddleware)

    fastapi_app.add_middleware(AuthMiddleware)
    fastapi_app.add_middleware(APIKeyAuthMiddleware)
    fastapi_app.add_middleware(ResponseStandardizationMiddleware)
    fastapi_app.add_middleware(AutonoGuardMiddleware)

    # বাংলা মন্তব্য: সবার শেষে GZipMiddleware যোগ করা হলো bandwidth কমাতে।
    fastapi_app.add_middleware(GZipMiddleware, minimum_size=1000)

    fastapi_app.state.limiter = limiter

    @fastapi_app.exception_handler(HTTPException)
    async def custom_http_exception_handler(
        request: Request, exc: HTTPException
    ) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={
                "title": "Task Execution Failed",
                "detail": exc.detail,
                "instance": request.url.path,
            },
        )

    @fastapi_app.exception_handler(Exception)
    async def global_exception_handler(
        request: Request, exc: Exception
    ) -> JSONResponse:
        # বাংলা মন্তব্য: এরর ট্র্যাকিং ও ফিঙ্গারপ্রিন্টিং সিস্টেম ইন্টিগ্রেশন।
        failure = await ReliabilityController.register_failure(request, exc)
        logger.error(
            f"Unhandled Exception on {request.url.path}: {exc} [Correlation ID: {failure.correlation_id}, Fingerprint: {failure.fingerprint}]"
        )
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "title": "Internal Server Error",
                "detail": "An unexpected error occurred. This has been logged.",
                "instance": request.url.path,
                "correlation_id": failure.correlation_id,
            },
        )

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
        checks = {
            "redis": redis_ok,
            "api_keys_configured": api_keys_ok,
            "reliability_controller": ReliabilityController.health(),
            "startup_validation": StartupValidator.last_status(),
        }
        all_ok = (
            redis_ok
            and api_keys_ok
            and StartupValidator.last_status().get("success", True)
        )
        return {
            "status": "ok" if all_ok else "degraded",
            "orchestrator": "online",
            "checks": checks,
        }

    @fastapi_app.get("/actuator/health")
    def actuator_health() -> dict[str, str]:
        return {"status": "UP", "orchestrator": "online"}

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
