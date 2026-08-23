"""
SupremeAI Database Configuration — Optimized Connection Pool
v4.0: Connection pooling, slow query logging, health checks
"""

from __future__ import annotations

import logging
import time
from contextlib import contextmanager
from functools import lru_cache
from typing import Generator

from sqlalchemy import event, text
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
SLOW_QUERY_THRESHOLD_MS = float(__import__("os").getenv("SLOW_QUERY_MS", "200"))
POOL_SIZE = int(__import__("os").getenv("DB_POOL_SIZE", "10"))
MAX_OVERFLOW = int(__import__("os").getenv("DB_MAX_OVERFLOW", "5"))
POOL_RECYCLE = int(__import__("os").getenv("DB_POOL_RECYCLE", "3600"))  # 1 hour


class Base(DeclarativeBase):
    """Base class for all SQLAlchemy models."""
    pass


def _get_database_url() -> str:
    """Get database URL from environment with validation."""
    import os
    
    url = os.getenv("DATABASE_URL", "")
    if not url:
        logger.warning("DATABASE_URL not set, using SQLite fallback")
        return "sqlite+aiosqlite:///./local.db"
    
    # Convert postgres:// to postgresql+asyncpg:// for async
    if url.startswith("postgres://"):
        url = url.replace("postgres://", "postgresql+asyncpg://", 1)
    elif url.startswith("postgresql://") and "+asyncpg" not in url:
        url = url.replace("postgresql://", "postgresql+asyncpg://", 1)
    
    return url


@lru_cache
def get_engine():
    """
    Create async database engine with optimized connection pool.
    
    Pool Settings:
      - pool_size: Number of permanent connections (default: 10)
      - max_overflow: Extra connections beyond pool_size (default: 5)
      - pool_recycle: Recycle connections after N seconds (default: 3600)
      - pool_pre_ping: Verify connections before use
    """
    engine = create_async_engine(
        _get_database_url(),
        pool_size=POOL_SIZE,
        max_overflow=MAX_OVERFLOW,
        pool_recycle=POOL_RECYCLE,
        pool_pre_ping=True,  # Detect stale connections
        echo=os.getenv("DB_ECHO", "false").lower() == "true",
    )
    
    # Register slow query listener
    @event.listens_for(engine.sync_engine, "before_cursor_execute")
    def receive_before_cursor_execute(
        conn, cursor, statement, parameters, context, executemany
    ):
        context._query_start_time = time.monotonic()
    
    @event.listens_for(engine.sync_engine, "after_cursor_execute")
    def receive_after_cursor_execute(
        conn, cursor, statement, parameters, context, executemany
    ):
        total = (time.monotonic() - context._query_start_time) * 1000
        if total > SLOW_QUERY_THRESHOLD_MS:
            stmt_preview = statement[:100] + "..." if len(statement) > 100 else statement
            logger.warning(
                f"🐌 SLOW QUERY ({total:.0f}ms > {SLOW_QUERY_THRESHOLD_MS}ms): {stmt_preview}"
            )
    
    logger.info(
        f"Database engine created (pool={POOL_SIZE}, overflow={MAX_OVERFLOW}, "
        f"slow_query_threshold={SLOW_QUERY_THRESHOLD_MS}ms)"
    )
    
    return engine


engine = get_engine()

# Session factory
async_session_factory = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
)


async def get_db() -> Generator[AsyncSession, None, None]:
    """Dependency injection for FastAPI routes."""
    async with async_session_factory() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()


@contextmanager
async def db_session() -> AsyncSession:
    """Context manager for non-FastAPI usage."""
    async with async_session_factory() as session:
        yield session


async def check_db_health() -> dict[str, bool | str]:
    """Check database connectivity and performance."""
    try:
        start = time.monotonic()
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        latency_ms = (time.monotonic() - start) * 1000
        
        return {
            "healthy": True,
            "latency_ms": round(latency_ms, 2),
            "pool_size": POOL_SIZE,
        }
    except Exception as e:
        logger.error(f"Database health check failed: {e}")
        return {
            "healthy": False,
            "error": str(e)[:200],
        }


# -----------------------------------------------------------------------------
# FILE 6: backend/models/ai_memory.py — Vector Index Optimization
# -----------------------------------------------------------------------------
