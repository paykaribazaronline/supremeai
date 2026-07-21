# mypy: ignore-errors
"""Manages asynchronous interactions with a Redis cache for the SupremeAI ecosystem.

This module provides the `SecureRedisManager` class, a centralized and secure interface for
general key-value caching and specialized operations, including monitoring the
health and status of AI agents. It initializes an `aioredis` client using the
`REDIS_URL` environment variable, implementing a fail-closed mechanism if the
Redis endpoint is not configured. A singleton instance, `redis_manager`, is
exposed for application-wide use."""

import json
import os
import uuid

from loguru import logger
from redis import asyncio as aioredis


class SecureRedisManager:
    def __init__(self):
        from core.config import settings

        self.url = settings.redis_url or os.getenv("REDIS_URL")
        self._client = None
        self._initialized = False

    def _ensure_connected(self):
        if self.url:
            # বাংলা মন্তব্য: Upstash Redis free tier-এর ৩০টি concurrent connection সীমার সাথে সামঞ্জস্য রেখে সর্বোচ্চ ২০টি কানেকশন পুল করা হলো।
            pool = aioredis.ConnectionPool.from_url(
                self.url,
                max_connections=20,
                socket_keepalive=True,
                socket_connect_timeout=5.0,
                decode_responses=True,
            )
            self._client = aioredis.Redis(connection_pool=pool)
            logger.info(
                "⚡ Serverless Upstash Redis REST Provider Active with Connection Pool (limit=20)."
            )
        else:
            logger.critical(
                "🔥 CRITICAL: Serverless Redis Endpoint Missing! System entering Fail-Closed state."
            )
        self._initialized = True

    @property
    def client(self):
        if not self._initialized:
            self._ensure_connected()
        return self._client

    async def set_cache(self, key: str, value: str, ex_seconds: int = 3600) -> bool:
        """Native Redis API এর মাধ্যমে কি-ভ্যালু পেয়ার সেভ করার মেথড।"""
        if not self.client:
            return False
        try:
            await self.client.set(key, value, ex=ex_seconds)
            return True
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Redis Cache Write Operation Failed for {key}: {exc}")
            return False

    async def get_cache(self, key: str) -> str | None:
        """Native Redis API এর মাধ্যমে ডাটা রিড করার মেথড।"""
        if not self.client:
            return None
        try:
            return await self.client.get(key)
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Upstash Cache Read Operation Failed for {key}: {exc}")
            return None

    async def incrbyfloat(
        self, key: str, amount: float, ex_seconds: int = 86400
    ) -> float:
        """Atomic increment for floats (used by CostGuard)."""
        if not self.client:
            return 0.0
        try:
            result = await self.client.incrbyfloat(key, amount)
            await self.client.expire(key, ex_seconds)
            return float(result)
        except Exception as exc:  # noqa: BLE001
            logger.error(
                f"❌ Redis Cache INCRBYFLOAT Operation Failed for {key}: {exc}"
            )
            return 0.0

    async def set_agent_heartbeat(
        self, agent_id: str, status: str, latency_ms: int, ttl: int = 5
    ) -> bool:
        """এজেন্ট হার্টবিট সেট করার মেথড।"""
        if not self.client:
            return False

        key = f"health:{agent_id}"
        value = json.dumps({"status": status, "latency": latency_ms})
        return await self.set_cache(key, value, ex_seconds=ttl)

    async def get_agents_health(self, agent_ids: list[str]) -> dict:
        """একাধিক এজেন্টের হেলথ স্ট্যাটাস একসাথে MGET দিয়ে ফেচ করে।"""
        if not self.client or not agent_ids:
            return {}

        keys = [f"health:{agent_id}" for agent_id in agent_ids]
        try:
            res_data = await self.client.mget(keys)
            health_data = {}
            for agent_id, raw_val in zip(agent_ids, res_data, strict=False):
                if raw_val:
                    try:
                        health_data[agent_id] = (
                            json.loads(raw_val) if isinstance(raw_val, str) else raw_val
                        )
                    except json.JSONDecodeError:
                        health_data[agent_id] = {"status": "dead", "latency": 0}
                else:
                    health_data[agent_id] = {"status": "dead", "latency": 0}
            return health_data
        except Exception as exc:  # noqa: BLE001
            logger.error(f"❌ Redis MGET Failed for health check: {exc}")
            return {}

    async def close(self):
        if self._client:
            await self._client.aclose()
            logger.info("💀 Redis Async connection gracefully terminated.")


