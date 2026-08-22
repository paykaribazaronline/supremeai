from dataclasses import dataclass
from typing import Any

@dataclass
class BudgetContext:
    user_id: str
    monthly_limit: float
    spent_this_month: float
    cost_sensitivity: float

@dataclass
class OptimizationDecision:
    provider: str
    model: str
    estimated_cost: float
    reasoning: str

class EconomicOptimizer:
    def __init__(self, free_tier_tracker=None):
        self.free_tier_tracker = free_tier_tracker
        self.provider_tiers = {
            "groq": {"model": "llama-3.3-70b-versatile", "cost_per_1k": 0.0001, "tier": "free/cheap"},
            "google": {"model": "gemini-2.5-pro", "cost_per_1k": 0.00025, "tier": "premium"},
            "together": {"model": "mixtral-8x7b", "cost_per_1k": 0.0002, "tier": "cheap"},
            "openrouter": {"model": "auto", "cost_per_1k": 0.001, "tier": "premium"},
            "nvidia": {"model": "nemotron-4", "cost_per_1k": 0.0005, "tier": "mid"},
            "huggingface": {"model": "zephyr-7b", "cost_per_1k": 0.00005, "tier": "free/cheap"},
        }
    
    async def optimize_route(self, prompt: str, task_type: str, budget_context: BudgetContext) -> OptimizationDecision:
        remaining_budget = budget_context.monthly_limit - budget_context.spent_this_month
        
        # Determine allowed tier based on remaining budget and cost sensitivity
        if remaining_budget < 1.0 or budget_context.cost_sensitivity > 0.8:
            allowed_tiers = ["free/cheap"]
        elif remaining_budget < 5.0 or budget_context.cost_sensitivity > 0.5:
            allowed_tiers = ["free/cheap", "cheap", "mid"]
        else:
            allowed_tiers = ["free/cheap", "cheap", "mid", "premium"]
            
        best_provider = "huggingface"
        best_cost = float('inf')
        
        for provider, info in self.provider_tiers.items():
            if info["tier"] in allowed_tiers:
                if info["cost_per_1k"] < best_cost:
                    best_cost = info["cost_per_1k"]
                    best_provider = provider

        model = self.provider_tiers[best_provider]["model"]
        reasoning = f"Selected {best_provider} (tier: {self.provider_tiers[best_provider]['tier']}) due to remaining budget of ${remaining_budget:.2f} and cost sensitivity {budget_context.cost_sensitivity}"
        
        return OptimizationDecision(
            provider=best_provider,
            model=model,
            estimated_cost=best_cost,
            reasoning=reasoning
        )

_economic_optimizer_instance = None

async def get_economic_optimizer(free_tier_tracker=None) -> EconomicOptimizer:
    global _economic_optimizer_instance
    if _economic_optimizer_instance is None:
        _economic_optimizer_instance = EconomicOptimizer(free_tier_tracker=free_tier_tracker)
    return _economic_optimizer_instance
