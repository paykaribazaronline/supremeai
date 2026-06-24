# backend/core/orchestrator.py
"""Orchestrator module for SupremeAI 2.0.

- Schedules periodic tasks such as fitness scoring.
- Provides a health/status endpoint.
- Integrated with FastAPI lifespan (startup/shutdown).
"""
import asyncio
import logging
from datetime import datetime, timezone
from typing import Any, Callable, List

from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from core.config import settings
from core.telemetry import tracer  # Assuming OpenTelemetry tracer is set up in core.telemetry
from evolution.fitness_engine import FitnessEngine

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/orchestrator", tags=["orchestrator"])


class Orchestrator:
    """Central orchestrator responsible for coordinating scheduled pipelines.

    It does **not** implement the pipelines themselves – those live in their respective modules.
    The orchestrator merely triggers them at configured intervals and exposes a status endpoint.
    """

    def __init__(self, interval_seconds: int = 300):
        self.interval = interval_seconds
        self._task: asyncio.Task | None = None
        self._running: bool = False
        self.fitness_engine = FitnessEngine()
        self._tasks: List[Callable[[], Any]] = [self._run_fitness_scoring]

    async def _run_fitness_scoring(self) -> None:
        """Trigger the fitness engine to evaluate recent skill executions.

        This is intentionally lightweight – the heavy‑lifting is performed inside the engine.
        """
        try:
            logger.info("Orchestrator: Running fitness scoring cycle")
            # The fitness engine maintains internal state and persists scores.
            await self.fitness_engine.evaluate_pending()
        except Exception as exc:
            logger.exception(f"Fitness scoring failed: {exc}")

    async def _loop(self) -> None:
        """Main background loop that runs scheduled tasks at ``self.interval`` seconds.
        """
        self._running = True
        while self._running:
            start = datetime.now(timezone.utc)
            logger.debug(f"Orchestrator loop tick at {start.isoformat()}")
            with tracer.start_as_current_span("orchestrator.tick"):
                for task_fn in self._tasks:
                    await task_fn()
            # Sleep until next interval, taking into account execution time.
            elapsed = (datetime.now(timezone.utc) - start).total_seconds()
            await asyncio.sleep(max(0, self.interval - elapsed))

    def start(self) -> None:
        """Create and schedule the background asyncio task.
        """
        if self._task is None or self._task.done():
            logger.info("Starting Orchestrator background task")
            self._task = asyncio.create_task(self._loop())
        else:
            logger.warning("Orchestrator already running")

    async def stop(self) -> None:
        """Gracefully stop the background loop.
        """
        logger.info("Stopping Orchestrator background task")
        self._running = False
        if self._task:
            await self._task
            self._task = None

    def status(self) -> dict:
        return {"running": self._running, "next_interval_secs": self.interval}


# FastAPI router exposing a simple status endpoint
@router.get("/status")
async def get_status(request: Request):
    orchestrator: Orchestrator = request.app.state.orchestrator  # type: ignore[attr-defined]
    return JSONResponse(content=orchestrator.status())
