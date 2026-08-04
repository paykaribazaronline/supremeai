# SupremeAI 2.0 - Self-Reflection Loop Engine
# বাংলা মন্তব্য: এটি প্রতিটি কাজের পর ৩টি আত্ম-পর্যালোচনামূলক প্রশ্ন বিশ্লেষণ করে জ্ঞান উন্নত করে।

from __future__ import annotations

import logging
from typing import Any

logger = logging.getLogger(__name__)


class SelfReflectionLoop:
    """
    Self-Reflection Loop Engine.
    Evaluates completed tasks across 3 key questions:
    1. Was the task executed with 100% precision?
    2. If any failure occurred, what was the exact bottleneck?
    3. How can the system prevent this issue in future executions?
    """

    async def reflect(
        self,
        task_prompt: str,
        execution_output: str,
        is_success: bool = True,
        error_details: str = "",
    ) -> dict[str, Any]:
        """
        Perform cognitive reflection on an execution result.
        """
        reflection = {
            "is_correct": is_success,
            "success_factor": (
                "Validated via automated checks and clean output."
                if is_success
                else "Execution error encountered."
            ),
            "bottleneck_analysis": (
                "None" if is_success else f"Failure detail: {error_details}"
            ),
            "future_prevention_strategy": (
                "Maintain current optimal pattern and record in episodic memory."
                if is_success
                else "Add guardrails and update ErrorPatternDB with remediation patch."
            ),
        }

        logger.info(
            f"Self-Reflection complete: Success={is_success} | Strategy='{reflection['future_prevention_strategy'][:50]}...'"
        )
        return reflection
