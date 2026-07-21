import os
from typing import Any

from loguru import logger


class DecisionEngine:
    def __init__(self) -> None:
        self.langsmith_api_key = os.getenv("LANGSMITH_API_KEY")
        self._min_confidence = float(os.getenv("DECISION_MIN_CONFIDENCE", "0.6"))

    async def decide(self, context: dict[str, Any]) -> dict[str, Any]:
        risk_flags = context.get("risk_flags") or []
        error_rate = context.get("recent_error_rate", 0.0)
        sandbox_passed = context.get("sandbox_passed")

        if sandbox_passed is False:
            logger.warning("DecisionEngine: sandbox failed, blocking action")
            return {
                "action": "block",
                "confidence": 1.0,
                "reason": "sandbox_failed",
                "trace": None,
            }

        if risk_flags:
            logger.info(
                f"DecisionEngine: risk flags present {risk_flags}, routing to review"
            )
            return {
                "action": "review",
                "confidence": 0.5,
                "reason": "risk_flags",
                "trace": None,
            }

        confidence = max(0.0, 1.0 - error_rate)
        action = "proceed" if confidence >= self._min_confidence else "review"
        logger.debug(f"DecisionEngine: action={action} confidence={confidence:.2f}")
        return {
            "action": action,
            "confidence": confidence,
            "reason": None,
            "trace": None,
        }
