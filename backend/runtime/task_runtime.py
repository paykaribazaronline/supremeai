# backend/runtime/task_runtime.py
"""Canonical Task Runtime (The Authoritative Control Plane).

Orchestrates:
TaskContract -> TaskStateMachine -> TaskExecutor -> VerifierEngine -> Memory -> TaskResult
"""

from __future__ import annotations

import asyncio
from datetime import datetime
import logging
import time
from typing import Any, Dict, Optional

from core.integration_layer import SupremeAIIntegrator
from core.task_contract import TaskContract, TaskStatus, VerificationPolicy
from runtime.task_context import TaskContext
from runtime.task_executor import TaskExecutor
from runtime.task_result import TaskResult, VerificationSummary
from verification.verifier import VerifierEngine, get_verifier

logger = logging.getLogger("supremeai.runtime")


class TaskRuntime:
    """Canonical Task Runtime governing all execution across SupremeAI."""

    def __init__(
        self,
        ai_system: Optional[SupremeAIIntegrator] = None,
        verifier: Optional[VerifierEngine] = None,
    ) -> None:
        self.ai_system = ai_system
        self.verifier = verifier or get_verifier()
        self.executor = TaskExecutor(ai_system=self.ai_system)
        self.experience_ledger: list[Dict[str, Any]] = []

    async def execute_task(
        self,
        task: TaskContract,
        context: Optional[TaskContext] = None,
    ) -> TaskResult:
        """Execute task through the canonical 5-stage lifecycle."""
        start_time = time.perf_counter()
        ctx = context or TaskContext()
        logger.info(f"🚀 [Runtime] Initiating task execution: {task.task_id}")

        try:
            # Stage 1: Planning / Preparation
            task.transition_to(TaskStatus.PLANNING)
            ctx.checkpoint("planning_started")

            # Stage 2: Execution
            task.transition_to(TaskStatus.EXECUTING)
            ctx.checkpoint("execution_started")

            exec_res = await asyncio.wait_for(
                self.executor.execute(task, ctx),
                timeout=task.budget.max_execution_seconds,
            )

            if not exec_res.get("success"):
                task.fail(exec_res.get("error", "Execution failed"))
                return self._create_result(task, exec_res, start_time)

            candidate_output = exec_res.get("output")

            # Stage 3: Verification Gate
            task.transition_to(TaskStatus.VERIFYING)
            ctx.checkpoint("verification_started")

            ver_report = await self.verifier.verify(task, candidate_output, ctx)

            # Stage 4: Completion & Experience Ledgering
            if ver_report.verified:
                task.complete(candidate_output, confidence=ver_report.confidence)
            else:
                if task.verification_policy == VerificationPolicy.STRICT:
                    task.fail(f"Verification failed: {', '.join(ver_report.criteria_failed)}")
                else:
                    task.complete(candidate_output, confidence=ver_report.confidence)

            # Record into Experience Ledger (Finding #4 / Phase 3)
            self._record_experience(task, ctx, ver_report)

            return self._create_result(task, exec_res, start_time, ver_report)

        except asyncio.TimeoutError:
            task.fail(f"Task exceeded budget timeout ({task.budget.max_execution_seconds}s)")
            return TaskResult(
                task_id=task.task_id,
                success=False,
                answer="",
                confidence=0.0,
                execution_time_ms=(time.perf_counter() - start_time) * 1000,
                error="Timeout exceeded",
            )
        except Exception as exc:
            task.fail(str(exc))
            logger.error(f"💥 Runtime error on [{task.task_id}]: {exc}", exc_info=True)
            return TaskResult(
                task_id=task.task_id,
                success=False,
                answer="",
                confidence=0.0,
                execution_time_ms=(time.perf_counter() - start_time) * 1000,
                error=str(exc),
            )

    def _create_result(
        self,
        task: TaskContract,
        exec_res: Dict[str, Any],
        start_time: float,
        ver_report: Optional[VerificationSummary] = None,
    ) -> TaskResult:
        total_time_ms = round((time.perf_counter() - start_time) * 1000, 2)
        return TaskResult(
            task_id=task.task_id,
            success=task.status == TaskStatus.COMPLETED,
            answer=task.result or "",
            confidence=task.confidence,
            execution_time_ms=total_time_ms,
            provider_used=exec_res.get("provider_used", "Gemini"),
            verification=ver_report or VerificationSummary(),
            components_used=exec_res.get("components_used", ["reasoning_engine"]),
            error=task.error,
            metadata={
                "task_status": task.status.value,
                "risk_level": task.risk_level.value,
                "tokens_used": task.budget.tokens_used,
            },
        )

    def _record_experience(
        self,
        task: TaskContract,
        context: TaskContext,
        ver_report: VerificationSummary,
    ) -> None:
        """Store immutable execution experience in ledger for continual learning."""
        experience = {
            "experience_id": f"exp_{task.task_id}",
            "task_id": task.task_id,
            "goal": task.goal,
            "success": task.status == TaskStatus.COMPLETED,
            "confidence": task.confidence,
            "provider_used": context.active_provider,
            "tokens_used": context.token_usage["total"],
            "verification_passed": ver_report.verified,
            "timestamp": datetime.now().isoformat(),
        }
        self.experience_ledger.append(experience)
        if len(self.experience_ledger) > 1000:
            self.experience_ledger.pop(0)


# Global Singleton
_task_runtime_instance: Optional[TaskRuntime] = None


def get_task_runtime(ai_system: Optional[SupremeAIIntegrator] = None) -> TaskRuntime:
    global _task_runtime_instance
    if _task_runtime_instance is None:
        _task_runtime_instance = TaskRuntime(ai_system=ai_system)
    elif ai_system and not _task_runtime_instance.ai_system:
        _task_runtime_instance.ai_system = ai_system
        _task_runtime_instance.executor.ai_system = ai_system
    return _task_runtime_instance
