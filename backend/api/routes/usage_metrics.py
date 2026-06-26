from fastapi import APIRouter
from fastapi import HTTPException
from fastapi import Query
from pydantic import BaseModel

from database.supabase_client import db


router = APIRouter(prefix="/metrics/usage", tags=["usage-metrics"])


class UsageMetricUpsert(BaseModel):
    metric_date: str
    total_requests: int
    total_tokens: int
    total_cost: float
    unique_users: int
    avg_latency_ms: int
    error_rate: float


@router.get("/")
async def get_usage_metrics(
    start: str | None = None,
    end: str | None = None,
    limit: int = Query(default=30, le=365),
):
    if not db.client:
        return {"items": [], "total": 0}
    try:
        query = db.client.table("usage_metrics").select("*")
        if start:
            query = query.gte("date", start)
        if end:
            query = query.lte("date", end)
        res = query.order("date", desc=True).limit(limit).execute()
        return {"items": res.data or [], "total": len(res.data or [])}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@router.post("/")
async def upsert_usage_metric(payload: UsageMetricUpsert):
    if not db.client:
        raise HTTPException(status_code=503, detail="Database not configured")
    try:
        data = payload.dict()
        res = db.client.table("usage_metrics").upsert(data).execute()
        return {"status": "success", "metric": res.data[0] if res.data else data}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))
