#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> rate_limiter.py
# project >> SupremeAI 2.0
# purpose >> Rate limiting
# module >> core
# ============================================================================
from __future__ import annotations

import time
from typing import Dict

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse

from loguru import logger


class RateLimiter:
    def __init__(self, requests_per_minute: int = 60, burst: int = 10) -> None:
        self.requests_per_minute = requests_per_minute
        self.burst = burst
        self._hits: Dict[str, list[float]] = {}

    def _cleanup(self, key: str, now: float) -> None:
        window = 60.0
        self._hits[key] = [t for t in self._hits.get(key, []) if now - t < window]

    def is_allowed(self, key: str) -> bool:
        now = time.time()
        self._cleanup(key, now)
        hits = self._hits.setdefault(key, [])
        if len(hits) >= self.burst:
            return False
        hits.append(now)
        return True

    def remaining(self, key: str) -> int:
        now = time.time()
        self._cleanup(key, now)
        return max(0, self.burst - len(self._hits.get(key, [])))


class RedisRateLimiter:
    def __init__(self, requests_per_minute: int = 60, burst: int = 10, window: int = 60) -> None:
        self.requests_per_minute = requests_per_minute
        self.burst = burst
        self.window = window
        self._redis = None
        self._configure_redis()

    def _configure_redis(self) -> None:
        try:
            from core.upstash_redis_queue import UpstashRedisQueue
            self._redis = UpstashRedisQueue()
        except Exception as exc:
            logger.warning(f"Redis rate limiter unavailable, falling back to in-memory: {exc}")
            self._redis = None

    def is_allowed(self, key: str) -> bool:
        if not self._redis or not self._redis.configured:
            fallback = RateLimiter(self.requests_per_minute, self.burst)
            return fallback.is_allowed(key)
        now = time.time()
        redis_key = f"rate_limit:{key}"
        try:
            count = self._redis.incr(redis_key)
            if count == 1:
                self._redis.set(redis_key, "1", ex=self.window)
            elif count and count > self.burst:
                return False
            return True
        except Exception as exc:
            logger.warning(f"Redis rate limit check failed, allowing request: {exc}")
            return True

    def remaining(self, key: str) -> int:
        if not self._redis or not self._redis.configured:
            fallback = RateLimiter(self.requests_per_minute, self.burst)
            return fallback.remaining(key)
        redis_key = f"rate_limit:{key}"
        try:
            value = self._redis.get(redis_key)
            count = int(value) if value is not None else 0
            return max(0, self.burst - count)
        except Exception as exc:
            logger.warning(f"Redis rate limit remaining check failed: {exc}")
            return self.burst


class RateLimitMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, requests_per_minute: int = 60, burst: int = 10) -> None:
        super().__init__(app)
        self.limiter = RedisRateLimiter(requests_per_minute=requests_per_minute, burst=burst)

    async def dispatch(self, request: Request, call_next):
        client = request.client.host if request.client else "unknown"
        if not self.limiter.is_allowed(client):
            logger.warning(f"Rate limit exceeded for {client}")
            return JSONResponse(
                status_code=429,
                content={"detail": "Too many requests. Please try again later."},
            )
        response = await call_next(request)
        return response
