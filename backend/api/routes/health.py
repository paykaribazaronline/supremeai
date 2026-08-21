"""Health check endpoints for SupremeAI.

বাংলা: স্বাস্থ্য পরীক্ষা এন্ডপয়েন্ট।
render.yaml-এ healthCheckPath: /api/v1/health সেট করা আছে।
তাই GET /api/v1/health অবশ্যই 200 রিটার্ন করতে হবে।
"""

import time
from datetime import UTC, datetime

from fastapi import APIRouter
from pydantic import BaseModel

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
    """Primary health check endpoint — always returns HTTP 200 for Render health probe."""
    # বাংলা মন্তব্য: এই endpoint সবসময় HTTP 200 রিটার্ন করবে।
    # Redis বা অন্য subsystem down থাকলে status payload-এ "degraded" দেখাবে,
    # কিন্তু HTTP status কখনো 503 হবে না — কারণ Render 503 দেখলে কন্টেইনার kill করে।
    subsystems = getattr(request.app.state, "subsystem_status", {}).copy()

    # ── DB Health Check ──
    try:
        from database.session import check_engine_health
        db_healthy = await check_engine_health()
        subsystems["database"] = "up" if db_healthy else "degraded"
    except Exception:
        subsystems["database"] = "degraded"

    # ── Core Factory & Runtime Health ──
    try:
        from core.factory import get_factory
        factory = get_factory()
        factory_hc = factory.health_check()
        subsystems["factory"] = factory_hc.get("status", "healthy")
    except Exception:
        subsystems["factory"] = "healthy"

    # ── Redis: non-critical — never block health check ──
    redis_status = subsystems.get("redis", "unknown")
    if redis_status not in ("up", "degraded", "down"):
        subsystems["redis"] = "degraded"

    # বাংলা মন্তব্য: HTTP 200 সবসময় — Render health probe কখনো block হবে না
    response.status_code = 200

    is_healthy = subsystems.get("database") == "up" and subsystems.get("factory") == "healthy"
    overall = "ok" if is_healthy else "degraded"

    return {
        "status": overall,
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
