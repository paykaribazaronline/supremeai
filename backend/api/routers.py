"""Centralized router registration for SupremeAI API."""

from __future__ import annotations

from fastapi import FastAPI
from loguru import logger

from api import register_router
from core.config import settings

core_routers: list[tuple[str, str]] = [
    ("api.routes.memory", ""),
    ("api.routes.task", ""),
    ("api.routes.markdown", "/api/v1"),
    ("api.routes.simulator", ""),
    ("api.routes.site_actions", ""),
    # বাংলা মন্তব্য: llm_gateway ইচ্ছাকৃতভাবে optional_routers-এ সরানো হয়েছে।
    # এটি দুইবার bad import-এর কারণে পুরো test suite crash করেছে (cv2 এবং core.auth)।
    # optional রাখলে ভবিষ্যতে একটি broken route file পুরো app object poison করতে পারবে না।
    ("api.routes.browser", ""),
    ("api.routes.stream", ""),
    ("api.routes.media", ""),
    ("api.routes.graph", ""),
    ("api.routes.marketplace_endpoints", ""),
    ("api.routes.auth", "/api/v1"),
    ("api.routes.onboarding", "/api/v1"),
    ("api.routes.evolution", "/api/v1"),
    ("api.routes.meta_ai", "/api/v1"),
    ("api.routes.localization", "/api/v1"),
    ("api.routes.analytics", "/api/v1"),
    ("api.routes.admin_dashboard", ""),
    ("api.routes.email", ""),
    ("api.routes.github", ""),
    ("api.routes.internal", ""),
    ("api.routes.config", ""),
    ("api.routes.repos", ""),
    ("api.routes.tools_ops", ""),
    ("api.routes.agents", ""),
    ("api.routes.agent", ""),
    ("api.routes.admin", ""),
    ("api.routes.tools_registry", ""),
    # বাংলা মন্তব্য: AUDIT-018 ফিক্স — এই router-টা আগে কোথাও register-ই করা হয়নি,
    # ফলে frontend-এর skillsService.fetchSkillCatalog() /api/skills/catalog-এ
    # সবসময় 404 পেত। router-এর নিজস্ব prefix ("/skills") + এখানে "/api" যোগ করলে
    # final path মিলে যায়।
    ("api.routes.skills", "/api"),
    ("api.routes.preferences", "/api"),
    ("api.routes.usage_metrics", ""),
    ("api.routes.sso", ""),
    ("api.routes.health", "/api/v1"),
    ("api.routes.api_keys", ""),
    ("api.routes.ci_webhooks", ""),
    ("api.routes.task_workspace", "/api/v1"),
    ("api.routes.websocket_agent", ""),
    ("api.routes.agent_workspace", "/api/v1"),
    ("api.routes.integrations", "/api/v1"),
    ("api.routes.public_config", "/api"),
    ("api.routes.traffic_monitor", ""),
    ("api.routes.agent_action", "/api/v1"),
    ("api.routes.websocket_hitl", ""),
    ("api.routes.syncguard", "/api/v1"),
    ("api.routes.admin_librarian", "/api"),
    # বাংলা মন্তব্ব্য: এই রাউটারটি আগে এখানে যোগই করা হয়নি — ফলে /api/v1/swarm/*
    # (real-time SSE stream, patch-telemetry persistence, VSCode self-healing
    # endpoint, এবং নতুন emergency-stop /halt+/resume) সব HTTP 404 দিত।
    # Kill-switch ও Swarm Health স্ক্রিন কাজ না করার আসল root cause এটিই ছিল।
    ("api.routes.swarm", "/api/v1/swarm"),
    # Added real-time dashboard WebSocket endpoint for Phase 2.1 of roadmap
    ("api.routes.realtime_dashboard", ""),
]

optional_routers: list[tuple[str, str]] = [
    # বাংলা মন্তব্য: llm_gateway এখানে optional — ব্যর্থ হলে warn করে, পুরো app crash করে না।
    # Systemic fix: core_routers থেকে এখানে সরানো হয়েছে (দেখুন উপরের কমেন্ট)।
    ("api.routes.llm_gateway", ""),
    # বাংলা মন্তব্ব্য: chromadb নির্ভর হওয়ায় নলেজ বেস রাউটারটিকে অপশনাল হিসেবে রেজিস্টার করা হলো
    ("api.routes.knowledge", ""),
    ("api.routes.dock_actions", "/api"),
    ("api.routes.websocket_voice", ""),
    ("tools.collaborative_editor", "/api/v1"),
    ("tools.image_to_code", ""),
    ("tools.style_learner", "/api"),
    ("api.routes.codeflow", ""),
    ("api.routes.feedback", ""),
    ("tools.media.multilingual_tts", "/api"),
    ("api.routes.voice", "/api/voice"),
    ("tools.comment_thread_ai", "/api"),
    ("api.routes.tenant_admin", "/api"),
    ("api.routes.mobile_bff", ""),
    ("api.routes.billing_api", ""),
    ("api.routes.metrics", ""),
    ("api.routes.cloud_mesh", ""),
    ("api.routes.events", "/api"),
    ("api.routes.payments", ""),
    ("api.routes.maintenance", "/api/v1"),
    ("api.routes.sandbox_api", ""),
    ("api.routes.pr_review_api", ""),
    # Added telemetry router for performance monitoring and system health
    ("api.v1.telemetry", "/api/v1"),
]


