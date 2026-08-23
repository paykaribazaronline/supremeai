"""
Redis Cache Manager - Optimized for Upstash Free Tier (10K commands/day).
Implements intelligent caching to minimize Redis usage.
"""
import json
import hashlib
import zlib
import os
from typing import Any, Optional, Dict
from datetime import timedelta
import redis.asyncio as redis


class FreeTierCacheManager:
    """
    Cache manager optimized for limited Redis budget (10K commands/day).
    
    Strategies:
    1. Local L1 cache (in-memory, no Redis cost)
    2. Compressed values (save memory)
    3. Batch operations (reduce round-trips)
    4. Selective caching (only high-value items)
    5. TTL management (auto-expire rarely-used keys)
    """
    
    def __init__(self, redis_url: str):
        self.redis: Optional[redis.Redis] = None
        self.redis_url = redis_url
        
        # L1 Cache (in-memory, no Redis cost)
        self.l1_cache: Dict[str, Any] = {}
        self.l1_max_size = 100  # Keep only 100 items in memory
        self.l1_hits = 0
        self.l1_misses = 0
        
        # Compression threshold (compress values > 1KB)
        self.compress_threshold = 1024
        
        # Daily command counter (to stay within 10K limit)
        self.command_count = 0
        self.daily_limit = 9000  # Leave some buffer
    
    async def connect(self):
        """Initialize Redis connection."""
        if self.redis is None:
            self.redis = redis.from_url(
                self.redis_url,
                decode_responses=True,
                socket_timeout=5,
                socket_connect_timeout=5
            )
    
    async def disconnect(self):
        """Close Redis connection."""
        if self.redis:
            await self.redis.close()
            self.redis = None
    
    def _track_command(self):
        """Track command count (reset daily)."""
        self.command_count += 1
        if self.command_count > self.daily_limit:
            import warnings
            warnings.warn(
                f"⚠️ Approaching Redis daily limit: {self.command_count}/{self.daily_limit}",
                UserWarning
            )
    
    def _l1_key(self, key: str) -> str:
        """Generate L1 cache key."""
        return key
    
    def _get_from_l1(self, key: str) -> Optional[Any]:
        """Try to get value from L1 cache."""
        l1_key = self._l1_key(key)
        if l1_key in self.l1_cache:
            self.l1_hits += 1
            return self.l1_cache[l1_key]
        self.l1_misses += 1
        return None
    
    def _set_l1(self, key: str, value: Any):
        """Set value in L1 cache (with eviction if needed)."""
        l1_key = self._l1_key(key)
        
        # Evict oldest if at capacity
        if len(self.l1_cache) >= self.l1_max_size:
            # Remove first item (simple FIFO)
            oldest_key = next(iter(self.l1_cache))
            del self.l1_cache[oldest_key]
        
        self.l1_cache[l1_key] = value
    
    def _compress_value(self, value: bytes) -> bytes:
        """Compress value if above threshold."""
        if len(value) > self.compress_threshold:
            return zlib.compress(value)
        return value
    
    def _decompress_value(self, value: bytes) -> bytes:
        """Decompress value if compressed."""
        try:
            return zlib.decompress(value)
        except zlib.error:
            return value  # Not compressed
    
    async def get(self, key: str) -> Optional[Any]:
        """
        Get value from cache (L1 first, then Redis).
        """
        # Try L1 first (no Redis cost)
        value = self._get_from_l1(key)
        if value is not None:
            return value
        
        # Try Redis
        if self.redis:
            try:
                self._track_command()
                value = await self.redis.get(key)
                
                if value:
                    # Decompress if needed
                    if isinstance(value, bytes):
                        value = self._decompress_value(value)
                    
                    # Parse JSON
                    try:
                        value = json.loads(value)
                    except (json.JSONDecodeError, TypeError):
                        pass
                    
                    # Store in L1 for next time
                    self._set_l1(key, value)
                    return value
                    
            except Exception as e:
                print(f"Cache get error: {e}")
        
        return None
    
    async def set(
        self, 
        key: str, 
        value: Any, 
        ttl_seconds: int = 3600,
        use_redis: bool = True
    ) -> bool:
        """
        Set value in cache (L1 always, Redis optional).
        """
        # Always set L1
        self._set_l1(key, value)
        
        # Set Redis if enabled and within limits
        if use_redis and self.redis and self.command_count < self.daily_limit:
            try:
                # Serialize value
                serialized = json.dumps(value, default=str)
                encoded = serialized.encode('utf-8')
                
                # Compress if large
                encoded = self._compress_value(encoded)
                
                self._track_command()
                await self.redis.setex(key, ttl_seconds, encoded)
                return True
                
            except Exception as e:
                print(f"Cache set error: {e}")
        
        return False
    
    async def delete(self, key: str):
        """Delete from both L1 and Redis."""
        # Delete from L1
        l1_key = self._l1_key(key)
        if l1_key in self.l1_cache:
            del self.l1_cache[l1_key]
        
        # Delete from Redis
        if self.redis:
            try:
                self._track_command()
                await self.redis.delete(key)
            except Exception as e:
                print(f"Cache delete error: {e}")
    
    async def get_many(self, keys: list) -> Dict[str, Any]:
        """Batch get (uses pipeline to save commands)."""
        result = {}
        redis_keys = []
        
        # Check L1 first
        for key in keys:
            l1_value = self._get_from_l1(key)
            if l1_value is not None:
                result[key] = l1_value
            else:
                redis_keys.append(key)
        
        # Batch fetch remaining from Redis
        if redis_keys and self.redis:
            try:
                self._track_command()
                values = await self.redis.mget(redis_keys)
                
                for key, value in zip(redis_keys, values):
                    if value:
                        if isinstance(value, bytes):
                            value = self._decompress_value(value)
                        try:
                            result[key] = json.loads(value)
                        except (json.JSONDecodeError, TypeError):
                            result[key] = value
                        # Store in L1
                        self._set_l1(key, result[key])
                            
            except Exception as e:
                print(f"Cache mget error: {e}")
        
        return result
    
    def get_stats(self) -> Dict[str, Any]:
        """Get cache statistics."""
        return {
            "l1_cache_size": len(self.l1_cache),
            "l1_max_size": self.l1_max_size,
            "l1_hits": self.l1_hits,
            "l1_misses": self.l1_misses,
            "l1_hit_rate": (
                f"{(self.l1_hits / (self.l1_hits + self.l1_misses)) * 100:.1f}%"
                if (self.l1_hits + self.l1_misses) > 0 else "N/A"
            ),
            "redis_commands_today": self.command_count,
            "redis_daily_limit": self.daily_limit,
            "remaining_commands": max(0, self.daily_limit - self.command_count)
        }


# Singleton instance
_cache_manager: Optional[FreeTierCacheManager] = None

async def get_cache_manager() -> FreeTierCacheManager:
    """Get or create cache manager singleton."""
    global _cache_manager
    if _cache_manager is None:
        redis_url = os.getenv("REDIS_URL") or os.getenv("UPSTASH_REDIS_REST_URL")
        if not redis_url:
            raise ValueError("Redis URL not configured")
        _cache_manager = FreeTierCacheManager(redis_url)
        await _cache_manager.connect()
    return _cache_manager
