# backend/brain/smart_router.py
"""
SupremeAI Self-Sovereign Smart Router — Facade Bridge to AdvancedModelRouter.
"""

from __future__ import annotations

from typing import Any

from core.llm.advanced_model_router import (
    TaskComplexityAnalyzer,
    get_advanced_router,
)


class SelfSovereignRouter:
    """Backward-compatible facade bridging to core.llm.advanced_model_router."""

    def __init__(self):
        self._core_router = get_advanced_router()
        self.analyzer = TaskComplexityAnalyzer()
        self.stats = {
            "local": 0,
            "managed": 0,
            "frontier": 0,
            "total": 0,
            "cost_saved": 0.0,
        }

    async def route(self, prompt: str, task_type: str = "general") -> dict[str, Any]:
        decision = await self._core_router.route_request(prompt, task_type)
        return {
            # বাংলা মন্তব্য: "complexity" পুরনো (pre-consolidation) SelfSovereignRouter
            # contract-এর key ছিল — facade backward-compatible রাখতে দুটো key-ই রাখা হলো।
            "complexity": decision.complexity_tier,
            "tier": decision.complexity_tier,
            "provider": decision.provider,
            "model": decision.model,
            "cost_saved": max(0.0, 0.005 - decision.expected_cost),
        }


def get_self_sovereign_router() -> SelfSovereignRouter:
    return SelfSovereignRouter()
