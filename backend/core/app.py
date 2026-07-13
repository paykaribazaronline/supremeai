"""This module serves as the central bootstrapping and configuration point for the SupremeAI FastAPI application. It initializes the core FastAPI instance, applies essential middleware for security, observability, and resilience, configures logging and error tracking, and dynamically loads all API routers, ensuring a robust, production-ready, and fail-fast backend ecosystem for the highly scalable AI project.

Key Components:
- `InterceptHandler`: A custom logging handler that redirects standard Python logging records to Loguru for centralized and enhanced logging.
- `_docs_auth()`: A dependency function that authenticates access to the FastAPI documentation using HTTP Basic authentication.
- `_maybe_docs_auth()`: A utility function that conditionally enables or disables documentation authentication based on application settings and debug mode.
- `app`: The main FastAPI application instance, meticulously configured with metadata, dependencies, and a comprehensive suite of middleware.
- `custom_http_exception_handler()`: A custom exception handler that provides a standardized JSON response format for all `HTTPException` instances.
- `_safe_include_router()`: A robust utility function designed to dynamically import and include FastAPI routers, logging warnings for missing optional routers and critically failing on essential router load errors to ensure application integrity.
- `health()`: An endpoint that exposes a comprehensive health check, verifying the operational status of critical services like Redis and the configuration of essential API keys.
- `actuator_health()`: A simple health status endpoint, typically used by external monitoring systems for quick liveness checks.
- `router_health_check()`: A critical startup check that ensures a minimum number of API routes have been successfully loaded, enforcing a strict fail-fast policy during application initialization.

Dependencies:
- `core.config`: For accessing application-wide settings and environment variables.
- `core.lifespan`: Defines the application's startup and shutdown lifecycle events.
- `core.services`: Provides access to shared application services, such as Redis.
- `core.admin_routes`: Contains administrative API endpoints for system management.
- `core.security.*`: Modules for various security features, including API key authentication, general authentication, honeypot detection, and trusted origin validation.
- `core.observability.observability_middleware`: Integrates observability features into the request lifecycle for monitoring and tracing.
- `core.messaging.event_bus`: Facilitates event-driven error reporting and communication within the application.
- `middleware.chaos_injector`: Injects controlled failures for resilience testing and chaos engineering.
- `middleware.idempotency`: Ensures that repeated requests have the same effect as a single request, preventing unintended side effects.
- `api.routes.*`: A collection of modules defining the core API endpoints for various SupremeAI functionalities (e.g., agents, tasks, marketplace, tools).
- `tools.*`: Modules defining API endpoints for optional or external AI tools and integrations.
- `logging`: Standard Python library for logging.
- `os`, `sys`, `secrets`, `pathlib`: Standard Python utilities for system interaction, security, and path manipulation.
- `typing`: Provides support for type hints, enhancing code readability and maintainability.
- `sentry_sdk`: Integrates Sentry for robust error tracking and performance monitoring.
- `fastapi`: The high-performance web framework used for building the API.
- `loguru`: A robust logging library used for enhanced and structured logging.
- `slowapi`: Provides rate limiting functionality for API endpoints to prevent abuse.
- `importlib`: Used for dynamic module loading, enabling flexible router inclusion."""

# backend/core/app.py
# ⚠️ WARNING: DO NOT MOVE THIS FILE. It is heavily integrated into the FastAPI startup lifecycle.
# Moving this file will break relative paths, imports, and core app bootstrapping across the entire project.
# বাংলা মন্তব্য: সম্পূর্ণ রি-ফ্যাক্টর — Fail-Fast, No Suppression, Encapsulated Guards।
# Missing env variant = sys.exit(1)
# 100% Strict Typing and production-ready setup.

import logging
import os
import secrets
import sys
from pathlib import Path


# Add project root to sys.path so 'skills' module can be imported
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent))

from typing import Any

import sentry_sdk
from fastapi import Depends
from fastapi import FastAPI
from fastapi import HTTPException
from fastapi import Request
from fastapi import status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.security import HTTPBasic
from fastapi.security import HTTPBasicCredentials
from loguru import logger

from core import lifespan
from core import services
from core.admin_routes import router as admin_router
from core.config import settings
from core.messaging.event_bus import ErrorEvent
from core.messaging.event_bus import error_event_bus
from core.observability.observability_middleware import ObservabilityMiddleware
from core.security.api_key_middleware import APIKeyAuthMiddleware
from core.security.auth_middleware import AuthMiddleware
from core.security.honeypot_middleware import HoneypotMiddleware
from core.security.origin_validator import TrustedOriginMiddleware
from middleware.chaos_injector import ChaosInjectorMiddleware
from middleware.idempotency import IdempotencyMiddleware


