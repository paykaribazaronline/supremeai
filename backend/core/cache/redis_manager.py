"""Manages asynchronous interactions with a Redis cache for the SupremeAI ecosystem.

বাংলা: SupremeAI-এর জন্য Redis ক্যাশ ব্যবস্থাপনা। পুরোপুরি async,
event-loop blocking মুক্ত, fail-closed প্যাটার্ন অনুসরণ করে।
"""

import asyncio
import json
import os
from typing import Any

from loguru import logger
from redis import asyncio as aioredis


class SecureRedisManager:
    def __init__(self):
        from core.config import settings

        self.url = settings.redis_url or os.getenv("REDIS_URL")
        self._client = None
        self._initialized = False
        self._init_lock = asyncio.Lock()

    async def _ensure_connected(self) -> None:
        """Async-safe Redis connection initialization with locking.

        বাংলা: async lock ব্যবহার করে শুধুমাত্র একবার Redis কানেকশন ইনিশিয়ালাইজ করে।
        Synchronous fallback সম্পূর্ণ মুক্ত — event loop ব্লক করে না।
        """
        if self._initialized:
            return
        async with self._init_lock:
            if self._initialized:
                return
            if self.url:
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

    async def get_client_async(self) -> aioredis.Redis | None:
        """Get Redis client with async-safe initialization.

        বাংলা: async-safe Redis ক্লায়েন্ট রিটার্ন করে।
        সকল মডিউলের উচিত এই মেথড ব্যবহার করা, `.client` প্রপার্টি না।
        """
        await self._ensure_connected()
        return self._client

    @property
    def client(self) -> aioredis.Redis | None:
        """Sync property accessor — returns existing client without blocking.

        বাংলা: synchronous accessor যা event loop ব্লক না করেই বিদ্যমান ক্লায়েন্ট রিটার্ন করে।
        যদি ক্লায়েন্ট এখনো ইনিশিয়ালাইজ না হয়, তাহলে None রিটার্ন করে (fail-closed)।
        সকল কনজিউমার ইতিমধ্যেই ``if redis_manager.client:`` চেক করে, তাই এটি নিরাপদ।

        Note: যদি ক্লায়েন্ট ইনিশিয়ালাইজ না হয়, তাহলে None রিটার্ন হবে।
        Prefer ``await redis_manager.get_client_async()`` for guaranteed initialization.
        """
        # বাংলা: কোনো অবস্থাতেই event loop ব্লক করে এমন synchronous init করব না।
        # _initialized True না হলে None রিটার্ন করি — consumer fail-closed behaviour handle করবে।
        return self._client

    @property
    def is_connected(self) -> bool:
        """Check if Redis client is initialized and ready.

        বাংলা: Redis ক্লায়েন্ট ইনিশিয়ালাইজ হয়েছে কিনা চেক করে।
        """
        return self._client is not None

    async def close(self) -> None:
        if self._client:
            await self._client.aclose()
            self._client = None
            self._initialized = False

    async def set(self, key: str, value: str, ex: int | None = None) -> bool:
        client = await self.get_client_async()
        if not client:
            return False
        try:
            await client.set(key, value, ex=ex)
            return True
        except Exception as exc:
            logger.error(f"Redis SET error: {exc}")
            return False

    async def set_cache(
        self, key: str, value: str, ex_seconds: int | None = None
    ) -> bool:
        """Alias for set(), supporting ex_seconds parameter."""
        return await self.set(key, value, ex=ex_seconds)

    async def get(self, key: str) -> str | None:
        client = await self.get_client_async()
        if not client:
            return None
        try:
            return await client.get(key)
        except Exception as exc:
            logger.error(f"Redis GET error: {exc}")
            return None

    async def get_cache(self, key: str) -> str | None:
        """Alias for get()."""
        return await self.get(key)

    async def delete(self, key: str) -> bool:
        client = await self.get_client_async()
        if not client:
            return False
        try:
            await client.delete(key)
            return True
        except Exception as exc:
            logger.error(f"Redis DELETE error: {exc}")
            return False

    async def set_json(self, key: str, data: dict, ex: int | None = None) -> bool:
        return await self.set(key, json.dumps(data), ex=ex)

    async def get_json(self, key: str) -> dict | None:
        val = await self.get(key)
        if not val:
            return None
        try:
            return json.loads(val)
        except Exception:
            return None


redis_manager = SecureRedisManager()


class IdempotencyUnavailableError(Exception):
    """Raised when Redis idempotency lock fails or is unavailable."""

    pass


class _AcquireIdempotencyLockContext:
    def __init__(self, key: str, ttl: int = 60, fail_closed: bool = True):
        self.key = f"idempotency:{key}"
        self.ttl = ttl
        self.fail_closed = fail_closed
        self.acquired = False

    async def __aenter__(self):
        client = await redis_manager.get_client_async()
        if client:
            try:
                self.acquired = await client.set(
                    self.key, "locked", nx=True, ex=self.ttl
                )
            except Exception as exc:
                logger.error(f"Failed to set idempotency lock in Redis: {exc}")
                self.acquired = False
        if not self.acquired and self.fail_closed:
            raise IdempotencyUnavailableError(
                f"Idempotency lock unavailable for key: {self.key}"
            )
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.acquired:
            try:
                await redis_manager.delete(self.key)
            except Exception as exc:
                logger.error(f"Failed to release idempotency lock: {exc}")

    def __await__(self):
        async def _run():
            try:
                async with self as lock:
                    return True if not self.fail_closed else lock.acquired
            except IdempotencyUnavailableError:
                if self.fail_closed:
                    raise
                return True

        return _run().__await__()


def acquire_idempotency_lock(key: str, ttl: int = 60, fail_closed: bool = True):
    return _AcquireIdempotencyLockContext(key, ttl=ttl, fail_closed=fail_closed)


class MultiLevelCache:
    """Multi-Level Cache System — L1 (In-Memory) & L2 (Redis). (Bangla: মাল্টি-লেভেল ক্যাশে সিস্টেম)"""

    def __init__(self, redis_mgr: SecureRedisManager | None = None):
        self.local_cache: dict[str, Any] = {}
        self.redis_cache = redis_mgr or redis_manager

    async def get(self, key: str) -> Any:
        """L1 মেমরি ক্যাশ থেকে চেক করে দ্রুত ডাটা রিটার্ন করে, না থাকলে L2 (Redis) থেকে আনে।"""
        if key in self.local_cache:
            return self.local_cache[key]

        val = await self.redis_cache.get_cache(key)
        if val is not None:
            self.local_cache[key] = val  # Warm up L1 local cache
        return val

    async def set(self, key: str, value: Any, ttl: int = 3600) -> None:
        """L1 ও L2 উভয় জায়গায় ক্যাশ সংরক্ষণ করা (None ভ্যালু ক্যাশ এড়িয়ে স্থান সাশ্রয় করা)।"""
        if value is None:
            return  # Bangla: None ডাটা ক্যাশে করবেন না, স্পেস বাঁচান
        self.local_cache[key] = value
        await self.redis_cache.set_cache(key, str(value), ex_seconds=ttl)

    def invalidate_local(self, key: str | None = None) -> None:
        """L1 ইন-মেমরি ক্যাশ পরিষ্কার করা।"""
        if key:
            self.local_cache.pop(key, None)
        else:
            self.local_cache.clear()

    async def invalidate(self, key: str | None = None) -> None:
        """L1 এর সাথে L2 (Redis) ক্যাশেও ইনভ্যালিডেট করা (Stale Cache প্রবলেম রোধে)।"""
        self.invalidate_local(key)
        if key:
            await self.redis_cache.delete(key)
