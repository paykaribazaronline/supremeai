"""core/agent_supervisor.py — Centralized Agent Supervisor for SupremeAI 2.0.

বাংলা মন্তব্য: এই মডিউলটি সব background agent-এর lifecycle, health monitoring,
এবং auto-restart ব্যবস্থাপনা করে। আগে প্রতিটি agent নিজস্ব lifecycle ম্যানেজ করত,
যার ফলে agent crash করলে কেউ restart দিত না এবং shutdown-এ task leak হতো।

Key Components:
- `AgentSupervisor`: Centralized registry for all background agents with health monitoring,
  auto-restart on failure, and graceful shutdown with configurable timeout.
- `AgentHealth`: Data class tracking each agent's health status, restart count, and last error.

Usage:
    supervisor = AgentSupervisor()
    await supervisor.start_agent("sentinel", sentinel.run_periodic_loop(), health_check_interval=60)
    await supervisor.shutdown_all(timeout=30)
"""

from __future__ import annotations

import asyncio
import time
from collections.abc import Callable, Coroutine
from dataclasses import dataclass
from typing import Any

from loguru import logger

from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus


@dataclass
class AgentHealth:
    """Health status for a single agent."""

    status: str = "initializing"  # initializing | running | failed | stopped
    started_at: float = 0.0
    last_heartbeat: float = 0.0
    restart_count: int = 0
    last_error: str | None = None
    consecutive_failures: int = 0


