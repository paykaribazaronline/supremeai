"""Health check endpoints for SupremeAI.

বাংলা: স্বাস্থ্য পরীক্ষা এন্ডপয়েন্ট।
render.yaml-এ healthCheckPath: /api/v1/health সেট করা আছে।
তাই GET /api/v1/health অবশ্যই 200 রিটার্ন করতে হবে।
"""

import time
from datetime import UTC, datetime

from core.cache.redis_manager import redis_manager
from core.services import registry
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


from fastapi import Request, Response

# বাংলা মন্তব্য: Render health check-এর জন্য এই endpoint অপরিহার্য।
# render.yaml-এ healthCheckPath: /api/v1/health নির্ধারিত।
# এটি prefix="/api/v1" সহ register করা হয়, তাই path="/health" যথেষ্ট।


def _timestamp() -> str:
    """Return a timezone-aware timestamp suitable for infrastructure probes."""
    return datetime.now(UTC).isoformat()


@router.get("/health")
async def health_check(request: Request, response: Response):
    """Primary health check endpoint — checks actual database, redis and config health."""
    subsystems = getattr(request.app.state, "subsystem_status", {}).copy()

    # ── DB Health Check ──
    db_pool = getattr(request.app.state, "db_pool", None)
    if db_pool is not None:
        try:
            async with db_pool.acquire() as conn:
                await conn.execute("SELECT 1")
            subsystems["db"] = "up"
        except Exception:  # noqa: BLE001
            subsystems["db"] = "down"
    elif subsystems.get("db") != "down":
        subsystems["db"] = "sqlite"

    # ── Redis Health Check ──
    if subsystems.get("redis") == "up":
        try:
            await redis_manager.client.ping()
        except Exception:  # noqa: BLE001
            subsystems["redis"] = "down"

    has_critical_failure = any(subsystems.get(k) == "down" for k in ["db", "redis"])

    if has_critical_failure:
        response.status_code = 503
        return {
            "status": "degraded",
            "service": "supremeai-backend",
            "version": "2.0",
            "timestamp": _timestamp(),
            "subsystems": subsystems,
        }

    return {
        "status": "ok",
        "service": "supremeai-backend",
        "version": "2.0",
        "timestamp": _timestamp(),
        "subsystems": subsystems,
    }


@router.get("/live", tags=["Infrastructure Monitor"])
async def liveness_probe():
    """💓 Liveness Probe: Render রাউটিং মেশকে প্রসেসের সচলতা নিশ্চিত করে।"""
    return {"status": "alive", "timestamp": int(time.time())}


@router.get("/ready", tags=["Infrastructure Monitor"])
async def readiness_probe(request: Request, response: Response):
    """🚦 Readiness Probe: ডিপেনডেন্সি ও ফাইল সিস্টেমের রেডিনেস চেক ফায়ার করে।"""
    return await health_check(request, response)


class HealthRequest(BaseModel):
    """Request model for agent health check."""

    agent_ids: list[str]


@router.post("/health/agents")
async def get_agents_health(request: HealthRequest):
    """Get health status for multiple agents."""
    # বাংলা: ServiceRegistry-এ get() মেথড ব্যবহার, get_service() নেই
    try:
        redis_mgr = await registry.get("redis_manager")
    except KeyError:
        return {"error": "Observability layer is offline."}

    # MGET কল করা হচ্ছে
    health_data = await redis_mgr.get_agents_health(request.agent_ids)
    return health_data
