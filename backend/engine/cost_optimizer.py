from typing import Any

from loguru import logger


class CostOptimizer:
    ROUTE_LADDER = [
        "ollama/llama3.2",
        "openrouter/mistral-7b-free",
        "gemini/gemini-flash",
        "openai/gpt-4o-mini",
    ]

    def __init__(self) -> None:
        self.free_tier_tracker = None

    async def get_optimal_route(self, task: dict[str, Any], user_mode: str) -> str:
        try:
            from core.free_tier_tracker import get_tracker

            self.free_tier_tracker = get_tracker()
            provider = self.free_tier_tracker.get_best_provider()
            if provider:
                return f"{provider}/default"
        except Exception as exc:
            logger.debug(f"Free tier tracker unavailable: {exc}")
        return self.ROUTE_LADDER[0]
