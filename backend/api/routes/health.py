"""Health check endpoints for SupremeAI.

বাংলা: স্বাস্থ্য পরীক্ষা এন্ডপয়েন্ট।
render.yaml-এ healthCheckPath: /api/v1/health সেট করা আছে।
তাই GET /api/v1/health অবশ্যই 200 রিটার্ন করতে হবে।
"""

import time
from datetime import UTC, datetime

from fastapi import APIRouter
from loguru import logger
from pydantic import BaseModel

from core.cache.redis_manager import redis_manager
from core.services import registry

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
            # বাংলা: db_pool.acquire() একটি plain coroutine রিটার্ন করে (async context
            # manager নয়) — এটিকে সরাসরি `async with` দিয়ে ব্যবহার করলে
            # `__aenter__` না থাকায় AttributeError হয় এবং coroutine 'never awaited'
            # warning তৈরি হয়, ফলে health check প্রতিবার 503 দিত (Render deploy
            # verification ব্যর্থ হওয়ার মূল কারণ)। PgBouncerConnectionPool-এর
            # @asynccontextmanager-সজ্জিত `.connection()` মেথড এখানে ব্যবহার করা হলো,
            # যা acquire+release উভয়ই নিরাপদভাবে হ্যান্ডেল করে।
            async with db_pool.connection() as conn:
                await conn.execute("SELECT 1")
            subsystems["db"] = "up"
        except Exception as e:
            # বাংলা: debug লেভেলে লগ করা হচ্ছে (warning না) যাতে ঘন ঘন health-check
            # poll-এ log spam না হয়, কিন্তু কারণ ট্রেস করার সুযোগ থাকে
            logger.debug(f"DB health check failed: {e}")
            subsystems["db"] = "down"
    elif subsystems.get("db") != "down":
        subsystems["db"] = "sqlite"

    # ── Redis Health Check ──
    if subsystems.get("redis") == "up":
        try:
            await redis_manager.client.ping()
        except Exception as e:
            logger.debug(f"Redis health check failed: {e}")
    # বাংলা মন্তব্য: Redis হলো optional/fallback cache layer — এটি down থাকলেও সিস্টেম সচল থাকে।
    # Render infrastructure probe যেন 503 পেয়ে কন্টেইনার ফেল না করে, সেজন্য শুধু DB down হলেই 503 দেওয়া হবে।
    has_critical_failure = subsystems.get("db") == "down"

    if has_critical_failure:
        response.status_code = 503
        return {
            "status": "unhealthy",
            "service": "supremeai-backend",
            "version": "2.0",
            "timestamp": _timestamp(),
            "subsystems": subsystems,
        }

    is_degraded = any(subsystems.get(k) == "down" for k in ["redis"])
    return {
        "status": "degraded" if is_degraded else "ok",
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
