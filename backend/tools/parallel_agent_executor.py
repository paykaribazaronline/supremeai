import asyncio
import json
import os
import uuid
from collections.abc import AsyncIterator, Callable
from typing import Any

from loguru import logger


class ParallelAgentExecutor:
    """
    Executes multiple agents concurrently.
    Uses Redis Pub/Sub (Upstash or local) for shared state and inter-agent communication.
    (Closes Devin Gap #2 - Parallel Processing)
    """

    def __init__(
        self,
        redis_client=None,
        max_concurrent_tasks: int = 10,
        mcp_registry: dict[str, Any] | None = None,
    ):
        # বাংলা মন্তব্য: সমান্তরাল এক্সিকিউশনের জন্য সর্বোচ্চ টাস্ক লিমিট এবং গ্রুপ আইডি সেট করা হচ্ছে।
        self.redis_client = redis_client
        self.execution_group = uuid.uuid4().hex[:8]
        self.max_concurrent_tasks = max_concurrent_tasks
        self.active_tasks_count = 0
        self.mcp_registry = mcp_registry or {}
        logger.info(
            f"Initialized ParallelAgentExecutor (Group: {self.execution_group}, Max Tasks: {self.max_concurrent_tasks})"
        )

    async def _execute_agent_task(
        self, agent_name: str, task_def, *args, **kwargs
    ) -> dict[str, Any]:
        """Wrapper to execute an individual agent's task with optional MCP context."""
        if callable(task_def):
            task_func = task_def
            mcp_servers = []
        else:
            task_func = task_def.get("task") if isinstance(task_def, dict) else None
            mcp_servers = (
                task_def.get("mcp_servers", []) if isinstance(task_def, dict) else []
            )

        if task_func is None:
            return {
                "agent": agent_name,
                "status": "error",
                "error": "Invalid task definition: 'task' callable missing",
            }

        if self.active_tasks_count >= self.max_concurrent_tasks:
            logger.error(
                f"[Agent: {agent_name}] Task skipped: Concurrent task limit reached ({self.max_concurrent_tasks})."
            )
            return {
                "agent": agent_name,
                "status": "error",
                "error": "Max concurrent task limit reached",
            }

        self.active_tasks_count += 1
        logger.info(
            f"[Agent: {agent_name}] Starting task... (Active tasks: {self.active_tasks_count})"
        )

        mcp_clients = {}
        try:
            redis = self.redis_client
            if redis is None:
                try:
                    import core.services as app_mod

                    redis = app_mod.redis_queue
                except Exception as e:  # noqa: BLE001
                    try:
                        import loguru

                        loguru.logger.error(f"Tool execution error: {e}")
                    except Exception as e:  # noqa: BLE001
                        import logging

                        logging.warning(f"Exception suppressed: {e}")
                    redis = None

            if mcp_servers:
                mcp_clients = await self._initialize_mcp_clients(
                    agent_name, mcp_servers
                )

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
        except Exception as e:  # noqa: BLE001
            logger.error(f"[Agent: {agent_name}] Task failed: {str(e)}")
            try:
                redis = self.redis_client
                if redis is None:
                    import core.services as app_mod

                    redis = app_mod.redis_queue
                if redis and getattr(redis, "configured", False):
                    await self._publish_state(redis, agent_name, "failed", error=str(e))
            except Exception as e:  # noqa: BLE001
                try:
                    import loguru

                    loguru.logger.error(f"Tool execution error: {e}")
                except Exception as e:  # noqa: BLE001
                    import logging

                    logging.warning(f"Exception suppressed: {e}")
                pass
            return {"agent": agent_name, "status": "error", "error": str(e)}
        finally:
            await self._cleanup_mcp_clients(mcp_clients)
            self.active_tasks_count -= 1

    async def _initialize_mcp_clients(
        self, agent_name: str, mcp_servers: list[str]
    ) -> dict[str, Any]:
        """বাংলা মন্তব্য: এজেন্টের জন্য নির্দিষ্ট MCP সার্ভারগুলোর ক্লায়েন্ট সংযোগ স্থাপন করে।"""
        clients = {}
        for server_name in mcp_servers:
            config = self.mcp_registry.get(server_name)
            if not config:
                logger.warning(
                    f"[Agent: {agent_name}] Unknown MCP server: {server_name}"
                )
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

            # বাংলা মন্তব্য: এনভায়রনমেন্ট ভেরিয়েবল সেটআপ করা হচ্ছে।
            if "env" in config and isinstance(config["env"], dict):
                for k, v in config["env"].items():
                    os.environ.setdefault(k, v)

            def _connect(client=client):
                return client.connect()

            try:
                connected = await asyncio.to_thread(_connect)
            except RuntimeError as exc:
                logger.error(
                    f"[Agent: {agent_name}] MCP server '{server_name}' connection failed: {exc}"
                )
                connected = False

            if connected:
                clients[server_name] = client
                logger.info(
                    f"[Agent: {agent_name}] Connected to MCP server: {server_name}"
                )
            else:
                logger.warning(
                    f"[Agent: {agent_name}] Failed to connect to MCP server: {server_name}"
                )

        return clients

    async def _cleanup_mcp_clients(self, clients: dict[str, Any]) -> None:
        """বাংলা মন্তব্য: ব্যবহার শেষে MCP ক্লায়েন্টদের সংযোগ বিচ্ছিন্ন করা হচ্ছে।"""
        for name, mcp_client in clients.items():
            try:
                disconnect_fn = mcp_client.disconnect
                await asyncio.to_thread(disconnect_fn)
            except Exception as exc:  # noqa: BLE001
                logger.debug(f"MCP cleanup error for {name}: {exc}")

    async def _publish_state(self, redis, agent_name: str, state: str, **kwargs):
        """Publishes agent state to Redis."""
        # বাংলা মন্তব্য: এজেন্টের বর্তমান রান-টাইম অবস্থা পাবলিশ করার সময় রেডিস ফলব্যাক চেক হ্যান্ডেল করা হচ্ছে।
        payload = {
            "agent": agent_name,
            "state": state,
            "group": self.execution_group,
            **kwargs,
        }
        try:
            import inspect

            if inspect.iscoroutinefunction(redis.publish):
                await redis.publish(
                    f"supremeai:agents:{self.execution_group}", json.dumps(payload)
                )
            else:
                redis.publish(
                    f"supremeai:agents:{self.execution_group}", json.dumps(payload)
                )
        except Exception as e:  # noqa: BLE001
            logger.warning(
                f"Failed to publish agent state: {e}. Running with local logger fallback."
            )

    async def run_parallel(
        self, agent_tasks: dict[str, Callable | dict[str, Any]]
    ) -> dict[str, Any]:
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


