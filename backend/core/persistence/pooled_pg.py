"""Shared, tightly-bounded synchronous Postgres connection pool.

Why this exists: `checkpoint_manager`, `error_pattern_db`, `audit_logger`, and
`memory_service` were previously each opening/closing a fresh sqlite3
connection on every single call, writing to plain files on Render's
ephemeral local disk. That meant their state was silently wiped on every
container restart or redeploy.

Rather than routing them through the app's main *async* SQLAlchemy engine
(which would require converting every caller to async — a large, risky
blast-radius change), these subsystems are synchronous by design and callers
expect that. So we give them their own small psycopg2 pool, pointed at the
*same* Supabase pooler connection string the async engine uses
(SUPABASE_DATABASE_URL_POOLER), with a deliberately tiny ceiling so they
can never crowd out the app's primary request-serving connections.

Ripple-Effect Guard: this module is additive. It does not touch
`database/session.py` or the async engine's pool sizing in any way.
"""

from __future__ import annotations

import atexit
import os
import threading
from contextlib import contextmanager
from typing import Any

from core.error_bus import with_error_bus

# psycopg2 মডিউল না থাকলে যেন সার্ভিস ক্র্যাশ না করে, সে জন্য সেফ ইমপোর্ট ফলব্যাক ব্যবহার করা হলো।
try:
    import psycopg2
    import psycopg2.pool
except ImportError:
    psycopg2 = None
from loguru import logger

from core.config import settings

# Deliberately small: these 4 subsystems are secondary telemetry/state, not
# primary request traffic. They must never meaningfully compete with the
# User (max 15) / Admin (max 3) pools already budgeted in database/session.py.
_MIN_CONN = 1
_MAX_CONN = int(os.getenv("PERSISTENCE_PG_POOL_MAX", "4"))

_pool_lock = threading.Lock()
_pool: psycopg2.pool.ThreadedConnectionPool | None = None
_pool_unavailable = False  # sticky flag once we've confirmed no PG URL is configured


def _resolve_dsn() -> str | None:
    dsn = settings.supabase_database_url
    if not dsn or dsn.startswith("sqlite"):
        return None
    return dsn


def _get_pool() -> Any:
    global _pool, _pool_unavailable
    if psycopg2 is None:
        return None
    if _pool is not None:
        return _pool
    if _pool_unavailable:
        return None
    with _pool_lock:
        if _pool is not None:
            return _pool
        dsn = _resolve_dsn()
        if not dsn:
            _pool_unavailable = True
            logger.warning(
                "persistence.pooled_pg: no Postgres DSN configured — "
                "checkpoint/audit/error-pattern/memory subsystems will run degraded (in-process only)."
            )
            return None
        try:
            _pool = psycopg2.pool.ThreadedConnectionPool(_MIN_CONN, _MAX_CONN, dsn, connect_timeout=10)
            logger.info(f"persistence.pooled_pg: initialized (max={_MAX_CONN} connections).")
        except Exception as exc:
            logger.error(f"persistence.pooled_pg: failed to initialize pool: {exc}")
            _pool_unavailable = True
            return None
    return _pool


@contextmanager
def get_conn():
    """Checkout a pooled connection. Raises if Postgres isn't configured/reachable —
    callers are expected to catch and fall back gracefully (see each subsystem's
    in-process fallback behavior)."""
    pool = _get_pool()
    if pool is None:
        raise RuntimeError("Postgres persistence pool unavailable")
    conn = pool.getconn()
    try:
        yield conn
    finally:
        pool.putconn(conn)


@with_error_bus("execute")
def execute(sql: str, params: tuple = ()) -> None:
    with get_conn() as conn:
        cur = conn.cursor()
        try:
            cur.execute(sql, params)
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            cur.close()


@with_error_bus("executemany")
def executemany(sql: str, params_list: list[tuple]) -> None:
    if not params_list:
        return
    with get_conn() as conn:
        cur = conn.cursor()
        try:
            cur.executemany(sql, params_list)
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            cur.close()


def query(sql: str, params: tuple = ()) -> list[tuple]:
    with get_conn() as conn:
        cur = conn.cursor()
        try:
            cur.execute(sql, params)
            return cur.fetchall()
        finally:
            cur.close()


def query_dicts(sql: str, params: tuple = ()) -> list[dict[str, Any]]:
    with get_conn() as conn:
        cur = conn.cursor()
        try:
            cur.execute(sql, params)
            cols = [d[0] for d in cur.description] if cur.description else []
            return [dict(zip(cols, row, strict=True)) for row in cur.fetchall()]
        finally:
            cur.close()


def is_configured() -> bool:
    # বাংলা মন্তব্য: কানেকশন পুল ইনিশিয়ালাইজ না করে শুধুমাত্র কনফিগারেশন চেক করার জন্য
    return _resolve_dsn() is not None


def is_available() -> bool:
    return _get_pool() is not None


def close_pool() -> None:
    global _pool
    with _pool_lock:
        if _pool is not None:
            try:
                _pool.closeall()
                logger.info("persistence.pooled_pg: pool closed.")
            except Exception as exc:
                logger.warning(f"persistence.pooled_pg: error closing pool: {exc}")
            _pool = None


atexit.register(close_pool)
