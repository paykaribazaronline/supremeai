# mypy: ignore-errors
from __future__ import annotations

import os
import time
from typing import TYPE_CHECKING

from loguru import logger

if TYPE_CHECKING:
    import redis.asyncio


class InMemoryFallbackLimiter:
    """Sliding-window rate limiter scoped per API key prefix as a fallback when Redis is down."""

    def __init__(self, burst: int = 20, window: float = 60.0) -> None:
        self.burst = burst
        self.window = window
        self._hits: dict[str, list[float]] = {}

    def _cleanup(self, key: str, now: float) -> None:
        # বাংলা মন্তব্য: মেমোরি লিক এড়াতে যদি কোনো কী-তে নতুন কোনো হিট না থাকে, তবে ডিকশনারি থেকে কী-টি ডিলিট করা হচ্ছে।
        if key in self._hits:
            self._hits[key] = [t for t in self._hits[key] if now - t < self.window]
            if not self._hits[key]:
                del self._hits[key]

    def is_allowed(self, key: str, limit: int = 6) -> bool:
        now = time.time()
        self._cleanup(key, now)
        hits = self._hits.setdefault(key, [])
        if len(hits) >= limit:
            return False
        hits.append(now)
        return True


class AsyncRateLimiter:
    """
    Async Redis rate limiter using redis.asyncio.
    Pipeline reduces network round-trips.
    Includes an in-memory fallback (Pre-Deletion Safety Check).
    """

    def __init__(self):
        self._redis: redis.asyncio.Redis | None = None
        self._rate_limit_enabled = os.getenv("RATE_LIMIT_ENABLED", "true").lower() in {
            "true",
            "1",
            "yes",
        }
        self._fallback_limiter = InMemoryFallbackLimiter()

    async def _get_redis(self):
        if self._redis is None:
            import redis.asyncio as aioredis
            # বাংলা মন্তব্য: settings.redis_url থেকে প্রাথমিক ভ্যালু নেওয়া হচ্ছে — fallback চেইন রক্ষিত
            from core.config import settings as app_settings

            redis_url = (
                getattr(app_settings, "redis_url", None)
                or os.getenv("REDIS_URL")
                or os.getenv("UPSTASH_REDIS_URL")
                or "redis://localhost:6379"
            )
            self._redis = aioredis.from_url(redis_url, decode_responses=True)
        return self._redis

    async def acquire(self, key: str, limit: int, window: int) -> bool:
        if not self._rate_limit_enabled:
            return True
        try:
            client = await self._get_redis()
            pipe = client.pipeline()
            pipe.incr(key)
            pipe.expire(key, window)
            results = await pipe.execute()
            current = results[0]
            return current <= limit
        except Exception as e:  # noqa: BLE001
            logger.warning(
                f"Redis rate limiter unavailable: {e}. Falling back to in-memory limiter (degraded mode)."
            )
            return self._fallback_limiter.is_allowed(key, limit=limit)

    async def close(self):
        if self._redis:
            await self._redis.close()
            self._redis = None
