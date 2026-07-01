import os
import logging
import secrets

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
from core.api_key_middleware import APIKeyAuthMiddleware
from core.config import settings
from core.honeypot_middleware import HoneypotMiddleware
from core.observability_middleware import ObservabilityMiddleware
from core.rate_limiter import RateLimitMiddleware
from core.telemetry import setup_tracing
from middleware.auth_middleware import ZeroTrustAuthMiddleware
from middleware.idempotency import IdempotencyMiddleware


class InterceptHandler(logging.Handler):
    def emit(self, record):
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = record.levelno
        frame, depth = logging.currentframe(), 2
        while frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1
        logger.opt(depth=depth, exception=record.exc_info).log(
            level, record.getMessage()
        )


logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)

security = HTTPBasic()

setup_tracing()

import sentry_sdk


if settings.sentry_dsn:
    try:
        sentry_sdk.init(
            dsn=settings.sentry_dsn,
            traces_sample_rate=0.2 if settings.env.lower() == "production" else 1.0,
            environment=settings.env,
        )
    except Exception as exc:
        logger.warning(f"Sentry initialization failed: {exc}")


def _docs_auth(credentials: HTTPBasicCredentials = Depends(security)):
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


def _maybe_docs_auth():
    if settings.docs_auth_enabled and not settings.debug:
        return [_docs_auth]
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
    dependencies=docs_auth_dep,
)

from core.origin_validator import TrustedOriginMiddleware
from middleware.chaos_injector import ChaosInjectorMiddleware


# বাংলা মন্তব্য: সিকিউরিটি জোরদার করতে CORS পলিসিতে ওয়াইল্ডকার্ড (*) বাদ দিয়ে নির্দিষ্ট মেথড এবং হেডার ডিফাইন করা হলো
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID", "X-Tenant-ID", "X-API-Key"],
)


app.add_middleware(ChaosInjectorMiddleware)
app.add_middleware(HoneypotMiddleware)
app.add_middleware(RateLimitMiddleware, requests_per_minute=120, burst=20)
app.add_middleware(IdempotencyMiddleware)
app.add_middleware(ZeroTrustAuthMiddleware)
app.add_middleware(ObservabilityMiddleware)
app.add_middleware(APIKeyAuthMiddleware)
app.add_middleware(TrustedOriginMiddleware)


@app.exception_handler(HTTPException)
async def custom_http_exception_handler(request: Request, exc: HTTPException):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "title": "Task Execution Failed",
            "detail": exc.detail,
            "instance": request.url.path,
        },
    )


@app.get("/health")
async def health():
    redis_ok = False
    if services.redis_queue.configured:
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
def actuator_health():
    return {
        "status": "UP",
        "orchestrator": "online",
    }


from api.routes import admin_dashboard_router
from api.routes import agent_router
from api.routes import agents_router
from api.routes import api_keys_router
from api.routes import async_task_router
from api.routes import auth_router
from api.routes import browser_router
from api.routes import cdc_router
from api.routes import ci_webhooks_router
from api.routes import codeflow_router
from api.routes import config_router
from api.routes import email_router
from api.routes import feedback_router
from api.routes import github_router
from api.routes import graph_router
from api.routes import internal_router
from api.routes import knowledge_router
from api.routes import markdown_router
from api.routes import marketplace_router
from api.routes import media_router
from api.routes import memory_router
from api.routes import metrics_router
from api.routes import onboarding_router
from api.routes import payments_router
from api.routes import preferences_router
from api.routes import repos_router
from api.routes import simulator_router
from api.routes import sso_router
from api.routes import stream_router
from api.routes import task_router
from api.routes import tools_ops_router
from api.routes import tools_registry_router
from api.routes import usage_metrics_router


app.include_router(admin_router)

if memory_router is not None:
    app.include_router(memory_router)
if task_router is not None:
    app.include_router(task_router)
if markdown_router is not None:
    app.include_router(markdown_router, prefix="/api/v1")
if simulator_router is not None:
    app.include_router(simulator_router)
if browser_router is not None:
    app.include_router(browser_router)
if stream_router is not None:
    app.include_router(stream_router)
if agent_router is not None:
    app.include_router(agent_router)
if async_task_router is not None:
    app.include_router(async_task_router)
if cdc_router is not None:
    app.include_router(cdc_router)
if media_router is not None:
    app.include_router(media_router)
if graph_router is not None:
    app.include_router(graph_router)
if knowledge_router is not None:
    app.include_router(knowledge_router)
if marketplace_router is not None:
    app.include_router(marketplace_router)
if metrics_router is not None:
    app.include_router(metrics_router)
if auth_router is not None:
    app.include_router(auth_router)
if admin_dashboard_router is not None:
    app.include_router(admin_dashboard_router)
if email_router is not None:
    app.include_router(email_router)
if github_router is not None:
    app.include_router(github_router)
if internal_router is not None:
    app.include_router(internal_router)
if config_router is not None:
    app.include_router(config_router)
if onboarding_router is not None:
    app.include_router(onboarding_router)
if repos_router is not None:
    app.include_router(repos_router)
if tools_ops_router is not None:
    app.include_router(tools_ops_router)
if agents_router is not None:
    app.include_router(agents_router)
