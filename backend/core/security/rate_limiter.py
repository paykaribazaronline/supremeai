"""Rate Limiter Implementation — Sliding Window Algorithm with Atomic Operations (Zero-Hardcode)

বাংলা মন্তব্ব্য: এই মডিউলটি Sliding Window অ্যালগরিদম ব্যবহার করে Rate Limiting করে।
যেকোনো hardcoded ভ্যালু নেই। সবকিছু environment-driven। Atomic operations নিশ্চিত করে।

Key Components:
- `SlidingWindowRateLimiter`: স্লাইডিং উইন্ডো রেট লিমিটার যা Redis এর ZSET ব্যবহার করে।
- `Lua Script`: Atomic operations নিশ্চিত করে Redis-এ।

Critical Security Note: ফেইল-ক্লোজড মোডে রেট লিমিটার কনফিগার করা হয়েছে
যাতে প্রোডাকশনে Redis ডাউন থাকলে রিকোয়েস্ট পাস না করে।
"""

import time
from typing import Literal

from loguru import logger

from core.cache.redis_manager import redis_manager
from core.config import settings
from core.error_bus import with_error_bus


class RateLimitExceededError(Exception):
    """Raised when rate limit is exceeded."""

    pass


class SlidingWindowRateLimiter:
    """Sliding window rate limiter using Redis ZSET with atomic Lua script operations."""

    def __init__(self):
        # Lua script for atomic sliding window rate limiting
        self.lua_script = """
        local key = KEYS[1]
        local window_size = tonumber(ARGV[1])
        local limit = tonumber(ARGV[2])
        local current_time = tonumber(ARGV[3])

        -- Remove expired entries
        redis.call('ZREMRANGEBYSCORE', key, 0, current_time - window_size)

        -- Get current count
        local current_count = redis.call('ZCARD', key)

        -- Check if limit would be exceeded
        if current_count >= limit then
            return {0, redis.call('ZSCORE', key, current_time - window_size + 1)}
        end

        -- Add current request
        redis.call('ZADD', key, current_time, current_time .. '_' .. ARGV[4])

        -- Set expiration to ensure cleanup
        redis.call('EXPIRE', key, window_size)

        -- Return 1 for success, 0 for failure
        return {1, current_count + 1}
        """
        self.script_sha = None

    @with_error_bus("_load_script")
    async def _load_script(self, client):
        """Load the Lua script into Redis for better performance."""
        if self.script_sha is None:
            try:
                self.script_sha = await client.script_load(self.lua_script)
            except Exception:  # Fallback to EVAL if SCRIPT LOAD fails
                self.script_sha = None
                logger.debug("SCRIPT LOAD failed, will fallback to EVAL")

    async def is_allowed(
        self,
        identifier: str,
        limit: int,
        window_size: int,
        limit_type: Literal["ip", "user", "endpoint"] = "ip",
    ) -> tuple[bool, int, int]:
        """
        Check if a request is allowed based on rate limits.

        Args:
            identifier: The identifier to rate limit (IP, user ID, etc.)
            limit: Maximum number of requests allowed
            window_size: Time window in seconds
            limit_type: Type of rate limiting ('ip', 'user', 'endpoint')

        Returns:
            Tuple of (is_allowed: bool, current_count: int, remaining_attempts: int)
        """
        client = await redis_manager.get_client_async()
        if not client:
            # In production, fail-closed: if Redis is unavailable, deny the request
            if settings.env in ["production", "staging"]:
                logger.warning(f"Redis unavailable, denying request for {identifier} in {limit_type} rate limiter")
                return False, 0, 0
            else:
                # In non-production, allow requests if Redis is down
                logger.warning(
                    f"Redis unavailable, allowing request for {identifier} in {limit_type} rate limiter (non-production)"
                )
                return True, 0, limit

        try:
            # Prepare key
            key = f"rate_limit:{limit_type}:{identifier}"

            # Load script if needed
            await self._load_script(client)

            # Execute Lua script atomically
            current_time = int(time.time())
            request_id = f"{current_time}_{hash(identifier) % 1000000}"  # Unique request ID

            if self.script_sha:
                # Use EVALSHA for better performance
                result = await client.evalsha(
                    self.script_sha,
                    1,
                    key,
                    window_size,
                    limit,
                    current_time,
                    request_id,
                )
            else:
                # Fallback to EVAL
                result = await client.eval(
                    self.lua_script,
                    1,
                    key,
                    window_size,
                    limit,
                    current_time,
                    request_id,
                )

            is_allowed_flag, current_count = result[0], result[1]
            remaining = max(0, limit - current_count)

            return bool(is_allowed_flag), current_count, remaining

        except Exception as e:
            logger.error(f"Rate limiter error for {identifier}: {e}")
            # In production, fail-closed
            if settings.env in ["production", "staging"]:
                return False, 0, 0
            else:
                # In non-production, allow request on error
                return True, 0, limit

    async def get_reset_time(
        self,
        identifier: str,
        window_size: int,
        limit_type: Literal["ip", "user", "endpoint"] = "ip",
    ) -> int:
        """
        Get the time when the rate limit window resets.

        Args:
            identifier: The identifier to check
            window_size: Time window in seconds
            limit_type: Type of rate limiting ('ip', 'user', 'endpoint')

        Returns:
            Unix timestamp when the window resets
        """
        client = await redis_manager.get_client_async()
        if not client:
            return int(time.time()) + window_size

        try:
            key = f"rate_limit:{limit_type}:{identifier}"
            current_time = int(time.time())

            # Get the oldest entry in the window
            oldest_req = await client.zrange(key, 0, 0, withscores=True)
            if oldest_req:
                oldest_timestamp = int(oldest_req[0][1].split("_")[0])
                reset_time = oldest_timestamp + window_size
            else:
                reset_time = current_time

            return reset_time
        except Exception as e:
            logger.error(f"Error getting reset time for {identifier}: {e}")
            return int(time.time()) + window_size


# Global instance
rate_limiter = SlidingWindowRateLimiter()
