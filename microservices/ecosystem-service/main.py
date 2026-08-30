"""Production-ready FastAPI entry point — SupremeAI ecosystem test harness.

বাংলা: এই version-টি production-deploy-এর জন্য। উন্নত বৈশিষ্ট্য:
- Sentry error tracking (যদি SENTRY_DSN set করা থাকে)
- Proper CORS (FRONTEND_ORIGIN env অনুযায়ী)
- Startup safety check (ADMIN_TOKEN দুর্বল হলে warning)
- সব ecosystem singleton eager-init (race condition নেই)
- Graceful shutdown
- Structured logging
"""

from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import settings

# ── Logging setup ────────────────────────────────────────────────────────────
logging.basicConfig(
    level=getattr(logging, settings.log_level.upper(), logging.INFO),
    format="%(asctime)s | %(levelname)-7s | %(name)s | %(message)s",
)
logger = logging.getLogger("supremeai.ecosystem")
logger.info(f">>> booting SupremeAI ecosystem (env={settings.env})")

# ── Sentry (optional) ────────────────────────────────────────────────────────
if settings.has_sentry():
    try:
        import sentry_sdk
        from sentry_sdk.integrations.asyncio import AsyncioIntegration

        sentry_sdk.init(
            dsn=settings.sentry_dsn,
            environment=settings.env,
            traces_sample_rate=0.1,
            integrations=[AsyncioIntegration()],
        )
        logger.info(">>> Sentry initialized")
    except ImportError:
        logger.warning("SENTRY_DSN set but sentry-sdk not installed — skipping")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup hook — eager-init everything + safety check."""
    # বাংলা: সব ecosystem module শুরুতেই initialize যাতে সব table create
    # হয়ে যায় — lazy-access race condition এড়াতে।
    from api.routes.ecosystem import router as user_router
    from api.routes.ecosystem_admin import router as admin_router
    from adapters import register_all_adapters
    from ecosystem import (
        get_approval_workflow,
        get_capability_registry,
        get_deployment_tracker,
        get_governance_engine,
        get_health_aggregator,
        get_learning_loop,
        get_resource_registry,
        get_task_engine,
    )

    app.include_router(user_router)
    app.include_router(admin_router)

    # বাংলা: eager-init সব singleton → সব table শুরুতেই create হয়।
    get_task_engine()
    get_approval_workflow()
    get_deployment_tracker()
    get_health_aggregator()
    get_governance_engine()
    get_learning_loop()

    # Register real provider adapters (only if credentials present)
    register_all_adapters(get_resource_registry())

    # Seed defaults (idempotent)
    if settings.auto_seed:
        try:
            from scripts.seed_ecosystem import seed_capabilities, seed_policies

            seed_capabilities()
            seed_policies()
            logger.info(">>> ecosystem seed complete")
        except Exception as exc:
            logger.warning(f">>> ecosystem seed failed (non-fatal): {exc}")

    # Auto-register configured providers
    _auto_register_providers()

    # Production safety check
    ok, warnings = settings.is_safe_for_production()
    if not ok:
        for w in warnings:
            logger.warning(f"⚠️  SAFETY: {w}")
        if settings.env == "production" and not settings.strict_admin_auth:
            logger.error("🚨 REFUSING TO START: STRICT_ADMIN_AUTH=false in production")
            raise RuntimeError("strict_admin_auth_required_in_production")
    else:
        logger.info(">>> production safety checks passed")

    logger.info(">>> SupremeAI ecosystem ready ✓")
    yield
    logger.info(">>> shutting down gracefully")


def _auto_register_providers() -> None:
    """ROADMAP §36 — auto-register resources from env credentials on first boot."""
    from ecosystem import ProviderKind, ResourceRecord, get_resource_registry

    rr = get_resource_registry()

    if settings.has_render():
        existing = [
            r
            for r in rr.list(provider=ProviderKind.RENDER)
            if r.metadata.get("service_id") == settings.render_service_id
        ]
        if not existing:
            rr.register(
                ResourceRecord(
                    name=f"render-{settings.render_service_id}",
                    provider=ProviderKind.RENDER,
                    type="web_service",
                    capabilities=["health", "metrics", "logs", "deploy", "restart"],
                    metadata={"service_id": settings.render_service_id, "managed_by": "test-harness"},
                    provider_config_ref="env://RENDER_API_KEY",
                )
            )
            logger.info(f">>> auto-registered Render resource (service_id={settings.render_service_id})")

    if settings.has_github():
        existing = [
            r
            for r in rr.list(provider=ProviderKind.GITHUB)
            if r.metadata.get("repo") == settings.github_repo
        ]
        if not existing:
            rr.register(
                ResourceRecord(
                    name=f"github-{settings.github_repo.replace('/', '-')}",
                    provider=ProviderKind.GITHUB,
                    type="repository",
                    capabilities=["health", "logs", "deploy", "create_pr"],
                    metadata={"repo": settings.github_repo, "managed_by": "test-harness"},
                    provider_config_ref="env://GITHUB_TOKEN",
                )
            )
            logger.info(f">>> auto-registered GitHub resource (repo={settings.github_repo})")

    if settings.has_supabase():
        existing = [
            r
            for r in rr.list(provider=ProviderKind.SUPABASE)
            if r.metadata.get("url") == settings.supabase_url
        ]
        if not existing:
            project_id = (
                settings.supabase_url.split("//")[1].split(".")[0]
                if "//" in settings.supabase_url
                else "project"
            )
            rr.register(
                ResourceRecord(
                    name=f"supabase-{project_id}",
                    provider=ProviderKind.SUPABASE,
                    type="database",
                    capabilities=["health", "metrics"],
                    metadata={"url": settings.supabase_url, "project_id": project_id, "managed_by": "test-harness"},
                    provider_config_ref="env://SUPABASE_SERVICE_KEY",
                )
            )
            logger.info(f">>> auto-registered Supabase resource (project={project_id})")


app = FastAPI(
    title="SupremeAI Ecosystem — Production Test Harness",
    description="Production-ready isolated environment to verify the ecosystem foundation before applying to production.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins(),
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Correlation-Request-Id"],
)


@app.get("/health")
def health() -> dict:
    """Render health check endpoint."""
    return {"status": "ok", "service": "supremeai-ecosystem", "env": settings.env}


@app.get("/")
def root() -> dict:
    ok, warnings = settings.is_safe_for_production()
    return {
        "name": "SupremeAI Ecosystem Production Test Harness",
        "version": "1.0.0",
        "env": settings.env,
        "configured_providers": {
            "render": settings.has_render(),
            "github": settings.has_github(),
            "supabase": settings.has_supabase(),
        },
        "safety_ok": ok,
        "safety_warnings": warnings if not ok else [],
        "endpoints": {
            "user": "/api/v1/ecosystem/*",
            "admin": "/api/v1/ecosystem/admin/* (Bearer ADMIN_TOKEN required)",
            "health": "/health",
            "docs": "/docs",
        },
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host=settings.host,
        port=settings.port,
        log_level=settings.log_level,
        access_log=True,
        workers=1,
        reload=False,
    )
