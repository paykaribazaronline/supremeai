import os
from typing import Optional, Dict, Any
from contextlib import asynccontextmanager
from loguru import logger
import asyncpg
from dataclasses import dataclass

@dataclass
class PoolConfig:
    min_size: int = 10
    max_size: int = 100
    max_queries: int = 50000
    max_inactive_connection_lifetime: float = 300.0

class PgBouncerConnectionPool:
    _instance: Optional['PgBouncerConnectionPool'] = None
    _pool: Optional[asyncpg.Pool] = None
    
    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self, pool_config: Optional[PoolConfig] = None):
        if getattr(self, "_initialized", False):
            return
        self._pool_config = pool_config or PoolConfig()
        self._dsn = os.getenv("DATABASE_URL", "") or os.getenv("SUPABASE_DATABASE_URL", "")
        self._pgbouncer_url = os.getenv("PGBOUNCER_URL", "") or os.getenv("DATABASE_URL", "")
        self._initialized = False
    
    async def initialize(self) -> None:
        if self._initialized:
            return
        try:
            if self._pgbouncer_url:
                self._pool = await asyncpg.create_pool(
                    self._pgbouncer_url,
                    min_size=self._pool_config.min_size,
                    max_size=self._pool_config.max_size,
                    max_queries=self._pool_config.max_queries,
                    max_inactive_connection_lifetime=self._pool_config.max_inactive_connection_lifetime,
                    command_timeout=60.0,
                )
                self._initialized = True
            elif self._dsn:
                self._pool = await asyncpg.create_pool(
                    self._dsn,
                    min_size=self._pool_config.min_size,
                    max_size=self._pool_config.max_size,
                    max_queries=self._pool_config.max_queries,
                    max_inactive_connection_lifetime=self._pool_config.max_inactive_connection_lifetime,
                    command_timeout=60.0,
                )
                self._initialized = True
            else:
                logger.warning("No database URL configured. Connection pool initialization skipped.")
                self._initialized = False
            if self._initialized:
                logger.info(f"PgBouncer connection pool initialized (min={self._pool_config.min_size}, max={self._pool_config.max_size})")
        except Exception as e:
            logger.warning(f"Failed to initialize PgBouncer pool, falling back to direct connections: {e}")
            self._initialized = False
    
    @asynccontextmanager
    async def acquire(self) -> asyncpg.Connection:
        if not self._pool:
            raise RuntimeError("Connection pool not initialized. Call initialize() first.")
        conn = None
        try:
            conn = await self._pool.acquire()
            yield conn
        finally:
            if conn:
                await self._pool.release(conn)
    
    async def execute(self, query: str, *args) -> str:
        async with self.acquire() as conn:
            return await conn.execute(query, *args)
    
    async def fetch(self, query: str, *args) -> list:
        async with self.acquire() as conn:
            return await conn.fetch(query, *args)
    
    async def fetchrow(self, query: str, *args) -> Optional[Dict[str, Any]]:
        async with self.acquire() as conn:
            row = await conn.fetchrow(query, *args)
            return dict(row) if row else None
    
    async def close(self) -> None:
        if self._pool:
            await self._pool.close()
            self._pool = None
            self._initialized = False

async def get_db_pool() -> PgBouncerConnectionPool:
    pool = PgBouncerConnectionPool()
    if not pool._initialized:
        await pool.initialize()
    return pool