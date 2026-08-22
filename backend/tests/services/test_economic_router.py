import pytest
from core.optimization.economic_optimizer import BudgetContext
from services.smart_model_router import SmartRouter, ModelTier, ModelConfig, RoutingDecision

@pytest.mark.asyncio
async def test_economic_routing_with_budget():
    router = SmartRouter()
    
    # 1. Very small budget (should force ECONOMY tier)
    small_budget = BudgetContext(user_id="user_1", monthly_budget=0.0001, used_budget=0.0)
    decision1 = router.route("Explain quantum computing", user_budget=small_budget)
    
    # Should select groq-llama or similar free/cheap model
    assert decision1.selected_model.tier == ModelTier.ECONOMY
    assert decision1.estimated_cost_usd <= small_budget.remaining
    
    # 2. Large budget (should allow STANDARD or PREMIUM based on complexity)
    large_budget = BudgetContext(user_id="user_2", monthly_budget=10.0, used_budget=0.0)
    
    # Complex query
    complex_query = "Write a comprehensive REST API in Python using FastAPI with SQLAlchemy and JWT auth, including tests."
    decision2 = router.route(complex_query, user_budget=large_budget)
    
    # Should pick a capable model since budget allows it
    assert decision2.selected_model.tier in [ModelTier.PREMIUM, ModelTier.STANDARD]
    
    # 3. Exhausted budget
    exhausted_budget = BudgetContext(user_id="user_3", monthly_budget=10.0, used_budget=10.0)
    
    # A decision should still be made using cheapest fallback, or raises exception
    # In smart_model_router, if max_cost_usd is 0, it should filter out everything that isn't completely free
    decision3 = router.route("Hello", user_budget=exhausted_budget)
    
    # Groq llama is free in our registry (0.0 cost)
    assert decision3.selected_model.model_id == "llama-3.3-70b-versatile"
    assert decision3.estimated_cost_usd == 0.0

@pytest.mark.asyncio
async def test_economic_budget_deduction():
    router = SmartRouter()
    
    # Mocking call provider
    async def mock_call_provider(model, messages, **kwargs):
        return {"content": "This is a mocked response that contains some tokens"}
        
    router._call_provider = mock_call_provider
    
    budget = BudgetContext(user_id="user_4", monthly_budget=5.0, used_budget=0.0)
    decision = router.route("Hello", user_budget=budget)
    
    messages = [{"role": "user", "content": "Hello"}]
    
    response = await router.execute(decision, messages, user_budget=budget)
    
    # Ensure budget was deducted
    assert budget.used_budget > 0.0
    assert budget.used_budget == pytest.approx(response['_routing']['actual_cost_usd'], abs=1e-5)
