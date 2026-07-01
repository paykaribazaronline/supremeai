# FILE_PATH: backend/core/pgbouncer_pool.py

import asyncpg
import logging
from .config import PoolConfig

logger = logging.getLogger(__name__)

# Original problematic line was: from asyncpg import Connection
# The 'Connection' type is reliably found within the 'asyncpg.connection' submodule.
from asyncpg.connection import Connection # Corrected import

class PgBouncerConnectionPool:
    def __init__(self, dsn: str):
        self._dsn = dsn
        self._pool = None

    async def connect(self):
        """Initializes the asyncpg connection pool."""
        self._pool = await asyncpg.create_pool(dsn=self._dsn, min_size=1, max_size=10)
        logger.info("PgBouncer connection pool initialized.")

    async def acquire(self) -> Connection:
        """Acquires a connection from the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized. Call connect() first.")
        return await self._pool.acquire()

    async def release(self, conn: Connection):
        """Releases a connection back to the pool."""
        if self._pool:
            await self._pool.release(conn)

    async def close(self):
        """Closes the connection pool."""
        if self._pool:
            await self._pool.close()
            logger.info("PgBouncer connection pool closed.")
            self._pool = None

_db_pool_instance = None

def get_db_pool() -> PgBouncerConnectionPool:
    """Provides a singleton instance of the PgBouncerConnectionPool."""
    global _db_pool_instance
    if _db_pool_instance is None:
        # In a production environment, DSN should be loaded securely from
        # environment variables or a configuration service.
        # This is a placeholder for demonstration.
        dsn = "postgresql://user:password@localhost:5432/dbname" # Placeholder DSN
        _db_pool_instance = PgBouncerConnectionPool(dsn)
        # In a real async application, `_db_pool_instance.connect()` should be awaited
        # during application startup, not implicitly here on first access.
        logger.warning("DB pool accessed via get_db_pool without explicit async connect. Ensure proper initialization in main app lifecycle.")
    return _db_pool_instance