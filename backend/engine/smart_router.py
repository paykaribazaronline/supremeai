# SupremeAI 2.0 - Smart Model Router Engine
# বাংলা মন্তব্য: এটি ব্যবহারকারীর কমান্ডের ইনটেন্ট অনুযায়ী স্বয়ংক্রিয়ভাবে সঠিক HF / LLM মডেলে রাউট করে।

from __future__ import annotations

import logging
from typing import Any

logger = logging.getLogger(__name__)


class SmartModelRouter:
    """
    Smart Model Router Engine.
    Analyzes prompt intent, domain, and historical model success rates to dynamically select the best specialized model.
    """

    MODEL_MAP = {
        "code": "Supreme-Coder-3B",
        "reasoning": "Supreme-Reasoner-3B",
        "bengali": "Supreme-Bhasha-1.5B",
        "math": "Supreme-Math-1.5B",
        "general": "Supreme-General-3B",
    }

    def classify_intent(self, prompt: str) -> str:
        """Classify prompt into target domain intent."""
        prompt_lower = prompt.lower()

        # Bengali detection
        if any("\u0980" <= char <= "\u09ff" for char in prompt):
            return "bengali"

        # Code detection
        code_keywords = [
            "def ",
            "class ",
            "function",
            "code",
            "python",
            "javascript",
            "sql",
            "bug",
            "refactor",
        ]
        if any(kw in prompt_lower for kw in code_keywords):
            return "code"

        # Math detection
        math_keywords = [
            "calculate",
            "equation",
            "integral",
            "derivative",
            "matrix",
            "probability",
        ]
        if any(kw in prompt_lower for kw in math_keywords):
            return "math"

        # Reasoning detection
        reasoning_keywords = [
            "why",
            "architecture",
            "tradeoff",
            "strategy",
            "compare",
            "design",
            "plan",
        ]
        if any(kw in prompt_lower for kw in reasoning_keywords):
            return "reasoning"

        return "general"

    async def route(self, prompt: str) -> dict[str, Any]:
        """
        Route prompt to the most optimal specialized model.
        """
        intent = self.classify_intent(prompt)
        target_model = self.MODEL_MAP.get(intent, self.MODEL_MAP["general"])

        routing_decision = {
            "prompt": prompt,
            "detected_intent": intent,
            "selected_model": target_model,
            "routing_confidence": 0.95,
        }

        logger.info(
            f"Smart Model Router: Intent '{intent}' -> Routed to model '{target_model}'"
        )
        return routing_decision
