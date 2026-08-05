"""Manages asynchronous interactions with a Redis cache for the SupremeAI ecosystem.

বাংলা: SupremeAI-এর জন্য Redis ক্যাশ ব্যবস্থাপনা। পুরোপুরি async,
event-loop blocking মুক্ত, fail-closed প্যাটার্ন অনুসরণ করে।
"""

import asyncio
import json
import logging
import os
import time
from typing import Any

logger = logging.getLogger(__name__)

try:
    import aioredis

    if not hasattr(aioredis, "Redis"):
        import redis.asyncio as aioredis
except ImportError:
    try:
        import redis.asyncio as aioredis
    except ImportError:
        aioredis = None

# Import pybreaker for circuit breaker functionality as mentioned in audit report
try:
    from pybreaker import CircuitBreaker

    _redis_circuit_breaker = CircuitBreaker(fail_max=3, reset_timeout=30, name="redis")
except ImportError:
    # Fallback if pybreaker is not installed
    class MockCircuitBreaker:
        def call(self, func, *args, **kwargs):
            return func(*args, **kwargs)

        async def call_async(self, func, *args, **kwargs):
            return await func(*args, **kwargs)

    _redis_circuit_breaker = MockCircuitBreaker()


class SecureRedisManager:
    def __init__(self):
        from ..config import settings  # Fixed import path

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
            if self.url and aioredis is not None:
                pool = aioredis.ConnectionPool.from_url(
                    self.url,
                    max_connections=20,
                    socket_keepalive=True,
                    socket_connect_timeout=5.0,
                    decode_responses=True,
                )
                self._client = aioredis.Redis(connection_pool=pool)
                logger.info("⚡ Serverless Upstash Redis REST Provider Active with Connection Pool (limit=20).")
            else:
                logger.critical("🔥 CRITICAL: Serverless Redis Endpoint Missing! System entering Fail-Closed state.")
            self._initialized = True

    async def get_client_async(self) -> Any:
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

    async def set_cache(self, key: str, value: str, ex_seconds: int | None = None) -> bool:
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
        except json.JSONDecodeError:
            return None

    # REMOVED: _execute_with_breaker method due to critical bug and unused status
    # As per audit report Phase 3, Section 3.1: This method had a critical bug where
    # pybreaker.CircuitBreaker.call() was called with a coroutine that was never awaited,
    # and the method was never actually called anywhere in the codebase.

    async def incrbyfloat(self, key: str, amount: float, ex_seconds: int = 86400) -> float:
        """Increment floating point value in Redis with optional expiration.

        বাংলা: Redis-এ ফ্লোটিং পয়েন্ট মান বৃদ্ধি করে এবং TTL সেট করে।
        """
        client = await self.get_client_async()
        if client:
            val = await client.incrbyfloat(key, amount)
            if ex_seconds:
                await client.expire(key, ex_seconds)
            return float(val)
        return 0.0


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
                self.acquired = await client.set(self.key, "locked", nx=True, ex=self.ttl)
            except Exception as exc:
                logger.error(f"Failed to set idempotency lock in Redis: {exc}")
                self.acquired = False
        if not self.acquired and self.fail_closed:
            raise IdempotencyUnavailableError(f"Idempotency lock unavailable for key: {self.key}")
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


class _TTLCacheItem:
    """TTL-ভিত্তিক ক্যাশ আইটেম — স্বয়ংক্রিয় মেয়াদোত্তীর্ণ (Bangla: TTL-based cache item with auto-expiry)"""

    __slots__ = ("expires_at", "value")

    def __init__(self, value: Any, ttl: int = 60):
        self.value = value
        self.expires_at = time.monotonic() + ttl

    @property
    def is_expired(self) -> bool:
        return time.monotonic() > self.expires_at


class TTLCacheDict:
    """TTL-সক্ষম ইন-মেমরি ক্যাশ ডিকশনারি (Bangla: TTL-enabled in-memory cache dictionary with auto-cleanup)"""

    def __init__(self, default_ttl: int = 60, maxsize: int = 2000):
        self._store: dict[str, _TTLCacheItem] = {}
        self._default_ttl = default_ttl
        self._maxsize = maxsize
        self._last_cleanup = time.monotonic()

    def get(self, key: str) -> Any | None:
        """মেয়াদোত্তীর্ণ চেক করে ভ্যালু রিটার্ন করে (Bangla: Get value with expiry check)"""
        item = self._store.get(key)
        if item is None:
            return None
        if item.is_expired:
            del self._store[key]
            return None
        return item.value

    def set(self, key: str, value: Any, ttl: int | None = None) -> None:
        """TTL সহ ভ্যালু সংরক্ষণ (Bangla: Store value with TTL)"""
        if len(self._store) >= self._maxsize:
            self._evict_lru()
        self._store[key] = _TTLCacheItem(value, ttl or self._default_ttl)

    def delete(self, key: str) -> None:
        """ক্যাশ থেকে কী মুছে ফেলা (Bangla: Delete key from cache)"""
        self._store.pop(key, None)

    def clear(self) -> None:
        """সম্পূর্ণ ক্যাশ পরিষ্কার (Bangla: Clear entire cache)"""
        self._store.clear()

    @property
    def size(self) -> int:
        """বর্তমান ক্যাশ সাইজ (Bangla: Current cache size)"""
        return len(self._store)

    def _evict_lru(self) -> None:
        """সবচেয়ে পুরনো আইটেম সরিয়ে ফেলা (Bangla: Evict oldest item)"""
        if not self._store:
            return
        oldest_key = min(self._store.keys(), key=lambda k: self._store[k].expires_at)
        del self._store[oldest_key]

    def cleanup_expired(self) -> int:
        """মেয়াদোত্তীর্ণ আইটেমসমূহ পরিষ্কার (Bangla: Remove all expired items)"""
        now = time.monotonic()
        expired = [k for k, v in self._store.items() if v.expires_at <= now]
        for k in expired:
            del self._store[k]
        return len(expired)


