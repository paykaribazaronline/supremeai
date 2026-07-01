import asyncio
import os
from contextlib import asynccontextmanager

import httpx
from loguru import logger

from core import services
from core.config import settings
from core.discord_bot import SupremeDiscordBot
from core.orchestrator import Orchestrator
from core.pgbouncer_pool import get_db_pool
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
    await pool.execute(
        "CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys(key_hash)"
    )
    await pool.execute(
        "CREATE INDEX IF NOT EXISTS idx_api_key_usage_key ON api_key_usage(api_key_id, created_at DESC)"
    )
    logger.info("✅ API key tables ensured")


@asynccontextmanager
async def app_lifespan(app):
    """
    SupremeAI 2.0 Core Lifespan Manager.
    Handles high-concurrency initialization and defensive teardowns.
    """
    logger.info("🌐 Core Infrastructure Bootstrapping Active...")

    services.global_http_client = httpx.AsyncClient(
        limits=httpx.Limits(max_keepalive_connections=50, max_connections=200),
        timeout=httpx.Timeout(30.0),
        headers={"User-Agent": "SupremeAI-Orchestrator/2.0"},
    )
    app.state.http_client = services.global_http_client
    services.model_router._http_client = services.global_http_client
    logger.info("✅ Global HTTP Connection Pool initialized [Max Cons: 200].")

    try:
        await get_db_pool()
        logger.info("PgBouncer connection pool initialized on startup")
        await _ensure_api_key_tables()
    except Exception as e:
        logger.warning(f"PgBouncer pool initialization deferred: {e}")

    try:
        await redis_manager.initialize()
    except Exception as e:
        logger.error(f"Failed to initialize Redis Manager: {e}")

    try:
        if settings.discord_bot_token and settings.discord_bot_token != "mock_token":
            bot = SupremeDiscordBot()
            app.state.discord_bot_task = asyncio.create_task(
                bot.start(settings.discord_bot_token)
            )
            app.state.discord_bot = bot
            logger.info("🤖 Discord Bot background task initialized successfully.")
    except Exception as e:
        logger.warning(f"Deferred Discord Bot initialization: {e}")

    try:
        orch_inst = Orchestrator()
        app.state.orchestrator = orch_inst
        await orch_inst.start()
        logger.info("⚙️ Orchestrator background tasks initialized successfully.")
    except Exception as e:
        logger.error(f"Failed to initialize Orchestrator: {e}")

    try:
        from database import db as supabase_db

        if os.environ.get("SUPABASE_DATABASE_URL") or os.environ.get(
            "SUPABASE_DATABASE_URL_POOLER"
        ):
            supabase_db.bootstrap_schema()
            logger.info("Supabase schema bootstrap complete")
    except Exception as exc:
        logger.warning(
            f"Supabase bootstrap failed on startup: {exc}. Continuing without schema bootstrap."
        )

    yield  # এখানে অ্যাপ্লিকেশন ট্রাফিক রিসিভ করবে

    logger.critical(
        "🚨 Graceful Shutdown Sequence triggered via Cloud Run Orchestrator."
    )

    try:
        bot = getattr(app.state, "discord_bot", None)
        if bot:
            await bot.close()
            logger.info("✅ Discord Bot connection closed successfully.")
        orchestrator = getattr(app.state, "orchestrator", None)
        if orchestrator:
            await orchestrator.stop()
    except Exception as e:
        logger.error(f"Error closing Discord Bot: {e}")

    try:
        if services.global_http_client:
            await services.global_http_client.aclose()
        logger.info("✅ Global HTTP connection pool closed successfully.")
    except Exception as e:
        logger.error(f"Error during HTTP connection pool drainage: {str(e)}")

    try:
        from tools.browser_agent import shutdown_global_browser

        await shutdown_global_browser()
    except Exception as e:
        logger.error(f"Failed to shutdown global browser: {e}")

    logger.info("💀 Serverless runtime environment sequence successfully finalized.")
