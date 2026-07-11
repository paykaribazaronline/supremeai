from fastapi import APIRouter
from core.maintenance_pipeline import MaintenancePipeline

router = APIRouter(prefix="/maintenance", tags=["Maintenance"])
pipeline = MaintenancePipeline()

@router.get("/status")
async def get_maintenance_status():
    """
    Get the real-time status of the SupremeAI Immune System.
    """
    return await pipeline.run_health_check()
