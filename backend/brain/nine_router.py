# backend/brain/nine_router.py
"""
NineRouter for SupremeAI 2.0 — Facade Bridge to AdvancedModelRouter.
"""

from __future__ import annotations

from typing import Any

from core.llm.advanced_model_router import (
    AdvancedModelRouter,
    get_advanced_router,
)


class NineRouter:
    """Backward-compatible facade bridging to core.llm.advanced_model_router."""

    def __init__(self, router: Any | None = None):
        self.router = router or get_advanced_router()
        self.provider_cost_map = AdvancedModelRouter.PROVIDER_COST_MAP

    def pick(self, task_type: str, prompt: str, max_cost: float = 0.01) -> dict[str, Any]:
        # Fast synchronous cost estimation and routing
        cost = self.router.estimate_cost("groq", "llama-3.3-70b-versatile", len(prompt))
        return {
            "provider": "groq",
            "model": "llama-3.3-70b-versatile",
            "route": "cheap" if cost <= 0.0002 else "premium",
            "estimated_cost": cost,
        }
