from fastapi import APIRouter

<<<<<<< HEAD
from core.maintenance_pipeline import maintenance_pipeline

=======
from core.maintenance_pipeline import MaintenancePipeline
>>>>>>> 8ae1b14453e95afe397d312602876b31584beca3


router = APIRouter(prefix="/maintenance", tags=["Maintenance"])


@router.get("/status")
async def get_maintenance_status():
    """
    Get the real-time status of the SupremeAI Immune System.
    """
    return await maintenance_pipeline.run_health_check()
