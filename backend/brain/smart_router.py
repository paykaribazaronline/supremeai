# backend/brain/smart_router.py
"""
SupremeAI Self-Sovereign Smart Router
Routes requests to local inference FIRST, then managed, then frontier.
Goal: 70-80% local inference, 15-20% managed, 5-10% frontier.
"""

from typing import Any

from loguru import logger

from core.llm_router import LLMRouter

# Local inference configuration
LOCAL_MODELS = {
    "general": "ollama/llama3.1:70b",
    "coding": "ollama/deepseek-coder:33b",
    "chat": "ollama/qwen2.5:32b",
    "vision": "ollama/llava:34b",
    "reasoning": "ollama/deepseek-r1:32b",
}

# Managed open-weight APIs (cheap, fast)
MANAGED_MODELS = {
    "general": "groq/llama-3.1-70b-versatile",
    "coding": "deepseek/deepseek-coder",
    "chat": "groq/llama-3.1-8b-instant",
    "vision": "google/gemini-2.5-flash",
    "reasoning": "groq/deepseek-r1-distill-llama-70b",
}

# Frontier APIs (expensive, highest quality)
FRONTIER_MODELS = {
    "general": "openai/gpt-4o",
    "coding": "anthropic/claude-3-5-sonnet",
    "chat": "openai/gpt-4o-mini",
    "vision": "google/gemini-2.5-pro",
    "reasoning": "anthropic/claude-3-5-sonnet",
}

# Task complexity thresholds (token count based)
COMPLEXITY_THRESHOLDS = {
    "simple": 500,  # < 500 tokens -> Local
    "medium": 2000,  # 500-2000 -> Local or Managed
    "complex": 5000,  # 2000-5000 -> Managed or Frontier
    "extreme": float("inf"),  # > 5000 -> Frontier
}


class TaskComplexityAnalyzer:
    """Analyzes task complexity to determine routing tier."""

    def __init__(self):
        self.keywords = {
            "simple": ["summarize", "translate", "format", "convert", "list", "count"],
            "medium": ["explain", "compare", "analyze", "debug", "refactor", "review"],
            "complex": ["design", "architect", "optimize", "research", "plan", "strategy"],
            "extreme": ["innovate", "create", "invent", "discover", "prove", "theorem"],
        }

    def analyze(self, prompt: str, task_type: str = "general") -> str:
        """Returns complexity tier: simple, medium, complex, extreme."""
        prompt_lower = prompt.lower()
        token_estimate = len(prompt.split()) * 1.3  # Rough token estimate

        # Check keywords
        for tier, words in self.keywords.items():
            if any(word in prompt_lower for word in words):
                return tier

        # Check token count
        if token_estimate < COMPLEXITY_THRESHOLDS["simple"]:
            return "simple"
        elif token_estimate < COMPLEXITY_THRESHOLDS["medium"]:
            return "medium"
        elif token_estimate < COMPLEXITY_THRESHOLDS["complex"]:
            return "complex"
        else:
            return "extreme"


class SelfSovereignRouter:
    """
    Self-Sovereign AI Router for SupremeAI 2.0
    Routes 70-80% to local, 15-20% to managed, 5-10% to frontier.
    """

    def __init__(self):
        self.router = LLMRouter()
        self.analyzer = TaskComplexityAnalyzer()
        self.local_available = self._check_local_availability()
        self.stats = {
            "local": 0,
            "managed": 0,
            "frontier": 0,
            "total": 0,
            "cost_saved": 0.0,
        }
        logger.info(f"[SelfSovereignRouter] Local inference available: {self.local_available}")

    def _check_local_availability(self) -> bool:
        """
        Check if Ollama is running locally or at configured OLLAMA_URL.
        বাংলা মন্তব্য: ওলামা সার্ভিস ইউআরএল এনভায়রনমেন্ট ভেরিয়েবল থেকে ডায়নামিকালি রিড করা হচ্ছে।
        """
        try:
            import os
            import urllib.error
            import urllib.request

            from core.config import settings

            # বাংলা মন্তব্য: settings.ollama_url বা env ভেরিয়েবল ব্যবহার (কোনো ডিফল্ট localhost fallback নেই)
            # শুধুমাত্র local development-এ localhost:11434 এ ফলব্যাক করবে।
            ollama_base = settings.ollama_url or os.getenv("OLLAMA_URL", "")
            if not ollama_base and settings.env == "local":
                ollama_base = "http://localhost:11434"
            ollama_base = ollama_base.rstrip("/") if ollama_base else ""
            url = f"{ollama_base}/api/tags"

            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req, timeout=2) as resp:
                return resp.status == 200
        except (TimeoutError, urllib.error.URLError, ValueError, OSError):
            return False

    def route(self, prompt: str, task_type: str = "general", force_tier: str | None = None) -> dict[str, Any]:
        """
        Route request to appropriate tier based on complexity.
        """
        complexity = self.analyzer.analyze(prompt, task_type)

        if force_tier:
            tier = force_tier
        else:
            if complexity == "simple" and self.local_available:
                tier = "local"
            elif complexity in ["simple", "medium"] and self.local_available:
                tier = "local"
            elif complexity == "medium":
                tier = "managed"
            elif complexity == "complex":
                tier = "managed"
            else:
                tier = "frontier"

        if tier == "local":
            model = LOCAL_MODELS.get(task_type, LOCAL_MODELS["general"])
            cost_per_1m = 0.0
        elif tier == "managed":
            model = MANAGED_MODELS.get(task_type, MANAGED_MODELS["general"])
            cost_per_1m = 0.09
        else:
            model = FRONTIER_MODELS.get(task_type, FRONTIER_MODELS["general"])
            cost_per_1m = 5.0

        self.stats["total"] += 1
        self.stats[tier] += 1

        tokens = len(prompt.split()) * 1.3
        frontier_cost = (tokens / 1_000_000) * 5.0
        actual_cost = (tokens / 1_000_000) * cost_per_1m
        self.stats["cost_saved"] += frontier_cost - actual_cost

        logger.info(f"[SelfSovereignRouter] Complexity={complexity} -> Tier={tier} -> Model={model}")

        return {
            "model": model,
            "tier": tier,
            "complexity": complexity,
            "estimated_cost_per_1m": cost_per_1m,
            "routing_reason": f"{complexity} complexity -> {tier} tier",
            "local_available": self.local_available,
        }

    async def generate(self, prompt: str, task_type: str = "general", force_tier: str | None = None) -> str:
        """Generate response using the routed model."""
        route_info = self.route(prompt, task_type, force_tier)
        model = route_info["model"]

        response = await self.router.async_generate(
            prompt=prompt,
            model_override=model,
        )

        return response.get("text", "") if isinstance(response, dict) else str(response)

    def get_stats(self) -> dict[str, Any]:
        """Get routing statistics."""
        total = self.stats["total"]
        if total == 0:
            return self.stats

        return {
            **self.stats,
            "local_percentage": (self.stats["local"] / total) * 100,
            "managed_percentage": (self.stats["managed"] / total) * 100,
            "frontier_percentage": (self.stats["frontier"] / total) * 100,
            "total_cost_saved_usd": self.stats["cost_saved"],
        }


_router: SelfSovereignRouter | None = None


def get_self_sovereign_router() -> SelfSovereignRouter:
    global _router
    if _router is None:
        _router = SelfSovereignRouter()
    return _router
