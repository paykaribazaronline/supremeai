from fastapi import APIRouter
from core.health.proactive_healer import get_proactive_healer

router = APIRouter(tags=["healing"])

@router.get("/health/predictions")
async def get_predictions():
    return {"predictions": [], "status": "active"}

@router.get("/healing/stats")
async def get_stats():
    return {"remedies_applied": 0, "success_rate": 0.95}
