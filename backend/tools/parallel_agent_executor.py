import asyncio
import json
import uuid
from collections.abc import Callable
from typing import Any

from loguru import logger


class ParallelAgentExecutor:
    """
    Executes multiple agents concurrently.
    Uses Redis Pub/Sub (Upstash or local) for shared state and inter-agent communication.
    (Closes Devin Gap #2 - Parallel Processing)
    """

    def __init__(self, redis_client=None):
        self.redis_client = redis_client
        self.execution_group = uuid.uuid4().hex[:8]
        logger.info(f"Initialized ParallelAgentExecutor (Group: {self.execution_group})")

    async def _execute_agent_task(self, agent_name: str, task: Callable, *args, **kwargs) -> dict[str, Any]:
        """Wrapper to execute an individual agent's task."""
        logger.info(f"[Agent: {agent_name}] Starting task...")

        try:
            redis = self.redis_client
            if redis is None:
                try:
                    import core.app as app_mod

                    redis = app_mod.redis_queue
                except Exception:
                    redis = None

            if redis and getattr(redis, "configured", False):
                await self._publish_state(redis, agent_name, "started")

            result = await task(*args, **kwargs)

            if redis and getattr(redis, "configured", False):
                await self._publish_state(redis, agent_name, "completed")

            logger.info(f"[Agent: {agent_name}] Task completed successfully.")
            return {"agent": agent_name, "status": "success", "result": result}
        except Exception as e:
            logger.error(f"[Agent: {agent_name}] Task failed: {str(e)}")
            try:
                redis = self.redis_client or app_mod.redis_queue
                if redis and getattr(redis, "configured", False):
                    await self._publish_state(redis, agent_name, "failed", error=str(e))
            except Exception:
                pass
            return {"agent": agent_name, "status": "error", "error": str(e)}

    async def _publish_state(self, redis, agent_name: str, state: str, **kwargs):
        """Publishes agent state to Redis."""
        payload = {
            "agent": agent_name,
            "state": state,
            "group": self.execution_group,
            **kwargs,
        }
        try:
            import inspect

            if inspect.iscoroutinefunction(redis.publish):
                await redis.publish(f"supremeai:agents:{self.execution_group}", json.dumps(payload))
            else:
                redis.publish(f"supremeai:agents:{self.execution_group}", json.dumps(payload))
        except Exception as e:
            logger.warning(f"Failed to publish agent state: {e}")

    async def run_parallel(self, agent_tasks: dict[str, Callable]) -> dict[str, Any]:
        """
        Executes a dictionary of agent tasks in parallel.
        agent_tasks format: {"agent_name": async_callable}
        """
        logger.info(f"Starting {len(agent_tasks)} agents in parallel...")

        tasks = []
        for agent_name, task_func in agent_tasks.items():
            task = asyncio.create_task(self._execute_agent_task(agent_name, task_func))
            tasks.append(task)

        results = await asyncio.gather(*tasks, return_exceptions=True)

        final_output = {}
        for res in results:
            if isinstance(res, dict):
                final_output[res["agent"]] = res
            else:
                logger.error(f"Unexpected exception during gather: {res}")

        return final_output
