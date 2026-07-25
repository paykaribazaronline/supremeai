"""Per-API-Key Rate Limiting using sliding window counter.

বাংলা মন্তব্য: একক API key দিয়ে যেন কেউ পুরো সিস্টেম abuse করতে না পারে, সেজন্য প্রতি কি (Key) ভিত্তিক ডিস্ট্রিবিউটেড রেট লিমিটিং।
"""

import time

from fastapi import HTTPException
from loguru import logger

API_KEY_LIMIT_PREFIX = "apikey:rate:"
DEFAULT_MAX_REQUESTS_PER_MINUTE = 60


async def enforce_api_key_rate_limit(
    api_key_hash: str, max_requests: int = DEFAULT_MAX_REQUESTS_PER_MINUTE
) -> None:
    """Enforce rate limits per API Key hash using atomic Redis counters."""
    from core.cache.redis_manager import redis_manager

    if not redis_manager or not getattr(redis_manager, "client", None):
        return  # Fail open gracefully if Redis is down

    current_minute = int(time.time() / 60)
    window_key = f"{API_KEY_LIMIT_PREFIX}{api_key_hash[:16]}:{current_minute}"

    try:
        pipe = redis_manager.client.pipeline()
        pipe.incr(window_key)
        pipe.expire(window_key, 120)  # 2 minute TTL window safety
        results = await pipe.execute()
        current_count = results[0]

        if current_count > max_requests:
            logger.warning(
                f"🚨 API key rate limit exceeded for key hash prefix {api_key_hash[:8]}: ({current_count} hits)"
            )
            raise HTTPException(status_code=429, detail="API key rate limit exceeded")
    except HTTPException:
        raise
    except Exception as exc:
        logger.warning(
            f"⚠️ API Key rate limiter error: {exc}. Failing open for resilience."
        )
