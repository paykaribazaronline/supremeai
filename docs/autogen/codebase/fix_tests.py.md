# 📄 ফাইল: fix_tests.py

**প্রকার:** .py  
**সাইজ:** 2,155 বাইট  
**আপডেট:** 2026-07-11T20:08:21.307085

---

## কোড

```py
import re

filepath = 'backend/core/app.py'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
if 'from core import services' not in content:
    content = content.replace('from core import lifespan', 'from core import lifespan\nfrom core import services\nfrom typing import Any')

# Add health and actuator endpoints
endpoints = """
@app.get("/health")
async def health() -> dict[str, Any]:
    redis_ok = False
    if hasattr(services, 'redis_queue') and services.redis_queue.configured:
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
"""
content = content.replace('app.include_router(admin_router)', endpoints)

# Restore core_routers
routes_to_add = """    ("api.routes.marketplace", ""),
    ("api.routes.auth", "/api/v1"),
    ("api.routes.onboarding", "/api/v1/onboarding"),
    ("api.routes.evolution", "/api/v1/evolution"),"""

content = content.replace('    ("api.routes.marketplace", ""),', routes_to_add)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Patch applied")

```