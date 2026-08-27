import asyncio

from loguru import logger

from core.cache.redis_manager import redis_manager
from core.config import settings
from core.config_cache import config_cache
from core.maintenance_pipeline import maintenance_pipeline
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from core.pgbouncer_pool import PgBouncerConnectionPool, get_db_pool, init_db_pool
from core.reliability_controller import ReliabilityController
from core.startup.api_key_tables import ensure_api_key_tables as _ensure_api_key_tables


async def initialize_independent_services(app):
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
                # Helper function to initialize and health check a specific DB pool
                async def _try_connect_and_check(db_url: str) -> PgBouncerConnectionPool | None:
                    await init_db_pool(db_url)
                    pool = await get_db_pool()
                    if pool:
                        try:
                            conn = await pool.acquire()
                            try:
                                await conn.fetchval("SELECT 1")
                            finally:
                                await pool.release(conn)
                        except Exception as health_exc:
                            raise health_exc
                    return pool

                # 1. Attempt Primary DB (Supabase)
                try:
                    pool = await _try_connect_and_check(_db_url)
                    logger.info("✅ Database connection pool health check passed. Connected to Primary DB (Supabase).")
                except Exception as primary_exc:
                    logger.error(f"❌ Primary DB (Supabase) failed: {primary_exc}. Attempting fallback...")
                    # 2. Attempt Secondary DB (Neon)
                    _neon_url = getattr(settings, "neon_database_url", None)
                    if not _neon_url:
                        raise Exception(f"Primary DB failed and no neon_database_url found. Error: {primary_exc}")

                    try:
                        pool = await _try_connect_and_check(_neon_url)
                        logger.warning("⚠️ Primary DB failed! Fallback to Secondary DB (Neon.tech) successful!")
                        app.state.subsystem_status["db"] = "degraded" # Mark as degraded since we are on fallback
                    except Exception as secondary_exc:
                        raise Exception(f"Both Primary and Secondary DBs failed. Primary: {primary_exc}, Secondary: {secondary_exc}")

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
    # Start SupremeAI Immune System zero-cost background probing
    maintenance_pipeline.start_monitoring()

