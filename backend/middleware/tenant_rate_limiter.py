# backend/middleware/tenant_rate_limiter.py
"""Upstash / Redis atomics-based tenant rate limiter middleware.

বাংলা মন্তব্য: ডিস্ট্রিবিউটেড টেন্যান্ট রেট লিমিটিং। race condition রোধ করতে Redis pipeline/incr পরমাণু (atomic) অপারেশন ব্যবহার করা হয়েছে।
"""

from fastapi import HTTPException, Request
from loguru import logger


async def enforce_tenant_rate_limit(request: Request):
    """Upstash / Redis atomic sliding window tenant rate limiting guard."""
    tenant_id = request.headers.get("x-tenant-id", "anonymous_pool")

    from core.cache.redis_manager import redis_manager

    if not redis_manager or not getattr(redis_manager, "client", None):
        logger.warning(
            "⚠️ Redis manager unavailable. Bypassing rate limiter gateway for resilience."
        )
        return

    cache_key = f"rate_limit:{tenant_id}"

    try:
        pipe = redis_manager.client.pipeline()
        pipe.incr(cache_key)
        pipe.expire(cache_key, 60)
        results = await pipe.execute()
        current_hits = results[0]

        if current_hits > 100:
            logger.critical(
                f"🚨 Rate Limit Exceeded for Tenant: {tenant_id} ({current_hits} hits)!"
            )
            raise HTTPException(
                status_code=429, detail="Too Many Requests. Rate limit exceeded."
            )
    except HTTPException:
        raise
    except Exception as exc:
        logger.warning(f"⚠️ Rate limiter error: {exc}. Failing open for resilience.")
        return