# Identify admin router paths
# বাংলা মন্তব্ব্য: tools_ops যোগ করা হলো — এটি DevOps/deploy টুলিং (docker-compose/helm
# ফাইল-রাইট সহ) যা আগে ভুলবশত User API-তে এক্সপোজড ছিল (route-leakage)।
_admin_paths = {
    "api.routes.simulator_admin",
    "api.routes.site_actions",
    "api.routes.llm_gateway",
    "api.routes.browser",
    "api.routes.evolution",
    "api.routes.meta_ai",
    "api.routes.admin_dashboard",
    "api.routes.internal",
    "api.routes.admin",
    "api.routes.traffic_monitor",
    "api.routes.admin_librarian",
    "api.routes.tenant_admin",
    "api.routes.metrics",
    "api.routes.cloud_mesh",
    "api.routes.tools_ops",
}

# ADMIN_ROUTERS includes health and specific admin routes
# বাংলা মন্তব্ব্য: অ্যাডমিন এপিআই রাউটারসমূহ
ADMIN_ROUTERS: list[tuple[str, str]] = [
    ("api.routes.health", "/api/v1"),
    # বাংলা মন্তব্ব্য: অ্যাডমিন পোর্টালে গ্লোবাল কনফিগারেশন লোড করার জন্য public_config রাউটার যুক্ত করা হলো
    ("api.routes.public_config", "/api"),
    # বাংলা মন্তব্য: অ্যাডমিন প্যানেলের safe-default preferences স্ট্রিম (/api/preferences/default/stream)
    # রাউটারটি আগে শুধু core_routers-এ ছিল, তাই অ্যাডমিন অ্যাপে 404 দিচ্ছিল। এখানে যোগ করা হলো।
    ("api.routes.preferences", "/api"),
    ("api.routes.simulator_admin", ""),
    ("api.routes.site_actions", ""),
    ("api.routes.llm_gateway", ""),
    ("api.routes.browser", ""),
    ("api.routes.evolution", "/api/v1"),
    ("api.routes.meta_ai", "/api/v1"),
    ("api.routes.admin_dashboard", ""),
    ("api.routes.internal", ""),
    ("api.routes.admin", ""),
    ("api.routes.traffic_monitor", ""),
    ("api.routes.admin_librarian", "/api"),
    ("api.routes.tenant_admin", "/api"),
    ("api.routes.metrics", ""),
    ("api.routes.cloud_mesh", ""),
    ("api.routes.tools_ops", ""),
]

# USER_ROUTERS is all other routers
# বাংলা মন্তব্ব্য: ইউজার এপিআই রাউটারসমূহ
USER_ROUTERS: list[tuple[str, str]] = [r for r in (core_routers + optional_routers) if r[0] not in _admin_paths]


def register_all_routers(app: FastAPI) -> None:
    """Register all core and optional routers on the FastAPI app."""
    for router_path, prefix in core_routers:
        register_router(app, router_path, prefix=prefix)
    for router_path, prefix in optional_routers:
        register_router(app, router_path, prefix=prefix, optional=True)
    if settings.encryption_key and settings.encryption_key.get_secret_value():
        register_router(app, "api.routes.byoc_api", "", optional=True)
    else:
        logger.warning("Universal BYOC router not loaded: ENCRYPTION_KEY missing")


def include_user_routers(app: FastAPI) -> None:
    """Register all user/client-facing routers on the FastAPI app."""
    for router_path, prefix in USER_ROUTERS:
        register_router(app, router_path, prefix=prefix, optional=True)
    if settings.encryption_key and settings.encryption_key.get_secret_value():
        register_router(app, "api.routes.byoc_api", "", optional=True)


def include_admin_routers(app: FastAPI) -> None:
    """Register all admin-facing routers on the FastAPI app."""
    for router_path, prefix in ADMIN_ROUTERS:
        register_router(app, router_path, prefix=prefix, optional=True)


__all__ = [
    "register_all_routers",
    "include_user_routers",
    "include_admin_routers",
    "core_routers",
    "optional_routers",
    "USER_ROUTERS",
    "ADMIN_ROUTERS",
]
