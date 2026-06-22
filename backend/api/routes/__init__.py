_safe_imports = {}

try:
    from .admin_dashboard import router as admin_dashboard_router
    _safe_imports["admin_dashboard_router"] = admin_dashboard_router
except Exception:
    admin_dashboard_router = None

try:
    from .agent_tasks import agent_router as agent_router
    _safe_imports["agent_router"] = agent_router
except Exception:
    agent_router = None

try:
    from .auth import router as auth_router
    _safe_imports["auth_router"] = auth_router
except Exception:
    auth_router = None

try:
    from .async_task_router import router as async_task_router
    _safe_imports["async_task_router"] = async_task_router
except Exception:
    async_task_router = None

try:
    from .cdc_webhooks import router as cdc_router
    _safe_imports["cdc_router"] = cdc_router
except Exception:
    cdc_router = None

try:
    from .browser import router as browser_router
    _safe_imports["browser_router"] = browser_router
except Exception:
    browser_router = None

try:
    from .codeflow import router as codeflow_router
    _safe_imports["codeflow_router"] = codeflow_router
except Exception:
    codeflow_router = None

try:
    from .feedback import router as feedback_router
    _safe_imports["feedback_router"] = feedback_router
except Exception:
    feedback_router = None

try:
    from .knowledge import router as knowledge_router
    _safe_imports["knowledge_router"] = knowledge_router
except Exception:
    knowledge_router = None

try:
    from .marketplace import router as marketplace_router
    _safe_imports["marketplace_router"] = marketplace_router
except Exception:
    marketplace_router = None

try:
    from .media import router as media_router
    _safe_imports["media_router"] = media_router
except Exception:
    media_router = None

try:
    from .memory import router as memory_router
    _safe_imports["memory_router"] = memory_router
except Exception:
    memory_router = None

try:
    from .metrics import router as metrics_router
    _safe_imports["metrics_router"] = metrics_router
except Exception:
    metrics_router = None

try:
    from .simulator import router as simulator_router
    _safe_imports["simulator_router"] = simulator_router
except Exception:
    simulator_router = None

try:
    from .stream import router as stream_router
    _safe_imports["stream_router"] = stream_router
except Exception:
    stream_router = None

try:
    from .task import router as task_router
    _safe_imports["task_router"] = task_router
except Exception:
    task_router = None

try:
    from .email import router as email_router
    _safe_imports["email_router"] = email_router
except Exception:
    email_router = None

try:
    from .github import router as github_router
    _safe_imports["github_router"] = github_router
except Exception:
    github_router = None

try:
    from .internal import router as internal_router
    _safe_imports["internal_router"] = internal_router
except Exception:
    internal_router = None

try:
    from api.marketplace import router as marketplace_endpoints_router
    _safe_imports["marketplace_endpoints_router"] = marketplace_endpoints_router
except Exception:
    marketplace_endpoints_router = None

try:
    from .sso import router as sso_router
    _safe_imports["sso_router"] = sso_router
except Exception:
    sso_router = None

try:
    from .repos import router as repos_router
    _safe_imports["repos_router"] = repos_router
except Exception:
    repos_router = None

try:
    from .tools_ops import router as tools_ops_router
    _safe_imports["tools_ops_router"] = tools_ops_router
except Exception:
    tools_ops_router = None

try:
    from .onboarding import router as onboarding_router
    _safe_imports["onboarding_router"] = onboarding_router
except Exception:
    onboarding_router = None

try:
    from .tools_registry import router as tools_registry_router
    _safe_imports["tools_registry_router"] = tools_registry_router
except Exception:
    tools_registry_router = None

try:
    from .preferences import router as preferences_router
    _safe_imports["preferences_router"] = preferences_router
except Exception:
    preferences_router = None

try:
    from .usage_metrics import router as usage_metrics_router
    _safe_imports["usage_metrics_router"] = usage_metrics_router
except Exception:
    usage_metrics_router = None

try:
    from .agents import router as agents_router
    _safe_imports["agents_router"] = agents_router
except Exception:
    agents_router = None

try:
    from .payments import router as payments_router
    _safe_imports["payments_router"] = payments_router
except Exception:
    payments_router = None

__all__ = list(_safe_imports.keys())

