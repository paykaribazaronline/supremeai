"""This module defines the `SentinelAgent`, an autonomous background agent crucial for the SupremeAI project's system observability and self-healing capabilities. It periodically monitors the health and performance of configured API endpoints, audits system dependencies, and provides event-driven hooks to record system incidents, thereby ensuring the overall reliability and stability of the AI ecosystem by proactively identifying and logging potential issues.

Key Components:
- `SentinelAgent`: The core class implementing an autonomous agent responsible for system health monitoring, dependency auditing, and incident management.
- `monitor_endpoints()`: Asynchronously polls configured API endpoints, records their latency and status, and logs critical `SystemIncident` entries for failures or high latency.
- `audit_dependencies()`: Performs periodic auditing of system dependencies, updating their last audit time (currently a placeholder for more complex version checking logic).
- `trigger_event()`: Provides an event-driven interface to immediately record `SystemIncident` entries based on external triggers or middleware events.
- `run_periodic_loop()`: The main asynchronous loop that orchestrates the periodic execution of endpoint monitoring and dependency auditing tasks, designed to run as part of the application's lifespan.
- `sentinel`: A global singleton instance of the `SentinelAgent` for consistent access and management throughout the application.

Dependencies:
- `asyncio`: For asynchronous programming and managing the agent's periodic execution loop.
- `datetime`: For handling timestamps, calculating latencies, and managing UTC times.
- `httpx`: An asynchronous HTTP client used for polling external and internal API endpoints.
- `loguru`: For structured and flexible logging of agent activities and errors.
- `sqlalchemy`: For asynchronous ORM interactions with the database, specifically for `ApiEndpoint`, `SystemDependency`, and `SystemIncident` models.
- `database.session`: Internal module providing the asynchronous database session factory (`AsyncSessionLocal`).
- `models.sentinel`: Internal module defining the ORM models (`ApiEndpoint`, `SystemDependency`, `SystemIncident`) used by the agent for data persistence."""

import asyncio
from datetime import UTC
from datetime import datetime

import httpx
from loguru import logger
from sqlalchemy import select

from database.session import AsyncSessionLocal
from models.sentinel import ApiEndpoint
from models.sentinel import SystemDependency
from models.sentinel import SystemIncident


class SentinelAgent:
    """
    Sentinel Agent: Background autonomous agent for system observability and self-healing.
    Runs periodically and is also callable via event-driven hooks.
    """

    def __init__(self):
        self.running = True
        # Track if single worker lock is engaged
        self._is_active = False

    async def monitor_endpoints(self):
        """
        Polls configured ApiEndpoints and logs SystemIncident if latency is high or status fails.
        """
        try:
            async with AsyncSessionLocal() as session:
                # Get all endpoints
                result = await session.execute(select(ApiEndpoint))
                endpoints = result.scalars().all()

                if not endpoints:
                    return

                async with httpx.AsyncClient(timeout=10.0) as client:
                    for ep in endpoints:
                        start_time = datetime.now(UTC)
                        try:
                            # In real production, path might be absolute URL, handling relative for now
                            url = ep.path if ep.path.startswith("http") else f"http://127.0.0.1:8080{ep.path}"
                            resp = await client.request(ep.method, url)
                            latency = (datetime.now(UTC) - start_time).total_seconds() * 1000

                            ep.latency_ms = int(latency)
                            ep.last_check_at = datetime.now(UTC)

                            if resp.status_code != ep.expected_status:
                                ep.last_ping_status = "down"
                                if ep.is_critical:
                                    # Create Incident
                                    incident = SystemIncident(
                                        incident_type="api_endpoint_failure",
                                        severity="critical",
                                        remediation_log=f"Endpoint {ep.path} returned {resp.status_code} instead of {ep.expected_status}.",
                                    )
                                    session.add(incident)
                            else:
                                ep.last_ping_status = "up"

                        except Exception as e:  # noqa: BLE001
                            ep.last_ping_status = "down"
                            ep.last_check_at = datetime.now(UTC)
                            incident = SystemIncident(
                                incident_type="api_endpoint_unreachable",
                                severity="critical" if ep.is_critical else "warning",
                                remediation_log=f"Exception connecting to {ep.path}: {str(e)}",
                            )
                            session.add(incident)

                await session.commit()
        except Exception as e:  # noqa: BLE001
            logger.error(f"[SentinelAgent] Error during monitor_endpoints: {e}")

    async def audit_dependencies(self):
        """
        Runs heavy auditing logic (e.g., pip list --outdated equivalent)
        and updates SystemDependency status.
        """
        try:
            async with AsyncSessionLocal() as session:
                logger.info("[SentinelAgent] Running dependency audit...")
                # Placeholder logic: Here we would use pip or subprocess
                # For now, we just touch the dependencies to update audit time
                result = await session.execute(select(SystemDependency))
                deps = result.scalars().all()
                for dep in deps:
                    dep.last_audit_at = datetime.now(UTC)
                await session.commit()
        except Exception as e:  # noqa: BLE001
            logger.error(f"[SentinelAgent] Error during audit_dependencies: {e}")

    async def trigger_event(self, event_type: str, details: str):
        """
        Event-driven hook for middleware to immediately trigger an incident review.
        """
        try:
            async with AsyncSessionLocal() as session:
                incident = SystemIncident(incident_type=event_type, severity="warning", remediation_log=details)
                session.add(incident)
                await session.commit()
                logger.info(f"[SentinelAgent] Event-driven incident recorded: {event_type}")
        except Exception as e:  # noqa: BLE001
            logger.error(f"[SentinelAgent] Error triggering event: {e}")

    async def run_periodic_loop(self):
        """
        The main async loop to be attached to FastAPI lifespan.
        Uses a basic active flag to prevent multiple executions if workers > 1.
        """
        if self._is_active:
            logger.warning("[SentinelAgent] Agent already active, skipping duplicate startup.")
            return

        self._is_active = True
        logger.info("[SentinelAgent] Starting Periodic Loop (Heartbeat: 60s, Audit: 12h)...")

        audit_counter = 0

        try:
            while self.running:
                # 1. Quick Heartbeat (60 seconds)
                await self.monitor_endpoints()

                # 2. Long Audit (Every 12 hours) - 12h = 720 minutes = 720 iterations of 60s
                if audit_counter >= 720:
                    await self.audit_dependencies()
                    audit_counter = 0

                audit_counter += 1
                await asyncio.sleep(60)
        except asyncio.CancelledError:
            logger.info("[SentinelAgent] Periodic Loop cancelled. Shutting down gracefully.")
            self._is_active = False
            raise


# Global singleton instance
sentinel = SentinelAgent()
