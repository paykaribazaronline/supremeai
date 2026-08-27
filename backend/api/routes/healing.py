from fastapi import APIRouter

router = APIRouter(tags=["healing"])

@router.get("/health/predictions")
async def get_predictions():
    return {"predictions": [], "status": "active"}

@router.get("/healing/stats")
async def get_stats():
    return {"remedies_applied": 0, "success_rate": 0.95}
