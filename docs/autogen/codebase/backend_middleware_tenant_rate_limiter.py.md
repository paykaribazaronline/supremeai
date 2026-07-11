# 📄 ফাইল: backend/middleware/tenant_rate_limiter.py

**প্রকার:** .py  
**সাইজ:** 1,587 বাইট  
**আপডেট:** 2026-07-11T14:23:58.604467

---

## কোড

```py
# backend/middleware/tenant_rate_limiter.py
from fastapi import HTTPException
from fastapi import Request
from loguru import logger

from core.services import registry  # আমাদের নতুন ডায়নামিক সার্ভিস রেজিস্ট্রি চেইন


async def enforce_tenant_rate_limit(request: Request):
    """Upstash REST ভিত্তিক টেন্যান্ট রেট লিমিটিং গার্ড।"""
    tenant_id = request.headers.get("x-tenant-id", "anonymous_pool")
    redis_mgr = registry.get_service("redis_manager")

    if not redis_mgr:
        logger.warning("⚠️ Redis manager unavailable. Bypassing rate limiter gateway for resilience.")
        return

    cache_key = f"rate_limit:{tenant_id}"
    current_hits = await redis_mgr.get_cache(cache_key)

    if current_hits is None:
        # প্রথম হিটের ক্ষেত্রে উইন্ডো ইনিশিয়ালাইজ করা হলো (১ মিনিটে সর্বোচ্চ ১০০ রিকোয়েস্ট)
        await redis_mgr.set_cache(cache_key, "1", ex_seconds=60)
    else:
        hits = int(current_hits)
        if hits >= 100:
            logger.critical(f"🚨 Rate Limit Exceeded for Tenant: {tenant_id}!")
            raise HTTPException(status_code=429, detail="Too Many Requests. Rate limit exceeded.")

        # কাউন্টার ইনক্রিমেন্ট এবং আপডেট
        await redis_mgr.set_cache(cache_key, str(hits + 1), ex_seconds=60)

```