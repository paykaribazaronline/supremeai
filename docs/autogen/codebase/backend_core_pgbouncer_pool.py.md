# 📄 ফাইল: backend/core/pgbouncer_pool.py

**প্রকার:** .py  
**সাইজ:** 3,512 বাইট  
**আপডেট:** 2026-07-08T11:32:31.845209

---

## কোড

```py
# FILE_PATH: backend/core/pgbouncer_pool.py

import logging

import asyncpg
from asyncpg.connection import Connection  # Corrected import


logger = logging.getLogger(__name__)


class PgBouncerConnectionPool:
    def __init__(self, dsn: str):
        self._dsn = dsn
        self._pool = None

    async def connect(self):
        """Initializes the asyncpg connection pool."""
        self._pool = await asyncpg.create_pool(
            dsn=self._dsn,
            min_size=5,
            max_size=30,
            max_inactive_connection_lifetime=300,
            statement_cache_size=0,
            command_timeout=30,
        )
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

    # asyncpg.Pool এর মেথডগুলোকে সরাসরি কল করার জন্য proxy মেথডগুলো যুক্ত করা হলো
    # যাতে কোডবেসে pool.execute() বা pool.fetch() কল করলে কোনো Attribute Error না দেয়।
    async def execute(self, query: str, *args, **kwargs):
        """Executes a query using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.execute(query, *args, **kwargs)

    async def fetch(self, query: str, *args, **kwargs):
        """Fetches rows using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.fetch(query, *args, **kwargs)

    async def fetchrow(self, query: str, *args, **kwargs):
        """Fetches a single row using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.fetchrow(query, *args, **kwargs)

    async def fetchval(self, query: str, *args, **kwargs):
        """Fetches a single value using the pool."""
        if not self._pool:
            raise RuntimeError("Connection pool not initialized.")
        return await self._pool.fetchval(query, *args, **kwargs)

    async def close(self):
        """Closes the connection pool."""
        if self._pool:
            await self._pool.close()
            logger.info("PgBouncer connection pool closed.")
            self._pool = None

_db_pool_instance = None


async def get_db_pool() -> PgBouncerConnectionPool:
    """Provides a singleton instance of the PgBouncerConnectionPool.

    RuntimeError is raised if the pool has not been initialized yet.
    """
    if _db_pool_instance is None:
        raise RuntimeError(
            "DB pool was accessed before app startup initialized it. "
            "Call init_db_pool() explicitly during the FastAPI lifespan."
        )
    return _db_pool_instance


async def init_db_pool(dsn: str) -> PgBouncerConnectionPool:
    """Initializes the DB pool singleton and returns it."""
    global _db_pool_instance
    if _db_pool_instance is None:
        pool = PgBouncerConnectionPool(dsn)
        await pool.connect()
        _db_pool_instance = pool
    return _db_pool_instance

```