from fastapi import APIRouter
from pydantic import BaseModel
from brain.economic_optimizer import get_economic_optimizer, BudgetContext

router = APIRouter(prefix="/economics", tags=["economics"])

class RouteRequest(BaseModel):
    prompt: str
    task_type: str = "general"
    user_id: str
    monthly_limit: float
    spent_this_month: float
    cost_sensitivity: float

@router.post("/optimize-route")
async def optimize_route(req: RouteRequest):
    optimizer = await get_economic_optimizer()
    budget_context = BudgetContext(
        user_id=req.user_id,
        monthly_limit=req.monthly_limit,
        spent_this_month=req.spent_this_month,
        cost_sensitivity=req.cost_sensitivity
    )
    decision = await optimizer.optimize_route(prompt=req.prompt, task_type=req.task_type, budget_context=budget_context)
    return {
        "provider": decision.provider,
        "model": decision.model,
        "estimated_cost": decision.estimated_cost,
        "reasoning": decision.reasoning
    }

@router.get("/stats")
async def get_stats():
    return {"status": "active", "total_savings_percentage": 25.0}
