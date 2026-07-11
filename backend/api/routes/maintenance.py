from fastapi import APIRouter

from core.maintenance_pipeline import maintenance_pipeline


router = APIRouter(prefix="/maintenance", tags=["Maintenance"])

@router.get("/status")
async def get_maintenance_status():
    """
    Get the real-time status of the SupremeAI Immune System.
    """
    return await maintenance_pipeline.run_health_check()