class DAGNode:
    """টাস্ক DAG-এর একটি নোড (node) — এজেন্ট ও তার ডিপেন্ডেন্সি সংরক্ষণ করে।"""

    def __init__(self, name: str, task_def: Any, depends_on: list[str] | None = None):
        self.name = name
        self.task_def = task_def
        self.depends_on = depends_on or []
        self.result: dict[str, Any] | None = None


class AgentDAGScheduler:
    """
    ডিপেন্ডেন্সি-সচেতন (dependency-aware) টাস্ক DAG শিডিউলার।

    Devin Gap #2 (Parallel Processing) বন্ধ করতে এটি:
      - Redis Pub/Sub দিয়ে এজেন্টদের মধ্যে রিয়েল-টাইম যোগাযোগ করে
      - Shared state: একজন Coder কোড লিখলে Tester সাথে সাথে টেস্ট লিখতে শুরু করে
      - একাধিক এজেন্টের আউটপুট থেকে ভোটিং (voting) দিয়ে সেরাটি বাছাই করে
    """

    def __init__(self, redis_client=None, max_concurrent_tasks: int = 10):
        self.redis_client = redis_client
        self.max_concurrent_tasks = max_concurrent_tasks
        self.executor = ParallelAgentExecutor(
            redis_client=redis_client, max_concurrent_tasks=max_concurrent_tasks
        )
        logger.info("Initialized AgentDAGScheduler (dependency-aware scheduling)")

    async def broadcast_state(self, channel: str, state: dict[str, Any]) -> None:
        """একটি চ্যানেলে এজেন্টের শেয়ার্ড স্টেট পাবলিশ করে।"""
        if self.redis_client is None:
            logger.debug(f"[broadcast] {channel}: {state}")
            return
        try:
            import json

            publish = self.redis_client.publish
            if callable(getattr(publish, "__await__", None)):
                await publish(channel, json.dumps(state))
            else:
                publish(channel, json.dumps(state))
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to broadcast state on {channel}: {e}")

    async def subscribe_to_updates(self, channel: str) -> AsyncIterator[dict[str, Any]]:
        """একটি চ্যানেল থেকে আপডেট সাবস্ক্রাইব করে (async generator)।"""
        if self.redis_client is None:
            return
        try:
            pubsub = self.redis_client.pubsub()
            if callable(getattr(pubsub.subscribe, "__await__", None)):
                await pubsub.subscribe(channel)
            else:
                pubsub.subscribe(channel)
            import json

            async for message in pubsub.listen():
                if isinstance(message, dict) and message.get("type") == "message":
                    try:
                        yield json.loads(message["data"])
                    except (json.JSONDecodeError, TypeError) as parse_err:
                        logger.error(
                            f"[PubSub] Failed to parse message on {channel}. Payload: {message.get('data')}. Error: {parse_err}"
                        )
                        continue
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Subscription to {channel} ended: {e}")
            return

    async def execute_dag(self, task_graph: dict[str, DAGNode]) -> dict[str, Any]:
        """
        ডিপেন্ডেন্সি অনুযায়ী টপোলজিক্যাল অর্ডারে DAG এক্সিকিউট করে।

        task_graph: {node_name: DAGNode}
        """
        ordered = self._topological_sort(task_graph)
        aggregated: dict[str, Any] = {"nodes": {}, "order": ordered}

        for level in ordered:
            # বাংলা মন্তব্য: একই লেভেলের নোডগুলো সমান্তরালে (parallel) চালানো হচ্ছে।
            tasks = {name: task_graph[name].task_def for name in level}
            results = await self.executor.run_parallel(tasks)
            for name in level:
                task_graph[name].result = results.get(name)
                aggregated["nodes"][name] = results.get(name)
                # বাংলা মন্তব্য: শেয়ার্ড স্টেট হিসেবে অন্যদের জানানো হচ্ছে।
                await self.broadcast_state(
                    "supremeai:dag:updates",
                    {"node": name, "status": "completed", "result": results.get(name)},
                )

        aggregated["voted_best"] = self._aggregate_with_voting(
            [n.result for n in task_graph.values()]
        )
        return aggregated

    def _topological_sort(self, task_graph: dict[str, DAGNode]) -> list[list[str]]:
        """স্তরভিত্তিক (level-based) টপোলজিক্যাল সর্ট — প্রতিটি স্তর সমান্তরালে চালানো যায়।"""
        in_degree = dict.fromkeys(task_graph, 0)
        for name, node in task_graph.items():
            for dep in node.depends_on:
                if dep in in_degree:
                    in_degree[name] += 1

        levels: list[list[str]] = []
        remaining = dict(in_degree)
        completed: set[str] = set()

        while remaining:
            current_level = [
                n for n, d in remaining.items() if d == 0 and n not in completed
            ]
            if not current_level:
                # বাংলা মন্তব্য: সাইক্লিক ডিপেন্ডেন্সি থাকলে বাকিগুলো সরাসরি যোগ করা হচ্ছে।
                logger.warning(
                    "Cyclic dependency detected in DAG; forcing remaining nodes into final level."
                )
                current_level = list(remaining.keys())
            for n in current_level:
                completed.add(n)
                del remaining[n]
            for name, node in task_graph.items():
                if name in remaining:
                    for dep in node.depends_on:
                        if dep in completed:
                            remaining[name] -= 1
            levels.append(current_level)

        return levels

    def _aggregate_with_voting(
        self, results: list[dict[str, Any] | None]
    ) -> dict[str, Any] | None:
        """একাধিক এজেন্টের আউটপুট থেকে ভোটিং দিয়ে সেরাটি বাছাই করে।"""
        valid = [
            r for r in results if isinstance(r, dict) and r.get("status") == "success"
        ]
        if not valid:
            return None
        # বাংলা মন্তব্য: সবচেয়ে বড় আউটপুটকে 'সেরা' ধরা হচ্ছে।
        best = max(valid, key=lambda r: len(str(r.get("result", ""))))
        return {"selected_agent": best.get("agent"), "result": best.get("result")}
