import sys
from unittest.mock import MagicMock, patch

if "asyncpg" not in sys.modules:
    sys.modules["asyncpg"] = MagicMock()

import pytest
from core.pgbouncer_pool import PgBouncerConnectionPool, PoolConfig


def test_pool_config_defaults():
    config = PoolConfig()
    assert config.min_size == 10
    assert config.max_size == 100
    assert config.max_queries == 50000
    assert config.max_inactive_connection_lifetime == 300.0


def test_singleton_pattern():
    pool1 = PgBouncerConnectionPool()
    pool2 = PgBouncerConnectionPool()
    assert pool1 is pool2


@pytest.mark.asyncio
async def test_initialize_no_env_vars():
    pool = PgBouncerConnectionPool.__new__(PgBouncerConnectionPool)
    pool._pool_config = PoolConfig()
    pool._dsn = ""
    pool._pgbouncer_url = ""
    pool._initialized = False
    pool._pool = None
    
    await pool.initialize()
    assert pool._initialized is False
    assert pool._pool is None


@pytest.mark.asyncio
async def test_acquire_without_initialization():
    pool = PgBouncerConnectionPool.__new__(PgBouncerConnectionPool)
    pool._initialized = False
    pool._pool = None
    with pytest.raises(RuntimeError, match="Connection pool not initialized"):
        async with pool.acquire():
            pass


@pytest.mark.asyncio
async def test_close_resets_pool():
    pool = PgBouncerConnectionPool.__new__(PgBouncerConnectionPool)
    pool._initialized = True
    mock_pool = MagicMock()
    pool._pool = mock_pool
    await pool.close()
    assert pool._pool is None
    assert pool._initialized is False
    mock_pool.close.assert_called_once()


def test_get_db_pool_returns_instance():
    with patch("core.pgbouncer_pool.PgBouncerConnectionPool.initialize"):
        from core.pgbouncer_pool import get_db_pool
        pool = get_db_pool()
        assert isinstance(pool, PgBouncerConnectionPool)
