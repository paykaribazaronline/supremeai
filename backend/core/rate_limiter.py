from __future__ import annotations

import os
import time

from core.cache.redis_manager import redis_manager
from loguru import logger


class InMemoryFallbackLimiter:
    """Sliding-window rate limiter scoped per API key prefix as a fallback when Redis is down."""

    def __init__(self, burst: int = 20, window: float = 60.0) -> None:
        self.burst = burst
        self.window = window
        self._hits: dict[str, list[float]] = {}

    def _cleanup(self, key: str, now: float) -> None:
        # বাংলা মন্তব্য: মেমোরি লিক এড়াতে যদি কোনো কী-তে নতুন কোনো হিট না থাকে, তবে ডিকশনারি থেকে কী-টি ডিলিট করা হচ্ছে।
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
        self._rate_limit_enabled: bool = os.getenv(
            "RATE_LIMIT_ENABLED", "true"
        ).lower() in {
            "true",
            "1",
            "yes",
        }
        self._fallback_limiter = InMemoryFallbackLimiter()

    async def acquire(self, key: str, limit: int, window: int) -> bool:
        if not self._rate_limit_enabled:
            return True
        try:
            client = await redis_manager.get_client_async()
            if client is None:
                return self._fallback_limiter.is_allowed(key, limit=limit)
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

    async def acquire_tenant(self, tenant_id: str, tier: str = "free") -> bool:
        """Multi-tenant tier-based rate limiting. (Bangla: টেন্যান্ট-ভিত্তিক টিয়ার্ড রেট লিমিট)"""
        tiers = {
            "free": (60, 60),  # 60 requests per 60 seconds
            "pro": (600, 60),  # 600 requests per 60 seconds
            "enterprise": (6000, 60),  # 6000 requests per 60 seconds
        }
        limit, window = tiers.get(tier.lower(), tiers["free"])
        key = f"rate_limit:tenant:{tenant_id}:{tier}"
        return await self.acquire(key, limit=limit, window=window)

    async def close(self) -> None:
        # বাংলা মন্তব্য: আলাদা Redis connection নেই — centralized redis_manager বন্ধ করা যাবে না এখান থেকে
        pass


rate_limiter = AsyncRateLimiter()


async def advanced_rate_limit_check(
    key: str,
    limit: int = 100,
    window: int = 3600,
    burst_multiplier: float = 1.5,
) -> bool:
    """Advanced rate limiting with burst capability. (Bangla: বার্স্ট ক্যাপাবিলিটি সহ অ্যাডভান্সড রেট লিমিটিং)

    Args:
        key: The rate limit identifier key (IP or user ID).
        limit: Base rate limit per window.
        window: Time window in seconds.
        burst_multiplier: Multiplier for burst allowance.

    Returns:
        bool: True if request is allowed, False otherwise.
    """
    effective_limit = int(limit * burst_multiplier)
    return await rate_limiter.acquire(key, limit=effective_limit, window=window)
