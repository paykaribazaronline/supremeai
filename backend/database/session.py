from __future__ import annotations

from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager
from typing import Any

from loguru import logger
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.pool import StaticPool

from core.config import settings


# বাংলা মন্তব্য: কানেকশন স্ট্রিংয়ে postgresql:// বা postgres:// থাকলে তা asyncpg-এর জন্য postgresql+asyncpg:// দিয়ে প্রতিস্থাপন করা হচ্ছে
def get_async_url(url: str) -> str:
    if not url or not isinstance(url, str):
        return "sqlite+aiosqlite:///:memory:"
    if url.startswith("postgresql://"):
        return url.replace("postgresql://", "postgresql+asyncpg://", 1)
    if url.startswith("postgres://"):
        return url.replace("postgres://", "postgresql+asyncpg://", 1)
    if url.startswith(("sqlite://", "sqlite+aiosqlite://", "postgresql+asyncpg://")):
        return url
    return "sqlite+aiosqlite:///:memory:"


# ── Lazy Engine Initialization ──────────────────────────────────────────────
# বাংলা মন্তব্য: engine ও AsyncSessionLocal এখন lazily initialize হয় — module import-এ নয়।
# এতে playwright-এর config.webServer বা অ্যাসিনক্রোনাস বুট সিকোয়েন্সে
# ডাটাবেস কানেকশন ছাড়াই সার্ভার বুট হতে পারে।
# প্রথমবার engine/AsyncSessionLocal অ্যাক্সেস করলেই কেবল create_async_engine() কল হবে।

_engine_instance: AsyncEngine | None = None
_session_maker_instance: async_sessionmaker[AsyncSession] | None = None


def _build_engine_kwargs(async_url: str) -> dict[str, Any]:
    """বাংলা: async_url-এর ধরণ (sqlite/postgresql) অনুসারে engine kwargs তৈরি করে।"""
    engine_kwargs: dict[str, Any] = {"echo": False}
    if async_url.startswith("sqlite"):
        engine_kwargs["poolclass"] = StaticPool
        engine_kwargs["connect_args"] = {"check_same_thread": False}
    elif async_url.startswith("postgresql"):
        _role = settings.service_role.lower()
        if _role == "admin":
            _pool_size, _max_overflow = 1, 2
        else:
            _pool_size, _max_overflow = 2, 13

        # বাংলা মন্তব্য: asyncpg এর জন্য prepared statement সংক্রান্ত সেটিংস connect_args-এর ভেতরে থাকতে হবে
        engine_kwargs.update(
            {
                "pool_size": _pool_size,
                "max_overflow": _max_overflow,
                "pool_timeout": 30,
                "pool_recycle": 1800,
                "pool_pre_ping": True,
                "connect_args": {
                    "command_timeout": 30,
                    "server_settings": {"application_name": f"supremeai_2_0_{_role}"},
                    "statement_cache_size": 0,
                    "prepared_statement_cache_size": 0,
                    "max_cached_statement_lifetime": 0,
                },
            }
        )
        logger.info(
            f"🔌 DB pool configured for SERVICE_ROLE='{_role}': pool_size={_pool_size}, max_overflow={_max_overflow}"
        )
    return engine_kwargs


import os
import time
from sqlalchemy import event

# বাংলা মন্তব্য: স্লো কুয়েরি ডিটেকশনের থ্রেশহোল্ড (ডিফল্ট: 0.2 সেকেন্ড / 200ms)
SLOW_QUERY_THRESHOLD_SECONDS = float(os.getenv("DB_SLOW_QUERY_THRESHOLD", "0.2"))


def _attach_query_listeners(async_engine: AsyncEngine) -> None:
    """বাংলা: স্লো কুয়েরি মনিটরিং এবং কোডবেস প্রোফাইলিংয়ের জন্য ইভেন্ট লিসেনার।"""
    sync_engine = async_engine.sync_engine

    @event.listens_for(sync_engine, "before_cursor_execute")
    def before_cursor_execute(conn, cursor, statement, parameters, context, executemany):
        conn.info.setdefault("_query_start_times", []).append(time.monotonic())

    @event.listens_for(sync_engine, "after_cursor_execute")
    def after_cursor_execute(conn, cursor, statement, parameters, context, executemany):
        start_times = conn.info.get("_query_start_times")
        if start_times:
            duration = time.monotonic() - start_times.pop()
            if duration > SLOW_QUERY_THRESHOLD_SECONDS:
                clean_stmt = " ".join(statement.split())[:300]
                logger.warning(
                    f"🐢 [SLOW DB QUERY] {duration:.3f}s (> {SLOW_QUERY_THRESHOLD_SECONDS}s) | Query: {clean_stmt}"
                )


