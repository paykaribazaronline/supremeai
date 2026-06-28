import asyncio
import json
import os
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

    def __init__(self, redis_client=None, max_concurrent_tasks: int = 10, mcp_registry: dict[str, Any] | None = None):
        # বাংলা মন্তব্য: সমান্তরাল এক্সিকিউশনের জন্য সর্বোচ্চ টাস্ক লিমিট এবং গ্রুপ আইডি সেট করা হচ্ছে।
        self.redis_client = redis_client
        self.execution_group = uuid.uuid4().hex[:8]
        self.max_concurrent_tasks = max_concurrent_tasks
        self.active_tasks_count = 0
        self.mcp_registry = mcp_registry or {}
        logger.info(f"Initialized ParallelAgentExecutor (Group: {self.execution_group}, Max Tasks: {self.max_concurrent_tasks})")

    async def _execute_agent_task(self, agent_name: str, task_def, *args, **kwargs) -> dict[str, Any]:
        """Wrapper to execute an individual agent's task with optional MCP context."""
        if callable(task_def):
            task_func = task_def
            mcp_servers = []
        else:
            task_func = task_def.get("task") if isinstance(task_def, dict) else None
            mcp_servers = task_def.get("mcp_servers", []) if isinstance(task_def, dict) else []

        if task_func is None:
            return {"agent": agent_name, "status": "error", "error": "Invalid task definition: 'task' callable missing"}

        if self.active_tasks_count >= self.max_concurrent_tasks:
            logger.error(f"[Agent: {agent_name}] Task skipped: Concurrent task limit reached ({self.max_concurrent_tasks}).")
            return {"agent": agent_name, "status": "error", "error": "Max concurrent task limit reached"}

        self.active_tasks_count += 1
        logger.info(f"[Agent: {agent_name}] Starting task... (Active tasks: {self.active_tasks_count})")

        mcp_clients = {}
        try:
            redis = self.redis_client
            if redis is None:
                try:
                    import core.app as app_mod
                    redis = app_mod.redis_queue
                except Exception:
                    redis = None

            if mcp_servers:
                mcp_clients = await self._initialize_mcp_clients(agent_name, mcp_servers)

            if redis and getattr(redis, "configured", False):
                await self._publish_state(redis, agent_name, "started")

            # বাংলা মন্তব্য: MCP ক্লায়েন্টগুলোকে কেবলমাত্র এমন টাস্কে ইনজেক্ট করা হচ্ছে যা সেটিকে রিসিভ করতে পারে
            import inspect
            sig = inspect.signature(task_func)
            task_kwargs = dict(kwargs)
            if "mcp_clients" in sig.parameters:
                task_kwargs["mcp_clients"] = mcp_clients

            result = await task_func(*args, **task_kwargs)

            if redis and getattr(redis, "configured", False):
                await self._publish_state(redis, agent_name, "completed")

            logger.info(f"[Agent: {agent_name}] Task completed successfully.")
            return {"agent": agent_name, "status": "success", "result": result}
        except Exception as e:
            logger.error(f"[Agent: {agent_name}] Task failed: {str(e)}")
            try:
                redis = self.redis_client
                if redis is None:
                    import core.app as app_mod
                    redis = app_mod.redis_queue
                if redis and getattr(redis, "configured", False):
                    await self._publish_state(redis, agent_name, "failed", error=str(e))
            except Exception:
                pass
            return {"agent": agent_name, "status": "error", "error": str(e)}
        finally:
            await self._cleanup_mcp_clients(mcp_clients)
            self.active_tasks_count -= 1

    async def _initialize_mcp_clients(self, agent_name: str, mcp_servers: list[str]) -> dict[str, Any]:
        """বাংলা মন্তব্য: এজেন্টের জন্য নির্দিষ্ট MCP সার্ভারগুলোর ক্লায়েন্ট সংযোগ স্থাপন করে।"""
        clients = {}
        for server_name in mcp_servers:
            config = self.mcp_registry.get(server_name)
            if not config:
                logger.warning(f"[Agent: {agent_name}] Unknown MCP server: {server_name}")
                continue

            try:
                from brain.mcp_client import MCPClient
            except ImportError as exc:
                logger.error(f"[Agent: {agent_name}] Cannot import MCPClient: {exc}")
                continue

            command = [config.get("command", "uvx")] + config.get("args", [])
            client = MCPClient(
                server_name=server_name,
                command=command,
                startup_timeout=config.get("startup_timeout", 10),
            )

            # বাংলা মонтаজ: এনভায়রনমেন্ট ভেরিয়েবল সেটআপ করা হচ্ছে।
            if "env" in config and isinstance(config["env"], dict):
                for k, v in config["env"].items():
                    os.environ.setdefault(k, v)

            def _connect(client=client):
                return client.connect()

            try:
                connected = await asyncio.to_thread(_connect)
            except RuntimeError as exc:
                logger.error(f"[Agent: {agent_name}] MCP server '{server_name}' connection failed: {exc}")
                connected = False

            if connected:
                clients[server_name] = client
                logger.info(f"[Agent: {agent_name}] Connected to MCP server: {server_name}")
            else:
                logger.warning(f"[Agent: {agent_name}] Failed to connect to MCP server: {server_name}")

        return clients

    async def _cleanup_mcp_clients(self, clients: dict[str, Any]) -> None:
        """বাংলা মন্তব্য: ব্যবহার শেষে MCP ক্লায়েন্টদের সংযোগ বিচ্ছিন্ন করা হচ্ছে।"""
        for name, mcp_client in clients.items():
            try:
                disconnect_fn = mcp_client.disconnect
                await asyncio.to_thread(disconnect_fn)
            except Exception as exc:
                logger.debug(f"MCP cleanup error for {name}: {exc}")

    async def _publish_state(self, redis, agent_name: str, state: str, **kwargs):
        """Publishes agent state to Redis."""
        # বাংলা মন্তব্য: এজেন্টের বর্তমান রান-টাইম অবস্থা পাবলিশ করার সময় রেডিস ফলব্যাক চেক হ্যান্ডেল করা হচ্ছে।
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
            logger.warning(f"Failed to publish agent state: {e}. Running with local logger fallback.")

    async def run_parallel(self, agent_tasks: dict[str, Callable | dict[str, Any]]) -> dict[str, Any]:
        """
        Executes a dictionary of agent tasks in parallel.
        Supports both simple callables and task definitions with MCP servers.

        Task formats:
          {"agent_name": async_func}                          # backward compatible
          {"agent_name": {"task": async_func, "mcp_servers": [...]}}
        """
        logger.info(f"Starting {len(agent_tasks)} agents in parallel...")

        # বাংলা মন্তব্য: সমান্তরালে সব এজেন্টের কাজ একসাথে চালনা করা হচ্ছে।
        tasks = []
        for agent_name, task_def in agent_tasks.items():
            task = asyncio.create_task(self._execute_agent_task(agent_name, task_def))
            tasks.append(task)

        results = await asyncio.gather(*tasks, return_exceptions=True)

        final_output = {}
        for res in results:
            if isinstance(res, dict) and "agent" in res:
                final_output[res["agent"]] = res
            else:
                logger.error(f"Unexpected exception during gather: {res}")

        return final_output
