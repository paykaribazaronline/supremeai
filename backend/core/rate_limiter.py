from __future__ import annotations

import os
import time

from loguru import logger

from core.cache.redis_manager import redis_manager
from core.config import settings


class InMemoryFallbackLimiter:
    """Sliding-window rate limiter scoped per API key prefix as a fallback when Redis is down."""

    def __init__(self, burst: int = 20, window: float = 60.0) -> None:
        self.burst = burst
        self.window = window
        self._hits: dict[str, list[float]] = {}

    def _cleanup(self, key: str, now: float) -> None:
        # বাংলা মন্তব্ব্য: মেমোরি লিক এড়াতে যদি কোনো কী-তে নতুন কোনো হিট না থাকে, তবে ডিকশনারি থেকে কী-টি ডিলিট করা হচ্ছে।
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
    Async Redis rate limiter using centralized redis_manager.
    Pipeline reduces network round-trips.
    Includes an in-memory fallback (Pre-Deletion Safety Check).

    বাংলা: কেন্দ্রীয় redis_manager ব্যবহার করে — আলাদা Redis connection তৈরি করে না।
    Zero-Cost, ফ্রি-টিয়ার Upstash Redis এর সাথে সামঞ্জস্যপূর্ণ।
    """

    def __init__(self) -> None:
        self._rate_limit_enabled: bool = os.getenv("RATE_LIMIT_ENABLED", "true").lower() in {
            "true",
            "1",
            "yes",
        }

        # Initialize fallback limiter
        self._fallback_limiter = InMemoryFallbackLimiter()

        # Enhanced rate limiting tiers
        self._tier_limits = {
            "free": {"requests": 60, "window": 60},  # 60 req per minute
            "pro": {"requests": 600, "window": 60},  # 600 req per minute
            "premium": {"requests": 1200, "window": 60},  # 1200 req per minute
            "enterprise": {"requests": 6000, "window": 60},  # 6000 req per minute
        }

    async def _get_redis(self):
        """Helper for test mock compatibility."""
        return await redis_manager.get_client_async()

    async def close(self) -> None:
        """No-op: this limiter does not own a Redis connection.

        It shares the centralized `redis_manager` connection, which has its
        own lifecycle. This method exists for interface completeness so
        callers can treat AsyncRateLimiter symmetrically with other
        resources that need explicit shutdown.
        """
        return None

    async def acquire(self, key: str, limit: int | None = None, window: int | None = None) -> bool:
        """Redis-based sliding window rate limiting with fail-closed behavior.

        বাংলা মন্তব্ব্য: Redis-ভিত্তিক sliding window রেট লিমিটিং।
        """
        if not self._rate_limit_enabled:
            return True

        # Fallback values if not specified
        limit = limit or 100
        window = window or 60

        try:
            client = await self._get_redis()
            if client is None:
                if settings.env in ("production", "staging"):
                    logger.critical(f"Rate limiter Redis unavailable. Blocking request for {key} (fail-closed).")
                    return False
                logger.warning(f"Redis rate limiter unavailable. Allowing request for {key} (fail-open in dev).")
                return True

            now = time.time()
            # Ensure unique member for zadd to handle identical timestamps
            import secrets

            member = f"{now}_{secrets.token_hex(4)}"

            pipe = client.pipeline()
            zset_key = f"rate_limit:{key}"
            pipe.zadd(zset_key, {member: now})
            pipe.zremrangebyscore(zset_key, 0, now - window)
            pipe.zcard(zset_key)
            pipe.expire(zset_key, window)

            results = await pipe.execute()
            count = results[2]  # result of zcard
            is_allowed = count <= limit

            # Log near-limit cases for monitoring
            if count > limit * 0.8:
                logger.warning(f"Rate limit approaching for {key}: {count}/{limit}")

            return is_allowed
        except Exception as e:
            if settings.env in ("production", "staging"):
                logger.critical(f"Rate limiter failed critically in production: {e}. Blocking request (fail-closed).")
                return False
            else:
                logger.warning(f"Rate limiter failed in non-production: {e}. Allowing request (fail-open).")
                # Use in-memory fallback for dev/testing
                return self._fallback_limiter.is_allowed(key, limit)
