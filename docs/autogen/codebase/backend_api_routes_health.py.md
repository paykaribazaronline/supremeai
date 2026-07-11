# 📄 ফাইল: backend/api/routes/health.py

**প্রকার:** .py  
**সাইজ:** 1,115 বাইট  
**আপডেট:** 2026-07-11T14:41:19.338697

---

## কোড

```py
from fastapi import APIRouter
from pydantic import BaseModel

from core.services import registry


# Note: In app.py we registered this router with prefix="/api/health" but
# in the user's snippet they had router.post("/health/agents") without prefix
# in the router instantiation. I will ensure the final route matches /api/health/agents.
# Since app.py already has prefix "", I'll just use the exact snippet the user provided
# to avoid routing mismatches, but app.py prefix might be empty for health.
# Wait, in app.py we did `("api.routes.health", "")` which means no prefix.
# So I will use prefix="/api" in the router or just use the exact path from the snippet.

router = APIRouter()


class HealthRequest(BaseModel):
    agent_ids: list[str]


@router.post("/api/health/agents")
async def get_agents_health(request: HealthRequest):
    redis_mgr = registry.get_service("redis_manager")
    if not redis_mgr:
        return {"error": "Observability layer is offline."}

    # MGET কল করা হচ্ছে
    health_data = await redis_mgr.get_agents_health(request.agent_ids)
    return health_data

```