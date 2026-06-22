import time
from typing import Dict, Any
from loguru import logger

class TenantRateLimiter:
    def __init__(self, redis_client=None):
        self.redis_client = redis_client
        try:
            import core.app as app_mod
            self.redis_queue = app_mod.redis_queue
        except Exception:
            self.redis_queue = redis_client
        logger.info("Initialized TenantRateLimiter")

    async def check_quota(self, tenant_id: str, cost: float) -> bool:
        logger.debug(f"Checking quota for {tenant_id} (Cost: ${cost})")
        queue = getattr(self, "redis_queue", None)
        if queue and getattr(queue, "configured", False):
            try:
                key = f"quota:{tenant_id}"
                current = queue.get(key)
                limit = queue.get(f"quota_limit:{tenant_id}")
                current_float = float(current) if current else 0.0
                limit_float = float(limit) if limit else 100.0
                return current_float + cost <= limit_float
            except Exception as exc:
                logger.debug(f"Redis quota check failed: {exc}")
        return True

    async def record_usage(self, tenant_id: str, cost: float, tokens: int) -> Dict[str, Any]:
        logger.info(f"Recorded usage for {tenant_id}: ${cost} ({tokens} tokens)")
        queue = getattr(self, "redis_queue", None)
        if queue and getattr(queue, "configured", False):
            try:
                queue.incr(f"usage:{tenant_id}:{int(time.time() // 60)}")
                queue.set(f"cost:{tenant_id}", str(cost), ex=3600)
            except Exception as exc:
                logger.debug(f"Redis usage recording failed: {exc}")
        return {"status": "success", "tenant_id": tenant_id}
