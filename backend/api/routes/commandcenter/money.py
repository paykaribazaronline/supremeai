from fastapi import APIRouter

router = APIRouter(prefix="/admin-api/commandcenter", tags=["Command Center"])


@router.get("/money/cost")
async def get_cost():
    return {"report": "", "generated_at": ""}


@router.get("/money/usage")
async def get_usage():
    return {"daily": [], "cost_projected_monthly": 0, "cost_per_hour": 0}


@router.get("/money/budget")
async def get_budget():
    return {"default_cap": 0, "per_tenant": {}}


@router.post("/money/budget")
async def update_budget(payload: dict):
    return {"message": "updated"}


@router.get("/money/roi")
async def get_roi():
    return {
        "semantic_cache_hits": 0,
        "estimated_usd_saved": 0,
        "duplicate_executions_prevented": 0,
        "api_cost_reduction_ratio": 0,
    }
