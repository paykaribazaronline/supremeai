import asyncio
import os
from contextlib import asynccontextmanager

import httpx
from loguru import logger

from core import services
from core.config import settings
from core.config_cache import config_cache
from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus
from core.maintenance_pipeline import maintenance_pipeline
from core.orchestrator import Orchestrator
from core.pgbouncer_pool import get_db_pool
from core.pgbouncer_pool import init_db_pool
from core.redis_manager import redis_manager


async def _ensure_api_key_tables() -> None:
    pool = await get_db_pool()
    await pool.execute(
        """
        CREATE TABLE IF NOT EXISTS api_keys (
            id SERIAL PRIMARY KEY,
            user_id TEXT NOT NULL,
            name TEXT NOT NULL,
            key_hash TEXT NOT NULL UNIQUE,
            key_masked TEXT NOT NULL,
            key_prefix TEXT NOT NULL,
            rate_limit_rps INTEGER DEFAULT 6,
            revoked BOOLEAN DEFAULT FALSE,
            expires_at INTEGER,
            last_used_at INTEGER,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """
    )
    await pool.execute(
        """
        CREATE TABLE IF NOT EXISTS api_key_usage (
            id SERIAL PRIMARY KEY,
            api_key_id INTEGER NOT NULL REFERENCES api_keys(id),
            endpoint TEXT NOT NULL,
            status_code INTEGER NOT NULL,
            latency_ms DOUBLE PRECISION NOT NULL DEFAULT 0,
            ip_address TEXT,
            created_at INTEGER NOT NULL
        )
        """
    )
    await pool.execute(
        """
        CREATE TABLE IF NOT EXISTS api_key_events (
            id SERIAL PRIMARY KEY,
            api_key_id INTEGER NOT NULL REFERENCES api_keys(id),
            event_type TEXT NOT NULL,
            details TEXT,
            ip_address TEXT,
            created_at INTEGER NOT NULL
        )
        """
    )
    await pool.execute("CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys(key_hash)")
    await pool.execute("CREATE INDEX IF NOT EXISTS idx_api_key_usage_key ON api_key_usage(api_key_id, created_at DESC)")
    logger.info("✅ API key tables ensured")


