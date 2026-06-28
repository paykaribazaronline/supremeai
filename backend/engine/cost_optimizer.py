import re
from typing import Any

from loguru import logger


class ComplexityAnalyzer:
    KEYWORDS = {
        "simple": r"\b(hello|hi|ping|summary|list|brief)\b",
        "medium": r"\b(explain|compare|analyze|generate|draft|debug)\b",
        "complex": r"\b(implement|deploy|architecture|system design|codebase|refactor|migrate)\b",
    }

    @classmethod
    def classify(cls, prompt: str) -> str:
        text = (prompt or "").lower()
        for level, pattern in cls.KEYWORDS.items():
            if re.search(pattern, text):
                return level
        return "simple"


class CostOptimizer:
    ROUTE_LADDER = {
        "simple": [
            "ollama/llama3.2",
            "openrouter/mistral-7b-free",
            "gemini/gemini-flash",
        ],
        "medium": [
            "gemini/gemini-flash",
            "openai/gpt-4o-mini",
            "openrouter/mistral-7b-free",
        ],
        "complex": [
            "openai/gpt-4o-mini",
            "anthropic/claude-3-haiku",
            "ollama/llama3.2",
        ],
    }

    def __init__(self) -> None:
        self.free_tier_tracker = None
        self.litellm_callbacks: list[Any] = []

    def register_litellm_callback(self, callback: Any) -> None:
        if callback not in self.litellm_callbacks:
            self.litellm_callbacks.append(callback)

    def _get_best_free_provider(self) -> str | None:
        try:
            from core.free_tier_tracker import get_tracker

            self.free_tier_tracker = get_tracker()
            provider = self.free_tier_tracker.get_best_provider()
            if provider:
                return provider
        except Exception as exc:
            logger.debug(f"Free tier tracker unavailable: {exc}")
        return None

    async def get_optimal_route(self, task: dict[str, Any], user_mode: str) -> str:
        prompt = task.get("prompt") or task.get("request") or ""
        complexity = ComplexityAnalyzer.classify(prompt)
        candidates = self.ROUTE_LADDER.get(complexity, self.ROUTE_LADDER["simple"])
        free = self._get_best_free_provider()
        if free and user_mode != "paid":
            for candidate in candidates:
                if candidate.startswith(free):
                    return candidate
        return candidates[0]
