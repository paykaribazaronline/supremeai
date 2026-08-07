# বাংলা মন্তব্য: অব্যবহৃত ইম্পোর্ট (Depends, HTTPException, logger) সরিয়ে শুধু APIRouter রাখা হলো
from fastapi import APIRouter

router = APIRouter(prefix="/admin-api/commandcenter", tags=["Command Center"])


@router.get("/health")
async def command_health():
    return {"status": "ok"}


@router.get("/metrics")
async def command_metrics():
    return {}


@router.get("/events")
async def command_events():
    return []