class AgentSupervisor:
    """
    Centralized supervisor for all background agents.

    বাংলা মন্তব্য: এই supervisor সব agent-কে রেজিস্টার করে, তাদের health মনিটর করে,
    এবং কোনো agent crash করলে auto-restart করে। shutdown-এ সব agent-কে gracefulভাবে
    stop করে এবং timeout handle করে।

    Features:
    - Auto-restart with exponential backoff (1s → 2s → 4s → 8s → max 30s)
    - Health monitoring with heartbeat tracking
    - Graceful shutdown with configurable timeout
    - Failure metrics and event bus integration
    - Dead agent detection (no heartbeat within threshold)
    """

    def __init__(self) -> None:
        self._agents: dict[str, asyncio.Task] = {}
        self._health: dict[str, AgentHealth] = {}
        self._restart_events: dict[str, asyncio.Event] = {}
        self._shutdown_event = asyncio.Event()
        self._monitor_task: asyncio.Task | None = None
        self._lock = asyncio.Lock()

    # ── Public API ──────────────────────────────────────────────────────────────

    async def start_agent(
        self,
        name: str,
        coro_factory: Callable[[], Coroutine[Any, Any, None]],
        health_check_interval: int = 60,
        max_restarts: int = 10,
        restart_delay: float = 1.0,
    ) -> None:
        """Register and start an agent with health monitoring.

        Args:
            name: Unique agent name (e.g., "sentinel", "auto-healer")
            coro_factory: Async callable that returns the agent's main coroutine.
                         Must be a factory so we can re-create it on restart.
            health_check_interval: How often (seconds) the monitor checks this agent.
            max_restarts: Maximum consecutive restarts before giving up.
            restart_delay: Initial delay before restart (doubles on each failure).
        """
        if name in self._agents and not self._agents[name].done():
            logger.warning(f"Agent '{name}' is already running. Skipping duplicate start.")
            return

        self._health[name] = AgentHealth(
            status="initializing",
            started_at=time.time(),
        )
        self._restart_events[name] = asyncio.Event()

        task = asyncio.create_task(
            self._run_with_monitoring(name, coro_factory, max_restarts, restart_delay),
            name=f"agent-{name}",
        )
        self._agents[name] = task
        logger.info(f"✅ Agent '{name}' registered and started.")

    async def stop_agent(self, name: str, timeout: float = 10.0) -> None:
        """Gracefully stop a single agent."""
        task = self._agents.get(name)
        if not task or task.done():
            return

        health = self._health.get(name)
        if health:
            health.status = "stopped"

        task.cancel()
        try:
            await asyncio.wait_for(task, timeout=timeout)
            logger.info(f"✅ Agent '{name}' stopped gracefully.")
        except TimeoutError:
            logger.warning(f"⚠️ Agent '{name}' did not stop within {timeout}s timeout.")
        except asyncio.CancelledError:
            pass

    async def shutdown_all(self, timeout: int = 30) -> None:
        """Gracefully shut down all agents with a global timeout.

        বাংলা মন্তব্য: সব agent-কে cancel() পাঠায় এবং gather() দিয়ে wait করে।
        Timeout হলে বাকি agent-দের force-cancel করে।
        """
        self._shutdown_event.set()

        if not self._agents:
            logger.info("No agents to shut down.")
            return

        # Cancel monitor task first
        if self._monitor_task and not self._monitor_task.done():
            self._monitor_task.cancel()
            try:
                await asyncio.wait_for(self._monitor_task, timeout=5.0)
            except (TimeoutError, asyncio.CancelledError):
                pass

        # Signal all agents to stop
        for name in self._health:
            self._health[name].status = "stopped"

        # Cancel all agent tasks
        for _name, task in self._agents.items():
            if not task.done():
                task.cancel()

        # Wait for all tasks with timeout
        if self._agents:
            try:
                await asyncio.wait_for(
                    asyncio.gather(*self._agents.values(), return_exceptions=True),
                    timeout=timeout,
                )
                logger.info(f"✅ All {len(self._agents)} agents shut down gracefully.")
            except TimeoutError:
                logger.warning(
                    f"⚠️ Agent shutdown timed out after {timeout}s. "
                    f"Remaining: {sum(1 for t in self._agents.values() if not t.done())} agents."
                )
            except asyncio.CancelledError:
                pass

        self._agents.clear()
        self._health.clear()

    def get_health(self, name: str | None = None) -> dict[str, Any]:
        """Get health status for all agents or a specific agent."""
        if name:
            h = self._health.get(name)
            if not h:
                return {"error": f"Agent '{name}' not found"}
            return {
                "name": name,
                "status": h.status,
                "uptime": time.time() - h.started_at if h.started_at else 0,
                "restart_count": h.restart_count,
                "last_error": h.last_error,
                "consecutive_failures": h.consecutive_failures,
            }

        return {
            name: {
                "status": h.status,
                "uptime": time.time() - h.started_at if h.started_at else 0,
                "restart_count": h.restart_count,
                "last_error": h.last_error,
                "consecutive_failures": h.consecutive_failures,
            }
            for name, h in self._health.items()
        }

    def is_agent_running(self, name: str) -> bool:
        """Check if a specific agent is currently running."""
        task = self._agents.get(name)
        return task is not None and not task.done()

    async def wait_for_agent(self, name: str, timeout: float = 30.0) -> bool:
        """Wait for an agent to be running (useful for startup coordination)."""
        event = self._restart_events.get(name)
        if not event:
            return False
        try:
            await asyncio.wait_for(event.wait(), timeout=timeout)
            return True
        except TimeoutError:
            return False

    # ── Internal Monitoring ─────────────────────────────────────────────────────

    @with_error_bus("_run_with_monitoring")
    async def _run_with_monitoring(
        self,
        name: str,
        coro_factory: Callable[[], Coroutine[Any, Any, None]],
        max_restarts: int,
        restart_delay: float,
    ) -> None:
        """Run agent with auto-restart on failure.

        বাংলা মন্তব্য: agent crash করলে exponential backoff দিয়ে auto-restart করে।
        পর পর max_restarts বার fail করলে agent-কে permanently stop করা হয়।
        """
        health = self._health[name]
        restart_count = 0

        while not self._shutdown_event.is_set():
            try:
                health.status = "running"
                health.last_heartbeat = time.time()
                self._restart_events[name].set()

                # Run the agent's main coroutine
                coro = coro_factory()
                await coro

                # If coroutine completes without exception, agent stopped normally
                logger.info(f"Agent '{name}' completed normally.")
                break

            except asyncio.CancelledError:
                logger.info(f"Agent '{name}' cancelled. Shutting down.")
                health.status = "stopped"
                break

            except Exception as exc:
                restart_count += 1
                health.restart_count = restart_count
                health.consecutive_failures += 1
                health.last_error = str(exc)
                health.status = "failed"

                logger.error(f"❌ Agent '{name}' failed (attempt #{restart_count}): {exc}")

                # Emit error event
                try:
                    error_event_bus.emit(
                        ErrorEvent(
                            module="agent_supervisor",
                            error_type="AGENT_FAILED",
                            message=f"Agent '{name}' failed: {exc}",
                            severity="WARNING",
                            structured_context=ErrorContext(module="auto_fixed"),
                            context={
                                "agent": name,
                                "restart_count": restart_count,
                                "error": str(exc)[:200],
                            },
                        )
                    )
                except Exception as bus_exc:
                    logger.warning(f"Failed to emit agent restart event: {bus_exc}")

                # Check max restarts
                if restart_count >= max_restarts:
                    logger.critical(f"🔥 Agent '{name}' exceeded max restarts ({max_restarts}). Giving up permanently.")
                    health.status = "failed_permanent"
                    try:
                        error_event_bus.emit(
                            ErrorEvent(
                                module="agent_supervisor",
                                error_type="AGENT_PERMANENTLY_FAILED",
                                message=f"Agent '{name}' permanently failed after {max_restarts} restarts",
                                severity="CRITICAL",
                                structured_context=ErrorContext(module="auto_fixed"),
                                context={
                                    "agent": name,
                                    "max_restarts": max_restarts,
                                    "last_error": str(exc)[:200],
                                },
                            )
                        )
                    except Exception as bus_exc:
                        logger.warning(f"Failed to emit permanent failure event: {bus_exc}")
                    break

                # Exponential backoff before restart
                delay = min(restart_delay * (2 ** (restart_count - 1)), 30.0)
                logger.info(
                    f"🔄 Restarting agent '{name}' in {delay:.1f}s " f"(attempt {restart_count}/{max_restarts})..."
                )
                await asyncio.sleep(delay)

        # Cleanup
        health.status = "stopped"
        logger.info(f"Agent '{name}' supervisor loop ended.")

    async def _health_monitor_loop(self, check_interval: int = 30) -> None:
        """Periodic health check for all agents.

        বাংলা মন্তব্য: প্রতি check_interval সেকেন্ডে সব agent-এর heartbeat চেক করে।
        কোনো agent dead (no heartbeat for 3x interval) হলে alert পাঠায়।
        """
        dead_threshold = check_interval * 3

        while not self._shutdown_event.is_set():
            await asyncio.sleep(check_interval)

            for name, health in list(self._health.items()):
                if health.status != "running":
                    continue

                task = self._agents.get(name)
                if task and task.done() and not task.cancelled():
                    # Task died without triggering our exception handler
                    # (e.g., if the coroutine returned normally but shouldn't have)
                    logger.warning(f"⚠️ Agent '{name}' task completed unexpectedly. " f"Status was '{health.status}'.")
                    health.status = "failed"
                    health.last_error = "Task completed unexpectedly"

                # Check for dead agent (no heartbeat)
                if health.last_heartbeat > 0:
                    time_since_heartbeat = time.time() - health.last_heartbeat
                    if time_since_heartbeat > dead_threshold:
                        logger.warning(
                            f"⚠️ Agent '{name}' has no heartbeat for "
                            f"{time_since_heartbeat:.0f}s (threshold: {dead_threshold}s)."
                        )

    async def start_monitor(self, check_interval: int = 30) -> None:
        """Start the background health monitor."""
        if self._monitor_task and not self._monitor_task.done():
            return
        self._monitor_task = asyncio.create_task(
            self._health_monitor_loop(check_interval),
            name="agent-health-monitor",
        )
        logger.info("✅ Agent health monitor started.")


# Global singleton
agent_supervisor = AgentSupervisor()
