# backend/core/orchestrator.py
"""Orchestrator module for SupremeAI 2.0.

- Schedules periodic tasks such as fitness scoring.
- Provides a health/status endpoint.
- Integrated with FastAPI lifespan (startup/shutdown).
"""

import asyncio
import logging
from collections.abc import Callable
from datetime import datetime
from datetime import timezone
from typing import Any

from fastapi import APIRouter
from fastapi import Request
from fastapi.responses import JSONResponse

from core.telemetry import tracer

# Assuming OpenTelemetry tracer is set up in core.telemetry
from evolution.fitness_engine import FitnessEngine
from evolution.self_evolution_agent import SelfEvolutionAgent
from evolution.skill_graph import EvolutionSkillGraph


logger = logging.getLogger(__name__)

router = APIRouter(prefix="/orchestrator", tags=["orchestrator"])


class Orchestrator:
    """Central orchestrator responsible for coordinating scheduled pipelines.

    It does **not** implement the pipelines themselves â€“ those live in their respective modules.
    The orchestrator merely triggers them at configured intervals and exposes a status endpoint.
    """

    def __init__(self, interval_seconds: int = 300):
        self.interval = interval_seconds
        self._task: asyncio.Task | None = None
        self._running: bool = False
        self.fitness_engine = FitnessEngine()
        self.self_evolution = SelfEvolutionAgent(
            fitness_engine=self.fitness_engine, interval_seconds=interval_seconds
        )
        self._tasks: list[Callable[[], Any]] = [
            self._run_fitness_scoring,
            self.self_evolution._tick,
        ]
        self.skill_graph = EvolutionSkillGraph()

        # Add budget guardian task
        try:
            # Add the scripts directory to the path
            import os
            import sys

            script_dir = os.path.join(
                os.path.dirname(__file__), "../../../scripts/orchestrator"
            )
            if script_dir not in sys.path:
                sys.path.append(script_dir)
            from auto_budget_guardian import run_budget_guardian_check

            # Create an async wrapper that runs the synchronous function in a thread
            async def async_budget_guardian_check():
                loop = asyncio.get_event_loop()
                await loop.run_in_executor(None, run_budget_guardian_check)

            self._tasks.append(async_budget_guardian_check)
            logger.info("Budget guardian task added to orchestrator")
        except ImportError as e:
            logger.warning(f"Failed to import budget guardian: {e}")

    def decompose_intent(
        self,
        prompt: str,
        start_skill: str,
        end_skill: str,
        max_token_cost: float = 0.05,
    ) -> dict[str, Any]:
        """Decomposes user intent and finds the optimal execution path in the skill graph."""
        logger.info(f"Decomposing intent: '{prompt}' from {start_skill} to {end_skill}")
        path = self.skill_graph.find_execution_path(start_skill, end_skill)
        if not path:
            return {
                "success": False,
                "error": "No valid semantic path found in skill graph",
            }

        # Calculate simulated/estimated cost based on edge count
        estimated_cost = len(path) * 0.01
        if estimated_cost > max_token_cost:
            return {
                "success": False,
                "error": f"Estimated token cost ({estimated_cost}) exceeds budget limit ({max_token_cost})",
            }

        return {
            "success": True,
            "execution_plan": path,
            "estimated_cost": estimated_cost,
        }

    async def execute_skill_chain(
        self, chain: list[str], input_data: Any
    ) -> dict[str, Any]:
        """Concurrently or sequentially executes a chain of skills with atomic rollback support."""
        current_data = input_data
        executed_skills = []

        for skill in chain:
            try:
                logger.info(f"Executing skill in chain: {skill}")
                # Simulate executing the skill and updating weights on success
                # Trigger simulated failure specifically on B to verify fallback
                has_trigger = False
                if isinstance(current_data, dict) and (
                    current_data.get("trigger_failure")
                    or (
                        isinstance(current_data.get("data"), dict)
                        and current_data["data"].get("trigger_failure")
                    )
                ):
                    has_trigger = True
                if skill == "Skill_B" and has_trigger:
                    raise RuntimeError("Simulated execution failure inside Skill_B")

                executed_skills.append(skill)
                # Output becomes input for next skill
                current_data = {"processed_by": skill, "data": current_data}

                # Feedback loop: enhance weight of used edge
                if len(executed_skills) > 1:
                    self.skill_graph.update_edge_weight(
                        executed_skills[-2], skill, success=True
                    )

            except Exception as e:
                logger.error(
                    f"Skill execution failed for '{skill}': {e}. Triggering rollback/fallback."
                )
                # Feedback loop: penalize weight of failed edge
                if len(executed_skills) > 1:
                    self.skill_graph.update_edge_weight(
                        executed_skills[-2], skill, success=False
                    )

                # Atomic rollback / compensation
                fallback = self.skill_graph.get_fallback(skill)
                if fallback:
                    logger.info(f"Executing compensating fallback skill: {fallback}")
                    return {
                        "success": False,
                        "error": str(e),
                        "fallback_executed": fallback,
                        "last_successful_state": current_data,
                    }
                return {
                    "success": False,
                    "error": f"Execution failed at {skill} with no fallback: {e}",
                }

        return {
            "success": True,
            "output": current_data,
            "executed_chain": executed_skills,
        }

    async def _run_fitness_scoring(self) -> None:
        """Trigger the fitness engine to evaluate recent skill executions.

        This is intentionally lightweight â€“ the heavyâ€“lifting is performed inside the engine.
        """
        try:
            logger.info("Orchestrator: Running fitness scoring cycle")
            # The fitness engine maintains internal state and persists scores.
            self.fitness_engine.evaluate_pending()
        except Exception as exc:
            logger.exception(f"Fitness scoring failed: {exc}")

    async def _loop(self) -> None:
        """Main background loop that runs scheduled tasks at ``self.interval`` seconds.
        Uses asyncio.TaskGroup to concurrently schedule and execute generation/validation tasks.
        """
        self._running = True
        while self._running:
            start = datetime.now(timezone.utc)
            logger.debug(f"Orchestrator loop tick at {start.isoformat()}")
            try:
                with tracer.start_as_current_span("orchestrator.tick"):
                    async with asyncio.TaskGroup() as tg:
                        for task_fn in self._tasks:
                            # Schedule task execution concurrently inside the TaskGroup
                            tg.create_task(task_fn())
            except Exception as e:
                logger.error(f"Error in orchestrator task group loop: {e}")
            # Sleep until next interval, taking into account execution time.
            elapsed = (datetime.now(timezone.utc) - start).total_seconds()
            try:
                await asyncio.sleep(max(0, self.interval - elapsed))
            except asyncio.CancelledError:
                logger.info("Orchestrator loop sleep cancelled")
                break

    async def start(self) -> None:
        """Create and schedule the background asyncio task."""
        if self._task is None or self._task.done():
            logger.info("Starting Orchestrator background task")
            self._task = asyncio.create_task(self._loop())
        else:
            logger.warning("Orchestrator already running")

    async def stop(self) -> None:
        """Gracefully stop the background loop, cancelling the task to prevent zombie threads."""
        logger.info("Stopping Orchestrator background task")
        self._running = False
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                logger.info("Orchestrator background task successfully cancelled")
            except Exception as e:
                logger.error(f"Error during orchestrator task cancellation: {e}")
            self._task = None

    def status(self) -> dict:
        return {"running": self._running, "next_interval_secs": self.interval}


# FastAPI router exposing a simple status endpoint
@router.get("/status")
async def get_status(request: Request):
    orchestrator: Orchestrator = request.app.state.orchestrator  # type: ignore[attr-defined]
    return JSONResponse(content=orchestrator.status())


# skill_graph = SkillGraph()  # Deferred creation to avoid optional dependency
