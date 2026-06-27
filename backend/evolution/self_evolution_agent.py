import asyncio
import contextlib
import json
import time
from pathlib import Path
from typing import Any

from loguru import logger


class SelfEvolutionAgent:
    """Autonomous agent that monitors skill fitness, triggers creation of missing
    capabilities, and self-refactors underperforming skills on a cron-like loop."""

    def __init__(
        self,
        fitness_engine: Any | None = None,
        auto_skill_creator: Any | None = None,
        interval_seconds: int = 300,
        fitness_threshold: float = 0.5,
        refactor_penalty_threshold: float = 0.3,
        min_runs_before_action: int = 5,
        max_consecutive_penalties: int = 3,
    ) -> None:
        from evolution.auto_skill_creator import AutoSkillCreator
        from evolution.fitness_engine import FitnessEngine

        self.fitness_engine = fitness_engine or FitnessEngine()
        self.auto_skill_creator = auto_skill_creator or AutoSkillCreator()
        self.interval_seconds = interval_seconds
        self.fitness_threshold = fitness_threshold
        self.refactor_penalty_threshold = refactor_penalty_threshold
        self.min_runs_before_action = min_runs_before_action
        self.max_consecutive_penalties = max_consecutive_penalties

        base_dir = Path(__file__).resolve().parent.parent.parent
        self.log_path = base_dir / "backend" / "data" / "evolution_logs.jsonl"

        self._running: bool = False
        self._task: asyncio.Task | None = None
        self._consecutive_penalties: dict[str, int] = {}
        self._pending_demands: asyncio.Queue = asyncio.Queue()

    async def start(self) -> None:
        if self._running:
            return
        self._running = True
        self._task = asyncio.create_task(self._loop())
        logger.info(f"SelfEvolutionAgent started (interval={self.interval_seconds}s)")

    async def stop(self) -> None:
        self._running = False
        if self._task:
            self._task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._task
            self._task = None
        logger.info("SelfEvolutionAgent stopped")

    async def _loop(self) -> None:
        while self._running:
            start = time.time()
            try:
                await self._tick()
            except Exception:
                logger.exception("Self-evolution tick failed")
            elapsed = time.time() - start
            try:
                await asyncio.sleep(max(0.0, self.interval_seconds - elapsed))
            except asyncio.CancelledError:
                break

    async def _tick(self) -> None:
        metrics = self.fitness_engine.metrics
        if not metrics:
            return

        for skill_name in list(metrics.keys()):
            await self._evaluate_skill(skill_name)

        while not self._pending_demands.empty():
            demand = await self._pending_demands.get()
            await self._process_demand(demand)

    async def _evaluate_skill(self, skill_name: str) -> None:
        score = self.fitness_engine.calculate_fitness(skill_name)
        entry = self.fitness_engine.metrics.get(skill_name, {})
        total_runs = entry.get("success_count", 0) + entry.get("failure_count", 0)

        if total_runs < self.min_runs_before_action:
            return

        if score < self.refactor_penalty_threshold:
            self._consecutive_penalties[skill_name] = self._consecutive_penalties.get(skill_name, 0) + 1
            if self._consecutive_penalties[skill_name] >= self.max_consecutive_penalties:
                await self._trigger_refactor(skill_name)
                self._consecutive_penalties[skill_name] = 0
        else:
            self._consecutive_penalties.pop(skill_name, None)

        if score < self.fitness_threshold:
            pruned = self.fitness_engine.evaluate_and_prune(skill_name, self.fitness_threshold, self.min_runs_before_action)
            if pruned:
                self._log_action({"action": "prune", "skill_name": skill_name, "score": score})

    async def _trigger_refactor(self, skill_name: str) -> None:
        logger.warning(f"Skill '{skill_name}' hit consecutive penalty threshold. Refactoring...")
        current_code = self._read_skill_code(skill_name)
        user_demand = (
            f"Refactor the existing skill '{skill_name}' to drastically improve its fitness score.\n"
            f"Current source code:\n{current_code}\n"
            "Preserve the public interface (class name and async execute(self, kwargs) -> dict method).\n"
        )
        refactored_name = f"{skill_name}_v2"
        result = await self.auto_skill_creator.generate_and_deploy_skill(user_demand, refactored_name)
        self._log_action(
            {
                "action": "refactor",
                "skill_name": skill_name,
                "refactored_name": refactored_name,
                "success": result.get("success", False),
                "error": result.get("error"),
            }
        )
        if result.get("success"):
            logger.info(f"Refactored skill deployed as '{refactored_name}'")
        else:
            logger.error(f"Refactor failed for '{skill_name}': {result.get('error')}")

    def _read_skill_code(self, skill_name: str) -> str:
        base_dir = Path(__file__).resolve().parent.parent.parent
        candidates = [
            base_dir / "skills" / "dynamic" / skill_name / "main.py",
            base_dir / "skills" / "deprecated" / skill_name / "main.py",
        ]
        for path in candidates:
            if path.exists():
                return path.read_text(encoding="utf-8")
        return "// Source code not found"

    def _log_action(self, entry: dict[str, Any]) -> None:
        entry["timestamp"] = time.time()
        try:
            with open(self.log_path, "a", encoding="utf-8") as f:
                f.write(json.dumps(entry, default=str) + "\n")
        except Exception as e:
            logger.error(f"Failed to write evolution log: {e}")

    def has_high_fitness_path(self, skill_name: str) -> bool:
        if skill_name in self.fitness_engine.metrics:
            return self.fitness_engine.calculate_fitness(skill_name) >= self.fitness_threshold
        skill_data = self.fitness_engine.registry.get_skill(skill_name)
        return skill_data is not None

    def register_missing_path(self, task_demand: str, skill_name: str) -> None:
        """External hook for orchestrators to register task demands lacking a high-fitness path."""
        self._pending_demands.put_nowait({"task_demand": task_demand, "skill_name": skill_name})

    async def _process_demand(self, demand: dict[str, str]) -> None:
        task_demand = demand["task_demand"]
        skill_name = demand["skill_name"]
        if self.has_high_fitness_path(skill_name):
            return
        logger.info(f"No high-fitness path for '{skill_name}'. Triggering AutoSkillCreator.")
        result = await self.auto_skill_creator.generate_and_deploy_skill(task_demand, skill_name)
        self._log_action(
            {
                "action": "generate",
                "skill_name": skill_name,
                "task_demand": task_demand,
                "success": result.get("success", False),
                "error": result.get("error"),
            }
        )
        if result.get("success"):
            logger.info(f"Auto-generated skill '{skill_name}' deployed.")
        else:
            logger.error(f"Auto-skill generation failed for '{skill_name}': {result.get('error')}")