class InterceptHandler(logging.Handler):
    def emit(self, record: logging.LogRecord) -> None:
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = str(record.levelno)
        frame, depth = logging.currentframe(), 2

        while frame and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1
        logger.opt(depth=depth, exception=record.exc_info).log(level, record.getMessage())


logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)

security = HTTPBasic()

if settings.sentry_dsn:
    try:
        sentry_sdk.init(
            dsn=settings.sentry_dsn,
            traces_sample_rate=0.2 if settings.env.lower() == "production" else 1.0,
            environment=settings.env,
        )
    except Exception as exc:  # noqa: BLE001
        logger.critical(f"Sentry SDK initialization failed. Configuration error: {exc}")
        if os.getenv("ENV", "development").lower() != "test":
            sys.exit(1)


def _docs_auth(credentials: HTTPBasicCredentials = Depends(security)) -> str:
    correct = secrets.compare_digest(credentials.username, settings.docs_username) and secrets.compare_digest(
        credentials.password, settings.docs_password
    )
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

app = FastAPI(
    title=f"{settings.app_name} (Production Ready)",
    description="Multi-cloud AI orchestration platform with zero-cost edge computing.",
    version="2.0.0",
    openapi_tags=tags_metadata,
    debug=settings.debug,
    docs_url="/docs" if docs_enabled else None,
    redoc_url="/redoc" if docs_enabled else None,
    openapi_url="/openapi.json" if docs_enabled else None,
)

# Protect docs endpoints with basic auth if enabled
@app.middleware("http")
async def basic_auth_for_docs_middleware(request: Request, call_next):
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
            import base64
            try:
                decoded = base64.b64decode(auth[6:]).decode("utf-8")
                username, password = decoded.split(":", 1)
                if username != settings.docs_username or password != settings.docs_password:
                    raise ValueError("Mismatch")
            except Exception:
                return JSONResponse(
                    status_code=401,
                    content={"detail": "Invalid credentials"},
                    headers={"WWW-Authenticate": "Basic"},
                )
    return await call_next(request)


# বাংলা মন্তব্য: CORS Configuration. No wildcard allowed.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID", "X-Tenant-ID", "X-API-Key"],
)

app.add_middleware(TrustedOriginMiddleware)
app.add_middleware(ChaosInjectorMiddleware)
app.add_middleware(ObservabilityMiddleware)
app.add_middleware(HoneypotMiddleware)
app.add_middleware(AuthMiddleware)
# বাংলা মন্তব্য: Removed missing RateLimitMiddleware. APIKeyRateLimiter handles it inside its scope.
app.add_middleware(IdempotencyMiddleware)
app.add_middleware(APIKeyAuthMiddleware)


from slowapi import Limiter
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address


limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter


@app.exception_handler(HTTPException)
async def custom_http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "title": "Task Execution Failed",
            "detail": exc.detail,
            "instance": request.url.path,
        },
    )


app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)


def _safe_include_router(app: FastAPI, router_module: str, prefix: str = "") -> None:
    """বাংলা মন্তব্য: Lazy loader with strict exception handling and fail-fast."""
    import importlib

    try:
        module = importlib.import_module(router_module)
        router = getattr(module, "router", None)
        if router:
            app.include_router(router, prefix=prefix)
    except ImportError as exc:
        logger.warning(f"Optional router {router_module} not installed/found: {exc}")
        error_event_bus.emit(
            ErrorEvent(
                module="app",
                error_type="ROUTER_NOT_FOUND",
                message=str(exc)[:200],
                severity="WARNING",
                context={"router_module": router_module},
            )
        )
    except Exception as exc:  # noqa: BLE001
        logger.critical(f"Critical error loading router {router_module}: {exc}")
        error_event_bus.emit(
            ErrorEvent(
                module="app",
                error_type="ROUTER_LOAD_FAILED",
                message=str(exc)[:500],
                severity="CRITICAL",
                context={"router_module": router_module},
            )
        )
        # বাংলা মন্তব্য: Fail-fast on configuration or initialization errors
        sys.exit(1)


@app.get("/health")
async def health() -> dict[str, Any]:
    redis_ok = False
    if hasattr(services, "redis_queue") and services.redis_queue.configured:
        try:
            services.redis_queue.set("health", "ok", ex=5)
            redis_ok = services.redis_queue.get("health") == "ok"
        except Exception as exc:  # noqa: BLE001
            # বাংলা মন্তব্য: Anti-Suppression Rule
            logger.error(f"Health check failed on redis connection: {exc}")
            error_event_bus.emit(ErrorEvent(module="app.health", error_type="REDIS_HEALTH_FAIL", message=str(exc)[:200], severity="ERROR"))
            redis_ok = False
    else:
        redis_ok = True

    api_keys_ok = bool(
        settings.openrouter_api_key or settings.gemini_api_key or settings.deepseek_api_key or settings.groq_api_key or settings.nvidia_api_key
    )
    checks = {
        "redis": redis_ok,
        "api_keys_configured": api_keys_ok,
    }
    all_ok = all(checks.values())
    return {
        "status": "ok" if all_ok else "degraded",
        "orchestrator": "online",
        "checks": checks,
    }