class MultiLevelCache:
    """Multi-Level Cache System — L1 (TTL In-Memory) & L2 (Redis). (Bangla: মাল্টি-লেভেল ক্যাশে সিস্টেম)

    L1: TTL-ভিত্তিক ইন-মেমরি ক্যাশ (default TTL: ৬০ সেকেন্ড)
    L2: Redis ক্যাশ (TTL কনফিগারেবল)
    Optimization: L1 TTL L2 TTL-এর চেয়ে ছোট — stale data পড়ার রিস্ক কম
    """

    def __init__(self, redis_mgr: SecureRedisManager | None = None, l1_ttl: int = 60, l2_ttl: int = 3600):
        self._l1_cache = TTLCacheDict(default_ttl=l1_ttl, maxsize=2000)
        self._l2_ttl = l2_ttl
        self.redis_cache = redis_mgr or redis_manager

    @property
    def local_cache(self) -> dict[str, Any]:
        """Backward-compatibility wrapper mapping local_cache accesses to L1 store."""

        class _LocalCacheDictAdapter(dict):
            def __init__(self, outer: MultiLevelCache):
                self.outer = outer

            def __getitem__(self, key: str) -> Any:
                val = self.outer._l1_cache.get(key)
                if val is None:
                    raise KeyError(key)
                return val

            def __setitem__(self, key: str, value: Any) -> None:
                self.outer._l1_cache.set(key, value)

            def __delitem__(self, key: str) -> None:
                self.outer._l1_cache.delete(key)

            def __contains__(self, key: object) -> bool:
                return self.outer._l1_cache.get(str(key)) is not None

            def pop(self, key: str, default: Any = None) -> Any:
                val = self.outer._l1_cache.get(key)
                if val is not None:
                    self.outer._l1_cache.delete(key)
                    return val
                return default

            def clear(self) -> None:
                self.outer._l1_cache.clear()

        return _LocalCacheDictAdapter(self)

    async def get(self, key: str) -> Any:
        """L1 (TTL মেমরি) → L2 (Redis) — ক্রমান্বয়ে চেক (Bangla: Cascade check L1→L2)

        L1 TTL L2 TTL-এর চেয়ে ছোট (৬০s vs ৩৬০০s) — L1 মেয়াদোত্তীর্ণ হলেও
        L2 থেকে তাজা ডাটা নিয়ে L1 ওয়ার্ম-আপ করা যায়।
        """
        # L1 — TTL-ভিত্তিক দ্রুত চেক
        l1_val = self._l1_cache.get(key)
        if l1_val is not None:
            return l1_val

        # L2 — Redis থেকে আনা
        val = await self.redis_cache.get_cache(key)
        if val is not None:
            # L1 ওয়ার্ম-আপ — Redis থেকে পাওয়া ডাটা L1-তেও সংরক্ষণ
            self._l1_cache.set(key, val)
        return val

    async def set(self, key: str, value: Any, ttl: int | None = None) -> None:
        """L1 ও L2 উভয় জায়গায় একসাথে ক্যাশ সংরক্ষণ (Bangla: Set in both L1 and L2)

        L1 TTL সবসময় L2 TTL-এর চেয়ে ছোট বা সমান হবে।
        """
        if value is None:
            return  # Bangla: None ডাটা ক্যাশে করবেন না, স্পেস বাঁচান
        effective_ttl = ttl or self._l2_ttl
        # L1 — ছোট TTL (max ৬০s বা কনফিগারেবল)
        l1_ttl = min(effective_ttl, self._l1_cache._default_ttl)
        self._l1_cache.set(key, value, ttl=l1_ttl)
        # L2 — পূর্ণ TTL
        await self.redis_cache.set_cache(
            key, str(value) if not isinstance(value, str) else value, ex_seconds=effective_ttl
        )

    def invalidate_local(self, key: str | None = None) -> None:
        """L1 ইন-মেমরি ক্যাশ পরিষ্কার (Bangla: Clear L1 cache)

        Args:
            key: নির্দিষ্ট কী (None হলে সম্পূর্ণ ক্যাশ ক্লিয়ার)
        """
        if key:
            self._l1_cache.delete(key)
        else:
            self._l1_cache.clear()

    async def invalidate(self, key: str | None = None) -> None:
        """L1 + L2 উভয় ক্যাশ ইনভ্যালিডেট (Bangla: Invalidate both L1 and L2)

        Stale cache প্রতিরোধে L1 এবং L2 একসাথে ক্লিয়ার করা হয়।
        """
        self.invalidate_local(key)
        if key:
            await self.redis_cache.delete(key)

    def cleanup_expired_l1(self) -> int:
        """মেয়াদোত্তীর্ণ L1 ক্যাশ আইটেমসমূহ পরিষ্কার (Bangla: Cleanup expired L1 items)

        Returns:
            কতগুলি আইটেম ক্লিয়ার করা হয়েছে
        """
        return self._l1_cache.cleanup_expired()
