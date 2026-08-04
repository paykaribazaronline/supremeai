from core.error_bus import with_error_bus
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
from core.agent_supervisor import agent_supervisor
from core.cache.redis_manager import redis_manager
from core.config import settings
from core.config_cache import config_cache
from core.maintenance_pipeline import maintenance_pipeline
from core.messaging.event_bus import ErrorEvent, error_event_bus
from core.metrics_collector import metrics_collector, record_db_operation
from core.orchestration.orchestrator import Orchestrator
from core.persistence import pooled_pg
from core.persistence.write_behind import flush_all as flush_write_behind_batchers
from core.pgbouncer_pool import get_db_pool, init_db_pool
from core.reliability_controller import ReliabilityController
from core.startup_validator import StartupValidator


@with_error_bus("_ensure_api_key_tables")
async def _ensure_api_key_tables() -> None:
    """Ensure API key database tables exist."""
    pool = await get_db_pool()
    # Record the database operation
    start_time = time.time()
    success = True

    # বাংলা মন্তব্ব্য: PgBouncerConnectionPool.acquire() একটি coroutine হওয়ায় সরাসরি async context manager হিসেবে ব্যবহার করা যায় না।
    # তাই এটিকে প্রথমে await করে কানেকশনটি তুলে আনা হচ্ছে এবং finally ব্লকে রিলিজ করা হচ্ছে।
    conn = await pool.acquire()
    try:
        async with conn.transaction():
            await conn.execute("""
                CREATE TABLE IF NOT EXISTS api_keys (
                    id SERIAL PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    key_hash TEXT NOT NULL UNIQUE,
                    key_masked TEXT NOT NULL,
                    key_prefix TEXT NOT NULL,
                    rate_limit_rps INTEGER DEFAULT 6,
                    rate_limit_window INTEGER DEFAULT 60,
                    revoked BOOLEAN DEFAULT FALSE,
                    expires_at INTEGER,
                    last_used_at INTEGER,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """)
            await conn.execute("""
                CREATE TABLE IF NOT EXISTS api_key_usage (
                    id SERIAL PRIMARY KEY,
                    api_key_id INTEGER NOT NULL REFERENCES api_keys(id),
                    endpoint TEXT NOT NULL,
                    status_code INTEGER NOT NULL,
                    latency_ms DOUBLE PRECISION NOT NULL DEFAULT 0,
                    ip_address TEXT,
                    created_at INTEGER NOT NULL
                )
                """)
            await conn.execute("""
                CREATE TABLE IF NOT EXISTS api_key_events (
                    id SERIAL PRIMARY KEY,
                    api_key_id INTEGER NOT NULL REFERENCES api_keys(id),
                    event_type TEXT NOT NULL,
                    details TEXT,
                    ip_address TEXT,
                    created_at INTEGER NOT NULL
                )
                """)
            await conn.execute("CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys(key_hash)")
            await conn.execute("ALTER TABLE api_keys ADD COLUMN IF NOT EXISTS rate_limit_window INTEGER DEFAULT 60")
            await conn.execute(
                "CREATE INDEX IF NOT EXISTS idx_api_key_usage_key ON api_key_usage(api_key_id, created_at DESC)"
            )
    except Exception:
        success = False
        raise
    finally:
        await pool.release(conn)
        duration = time.time() - start_time
        await record_db_operation("ensure_api_key_tables", duration, success)

    logger.info("✅ API key tables ensured")


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
    @with_error_bus("_init_tracing")
    async def _init_tracing() -> None:
        """Initialize OpenTelemetry tracing in a thread to avoid blocking."""
        try:
            from core.observability.telemetry import setup_tracing

            await asyncio.to_thread(setup_tracing)
            logger.info("✅ OpenTelemetry tracing provider successfully initialized.")
        except Exception as exc:
            logger.warning(f"Failed to initialize tracing provider: {exc}")
            error_event_bus.emit(
                ErrorEvent(
                    module="lifespan",
                    error_type="TRACING_INIT_FAILED",
                    message=str(exc)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"component": "opentelemetry"},
                )
            )

    @with_error_bus("_init_db_pool")
    async def _init_db_pool() -> None:
        """Initialize database connection pool and API key tables with unified connection management."""
        _db_url = settings.supabase_database_url
        try:
            if "sqlite" in _db_url:
                logger.info(
                    "💾 SQLite Memory Database Detected for Agent Telemetry. Skipping PostgreSQL asyncpg pool initialization."
                )
                app.state.db_pool = None
            else:
                # Initialize connection pool with optimized settings
                await init_db_pool(_db_url)

                # Verify pool health with a quick test query
                pool = await get_db_pool()
                if pool:
                    try:
                        conn = await pool.acquire()
                        try:
                            # Test connection with a simple query
                            await conn.fetchval("SELECT 1")
                            logger.info("✅ Database connection pool health check passed.")
                        finally:
                            await pool.release(conn)
                    except Exception as health_exc:
                        logger.error(f"❌ Database pool health check failed: {health_exc}")
                        app.state.subsystem_status["db"] = "degraded"
                        raise health_exc

                logger.info("⚡ PgBouncer connection pool successfully initialized at startup.")
                await _ensure_api_key_tables()

                # Optimize queries with connection pooling best practices
                app.state.db_pool = pool
        except Exception as exc:
            logger.error(f"❌ Failed to initialize DB Pool: {exc}")
            app.state.db_pool = None
            app.state.subsystem_status["db"] = "down"
            error_event_bus.emit(
                ErrorEvent(
                    module="lifespan",
                    error_type="DB_POOL_INIT_FAILED",
                    message=str(exc)[:200],
                    severity="CRITICAL" if settings.env == "production" else "WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={
                        "_db_url": _db_url[:50] if _db_url else "",
                        "env": settings.env,
                    },
                )
            )
            if settings.env == "production":
                logger.critical(
                    "🔥 PRODUCTION DB UNAVAILABLE — running in degraded mode. DB-dependent endpoints will return 503."
                )

    @with_error_bus("_init_config_cache")
    async def _init_config_cache() -> None:
        """Initialize system configuration cache."""
        try:
            await config_cache.refresh_async()
            logger.info("✅ System configuration cache successfully initialized.")
        except Exception as exc:
            logger.warning(f"⚠️ Async config load failed, falling back to local DEFAULT_CONFIGS: {exc}")
            app.state.subsystem_status["config"] = "fallback"
            error_event_bus.emit(
                ErrorEvent(
                    module="lifespan",
                    error_type="CONFIG_CACHE_INIT_FAILED",
                    message=str(exc)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"fallback": "DEFAULT_CONFIGS"},
                )
            )
            from core.config_cache import DEFAULT_CONFIGS

            config_cache._cache = dict(DEFAULT_CONFIGS)

    @with_error_bus("_init_redis")
    async def _init_redis() -> None:
        """Verify Redis connection and restore reliability state."""
        try:
            if getattr(redis_manager, "client", None):
                await redis_manager.client.ping()
                logger.info("✅ Redis connection verified successfully.")
                await ReliabilityController.restore_from_persistence()
        except Exception as e:
            logger.error(f"Failed to initialize Redis Manager: {e}")
            app.state.subsystem_status["redis"] = "down"
            error_event_bus.emit(
                ErrorEvent(
                    module="lifespan",
                    error_type="REDIS_INIT_FAILED",
                    message=str(e)[:200],
                    severity="CRITICAL" if settings.env == "production" else "WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"env": settings.env},
                )
            )
            if settings.env == "production":
                logger.critical(
                    "🔥 PRODUCTION REDIS UNAVAILABLE — running in degraded mode. Redis-dependent features will fallback to memory or fail."
                )

    @with_error_bus("_init_cost_guard")
    async def _init_cost_guard() -> None:
        """Initialize CostGuard for distributed budget tracking."""
        try:
            from core.cost_guard import cost_guard

            await cost_guard.connect()
            logger.info("✅ CostGuard Redis connection initialized for budget tracking.")
        except Exception as e:
            logger.warning(f"CostGuard initialization failed (non-critical): {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="lifespan",
                    error_type="COST_GUARD_INIT_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"component": "cost_guard"},
                )
            )

    # Run all independent initializations in parallel
    init_results = await asyncio.gather(
        _init_tracing(),
        _init_db_pool(),
        _init_config_cache(),
        _init_redis(),
        _init_cost_guard(),
        return_exceptions=True,
    )

    for idx, result in enumerate(init_results):
        if isinstance(result, BaseException):
            logger.error(f"Startup initialization failed for component {idx}: {result}")

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

    # Start SupremeAI Immune System zero-cost background probing
    maintenance_pipeline.start_monitoring()

    # ── Start background agents via centralized Supervisor ────────────────────
    from core.cache.multi_layer_cache import start_swarm_cache_invalidator
    from core.sentinel_agent import sentinel

    # Agent 1: Sentinel Agent (periodic endpoint monitoring & dependency audit)
    await agent_supervisor.start_agent(
        "sentinel",
        lambda: sentinel.run_periodic_loop(),
        health_check_interval=60,
        max_restarts=10,
        restart_delay=1.0,
    )

    # Agent 2: Swarm Cache Invalidator (multi-layer cache maintenance)
    await agent_supervisor.start_agent(
        "swarm-cache",
        start_swarm_cache_invalidator,
        health_check_interval=60,
        max_restarts=5,
        restart_delay=5.0,
    )

    try:
        from core.telemetry.system_telemetry import run_system_telemetry_loop

        await agent_supervisor.start_agent(
            "system-telemetry",
            run_system_telemetry_loop,
            health_check_interval=60,
            max_restarts=5,
            restart_delay=2.0,
        )
        logger.info("✅ System Telemetry Broadcaster background loop started.")
    except Exception as exc:
        logger.warning(f"⚠️ System Telemetry Broadcaster failed to start: {exc}")

    # Agent 4: Bug Prophet Anomaly Detector
    try:
        from scripts.devops.bug_prophet import run_anomaly_detector_loop

        await agent_supervisor.start_agent(
            "bug-prophet-anomaly-detector",
            run_anomaly_detector_loop,
            health_check_interval=60,
            max_restarts=5,
            restart_delay=5.0,
        )
        logger.info("✅ BugProphet Anomaly Detector started.")
    except Exception as exc:
        logger.warning(f"⚠️ BugProphet Anomaly Detector failed to start: {exc}")

    import os

    # Start Tier-8 Meta-Self Agents
    try:
        if os.getenv("ENABLE_TIER8", "false").lower() == "true":
            from core.tier8.tier8_integration import init_tier8

            await init_tier8(services.registry)
            logger.info("✅ Tier-8 Meta-Self subsystem initialized successfully.")
        else:
            logger.info("ℹ️ Tier-8 Meta-Self subsystem disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ Tier-8 initialization failed: {exc}")

    # বাংলা মন্তব্ব্য: SelfEvolutionAgent শুরু করা — এখন AgentSupervisor-এর অধীনে চলবে।
    try:
        if os.getenv("ENABLE_EVOLUTION", "false").lower() == "true":
            from core.evolution.self_evolution_agent import SelfEvolutionAgent

            _evo_agent = SelfEvolutionAgent(interval_seconds=300)
            await _evo_agent.start()
            app.state.evo_agent = _evo_agent
            logger.info("✅ SelfEvolutionAgent background loop started (5-min evolution cycle).")
        else:
            app.state.evo_agent = None
            logger.info("ℹ️ SelfEvolutionAgent disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ SelfEvolutionAgent failed to start: {exc}")
        app.state.evo_agent = None

    # বাংলা মন্তব্ব্য: DailyLearner শুরু করা — এখন AgentSupervisor-এর অধীনে চলবে।
    try:
        if os.getenv("ENABLE_DAILY_LEARNER", "false").lower() == "true":
            from core.evolution.daily_learner import DailyLearner

            _daily_learner = DailyLearner()

            @with_error_bus("_daily_learner_loop")
            async def _daily_learner_loop() -> None:
                while True:
                    try:
                        await _daily_learner.learn_and_plan(
                            "Improve SupremeAI agent reasoning, error recovery, and free-tier efficiency"
                        )
                    except Exception as _exc:
                        logger.warning(f"⚠️ DailyLearner cycle failed: {_exc}")
                    await asyncio.sleep(86400)

            await agent_supervisor.start_agent(
                "daily-learner",
                lambda: _daily_learner_loop(),
                health_check_interval=3600,  # Check hourly (runs every 24h)
                max_restarts=5,
                restart_delay=60.0,
            )
            logger.info("✅ DailyLearner background task started (24h research scan cycle).")
        else:
            logger.info("ℹ️ DailyLearner disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ DailyLearner failed to start: {exc}")

    # বাংলা মন্তব্ব্য: AutoHealerService শুরু করা — DB/Redis স্বয়ংক্রিয়ভাবে ঠিক করে।
    try:
        if os.getenv("ENABLE_AUTO_HEALER", "true").lower() == "true":
            from core.auto_healer_service import auto_healer_service

            await auto_healer_service.start()
            app.state.auto_healer = auto_healer_service
            logger.info("✅ AutoHealerService started (DB/Redis healing active, 30s check interval).")
        else:
            logger.info("ℹ️ AutoHealerService disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ AutoHealerService failed to start: {exc}")

    # বাংলা মন্তব্ব্য: SelfHealer error listener এক্সপ্লিসিটলি রেজিস্টার করা হচ্ছে।
    try:
        from core.health.self_healer import register_self_healer_listener

        register_self_healer_listener()
        logger.info("✅ SelfHealer error listener registered in lifespan.")
    except Exception as exc:
        logger.warning(f"⚠️ SelfHealer listener registration failed: {exc}")

    # Start the agent health monitor
    await agent_supervisor.start_monitor(check_interval=30)

    yield  # এখানে অ্যাপ্লিকেশন ট্রাফিক রিসিভ করবে

    logger.critical("🚨 Graceful Shutdown Sequence triggered via Cloud Run Orchestrator.")

    # Shutdown Tier-8 Meta-Self Agents
    try:
        from core.tier8.tier8_integration import shutdown_tier8

        await shutdown_tier8()
        logger.info("✅ Tier-8 Meta-Self subsystem shutdown completed.")
    except Exception as exc:
        logger.warning(f"⚠️ Tier-8 shutdown failed: {exc}")

    # Orchestrator cleanup
    try:
        orch = getattr(app.state, "orchestrator", None)
        if orch and hasattr(orch, "stop"):
            await orch.stop()
    except Exception as e:
        logger.error(f"Error during orchestrator shutdown: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_ORCHESTRATOR_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"phase": "shutdown"},
            )
        )

    # Shutdown all background agents via centralized supervisor
    # বাংলা মন্তব্ব্য: AgentSupervisor graceful shutdown — পূর্বের ম্যানুয়াল task management প্রতিস্থাপন করে।
    try:
        await agent_supervisor.shutdown_all(timeout=30)
        logger.info("✅ All background agents shut down via centralized supervisor.")
    except Exception as e:
        logger.error(f"Error during agent supervisor shutdown: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_AGENT_SUPERVISOR_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"phase": "shutdown"},
            )
        )

    # Flush write-behind batchers (audit_logger, checkpoint_manager) so any
    # buffered-but-not-yet-flushed rows land before we tear down connections.
    # This is the graceful-shutdown half of the write-behind tradeoff: a hard
    # crash still loses at most one flush_interval window, but a normal
    # deploy/restart (this path) loses nothing.
    try:
        await asyncio.to_thread(flush_write_behind_batchers)
        logger.info("✅ Write-behind persistence batchers flushed successfully.")
    except Exception as exc:
        logger.error(f"Error flushing write-behind batchers: {exc}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="WRITE_BEHIND_FLUSH_FAILED",
                message=str(exc)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"phase": "shutdown"},
            )
        )

    # Database pool cleanup — single close with proper state tracking
    try:
        pool = await get_db_pool()
        if pool:
            await pool.close()
            logger.info("✅ Database connection pool closed successfully.")
    except Exception as e:
        logger.error(f"Error closing DB pool: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_DB_POOL_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"phase": "shutdown"},
            )
        )
    # Synchronous pgbouncer pool — only close if it was initialized
    try:
        if hasattr(pooled_pg, "_pool") and pooled_pg._pool is not None:
            await asyncio.to_thread(pooled_pg.close_pool)
            logger.info("✅ Synchronous pgbouncer pool closed successfully.")
    except Exception as e:
        logger.error(f"Error closing sync pgbouncer pool: {e}")

    # Redis cleanup
    try:
        await redis_manager.close()
        logger.info("✅ Redis Manager connection closed.")
    except Exception as e:
        logger.error(f"Error closing Redis Manager: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_REDIS_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"phase": "shutdown"},
            )
        )

    # HTTP client cleanup
    try:
        if services.global_http_client:
            await services.global_http_client.aclose()
        logger.info("✅ Global HTTP connection pool closed successfully.")
    except Exception as e:
        logger.error(f"Error during HTTP connection pool drainage: {e!s}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_HTTP_CLIENT_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"phase": "shutdown"},
            )
        )

    # Browser cleanup
    try:
        from core.playwright_manager import shutdown_global_browser

        await shutdown_global_browser()
    except Exception as e:
        logger.error(f"Failed to shutdown global browser: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_BROWSER_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"phase": "shutdown"},
            )
        )

    logger.info("💀 Serverless runtime environment sequence successfully finalized.")
    # Record system shutdown
    await metrics_collector.set_gauge("system_shutdown_time", time.time())