def init_engine() -> None:
    """বাংলা: engine ও AsyncSessionLocal একবার lazily initialize করে।

    import-এর সময় নয় — প্রথমবার engine/AsyncSessionLocal অ্যাক্সেসের সময় কল হয়।
    Safe to call multiple times — second call is a no-op.
    """
    global _engine_instance, _session_maker_instance
    if _engine_instance is not None:
        return

    DATABASE_URL = settings.supabase_database_url
    if not DATABASE_URL:
        # বাংলা: production-এ missing DB URL = fail-fast। test/CI-এ SQLite fallback।
        current_env = (getattr(settings, "env", "") or "").lower()
        if current_env in ("production", "prod"):
            logger.critical(
                "FATAL: SUPABASE_DATABASE_URL_POOLER missing in PRODUCTION. "
                "Refusing to boot with SQLite fallback (data loss risk)."
            )
            raise RuntimeError("Production environment requires SUPABASE_DATABASE_URL_POOLER")
        logger.warning("SUPABASE_DATABASE_URL_POOLER is missing. Falling back to SQLite in-memory (test/dev only).")

    _async_url = get_async_url(DATABASE_URL or "")
    engine_kwargs = _build_engine_kwargs(_async_url)

    try:
        _engine_instance = create_async_engine(_async_url, **engine_kwargs)
        _attach_query_listeners(_engine_instance)
        _session_maker_instance = async_sessionmaker(
            bind=_engine_instance,
            class_=AsyncSession,
            expire_on_commit=False,
            autoflush=False,
        )
    except Exception as exc:
        # বাংলা: production-ে ফেইল-ফাস্ট, test/staging-এ SQLite fallback।
        current_env = (getattr(settings, "env", "") or "").lower()
        if current_env in ("production", "prod"):
            logger.critical(f"FATAL: Failed to create DB engine in PRODUCTION: {exc}")
            raise RuntimeError(f"Production DB engine creation failed: {exc}") from exc
        logger.error(f"Failed to create DB engine for '{_async_url}': {exc}. Falling back to SQLite in-memory.")
        fallback_url = "sqlite+aiosqlite:///:memory:"
        _engine_instance = create_async_engine(
            fallback_url,
            echo=False,
            poolclass=StaticPool,
            connect_args={"check_same_thread": False},
        )
        _attach_query_listeners(_engine_instance)
        _session_maker_instance = async_sessionmaker(
            bind=_engine_instance,
            class_=AsyncSession,
            expire_on_commit=False,
            autoflush=False,
        )


# ── Internal accessor (resolves lazy init for intra-module use) ──────────────
def _get_session_maker() -> async_sessionmaker[AsyncSession]:
    """বাংলা: get_db_session_context()-এর ভিতরে AsyncSessionLocal-এর জন্য internal accessor।"""
    init_engine()
    # _session_maker_instance guaranteed non-None after init_engine()
    return _session_maker_instance  # type: ignore[return-value]


# ── Module-level __getattr__ for lazy backward-compatible access ─────────────
# বাংলা মন্তব্য: hundreds of files use `from database.session import engine, AsyncSessionLocal`.
# __getattr__ ensures those imports still work — engine is initialized on first real access.
def __getattr__(name: str):
    if name == "engine":
        init_engine()
        return _engine_instance
    if name == "AsyncSessionLocal":
        return _get_session_maker()
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


def __dir__() -> list[str]:
    """বাংলা: dir()-এ engine ও AsyncSessionLocal দেখানোর জন্য।"""
    return [*list(globals().keys()), "engine", "AsyncSessionLocal"]


@asynccontextmanager
async def get_db_session_context() -> AsyncGenerator[AsyncSession, None]:
    """Context manager for backend tasks or non-FastAPI usages.

    বাংলা: FastAPI-এর বাইরে বা ব্যাকগ্রাউন্ড টাস্কে ডাটাবেস সেশন ব্যবহারের জন্য।
    """
    from fastapi import HTTPException
    from sqlalchemy.exc import TimeoutError as SATimeoutError

    session_maker = _get_session_maker()
    try:
        async with session_maker() as session:
            try:
                yield session
            except Exception as e:
                await session.rollback()
                logger.error(f"Database transaction rolled back due to error: {e}")
                raise
    except (TimeoutError, SATimeoutError) as e:
        logger.error(f"Database pool exhausted: {e}")
        raise HTTPException(
            status_code=503,
            detail="Service temporarily unavailable due to high load (DB pool exhausted).",
        ) from e


# FastAPI Dependency Injection (with safe rollback)
async def get_db_session() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI Dependency for database sessions.

    বাংলা: FastAPI রুটগুলোর জন্য ডাটাবেস ডিপেন্ডেন্সি।
    """
    async with get_db_session_context() as session:
        yield session


async def check_engine_health() -> bool:
    """Check if database engine is reachable and responsive.

    বাংলা: ডাটাবেস ইঞ্জিন সচল ও সংযোগযোগ্য কিনা যাচাই করে।
    """
    from sqlalchemy import text

    try:
        engine = _get_engine()
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        return True
    except Exception as e:
        logger.warning(f"Database health check probe failed: {e}")
        return False


async def dispose_engine() -> None:
    """Dispose all engine pools cleanly. Call during graceful shutdown.

    বাংলা: শাটডাউনের সময় সমস্ত ডাটাবেস কানেকশন পুল ক্লোজ করে।
    """
    global _engine_instance, _session_maker_instance
    if _engine_instance is not None:
        logger.info("Disposing database engine pools...")
        await _engine_instance.dispose()
        _engine_instance = None
        _session_maker_instance = None

