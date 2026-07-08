# 📄 ফাইল: backend/core/rate_limiter.py

**প্রকার:** .py  
**সাইজ:** 6,448 বাইট  
**আপডেট:** 2026-07-08T03:11:56.320437

---

## কোড

```py
from __future__ import annotations

import threading
import time

from loguru import logger
from starlette.requests import Request
from starlette.responses import JSONResponse


class RateLimiter:
    # বাংলা মন্তব্য: মেমরি ভিত্তিক রেট লিমিটারের থ্রেড-সেফটি নিশ্চিত করার জন্য লক ব্যবহার করা হলো
    def __init__(self, requests_per_minute: int = 60, burst: int = 10) -> None:
        self.requests_per_minute = requests_per_minute
        self.burst = burst
        self._hits: dict[str, list[float]] = {}
        self._lock = threading.Lock()

    def _cleanup(self, key: str, now: float) -> None:
        window = 60.0
        self._hits[key] = [t for t in self._hits.get(key, []) if now - t < window]

    def is_allowed(self, key: str) -> bool:
        with self._lock:
            now = time.time()
            self._cleanup(key, now)
            hits = self._hits.setdefault(key, [])
            if len(hits) >= self.burst:
                return False
            hits.append(now)
            return True

    def remaining(self, key: str) -> int:
        with self._lock:
            now = time.time()
            self._cleanup(key, now)
            return max(0, self.burst - len(self._hits.get(key, [])))


class RedisRateLimiter:
    def __init__(
        self, requests_per_minute: int = 60, burst: int = 10, window: int = 60
    ) -> None:
        self.requests_per_minute = requests_per_minute
        self.burst = burst
        self.window = window
        self._redis = None
        self._configure_redis()
        self._fallback_limiter = RateLimiter(requests_per_minute, burst)

    def _configure_redis(self) -> None:
        try:
            from core.upstash_redis_queue import UpstashRedisQueue

            self._redis = UpstashRedisQueue()
        except Exception as exc:  # noqa: BLE001
            logger.warning(
                f"Redis rate limiter unavailable, falling back to in-memory: {exc}"
            )
            self._redis = None

    def is_allowed(self, key: str) -> bool:
        if not self._redis or not self._redis.configured:
            return self._fallback_limiter.is_allowed(key)
        redis_key = f"rate_limit:{key}"
        try:
            count = self._redis.incr(redis_key)
            if count == 1:
                self._redis.expire(redis_key, self.window)
            elif count and count > self.burst:
                return False
            return True
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Redis rate limit check failed, blocking request: {exc}")
            return self._fallback_limiter.is_allowed(key)

    def remaining(self, key: str) -> int:
        if not self._redis or not self._redis.configured:
            return self._fallback_limiter.remaining(key)
        redis_key = f"rate_limit:{key}"
        try:
            value = self._redis.get(redis_key)
            count = int(value) if value is not None else 0
            return max(0, self.burst - count)
        except Exception as exc:  # noqa: BLE001
            logger.warning(f"Redis rate limit remaining check failed: {exc}")
            return self._fallback_limiter.remaining(key)


class RateLimitMiddleware:
    def __init__(self, app, requests_per_minute: int = 60, burst: int = 10) -> None:
        self.app = app
        self.limiter = RedisRateLimiter(
            requests_per_minute=requests_per_minute, burst=burst
        )

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        from utils.environment import is_test_environment

        if is_test_environment():
            await self.app(scope, receive, send)
            return

        request = Request(scope, receive=receive)
        tenant_id = getattr(request.state, "tenant_id", None)
        if tenant_id is None:
            user_info = getattr(request.state, "user", None)
            if isinstance(user_info, dict):
                tenant_id = user_info.get("tenant_id") or user_info.get("sub")

        if tenant_id:
            try:
                from tools.tenant_rate_limiter import TenantRateLimiter

                if not hasattr(self, "_tenant_limiter"):
                    self._tenant_limiter = TenantRateLimiter()

                quota_status = await self._tenant_limiter.check_quota(
                    tenant_id, cost=0.0
                )
                if not quota_status.get("allowed", True):
                    logger.warning(
                        f"Tenant rate limit exceeded for {tenant_id}: {quota_status}"
                    )
                    response = JSONResponse(
                        status_code=429,
                        content={
                            "detail": f"Tenant rate limit exceeded: {quota_status.get('reason')}"
                        },
                    )
                    await response(scope, receive, send)
                    return
            except Exception as exc:  # noqa: BLE001
                logger.error(f"Error checking tenant rate limit: {exc}. Failing closed (503).")
                response = JSONResponse(
                    status_code=503,
                    content={"detail": "Service Unavailable: Rate limit service is offline."},
                )
                await response(scope, receive, send)
                return
            client = scope.get("client")

            x_forwarded_for = None
            headers = scope.get("headers", [])
            for k, v in headers:
                if k.lower() == b"x-forwarded-for":
                    x_forwarded_for = v.decode("utf-8")
                    break

            if x_forwarded_for:
                client_ip = x_forwarded_for.split(",")[0].strip()
            else:
                client_ip = client[0] if client else "unknown"

            if not self.limiter.is_allowed(client_ip):
                logger.warning(f"Rate limit exceeded for {client_ip}")
                response = JSONResponse(
                    status_code=429,
                    content={"detail": "Too many requests. Please try again later."},
                )
                await response(scope, receive, send)
                return

        await self.app(scope, receive, send)

```