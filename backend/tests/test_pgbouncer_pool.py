import sys
from unittest.mock import AsyncMock, MagicMock, patch
import pytest

if "asyncpg" not in sys.modules:
    mock_asyncpg = MagicMock()
    mock_pool_mod = MagicMock()
    mock_connection = MagicMock()
    sys.modules["asyncpg"] = mock_asyncpg
    sys.modules["asyncpg.pool"] = mock_pool_mod
    sys.modules["asyncpg.connection"] = mock_connection

from core.pgbouncer_pool import PgBouncerConnectionPool

def test_singleton_pattern():
    from core.pgbouncer_pool import get_db_pool
    pool1 = get_db_pool()
    pool2 = get_db_pool()
    assert pool1 is pool2

@pytest.mark.asyncio
async def test_connect():
    pool = PgBouncerConnectionPool("test_dsn")
    with patch("core.pgbouncer_pool.asyncpg.create_pool", new_callable=AsyncMock) as mock_create_pool:
        mock_pool = MagicMock()
        mock_create_pool.return_value = mock_pool
        await pool.connect()
        mock_create_pool.assert_called_once_with(dsn="test_dsn", min_size=1, max_size=10)
        assert pool._pool is mock_pool

@pytest.mark.asyncio
async def test_acquire_without_initialization():
    pool = PgBouncerConnectionPool("test_dsn")
    with pytest.raises(RuntimeError, match="Connection pool not initialized"):
        await pool.acquire()

@pytest.mark.asyncio
async def test_acquire_and_release():
    pool = PgBouncerConnectionPool("test_dsn")
    mock_pool = MagicMock()
    mock_pool.acquire = AsyncMock(return_value="mock_connection")
    mock_pool.release = AsyncMock()
    pool._pool = mock_pool

    conn = await pool.acquire()
    assert conn == "mock_connection"
    mock_pool.acquire.assert_called_once()

    await pool.release(conn)
    mock_pool.release.assert_called_once_with("mock_connection")

@pytest.mark.asyncio
async def test_close_resets_pool():
    pool = PgBouncerConnectionPool("test_dsn")
    mock_pool = MagicMock()
    mock_pool.close = AsyncMock()
    pool._pool = mock_pool

    await pool.close()
    assert pool._pool is None
    mock_pool.close.assert_called_once()
