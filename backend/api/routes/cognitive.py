from fastapi import APIRouter
from pydantic import BaseModel
from brain.cognitive_router import get_cognitive_router
from brain.economic_optimizer import get_economic_optimizer, BudgetContext

router = APIRouter(prefix="/cognitive", tags=["cognitive"])

class CognitiveRouteRequest(BaseModel):
    prompt: str
    user_id: str
    monthly_limit: float = 10.0
    spent_this_month: float = 0.0
    cost_sensitivity: float = 0.5

@router.post("/route")
async def route_cognitive(req: CognitiveRouteRequest):
    economic_optimizer = await get_economic_optimizer()
    cognitive_router_instance = get_cognitive_router(economic_optimizer=economic_optimizer)
    budget_context = BudgetContext(
        user_id=req.user_id,
        monthly_limit=req.monthly_limit,
        spent_this_month=req.spent_this_month,
        cost_sensitivity=req.cost_sensitivity
    )
    result = await cognitive_router_instance.route(prompt=req.prompt, user_id=req.user_id, budget_context=budget_context)
    return result
