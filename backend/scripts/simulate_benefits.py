import sys
import os
import asyncio
from typing import Any

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))

from brain.economic_optimizer import EconomicOptimizer, BudgetContext
from brain.cognitive_router import CognitiveRouter
from core.cache.predictive_cache_engine import PredictiveCacheEngine, Prediction
from brain.user_digital_twin import TwinManager, InteractionType

async def run_simulation():
    print("--- SupremeAI Patch Simulation Results ---\n")
    
    # 1. Economic Optimizer Test
    print("1. Testing Economic Optimizer")
    optimizer = EconomicOptimizer()
    total_cost_optimized = 0.0
    total_cost_premium = 0.0
    premium_cost_per_1k = 0.001  # e.g., openrouter auto premium
    
    users = [
        BudgetContext(user_id="u1", monthly_limit=1.0, spent_this_month=0.8, cost_sensitivity=0.9),  # very poor
        BudgetContext(user_id="u2", monthly_limit=10.0, spent_this_month=2.0, cost_sensitivity=0.3), # rich
        BudgetContext(user_id="u3", monthly_limit=5.0, spent_this_month=4.5, cost_sensitivity=0.7),  # almost out of budget
    ]
    
    print("Simulating 30 requests (10 per user type)...")
    for _ in range(10):
        for u in users:
            decision = await optimizer.optimize_route("Help me code", "general", u)
            total_cost_optimized += decision.estimated_cost
            total_cost_premium += premium_cost_per_1k

    savings = 100 * (1 - (total_cost_optimized / total_cost_premium)) if total_cost_premium > 0 else 0
    print(f"Total Premium Cost: ${total_cost_premium:.4f}")
    print(f"Total Optimized Cost: ${total_cost_optimized:.4f}")
    print(f"Cost Savings: {savings:.1f}%\n")

    # 2. Cognitive Router Test
    print("2. Testing Cognitive Router v2.0")
    cog_router = CognitiveRouter(optimizer)
    
    prompts = [
        "What is the weather?",
        "Analyze this python script and implement a rust version.",
        "Just say hello",
        "Please analyze my architecture, suggest improvements and implement the changes."
    ]
    
    decomposed_count = 0
    direct_count = 0
    for p in prompts:
        res = await cog_router.route(p, "u2", users[1])
        mode = res.get("routing_mode")
        if mode == "decomposed":
            decomposed_count += 1
            print(f" - PROMPT: '{p[:20]}...' -> DECOMPOSED into {res['task_graph']['task_count']} sub-tasks")
        else:
            direct_count += 1
            print(f" - PROMPT: '{p[:20]}...' -> DIRECT to {res.get('provider')}")
            
    print(f"Routing logic correctly identified {decomposed_count} complex tasks out of {len(prompts)}\n")
    
    # 3. Predictive Cache Engine Test
    print("3. Testing Predictive Cache Engine")
    cache_engine = PredictiveCacheEngine()
    await cache_engine.initialize(None)
    
    # Simulate user navigating through a flow
    flow = ["/home", "/dashboard", "/settings", "/home", "/dashboard", "/settings", "/home", "/dashboard"]
    for path in flow:
        await cache_engine.record_access("u1", path)
        
    predictions = cache_engine.predict("u1", "/dashboard")
    print(f"User navigated 8 times. Predicting next step after '/dashboard':")
    for p in predictions:
        print(f" - {p.key} (Confidence: {p.confidence * 100}%) -> {p.description}")
    print()

    # 4. User Digital Twin Test
    print("4. Testing User Digital Twin")
    twin_mgr = TwinManager()
    twin = twin_mgr.get_or_create("dev_user_1")
    
    # Simulate coding interactions
    for _ in range(40):
        await twin.record_interaction(InteractionType.CODE_REQUEST, "write some code", 200, True)
        
    for _ in range(15):
        await twin.record_interaction(InteractionType.DEBUGGING, "fix error", 500, False)
        
    print(f"Recorded 55 interactions for {twin.hashed_id[:8]}")
    preds = twin.predict_next_actions()
    print("Anticipatory Action Predictions for this user:")
    for p in preds:
        print(f" - {p.description} (Confidence: {p.confidence * 100}%)")

if __name__ == "__main__":
    asyncio.run(run_simulation())