# Create the singleton instance
redis_manager = SecureRedisManager()


# বাংলা মন্তব্য: Redis অনুপলব্ধ থাকলে বা কানেকশন ফেইল করলে এই এরর রেইজ হবে যাতে সাইলেন্ট ফেইলর না ঘটে।
class IdempotencyUnavailableError(Exception):
    """Redis অনুপলব্ধ হওয়ায় idempotency guarantee দেওয়া সম্ভব হচ্ছে না।"""


import contextvars

# Dictionary mapping key to unique lock token
_lock_tokens: contextvars.ContextVar[dict[str, str] | None] = contextvars.ContextVar(
    "_lock_tokens", default=None
)

# Idempotency Helper Functions (Task 8.6)
_RELEASE_LUA = """
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
"""


async def acquire_idempotency_lock(
    key: str, ttl: int = 120, fail_closed: bool = True
) -> bool:
    """
    বাংলা মন্তব্য: fail_closed=True হলে Redis না থাকলে বা ফেইল করলে IdempotencyUnavailableError রেইজ হবে।
    fail_closed=False হলে এটি সাইলেন্টলি fail-open হয়ে True রিটার্ন করবে।
    """
    if not redis_manager.client:
        if fail_closed:
            raise IdempotencyUnavailableError(
                "Redis অনুপলব্ধ — idempotency guarantee দেওয়া যাচ্ছে না।"
            )
        logger.warning(
            f"Idempotency lock for '{key}' skipped — Redis unavailable, fail-open mode."
        )
        return True
    try:
        # SET NX EX - atomic lock acquisition with unique value
        lock_value = str(uuid.uuid4())
        # বাংলা মন্তব্য: lock_value সংরক্ষণ করা হচ্ছে থ্রেড/কনটেক্সট লেভেলে যাতে রিলিজ করার সময় ওনারশিপ ম্যাচ করা যায়
        tokens = _lock_tokens.get()
        if tokens is None:
            tokens = {}
            _lock_tokens.set(tokens)
        tokens[key] = lock_value

        res = await redis_manager.client.set(key, lock_value, nx=True, ex=ttl)
        return bool(res)
    except Exception as e:  # noqa: BLE001
        logger.error(f"Idempotency lock failed for '{key}': {e}")
        if fail_closed:
            raise IdempotencyUnavailableError(
                f"Redis error during lock acquisition: {e}"
            ) from e
        return True


async def release_idempotency_lock(key: str) -> bool:
    """Release lock using Lua script for atomic owner verification."""
    if not redis_manager.client:
        return False
    try:
        # বাংলা মন্তব্য: কারেন্ট কনটেক্সট থেকে টোকেন চেক করে ওনারশিপ ভেরিফাই করা হচ্ছে, যদি না থাকে তবে ড্যাশবোর্ড বা রেডিস ফলব্যাক
        tokens = _lock_tokens.get()
        lock_value = tokens.get(key) if tokens else None

        if lock_value is None:
            # Fallback if no token in context
            lock_value = await redis_manager.client.get(key)
            if lock_value is None:
                return False
        # Use Lua script for atomic release - only delete if owner matches
        result = await redis_manager.client.eval(_RELEASE_LUA, 1, key, lock_value)
        return bool(result)
    except Exception as e:  # noqa: BLE001
        logger.error(f"Idempotency lock release failed: {e}")
        return False


async def cache_response_and_release_lock(
    key: str, response: str, ttl: int = 86400
) -> None:
    if not redis_manager.client:
        return
    try:
        await redis_manager.client.set(f"idempotency:response:{key}", response, ex=ttl)
        await release_idempotency_lock(key)
    except Exception as e:  # noqa: BLE001
        logger.error(f"Idempotency cache response failed: {e}")
