# 📄 ফাইল: backend/core/rate_limiter.py

**প্রকার:** .py  
**সাইজ:** 1,467 বাইট  
**আপডেট:** 2026-07-11T08:59:12.241867

---

## কোড

```py
from __future__ import annotations

import os
from typing import TYPE_CHECKING

from loguru import logger


if TYPE_CHECKING:
    import redis.asyncio


class AsyncRateLimiter:
    """
    Async Redis rate limiter using redis.asyncio.
    Pipeline reduces network round-trips.
    """

    def __init__(self):
        self._redis: redis.asyncio.Redis | None = None
        self._rate_limit_enabled = os.getenv("RATE_LIMIT_ENABLED", "true").lower() in {"true", "1", "yes"}

    async def _get_redis(self):
        if self._redis is None:
            import redis.asyncio as aioredis

            redis_url = os.getenv("REDIS_URL") or os.getenv("UPSTASH_REDIS_URL") or "redis://localhost:6379"
            self._redis = aioredis.from_url(redis_url, decode_responses=True)
        return self._redis

    async def acquire(self, key: str, limit: int, window: int) -> bool:
        if not self._rate_limit_enabled:
            return True
        client = await self._get_redis()
        try:
            pipe = client.pipeline()
            pipe.incr(key)
            pipe.expire(key, window)
            results = await pipe.execute()
            current = results[0]
            return current <= limit
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Rate limiter unavailable (fail-open): {e}")
            return True

    async def close(self):
        if self._redis:
            await self._redis.close()
            self._redis = None

```