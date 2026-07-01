# FILE_PATH: backend/core/pgbouncer_pool.py

import asyncpg
import asyncio
import logging
import os
from contextlib import asynccontextmanager
from typing import Optional, AsyncIterator

# FIX: Explicitly import 'Connection' for type hinting.
# The 'AttributeError: module 'asyncpg' has no attribute 'Connection''
# suggests that 'asyncpg.Connection' is not directly available at the top-level
# of the asyncpg module in the current environment/version, or that an explicit
# import helps resolve the type hint correctly during collection.
from asyncpg import Connection

logger = logging.getLogger(__name__)

class PgBouncerConnectionPool:
    _instance: Optional["PgBouncerConnectionPool"] = None
    _pool: Optional[asyncpg.pool.Pool] = None
    # Using a class-level lock to ensure initialization is atomic and happens once.
    # It will be assigned to a module-level lock in get_db_pool for proper singleton management.
    _init_lock: asyncio.Lock

    def __init__(self):
        # Prevent direct instantiation; enforce singleton pattern via get_db_pool()
        raise RuntimeError("Call get_db_pool() to get the PgBouncerConnectionPool instance.")

    @classmethod
    async def initialize(cls):
        """Initializes the asyncpg connection pool."""
        if cls._instance is None:
            # Double-checked locking to ensure thread-safe, single initialization.
            async with cls._init_lock:
                if cls._instance is None:
                    logger.info("Initializing PgBouncerConnectionPool...")
                    db_dsn = os.getenv("DATABASE_URL")
                    if not db_dsn:
                        raise ValueError("DATABASE_URL environment variable not set.")

                    min_conns = int(os.getenv("PG_MIN_CONNECTIONS", "5"))
                    max_conns = int(os.getenv("PG_MAX_CONNECTIONS", "10"))
                    timeout = int(os.getenv("PG_CONNECTION_TIMEOUT", "60"))

                    try:
                        cls._pool = await asyncpg.create_pool(
                            dsn=db_dsn,
                            min_size=min_conns,
                            max_size=max_conns,
                            timeout=timeout,
                        )
                        # Only assign _instance if pool creation is successful
                        cls._instance = super().__new__(cls)
                        logger.info(f"PgBouncerConnectionPool initialized with {min_conns}-{max_conns} connections.")
                    except Exception as e:
                        logger.error(f"Failed to initialize PgBouncerConnectionPool: {e}")
                        # Re-raise to indicate a critical failure in initialization
                        raise

    @classmethod
    def get_instance(cls) -> "PgBouncerConnectionPool":
        """Returns the initialized singleton instance of the connection pool."""
        if cls._instance is None:
            raise RuntimeError("PgBouncerConnectionPool has not been initialized. Call initialize() first.")
        return cls._instance

    @asynccontextmanager
    async def acquire_connection(self) -> AsyncIterator[Connection]:
        """
        An async context manager to acquire a connection from the pool.
        The connection is automatically released upon exiting the context.
        """
        if self._pool is None:
            raise RuntimeError("Connection pool is not initialized.")
        async with self._pool.acquire() as connection:
            yield connection

    # This is the method that caused the error (line 70 in the original log).
    # The type hint `asyncpg.Connection` is changed to `Connection` after explicit import.
    async def acquire(self) -> Connection: # FIX applied here
        """Acquires a connection directly from the pool."""
        if self._pool is None:
            raise RuntimeError("Connection pool is not initialized.")
        return await self._pool.acquire()

    async def release(self, connection: Connection): # Using the imported Connection
        """Releases a connection back to the pool."""
        if self._pool is None:
            raise RuntimeError("Connection pool is not initialized.")
        await self._pool.release(connection)

    async def close(self):
        """Closes the connection pool and resets the singleton instance."""
        if self._pool:
            logger.info("Closing PgBouncerConnectionPool...")
            await self._pool.close()
            # Reset instance and pool for potential re-initialization (e.g., in tests)
            self.__class__._pool = None
            self.__class__._instance = None
            logger.info("PgBouncerConnectionPool closed.")

# Module-level instance and lock for the singleton `get_db_pool` function.
# This ensures that `get_db_pool` is itself a singleton factory.
_db_pool_instance: Optional[PgBouncerConnectionPool] = None
_db_pool_init_lock = asyncio.Lock()

async def get_db_pool() -> PgBouncerConnectionPool:
    """
    Provides the singleton instance of PgBouncerConnectionPool,
    initializing it if it hasn't been already.
    """
    global _db_pool_instance
    if _db_pool_instance is None:
        async with _db_pool_init_lock:
            if _db_pool_instance is None:
                # Assign the module-level lock to the class for its internal initialization
                PgBouncerConnectionPool._init_lock = _db_pool_init_lock
                await PgBouncerConnectionPool.initialize()
                _db_pool_instance = PgBouncerConnectionPool.get_instance()
    return _db_pool_instance

async def close_db_pool():
    """Closes the module-level singleton database pool."""
    global _db_pool_instance
    if _db_pool_instance:
        await _db_pool_instance.close()
        _db_pool_instance = None