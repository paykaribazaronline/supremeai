from core.messaging.event_bus import ErrorContext

"""This module serves as the central FastAPI application lifespan manager for the SupremeAI project, orchestrating the robust startup and graceful shutdown of all critical backend infrastructure. It handles the initialization of essential services such as database connection pools, Redis caches, global HTTP clients, OpenTelemetry tracing, the core AI Orchestrator, and various background agents, ensuring the application is fully prepared to serve requests. The module is designed with defensive programming principles, allowing the application to start in a degraded mode if certain non-critical services fail to initialize, thereby enhancing operational stability and resilience in a highly scalable AI ecosystem.

Key Components:
- `_ensure_api_key_tables()`: Asynchronously creates or verifies the existence of database tables required for API key management, usage tracking, and event logging.
- `app_lifespan()`: An asynchronous context manager that manages the entire lifecycle of the FastAPI application, performing comprehensive setup during startup and orderly teardown during shutdown.

Dependencies:
- `asyncio`: For managing asynchronous operations and background tasks.
- `os`: For accessing environment variables to determine runtime context.
- `contextlib`: Provides the `asynccontextmanager` decorator for lifespan management.
- `httpx`: Used for creating and managing a global asynchronous HTTP client pool.
- `loguru`: For structured and informative logging throughout the application lifecycle.
- `core.services`: Provides global service instances, including the shared HTTP client.
- `core.config`: Accesses application-wide configuration settings.
- `core.config_cache`: Manages the system's dynamic configuration cache.
- `core.messaging.event_bus`: Facilitates internal error reporting and event emission.
- `core.maintenance_pipeline`: Manages background monitoring and health checks for the system.
- `core.orchestration.orchestrator`: Initializes and manages the core AI orchestration engine.
- `core.pgbouncer_pool`: Handles the initialization and closing of PostgreSQL database connection pools.
- `core.cache.redis_manager`: Manages connections to the Redis caching service.
- `core.observability.telemetry`: Initializes OpenTelemetry for distributed tracing.
- `core.sentinel_agent`: Manages a background agent responsible for periodic system tasks.
- `database.db`: Used for bootstrapping and ensuring the integrity of the Supabase database schema.
- `tools.ai_agents.browser_agent`: Provides functionality to shut down any globally managed browser instances.
- `core.metrics_collector`: Collects system metrics for observability and monitoring."""

# backend/core/lifespan.py
# ⚠️ WARNING: DO NOT MOVE THIS FILE. It is heavily integrated into the FastAPI startup lifecycle.
# Moving this file will break relative paths, imports, and core app lifespan management.
import asyncio
import time  # - Added for metrics collection
from contextlib import asynccontextmanager

import httpx
from loguru import logger

from core import services
from core.config import settings
from core.messaging.event_bus import ErrorEvent, error_event_bus
from core.metrics_collector import metrics_collector
from core.orchestration.orchestrator import Orchestrator
from core.reliability_controller import ReliabilityController
from core.startup_validator import StartupValidator


@asynccontextmanager
async def app_lifespan(app):
    """
    SupremeAI 2.0 Core Lifespan Manager.
    Handles high-concurrency initialization and defensive teardowns.
    """
    logger.info("🌐 Core Infrastructure Bootstrapping Active...")

    # Initialize Intelligent Silent Catcher for real-time observability
    from core.intelligent_silent_catcher import setup_silent_catcher

    setup_silent_catcher()

    # Record system startup
    await metrics_collector.set_gauge("system_startup_time", time.time())

    # বাংলা মন্তব্ব্য: স্টার্টআপ ভ্যালিডেশন এবং নির্ভরযোগ্যতা নিয়ন্ত্রণ প্যানেল বুটস্ট্র্যাপ করা।
    await StartupValidator.validate()
    await ReliabilityController.initialize()
    app.state.subsystem_status = {"db": "up", "redis": "up", "config": "up"}

    # Update metrics with subsystem status
    await metrics_collector.set_gauge(
        "subsystem_db_status", 1 if app.state.subsystem_status["db"] == "up" else 0, {"subsystem": "db"}
    )
    await metrics_collector.set_gauge(
        "subsystem_redis_status", 1 if app.state.subsystem_status["redis"] == "up" else 0, {"subsystem": "redis"}
    )
    await metrics_collector.set_gauge(
        "subsystem_config_status", 1 if app.state.subsystem_status["config"] == "up" else 0, {"subsystem": "config"}
    )

    # ── Parallelized Startup Phase 1: Independent services ──────────────────
    # বাংলা মন্তব্ব্য: P2 Fix — startup latency এবং cold start freeze এড়াতে
    # স্বাধীন সার্ভিসগুলো asyncio.gather() দিয়ে সমান্তরালে চালানো হচ্ছে।
    # Sequential dependency: HTTP client must be initialized first (others depend on it).

    # Global HTTP client initialization (sequential — dependency for others)
    services.global_http_client = httpx.AsyncClient(
        limits=httpx.Limits(max_keepalive_connections=50, max_connections=200),
        timeout=httpx.Timeout(30.0),
        headers={"User-Agent": "SupremeAI-Orchestrator/2.0"},
    )
    app.state.http_client = services.global_http_client
    services.model_router._http_client = services.global_http_client
    logger.info("✅ Global HTTP Connection Pool initialized [Max Cons: 200].")

    # Parallel Phase: DB pool, Config cache, Redis, Tracing, CostGuard
    # Phase 1: independent infrastructure initialization.
    from core.startup.services import initialize_independent_services

    await initialize_independent_services(app)

    # ── Sequential Phase 2: Services that depend on Phase 1 ─────────────────
    # Orchestrator initialization (depends on HTTP client + DB)
    try:
        orch_inst = Orchestrator()
        app.state.orchestrator = orch_inst
        logger.info("⚙️ Orchestrator background tasks initialized successfully.")
        await metrics_collector.increment_counter("orchestrator_init_success_total")
    except Exception as e:
        logger.error(f"Failed to initialize Orchestrator: {e}")
        await metrics_collector.increment_counter("orchestrator_init_failure_total")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="ORCHESTRATOR_INIT_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"component": "orchestrator"},
            )
        )
        app.state.orchestrator = None

    # Supabase schema bootstrap (depends on DB pool)
    try:
        from database import db as supabase_db

        if settings.supabase_database_url:
            await asyncio.wait_for(asyncio.to_thread(supabase_db.bootstrap_schema), timeout=30.0)
            logger.info("Supabase schema bootstrap complete")
    except TimeoutError:
        logger.warning("Supabase schema bootstrap timed out after 30s — continuing without full schema init.")
    except Exception as exc:
        logger.warning(f"Supabase bootstrap failed on startup: {exc}. Continuing without schema bootstrap.")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SUPABASE_BOOTSTRAP_FAILED",
                message=str(exc)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"component": "supabase"},
            )
        )


    # Background maintenance and agents are kept in a dedicated startup module.
    from core.startup.agents import start_background_services

    await start_background_services(app)

    yield  # এখানে অ্যাপ্লিকেশন ট্রাফিক রিসিভ করবে

    from core.shutdown import shutdown_services

    await shutdown_services(app)
