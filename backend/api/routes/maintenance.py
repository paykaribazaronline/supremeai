from fastapi import APIRouter

# গিট মার্জ কনফ্লিক্ট ম্যানুয়ালি সলভ করা হয়েছে। maintenance_pipeline অবজেক্টটি ইমপোর্ট করা হলো কারণ এটি নিচে ব্যবহৃত হচ্ছে।
from core.maintenance_pipeline import maintenance_pipeline


router = APIRouter(prefix="/maintenance", tags=["Maintenance"])


@router.get("/status")
async def get_maintenance_status():
    """
    Get the real-time status of the SupremeAI Immune System.
    """
    return await maintenance_pipeline.run_health_check()
