# 📄 ফাইল: backend/api/routes/__init__.py

**প্রকার:** .py  
**সাইজ:** 12,911 বাইট  
**আপডেট:** 2026-07-08T11:32:31.864174

---

## কোড

```py
_safe_imports = {}

try:
    from .approval_manager import router as approval_manager_router

    _safe_imports["approval_manager_router"] = approval_manager_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for approval_manager_router: {traceback.format_exc()}")
    approval_manager_router = None

try:
    from .admin_dashboard import router as admin_dashboard_router

    _safe_imports["admin_dashboard_router"] = admin_dashboard_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for admin_dashboard_router: {traceback.format_exc()}")
    admin_dashboard_router = None

try:
    from .agent_tasks import agent_router

    _safe_imports["agent_router"] = agent_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for agent_router: {traceback.format_exc()}")
    agent_router = None

try:
    from .auth import router as auth_router

    _safe_imports["auth_router"] = auth_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for auth_router: {traceback.format_exc()}")
    auth_router = None

try:
    from .async_task_router import router as async_task_router

    _safe_imports["async_task_router"] = async_task_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for async_task_router: {traceback.format_exc()}")
    async_task_router = None

try:
    from .cdc_webhooks import router as cdc_router

    _safe_imports["cdc_router"] = cdc_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for cdc_router: {traceback.format_exc()}")
    cdc_router = None

try:
    from .browser import router as browser_router

    _safe_imports["browser_router"] = browser_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for browser_router: {traceback.format_exc()}")
    browser_router = None

try:
    from .codeflow import router as codeflow_router

    _safe_imports["codeflow_router"] = codeflow_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for codeflow_router: {traceback.format_exc()}")
    codeflow_router = None

try:
    from .feedback import router as feedback_router

    _safe_imports["feedback_router"] = feedback_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for feedback_router: {traceback.format_exc()}")
    feedback_router = None

try:
    from .knowledge import router as knowledge_router

    _safe_imports["knowledge_router"] = knowledge_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for knowledge_router: {traceback.format_exc()}")
    knowledge_router = None

try:
    from .marketplace_endpoints import router as marketplace_router

    _safe_imports["marketplace_router"] = marketplace_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for marketplace_router: {traceback.format_exc()}")
    marketplace_router = None

try:
    from .media import router as media_router

    _safe_imports["media_router"] = media_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for media_router: {traceback.format_exc()}")
    media_router = None

try:
    from .memory import router as memory_router

    _safe_imports["memory_router"] = memory_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for memory_router: {traceback.format_exc()}")
    memory_router = None

try:
    from .metrics import router as metrics_router

    _safe_imports["metrics_router"] = metrics_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for metrics_router: {traceback.format_exc()}")
    metrics_router = None

# বাংলা মন্তব্য: site_actions_registry CRUD রাউটার — অ্যাডমিন ড্যাশবোর্ডের ভিজুয়াল এডিটরের জন্য
try:
    from .site_actions import router as site_actions_router

    _safe_imports["site_actions_router"] = site_actions_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for site_actions_router: {traceback.format_exc()}")
    site_actions_router = None

# বাংলা মন্তব্য: LLM Gateway ও System Rules কন্ট্রোলার রাউটার
try:
    from .llm_gateway import router as llm_gateway_router

    _safe_imports["llm_gateway_router"] = llm_gateway_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for llm_gateway_router: {traceback.format_exc()}")
    llm_gateway_router = None

try:
    from .simulator import router as simulator_router

    _safe_imports["simulator_router"] = simulator_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for simulator_router: {traceback.format_exc()}")
    simulator_router = None

try:
    from .stream import router as stream_router

    _safe_imports["stream_router"] = stream_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for stream_router: {traceback.format_exc()}")
    stream_router = None

try:
    from .task import router as task_router

    _safe_imports["task_router"] = task_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for task_router: {traceback.format_exc()}")
    task_router = None

try:
    from .email import router as email_router

    _safe_imports["email_router"] = email_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for email_router: {traceback.format_exc()}")
    email_router = None

try:
    from .github import router as github_router

    _safe_imports["github_router"] = github_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for github_router: {traceback.format_exc()}")
    github_router = None

try:
    from .internal import router as internal_router

    _safe_imports["internal_router"] = internal_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for internal_router: {traceback.format_exc()}")
    internal_router = None

try:
    from .config import router as config_router

    _safe_imports["config_router"] = config_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for config_router: {traceback.format_exc()}")
    config_router = None

try:
    from .sso import router as sso_router

    _safe_imports["sso_router"] = sso_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for sso_router: {traceback.format_exc()}")
    sso_router = None

try:
    from .repos import router as repos_router

    _safe_imports["repos_router"] = repos_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for repos_router: {traceback.format_exc()}")
    repos_router = None

try:
    from .tools_ops import router as tools_ops_router

    _safe_imports["tools_ops_router"] = tools_ops_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for tools_ops_router: {traceback.format_exc()}")
    tools_ops_router = None

try:
    from .voice import router as voice_router

    _safe_imports["voice_router"] = voice_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for voice_router: {traceback.format_exc()}")
    voice_router = None

try:
    from .onboarding import router as onboarding_router

    _safe_imports["onboarding_router"] = onboarding_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for onboarding_router: {traceback.format_exc()}")
    onboarding_router = None

try:
    from .tools_registry import router as tools_registry_router

    _safe_imports["tools_registry_router"] = tools_registry_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for tools_registry_router: {traceback.format_exc()}")
    tools_registry_router = None

try:
    from .preferences import router as preferences_router

    _safe_imports["preferences_router"] = preferences_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for preferences_router: {traceback.format_exc()}")
    preferences_router = None

try:
    from .usage_metrics import router as usage_metrics_router

    _safe_imports["usage_metrics_router"] = usage_metrics_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for usage_metrics_router: {traceback.format_exc()}")
    usage_metrics_router = None

try:
    from .agents import router as agents_router

    _safe_imports["agents_router"] = agents_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for agents_router: {traceback.format_exc()}")
    agents_router = None

try:
    from .payments import router as payments_router

    _safe_imports["payments_router"] = payments_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for payments_router: {traceback.format_exc()}")
    payments_router = None

try:
    from .markdown import router as markdown_router

    _safe_imports["markdown_router"] = markdown_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for markdown_router: {traceback.format_exc()}")
    markdown_router = None

try:
    from .api_keys import router as api_keys_router

    _safe_imports["api_keys_router"] = api_keys_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for api_keys_router: {traceback.format_exc()}")
    api_keys_router = None

try:
    from .graph import router as graph_router

    _safe_imports["graph_router"] = graph_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for graph_router: {traceback.format_exc()}")
    graph_router = None

try:
    from .ci_webhooks import router as ci_webhooks_router

    _safe_imports["ci_webhooks_router"] = ci_webhooks_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for ci_webhooks_router: {traceback.format_exc()}")
    ci_webhooks_router = None

try:
    from .websocket_voice import router as websocket_voice_router
    _safe_imports["websocket_voice_router"] = websocket_voice_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for websocket_voice_router: {traceback.format_exc()}")
    websocket_voice_router = None

try:
    from .integrations import router as integrations_router
    _safe_imports["integrations_router"] = integrations_router
except Exception:  # noqa: BLE001
    import traceback

    from loguru import logger
    logger.warning(f"Router import failed for integrations_router: {traceback.format_exc()}")
    integrations_router = None


__all__ = list(_safe_imports.keys()) + ["voice_router", "websocket_voice_router", "integrations_router"]

```