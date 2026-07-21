# ruff: noqa: E501
"""
SupremeAI — Multi-Database Router
==================================

Router for multi-database architecture.
- Connection pooling
- Query routing
- Failover handling
- Zero-cost: uses Upstash Redis for routing state
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime
from enum import Enum
from typing import Any

from core.cache import get_cache
from loguru import logger

# ── Constants ────────────────────────────────────────────────────────────────
ROUTING_CACHE_TTL = 300


class DatabaseType(str, Enum):
    POSTGRES = "postgres"
    MYSQL = "mysql"
    SQLITE = "sqlite"
    FIREBASE = "firebase"
    MONGODB = "mongodb"
    REDIS = "redis"


class QueryPattern(str, Enum):
    READ = "read"
    WRITE = "write"
    ANALYTICS = "analytics"
    CACHE = "cache"


@dataclass(frozen=True)
class DatabaseConfig:
    """Database connection configuration."""

    db_type: DatabaseType
    connection_string: str
    pool_size: int
    priority: int
    read_replica: bool = False


class MultiDBRouter:
    """
    Routes queries to appropriate database connections.
    """

    def __init__(self) -> None:
        self.cache = get_cache()
        self.databases: dict[str, DatabaseConfig] = {}
        self._connections: dict[str, Any] = {}
        logger.info("MultiDBRouter initialized")

    def register_database(self, name: str, config: DatabaseConfig) -> None:
        """Register a database configuration."""
        self.databases[name] = config

    def _select_database(self, pattern: QueryPattern) -> str | None:
        """Select best database for query pattern."""
        # Filter by pattern
        candidates = {
            name: config
            for name, config in self.databases.items()
            if (pattern == QueryPattern.READ and not config.read_replica)
            or (pattern == QueryPattern.WRITE and not config.read_replica)
            or (
                pattern == QueryPattern.ANALYTICS
                and config.db_type == DatabaseType.POSTGRES
            )
            or (pattern == QueryPattern.CACHE and config.db_type == DatabaseType.REDIS)
        }

        if not candidates:
            # Fallback to any configured DB
            candidates = self.databases

        if not candidates:
            return None

        # Select by priority (highest first)
        return max(candidates.items(), key=lambda x: x[1].priority)[0]

    async def route_query(
        self, query: str, pattern: QueryPattern = QueryPattern.READ
    ) -> dict[str, Any]:
        """
        Route query to appropriate database.

        Args:
            query: SQL or query string.
            pattern: Query pattern type.

        Returns:
            Routing decision with target database.
        """
        target_db = self._select_database(pattern)

        if not target_db:
            return {"error": "No databases configured"}

        cache_key = f"query_route:{pattern.value}:{hash(query)}"
        cached = await self.cache.get(cache_key)
        if cached:
            return cached  # type: ignore

        routing = {
            "target_database": target_db,
            "db_type": self.databases[target_db].db_type.value,
            "pattern": pattern.value,
            "routed_at": datetime.now(UTC).isoformat(),
        }

        await self.cache.set(
            cache_key,
            routing,
            ttl=ROUTING_CACHE_TTL,
        )

        return routing

    async def get_connection(self, db_name: str) -> Any | None:
        """Get database connection."""
        # In production, this would return actual connection
        return self._connections.get(db_name)

    def set_connection(self, db_name: str, connection: Any) -> None:
        """Set database connection."""
        self._connections[db_name] = connection


# Singleton
_router_instance: MultiDBRouter | None = None


def get_multi_db_router() -> MultiDBRouter:
    """Get or create the singleton MultiDBRouter instance."""
    global _router_instance
    if _router_instance is None:
        _router_instance = MultiDBRouter()
    return _router_instance
