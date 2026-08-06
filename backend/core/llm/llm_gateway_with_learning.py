# backend/core/llm/llm_gateway_with_learning.py
"""
SupremeAI LLM Gateway with Learning Integration
===============================================
This replaces or wraps your existing LLMRouter / LLMGateway to add learning capabilities.

HOW IT WORKS:
1. User sends a query
2. Learning Engine checks: "Can I answer this myself?"
3. If YES (confidence > 75%): Answer independently (NO external AI call!)
4. If NO: Call external AI (GPT/Claude/Gemini/etc.)
5. After external AI responds: LEARN from the interaction
6. Next time similar query comes: Answer independently!

OVER TIME: Self-sufficiency grows from 30% -> 80%+
"""

from brain.supreme_learning_engine import SupremeLearningEngine
from core.llm_router import LLMRouter
from loguru import logger

_learning_engine: SupremeLearningEngine | None = None


def get_learning_engine() -> SupremeLearningEngine:
    global _learning_engine
    if _learning_engine is None:
        _learning_engine = SupremeLearningEngine()
    return _learning_engine


class LLMGatewayWithLearning:
    """
    Wrapper around LLMRouter that adds learning and self-sufficiency.
    Drop-in replacement for LLMGateway.
    """

    def __init__(
        self,
        min_confidence: float = 0.75,
        learning_enabled: bool = True,
    ):
        self.router = LLMRouter()
        self.learning = get_learning_engine()
        self.min_confidence = min_confidence
        self.learning_enabled = learning_enabled

        logger.info("🧠 LLMGatewayWithLearning initialized")
        logger.info(f"   📊 Min confidence for self-answer: {min_confidence}")
        logger.info(f"   🎓 Learning enabled: {learning_enabled}")

    async def acompletion(
        self,
        model: str,
        messages: list[dict],
        task_type: str = "general",
        **kwargs,
    ) -> str:
        """
        Complete a conversation with learning and self-sufficiency.
        """
        user_query = messages[-1]["content"] if messages else ""

        # STEP 1: Try to answer independently
        can_answer, confidence, pattern = self.learning.can_answer_independently(
            query=user_query,
            task_type=task_type,
            min_confidence=self.min_confidence,
        )

        if can_answer and pattern:
            logger.info(f"🎯 Self-sufficient answer! Confidence: {confidence:.2f}")

            response = self.learning.generate_independent_response(
                query=user_query,
                pattern=pattern,
                context=kwargs.get("context"),
            )

            return f"[SupremeAI Brain] {response}"

        # STEP 2: Fall back to external AI
        logger.info(
            f"🤔 Confidence too low ({confidence:.2f}). Calling external AI model: {model}"
        )

        gen_resp = await self.router.async_generate(
            prompt=user_query, model_override=model, **kwargs
        )

        response = (
            gen_resp.get("text", "") if isinstance(gen_resp, dict) else str(gen_resp)
        )

        # STEP 3: LEARN from this interaction
        if self.learning_enabled and response:
            self.learning.learn_from_interaction(
                query=user_query,
                response=response,
                model_used=model,
                task_type=task_type,
            )

        return response

    def get_learning_stats(self) -> dict:
        """Get statistics about learning and self-sufficiency."""
        return self.learning.get_stats()

    def set_min_confidence(self, confidence: float):
        """Adjust the threshold for self-sufficient answers."""
        self.min_confidence = max(0.0, min(1.0, confidence))
        logger.info(f"📊 Min confidence updated to: {self.min_confidence}")
