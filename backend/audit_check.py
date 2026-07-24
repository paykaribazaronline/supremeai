import os

base = r"c:\Users\n\supremeai\supremeai_2.0"

# Check traffic_monitor.py for Depends
path = os.path.join(base, "backend/api/routes/traffic_monitor.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_depends = "Depends" in content
    has_admin_auth = (
        "get_current_admin" in content
        or "require_admin" in content
        or "verify_admin" in content
    )

# Check auth.py role assignment
path = os.path.join(base, "backend/api/routes/auth.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_substring_match = "'admin' in body.username" in content
    has_admin_emails = "admin_emails" in content
    for i, line in enumerate(content.split("\n")):
        if "admin" in line.lower() and "role" in line.lower():
            pass

# Check telemetry.py tracer setup
path = os.path.join(base, "backend/core/observability/telemetry.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    uses_globals = "globals()" in content
    has_public_tracer = "tracer: Tracer | None = None" in content

# Check auth_middleware.py admin paths
path = os.path.join(base, "backend/core/security/auth_middleware.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_api_admin = "/api/admin/" in content
    for line in content.split("\n"):
        if "admin_paths" in line or "/admin/" in line:
            pass

# Check secret_vault.py
path = os.path.join(base, "backend/core/security/secret_vault.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_prod_check = "production" in content.lower() and "allow_env" in content.lower()
    has_type_hint_fix = "str | None = None" in content

# Check agent_orchestrator.py
path = os.path.join(base, "backend/core/orchestration/agent_orchestrator.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_prod_fallback_log = "production" in content and "fallback" in content.lower()
    has_safe_get_queue = "logger.error" in content

# Check admin endpoints in admin.py
path = os.path.join(base, "backend/api/routes/admin.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    rules_has_depends = "Depends" in content and "get_current_admin" in content
    # Check if POST /rules has Depends
    if '@router.post("/rules")' in content:
        lines = content.split("\n")
        in_rules = False
        rules_depends = False
        for i, line in enumerate(lines):
            if '@router.post("/rules")' in line:
                in_rules = True
            if in_rules and "Depends" in line:
                rules_depends = True
                break
            if in_rules and line.strip().startswith("@router.post"):
                break

    if '@router.post("/actions/{action_type}")' in content:
        lines = content.split("\n")
        in_actions = False
        actions_depends = False
        for i, line in enumerate(lines):
            if '@router.post("/actions/{action_type}")' in line:
                in_actions = True
            if in_actions and "Depends" in line:
                actions_depends = True
                break
            if in_actions and line.strip().startswith("@router.post"):
                break

# Check admin router files for Depends at router level
admin_files = [
    "backend/api/routes/llm_gateway.py",
    "backend/api/routes/cloud_mesh.py",
    "backend/api/routes/site_actions.py",
    "backend/api/routes/tenant_admin.py",
    "backend/api/routes/metrics.py",
]
for relpath in admin_files:
    path = os.path.join(base, relpath)
    with open(path, encoding="utf-8") as f:
        content = f.read()
        has_depends_anywhere = "Depends" in content

# Check billing_api.py for webhook
path = os.path.join(base, "backend/api/routes/billing_api.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_stripe_webhook = "stripe" in content.lower() and "webhook" in content.lower()
    has_wallet_credit = "balance" in content.lower() and "Wallet" in content

# Check dock_actions.py for bare except
path = os.path.join(base, "backend/api/routes/dock_actions.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_github_exception_check = "GithubException" in content

# Check codegraph_integration.py for exception handling
path = os.path.join(base, "scripts/codegraph_integration.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_logger_debug = "logger.debug" in content and "logger.warning" in content
    has_except_bare_pass = (
        "except: pass" in content
        or "except:    pass" in content
        or "except:  pass" in content
    )

# Check execution_policies.py for admin Depends
path = os.path.join(base, "backend/api/routes/execution_policies.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_require_admin = "require_admin" in content

# Check selector_healing.py for admin Depends
path = os.path.join(base, "backend/api/routes/selector_healing.py")
with open(path, encoding="utf-8") as f:
    content = f.read()
    has_admin_depends = (
        "get_current_admin" in content
        or "require_admin" in content
        or "verify_admin" in content
    )