@asynccontextmanager
async def app_lifespan(app):
    """
    SupremeAI 2.0 Core Lifespan Manager.
    Handles high-concurrency initialization and defensive teardowns.
    """
    logger.info("🌐 Core Infrastructure Bootstrapping Active...")

    try:
        from core.telemetry import setup_tracing

        # বাংলা মন্তব্য: P2 Fix — startup latency এবং cold start freeze এড়াতে tracing initialization thread-এ offload করা হলো।
        await asyncio.to_thread(setup_tracing)
        logger.info("✅ OpenTelemetry tracing provider successfully initialized.")
    except Exception as exc:  # noqa: BLE001
        logger.warning(f"Failed to initialize tracing provider: {exc}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="TRACING_INIT_FAILED",
                message=str(exc)[:200],
                severity="WARNING",
                context={"component": "opentelemetry"},
            )
        )

    services.global_http_client = httpx.AsyncClient(
        limits=httpx.Limits(max_keepalive_connections=50, max_connections=200),
        timeout=httpx.Timeout(30.0),
        headers={"User-Agent": "SupremeAI-Orchestrator/2.0"},
    )
    app.state.http_client = services.global_http_client
    services.model_router._http_client = services.global_http_client
    logger.info("✅ Global HTTP Connection Pool initialized [Max Cons: 200].")

    try:
        db_url = settings.supabase_database_url
        if "sqlite" in db_url:
            logger.info("💾 SQLite Memory Database Detected for Agent Telemetry. Skipping PostgreSQL asyncpg pool initialization.")
            app.state.db_pool = None
        else:
            await init_db_pool(db_url)
            logger.info("⚡ PgBouncer connection pool successfully initialized at startup.")
            await _ensure_api_key_tables()
    except Exception as exc:  # noqa: BLE001
        # বাংলা মন্তব্য: P1 Fix — DB fail হলে startup crash করা হবে না।
        # DB-dependent features gracefully disabled হবে।
        # Health endpoint, SSE stream, config cache সব চলবে DB ছাড়া।
        logger.error(f"❌ Failed to initialize DB Pool: {exc}")
        app.state.db_pool = None
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="DB_POOL_INIT_FAILED",
                message=str(exc)[:200],
                severity="CRITICAL" if os.getenv("ENV") == "production" else "WARNING",
                context={"db_url": db_url[:50] if db_url else "", "env": os.getenv("ENV", "unknown")},
            )
        )
        if os.getenv("ENV") == "production":
            # Production-এ Sentry-তে alert পাঠান, কিন্তু crash করবেন না
            logger.critical("🔥 PRODUCTION DB UNAVAILABLE — running in degraded mode. DB-dependent endpoints will return 503.")

    try:
        await config_cache.refresh_async()
        logger.info("✅ System configuration cache successfully initialized.")
    except Exception as exc:  # noqa: BLE001
        # প্রোডাকশনে ডাটাবেজ সাময়িক ডাউন থাকলেও সার্ভার যেন বুট হতে পারে
        logger.warning(f"⚠️ Async config load failed, falling back to local DEFAULT_CONFIGS: {exc}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="CONFIG_CACHE_INIT_FAILED",
                message=str(exc)[:200],
                severity="WARNING",
                context={"fallback": "DEFAULT_CONFIGS"},
            )
        )
        from core.config_cache import DEFAULT_CONFIGS

        config_cache._cache = dict(DEFAULT_CONFIGS)
        # sys.exit(1) রিমুভ করা হলো যাতে ক্লাউড রান হেলথ চেক পাস করতে পারে

    try:
        pass
    except Exception as e:  # noqa: BLE001
        logger.error(f"Failed to initialize Redis Manager: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="REDIS_INIT_FAILED",
                message=str(e)[:200],
                severity="CRITICAL" if os.getenv("ENV") == "production" else "WARNING",
                context={"env": os.getenv("ENV", "unknown")},
            )
        )
        if os.getenv("ENV") == "production":
            raise e

    try:
        orch_inst = Orchestrator()
        app.state.orchestrator = orch_inst
        await orch_inst.start()
        logger.info("⚙️ Orchestrator background tasks initialized successfully.")
    except Exception as e:  # noqa: BLE001
        logger.error(f"Failed to initialize Orchestrator: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="ORCHESTRATOR_INIT_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                context={"component": "orchestrator"},
            )
        )

    try:
        from database import db as supabase_db

        if os.environ.get("SUPABASE_DATABASE_URL") or os.environ.get("SUPABASE_DATABASE_URL_POOLER"):
            supabase_db.bootstrap_schema()
            logger.info("Supabase schema bootstrap complete")
    except Exception as exc:  # noqa: BLE001
        logger.warning(f"Supabase bootstrap failed on startup: {exc}. Continuing without schema bootstrap.")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SUPABASE_BOOTSTRAP_FAILED",
                message=str(exc)[:200],
                severity="WARNING",
                context={"component": "supabase"},
            )
        )

    # Start SupremeAI Immune System zero-cost background probing
    maintenance_pipeline.start_monitoring()
    
    # Start the Sentinel Agent
    from core.sentinel_agent import sentinel
    asyncio.create_task(sentinel.run_periodic_loop())

    yield  # এখানে অ্যাপ্লিকেশন ট্রাফিক রিসিভ করবে

    logger.critical("🚨 Graceful Shutdown Sequence triggered via Cloud Run Orchestrator.")

    try:
        orchestrator = getattr(app.state, "orchestrator", None)
        if orchestrator:
            await orchestrator.stop()
    except Exception as e:  # noqa: BLE001
        logger.error(f"Error closing Orchestrator: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_ORCHESTRATOR_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                context={"phase": "shutdown"},
            )
        )

    try:
        pool = await get_db_pool()
        if pool:
            await pool.close()
            logger.info("✅ Database connection pool closed successfully.")
    except Exception as e:  # noqa: BLE001
        logger.error(f"Error closing DB pool: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_DB_POOL_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                context={"phase": "shutdown"},
            )
        )

    try:
        await redis_manager.close()
        logger.info("✅ Redis Manager connection closed.")
    except Exception as e:  # noqa: BLE001
        logger.error(f"Error closing Redis Manager: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_REDIS_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                context={"phase": "shutdown"},
            )
        )

    try:
        if services.global_http_client:
            await services.global_http_client.aclose()
        logger.info("✅ Global HTTP connection pool closed successfully.")
    except Exception as e:  # noqa: BLE001
        logger.error(f"Error during HTTP connection pool drainage: {str(e)}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_HTTP_CLIENT_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                context={"phase": "shutdown"},
            )
        )

    try:
        from tools.browser_agent import shutdown_global_browser

        await shutdown_global_browser()
    except Exception as e:  # noqa: BLE001
        logger.error(f"Failed to shutdown global browser: {e}")
        error_event_bus.emit(
            ErrorEvent(
                module="lifespan",
                error_type="SHUTDOWN_BROWSER_FAILED",
                message=str(e)[:200],
                severity="WARNING",
                context={"phase": "shutdown"},
            )
        )

    logger.info("💀 Serverless runtime environment sequence successfully finalized.")
