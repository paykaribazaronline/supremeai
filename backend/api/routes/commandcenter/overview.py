from fastapi import APIRouter, HTTPException
from loguru import logger
from pydantic import BaseModel

router = APIRouter(prefix="/admin-api/commandcenter", tags=["Command Center"])


class OverviewResponse(BaseModel):
    active_agents: int
    active_tasks: int
    requests_per_second: float
    latency_p95_ms: float
    error_rate: float
    cost_per_hour: float
    health_percent: float


@router.get("/overview", response_model=OverviewResponse)
async def get_overview():
    try:
        return OverviewResponse(
            active_agents=0,
            active_tasks=0,
            requests_per_second=0.0,
            latency_p95_ms=0.0,
            error_rate=0.0,
            cost_per_hour=0.0,
            health_percent=100.0,
        )
    except Exception as exc:
        logger.error(f"Command center overview failed: {exc}")
        raise HTTPException(status_code=503, detail="Overview unavailable")