if tools_registry_router is not None:
    app.include_router(tools_registry_router)
if preferences_router is not None:
    app.include_router(preferences_router)
if usage_metrics_router is not None:
    app.include_router(usage_metrics_router)
if payments_router is not None:
    app.include_router(payments_router)
if sso_router is not None:
    app.include_router(sso_router)
if api_keys_router is not None:
    app.include_router(api_keys_router)
if ci_webhooks_router is not None:
    app.include_router(ci_webhooks_router)
try:
    from api.routes import websocket_voice_router

    if websocket_voice_router is not None:
        app.include_router(websocket_voice_router)
except Exception as _e:
    logger.warning(f"websocket_voice router not loaded: {_e}")
# Include Orchestrator router
from core.orchestrator import router as orchestrator_router


if orchestrator_router is not None:
    app.include_router(orchestrator_router)

try:
    from tools.collaborative_editor import router as collab_router

    app.include_router(collab_router, prefix="/api/v1")
except Exception as _e:
    logger.warning(f"collaborative_editor router not loaded: {_e}")

from tools.image_to_code import router as image_to_code_router


if image_to_code_router is not None:
    app.include_router(image_to_code_router)

# New tool routers (Sprint C fixes)
try:
    from tools.browser_agent import router as browser_agent_router

    app.include_router(browser_agent_router, prefix="/api")
except Exception as _e:
    logger.warning(f"browser_agent router not loaded: {_e}")

try:
    from tools.voice_coder import router as voice_coder_router

    app.include_router(voice_coder_router, prefix="/api")
except Exception as _e:
    logger.warning(f"voice_coder router not loaded: {_e}")

try:
    from tools.style_learner import router as style_learner_router

    app.include_router(style_learner_router, prefix="/api")
except Exception as _e:
    logger.warning(f"style_learner router not loaded: {_e}")

try:
    from tools.diagram_to_architecture import router as diagram_router

    app.include_router(diagram_router, prefix="/api")
except Exception as _e:
    logger.warning(f"diagram_to_architecture router not loaded: {_e}")

try:
    from tools.ai_pair_programmer import router as pair_router

    app.include_router(pair_router, prefix="/api")
except Exception as _e:
    logger.warning(f"ai_pair_programmer router not loaded: {_e}")

try:
    from api.routes.onboarding import router as onboarding_api_router

    app.include_router(onboarding_api_router, prefix="/api")
except Exception as _e:
    logger.warning(f"onboarding API router not loaded: {_e}")

try:
    from api.routes.evolution import router as evolution_router

    app.include_router(evolution_router)
except Exception as _e:
    logger.warning(f"evolution API router not loaded: {_e}")

if codeflow_router is not None:
    app.include_router(codeflow_router)
if feedback_router is not None:
    app.include_router(feedback_router)

# Sprint G routers
try:
    from tools.multilingual_tts import router as tts_router

    app.include_router(tts_router, prefix="/api")
except Exception as _e:
    logger.warning(f"multilingual_tts router not loaded: {_e}")

# bhasa mourontto: voice streaming router properly loaded with /api/voice prefix
try:
    from api.routes import voice_router

    if voice_router is not None:
        app.include_router(voice_router, prefix="/api/voice")
except Exception as _e:
    logger.warning(f"voice streaming router not loaded: {_e}")

try:
    from tools.comment_thread_ai import router as comment_ai_router

    app.include_router(comment_ai_router, prefix="/api")
except Exception as _e:
    logger.warning(f"comment_thread_ai router not loaded: {_e}")

try:
    from tools.auto_test_generator import router as test_gen_router

    app.include_router(test_gen_router, prefix="/api")
except Exception as _e:
    logger.warning(f"auto_test_generator router not loaded: {_e}")

try:
    from api.routes.tenant_admin import router as tenant_admin_router

    app.include_router(tenant_admin_router, prefix="/api")
except Exception as _e:
    logger.warning(f"tenant_admin router not loaded: {_e}")

from api.routes.mobile_bff import router as mobile_bff_router


app.include_router(mobile_bff_router)

# Register Universal BYOC Router
try:
    if os.getenv("SUPREMEAI_ENCRYPTION_KEY"):
        from api.routes.byoc_api import router as byoc_api_router
        app.include_router(byoc_api_router)
        logger.info("Universal BYOC management router loaded successfully ✅")
    else:
        logger.warning("Universal BYOC router not loaded: SUPREMEAI_ENCRYPTION_KEY missing")
except Exception as _e:
    import traceback

    logger.critical(f"Failed to load Universal BYOC router: {traceback.format_exc()}")

# Register billing API Router
try:
    from api.routes.billing_api import router as billing_api_router

    app.include_router(billing_api_router)
    logger.info("P2P Credit System billing router loaded successfully ✅")
except Exception as _e:
    import traceback

    logger.critical(f"Failed to load Billing router: {traceback.format_exc()}")

from api.routes.metrics import router as admin_metrics_router


app.include_router(admin_metrics_router)

try:
    from api.routes.cloud_mesh import router as cloud_mesh_router

    app.include_router(cloud_mesh_router)
except Exception as _e:
    logger.warning(f"cloud_mesh router not loaded: {_e}")

app.router.lifespan_context = lifespan.app_lifespan
