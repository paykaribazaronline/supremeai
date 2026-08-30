"""Centralized router registration for SupremeAI API."""

from __future__ import annotations

from fastapi import Depends, FastAPI

from api import register_router
from api.deps import get_current_user_token
from core.config import settings
from core.logging_config import logger

# Unified declarative registry of all routers.
# Format: {"path": str, "prefix": str, "is_admin": bool, "is_critical": bool}
# Deduplicated and cleaned up according to Phase 2 API Cleanup.
ALL_ROUTERS = [
    # ---- Core & User Routes ----
    {"path": "api.routes.memory", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.unified_memory_api",
        "prefix": "",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.task", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.markdown", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {"path": "api.routes.simulator", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.stream", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.media", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.graph", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.marketplace_endpoints",
        "prefix": "",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.auth", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {"path": "api.routes.onboarding", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.localization",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.analytics", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {"path": "api.routes.email", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.github", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.config_routes", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.cognitive", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.cache_predictions",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.healing", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {"path": "api.routes.repos", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.agents", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.agent", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.tools_registry", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.skills", "prefix": "/api", "is_admin": False, "is_critical": False},
    {"path": "api.routes.files", "prefix": "/api", "is_admin": False, "is_critical": False},
    {"path": "api.routes.usage_metrics", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.sso", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.api_keys", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.ci_webhooks", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.n8n_webhooks", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.task_workspace",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.websocket_agent", "prefix": "", "is_admin": False, "is_critical": False},
    # R10 FIX: SSE shim for the WS /chat route (additive — WS route stays active while WS_FALLBACK=true)
    {"path": "api.routes.stream_chat_sse", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.agent_workspace",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {
        "path": "api.routes.integrations",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.admin_v1", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.agent_action",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.websocket_hitl", "prefix": "", "is_admin": False, "is_critical": False},
    # R10 FIX: SSE shim for the WS HITL route
    {"path": "api.routes.stream_hitl_sse", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.syncguard", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.session_stream",
        "prefix": "/api",
        "is_admin": False,
        "is_critical": False,
    },
    {
        "path": "api.routes.realtime_dashboard",
        "prefix": "",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.ci_dashboard_api", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.living_engine", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.scraper", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {"path": "api.routes.kaggle", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.dock_actions", "prefix": "/api", "is_admin": False, "is_critical": False},
    {"path": "api.routes.websocket_voice", "prefix": "", "is_admin": False, "is_critical": False},
    # R10 FIX: SSE shim for the WS /voice route
    {"path": "api.routes.stream_voice_sse", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "tools.collaborative_editor",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "tools.code.image_to_code", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "tools.learning.style_learner",
        "prefix": "/api",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.codeflow", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.feedback", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "tools.media.multilingual_tts",
        "prefix": "/api",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.voice", "prefix": "/api/voice", "is_admin": False, "is_critical": False},
    {"path": "tools.comment_thread_ai", "prefix": "/api", "is_admin": False, "is_critical": False},
    {"path": "api.routes.mobile_bff", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.payments", "prefix": "", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.maintenance",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.sandbox_api", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.pr_review_api", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.v1.telemetry", "prefix": "/api", "is_admin": False, "is_critical": False},
    {
        "path": "tools.social.telegram_bot",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.keys", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {
        "path": "api.routes.conversations",
        "prefix": "/api/v1",
        "is_admin": False,
        "is_critical": False,
    },
    # ---- Critical Routes ----
    {"path": "api.routes.llm_gateway_routes", "prefix": "", "is_admin": False, "is_critical": True},
    {"path": "api.routes.knowledge", "prefix": "/api", "is_admin": False, "is_critical": True},
    {"path": "api.routes.billing_api", "prefix": "", "is_admin": False, "is_critical": True},
    # ---- Admin & Health Routes ----
    {
        "path": "api.routes.health_aggregation",
        "prefix": "/api",
        "is_admin": False,
        "is_critical": False,
    },
    {"path": "api.routes.health", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    {"path": "api.routes.public_config", "prefix": "/api", "is_admin": False, "is_critical": False},
    {"path": "api.routes.preferences", "prefix": "/api", "is_admin": False, "is_critical": False},
    {"path": "api.routes.simulator_admin", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.site_actions", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.browser_routes", "prefix": "", "is_admin": True, "is_critical": False},
    {
        "path": "api.routes.hitl_admin",
        "prefix": "/api/v1/hitl",
        "is_admin": True,
        "is_critical": False,
    },
    {"path": "api.routes.evolution", "prefix": "/api/v1", "is_admin": True, "is_critical": False},
    {"path": "api.routes.meta_ai", "prefix": "/api/v1", "is_admin": True, "is_critical": False},
    {"path": "api.routes.admin_dashboard", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.internal", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.admin", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.traffic_monitor", "prefix": "", "is_admin": True, "is_critical": False},
    {
        "path": "api.routes.admin_librarian",
        "prefix": "/api",
        "is_admin": True,
        "is_critical": False,
    },
    {"path": "api.routes.tenant_admin", "prefix": "/api", "is_admin": True, "is_critical": False},
    {"path": "api.routes.metrics", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.cloud_mesh", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.tools_ops", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.execution_policies", "prefix": "", "is_admin": True, "is_critical": False},
    {"path": "api.routes.living_brain", "prefix": "", "is_admin": True, "is_critical": False},
    # ── Tier-S (all 12 routers via centralized registry) ──
    # CI FIX: Also register individual Tier-S modules directly so the API
    # contract diff analyzer can discover their @router decorators.
    # (tier_s_routes.py uses a tuple list, not @router decorators in-file.)
    {"path": "api.routes.ecosystem", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.global_memory", "prefix": "", "is_admin": False, "is_critical": False},
    {"path": "api.routes.zero_cost", "prefix": "/api/v1", "is_admin": False, "is_critical": False},
    # AUD-3.5 / Phase 4: the HITL approval REST surface was previously never
    # mounted (dead end: events were visible but could never be decided). The
    # router itself enforces verify_admin_session_fail_closed on every route.
    {
        "path": "api.routes.ecosystem_admin",
        "prefix": "",
        "is_admin": True,
        "is_critical": False,
    },
    {
        "path": "api.routes.approval_manager",
        "prefix": "",
        "is_admin": True,
        "is_critical": False,
    },
    # বাংলা: internet_monitor route আগে _safe_imports dict-এ ছিল যেটা কেউ consume করত না
    # (USAGE-A analysis-এ "ACTIVE LOADED, ROUTES UNWIRED" হিসেবে চিহ্নিত ছিল)।
    # এখন ALL_ROUTERS-এ registered — admin auth (get_current_admin) সব endpoint-এ আছে।
    {"path": "api.routes.internet_monitor", "prefix": "", "is_admin": True, "is_critical": False},
    # Audit fix (this session): service_topology (admin service health checker +
    # admin-token WebSocket health-stream consumed by the CI dashboard) was never
    # registered — doubly dead (missing ADMIN_URL_DEFAULT/SCRAPER_URL_DEFAULT
    # imports fixed in core/deployment_fallback_defaults.py + absent here).
    # Router enforces get_current_admin on routes and authenticate_websocket on
    # the WS endpoint; is_admin=True additionally applies the token dependency.
    {"path": "api.routes.service_topology", "prefix": "", "is_admin": True, "is_critical": False},
]


def register_all_routers(app: FastAPI) -> None:
    """Register all unified routers on the FastAPI app."""
    for router_def in ALL_ROUTERS:
        path = router_def["path"]
        prefix = router_def["prefix"]
        is_admin = router_def["is_admin"]
        is_critical = router_def["is_critical"]

        deps = [Depends(get_current_user_token)] if is_admin else None

        if is_critical:
            logger.info(f"Loading critical router: {path}")
            register_router(app, path, prefix=prefix, optional=False, dependencies=deps)
        else:
            register_router(app, path, prefix=prefix, optional=True, dependencies=deps)

    # BYOC Router logic remains unchanged
    if settings.encryption_key and settings.encryption_key.get_secret_value():
        register_router(app, "api.routes.byoc_api", "", optional=True)
    else:
        logger.warning("Universal BYOC router not loaded: ENCRYPTION_KEY missing")


def include_user_routers(app: FastAPI) -> None:
    """For compatibility/tests - registers non-admin routers."""
    for router_def in ALL_ROUTERS:
        if not router_def["is_admin"]:
            register_router(app, router_def["path"], prefix=router_def["prefix"], optional=True)


def include_admin_routers(app: FastAPI) -> None:
    """For compatibility/tests - registers admin routers."""
    for router_def in ALL_ROUTERS:
        if router_def["is_admin"]:
            deps = [Depends(get_current_user_token)]
            register_router(
                app,
                router_def["path"],
                prefix=router_def["prefix"],
                optional=True,
                dependencies=deps,
            )


__all__ = [
    "ALL_ROUTERS",
    "include_admin_routers",
    "include_user_routers",
    "register_all_routers",
]