@app.get("/actuator/health")
def actuator_health() -> dict[str, str]:
    return {
        "status": "UP",
        "orchestrator": "online",
    }


app.include_router(admin_router)


# Core Routers
core_routers = [
    ("api.routes.memory", ""),
    ("api.routes.task", ""),
    ("api.routes.markdown", "/api/v1"),
    ("api.routes.simulator", ""),
    ("api.routes.site_actions", ""),
    ("api.routes.llm_gateway", ""),
    ("api.routes.browser", ""),
    ("api.routes.stream", ""),
    ("api.routes.media", ""),
    ("api.routes.graph", ""),
    ("api.routes.knowledge", ""),
    # বাংলা মন্তব্য: ফাইলটির নাম marketplace_endpoints.py হওয়ার কারণে রাউটার রেজিস্ট্রেশনে 404 এরর আসছিল, তাই রাউটার পাথ আপডেট করা হলো।
    ("api.routes.marketplace_endpoints", ""),
    ("api.routes.auth", "/api/v1"),
    ("api.routes.onboarding", "/api/v1/onboarding"),
    ("api.routes.evolution", "/api/v1/evolution"),
    ("api.routes.admin_dashboard", ""),
    ("api.routes.email", ""),
    ("api.routes.github", ""),
    ("api.routes.internal", ""),
    ("api.routes.config", ""),
    ("api.routes.repos", ""),
    ("api.routes.tools_ops", ""),
    ("api.routes.agents", ""),
    ("api.routes.admin", ""),
    ("api.routes.tools_registry", ""),
    ("api.routes.preferences", ""),
    ("api.routes.usage_metrics", ""),
    ("api.routes.sso", ""),
    ("api.routes.health", ""),
    ("api.routes.api_keys", ""),
    ("api.routes.ci_webhooks", ""),
    ("api.routes.task_workspace", "/api/v1"),
    ("api.routes.websocket_agent", ""),
    ("api.routes.agent_workspace", "/api/v1"),
    ("api.routes.integrations", "/api/v1"),
    ("api.routes.public_config", "/api"),
    ("api.routes.traffic_monitor", ""),
    ("api.routes.swarm", "/api/v1"),
    ("core.orchestrator", ""),
]

for router_path, prefix in core_routers:
    _safe_include_router(app, router_path, prefix)

# Optional / External Tools Routers
optional_routers = [
    ("api.routes.websocket_voice", ""),
    ("tools.collaborative_editor", "/api/v1"),
    ("tools.image_to_code", ""),
    ("tools.browser_agent", "/api"),
    ("tools.voice_coder", "/api"),
    ("tools.style_learner", "/api"),
    ("tools.diagram_to_architecture", "/api"),
    ("tools.ai_pair_programmer", "/api"),
    ("api.routes.codeflow", ""),
    ("api.routes.feedback", ""),
    # বাংলা মন্তব্য: সঠিক ডিরেক্টরি (tools.media.multilingual_tts) থেকে লোড করার জন্য পাথ আপডেট করা হলো
    ("tools.media.multilingual_tts", "/api"),
    ("api.routes.voice", "/api/voice"),
    ("tools.comment_thread_ai", "/api"),
    ("tools.auto_test_generator", "/api"),
    ("api.routes.tenant_admin", "/api"),
    ("api.routes.mobile_bff", ""),
    ("api.routes.billing_api", ""),
    ("api.routes.metrics", ""),
    ("api.routes.cloud_mesh", ""),
    ("api.routes.events", "/api"),
    ("api.routes.payments", ""),
    ("api.routes.maintenance", "/api/v1"),
    # বাংলা মন্তব্য: নতুন autonomous engine রাউটারগুলো যুক্ত করা হলো (sandbox + pr-review)।
    ("api.routes.sandbox_api", ""),
    ("api.routes.pr_review_api", ""),
]

for router_path, prefix in optional_routers:
    _safe_include_router(app, router_path, prefix)


if settings.encryption_key and settings.encryption_key.get_secret_value():
    _safe_include_router(app, "api.routes.byoc_api", "")
else:
    logger.warning("Universal BYOC router not loaded: ENCRYPTION_KEY missing")

app.router.lifespan_context = lifespan.app_lifespan


def router_health_check(fastapi_app: FastAPI) -> None:
    expected_count = 20
    if len(fastapi_app.routes) < expected_count:
        logger.critical(
            f"🔥 CRITICAL: Only {len(fastapi_app.routes)} routes loaded. Expected at least {expected_count}. Some routers failed to load!"
        )
        # বাংলা মন্তব্য: Strict fail-fast rule
        sys.exit(1)


router_health_check(app)
