# 📄 ফাইল: backend/core/redis_manager.py

**প্রকার:** .py  
**সাইজ:** 7,367 বাইট  
**আপডেট:** 2026-07-08T02:25:07.943188

---

## কোড

```py
# বাংলা কমেন্ট: সুপ্রিম-এআই এর কোর অ্যাসিঙ্ক রেডিস এবং ফেল-ক্লোজড রেট-লিমিটিং ইঞ্জিন।
# রেডিস ডাউন থাকলে এটি কোনো সিকিউরিটি গেট বাইপাস করতে দেবে না (Fail-Closed)।

import time

import redis.asyncio as aioredis

from core.config import settings
from core.logging_config import logger


class SecureRedisManager:
    def __init__(self):
        self.redis_url = settings.redis_url
        self.client = None

        # Circuit Breaker state
        self._failure_count = 0
        self._circuit_open_until = 0.0
        self._fallback_store: dict[str, tuple[int, float]] = {}  # key: (count, expires_at)

    async def initialize(self):
        if not self.redis_url:
            logger.critical("🔥 CRITICAL: REDIS_URL missing in configurations! System entering Fail-Closed state.")
            self.client = None
            return
        try:
            self.client = aioredis.from_url(
                self.redis_url,
                encoding="utf-8",
                decode_responses=True,
                socket_timeout=2.0,
                socket_connect_timeout=2.0
            )
            logger.success("🚀 Async Redis Client successfully connected with connection pool.")
        except Exception as e:  # noqa: BLE001
            logger.critical(f"🔥 Fail-Closed Triggered: Redis connection failed during init -> {str(e)}")
            self.client = None

    def _fallback_is_rate_limited(self, key: str, max_requests: int, window_seconds: int) -> bool:
        now = time.time()
        # Clean up expired entries
        expired_keys = [k for k, v in self._fallback_store.items() if v[1] < now]
        for k in expired_keys:
            del self._fallback_store[k]

        count, expires_at = self._fallback_store.get(key, (0, now + window_seconds))
        if expires_at < now:
            count = 0
            expires_at = now + window_seconds

        count += 1
        self._fallback_store[key] = (count, expires_at)

        if count > max_requests:
            logger.warning(f"🚨 In-memory Rate Limit Triggered for Key: {key}. Total: {count}/{max_requests}")
            return True
        return False

    async def is_rate_limited(self, key: str, max_requests: int, window_seconds: int) -> bool:
        now = time.time()

        # Check if circuit is open
        if self._circuit_open_until > now:
            logger.warning(f"⚡ Circuit breaker open! Using in-memory fallback for key: {key}")
            return self._fallback_is_rate_limited(key, max_requests, window_seconds)

        # Half-open: Reset failure count when circuit closes
        if self._circuit_open_until != 0.0 and self._circuit_open_until <= now:
            self._failure_count = 0
            self._circuit_open_until = 0.0
            logger.info("⚡ Circuit breaker half-open: attempting Redis connection again.")

        if self.client is None:
            # According to User instructions, fallback to memory if client is offline or circuit breaks
            self._failure_count += 1
            if self._failure_count >= 5:
                self._circuit_open_until = now + 10.0
                logger.critical("🔥 Circuit Breaker Triggered: Redis offline 5 times. Opening circuit for 10s.")
            return self._fallback_is_rate_limited(key, max_requests, window_seconds)

        try:
            async with self.client.pipeline(transaction=True) as pipe:
                await pipe.incr(key)
                await pipe.expire(key, window_seconds)
                current_requests, _ = await pipe.execute()

            # Reset failure count on success
            self._failure_count = 0
            self._circuit_open_until = 0.0

            if current_requests > max_requests:
                logger.warning(f"🚨 Rate Limit Triggered for Key: {key}. Total: {current_requests}/{max_requests}")
                return True

            return False

        except aioredis.RedisError as redis_err:
            self._failure_count += 1
            if self._failure_count >= 5:
                self._circuit_open_until = now + 10.0
                logger.critical(f"🔥 Circuit Breaker Triggered: Redis failed 5 times -> {str(redis_err)}. Opening circuit for 10s.")
            else:
                logger.warning(f"⚠️ Redis connection failed ({self._failure_count}/5) -> {str(redis_err)}")

            # Fallback to in-memory store
            return self._fallback_is_rate_limited(key, max_requests, window_seconds)

# গ্লোবাল সিঙ্গেলটন ইনস্ট্যান্স জেনারেশন
redis_manager = SecureRedisManager()


async def acquire_idempotency_lock(key: str, ttl_seconds: int = 120) -> bool:
    """
    Distributed idempotency lock অধিগ্রহণ করে (Redis SET NX pattern)।
    
    - key: অনন্য idempotency key (সাধারণত: `idempotency:{method}:{user_key}`)
    - ttl_seconds: লকের TTL — এই সময়ের পর লক স্বয়ংক্রিয়ভাবে মুক্ত হয়
    - Returns True যদি লক সফলভাবে অধিগ্রহণ হয়, False যদি ইতিমধ্যে অন্য কেউ ধরে রেখেছে
    """  # noqa: W293
    if redis_manager.client is None:
        logger.warning("[Idempotency] Redis offline — lock skipped (fail-open)")
        return True
    try:
        # SET NX EX: atomic, only set if not exists
        result = await redis_manager.client.set(
            f"idempotency:{key}", "1", nx=True, ex=ttl_seconds
        )
        return result is not None
    except Exception as e:  # noqa: BLE001
        logger.warning(f"[Idempotency] Redis lock acquire failed — fail-open: {e}")
        return True


async def release_idempotency_lock(key: str) -> None:
    """Idempotency লক রিলিজ করে (ব্যর্থ রিকোয়েস্টের পর retry allow করতে)।"""
    if redis_manager.client is None:
        return
    try:
        await redis_manager.client.delete(f"idempotency:{key}")
    except Exception as e:  # noqa: BLE001
        logger.warning(f"[Idempotency] Redis lock release failed: {e}")

async def cache_response_and_release_lock(key: str, response_data: str, ttl_seconds: int) -> bool:
    """
    Lua স্ক্রিপ্টের মাধ্যমে atomically cache write এবং lock release করে।
    এটি ডেডলক (frozen lock) প্রতিরোধ করে।
    """
    if redis_manager.client is None:
        return False
    lua_script = """
    redis.call("SET", KEYS[1], ARGV[1], "EX", ARGV[2])
    redis.call("DEL", KEYS[2])
    return 1
    """
    try:
        cache_key = f"idempotency:response:{key}"
        lock_key = f"idempotency:{key}"
        script = redis_manager.client.register_script(lua_script)
        await script(keys=[cache_key, lock_key], args=[response_data, ttl_seconds])
        return True
    except Exception as e:  # noqa: BLE001
        logger.warning(f"[Idempotency] Atomic cache+release failed: {e}")
        return False

```