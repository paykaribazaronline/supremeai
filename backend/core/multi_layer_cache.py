import hashlib
import os
from functools import lru_cache
from typing import Any


try:
    import redis.asyncio as redis
except ImportError:  # pragma: no cover
    redis = None

from loguru import logger

from .semantic_cache import SemanticCache


class _InMemoryRedisStub:
    def __init__(self):
        self._store: dict[str, str] = {}

    async def get(self, key: str) -> str | None:
        return self._store.get(key)

    async def setex(self, key: str, ttl: int, value: str):
        self._store[key] = value


class _RedisFallback:
    @staticmethod
    def from_url(url: str, decode_responses: bool = True):
        logger.warning(
            "redis.asyncio is not installed; using in-memory fallback cache for multi-layer cache."
        )
        return _InMemoryRedisStub()


if redis is None:
    redis = _RedisFallback()


# Level 1: Exact Match Cache (Redis/Upstash)
exact_match_cache = redis.from_url(
    os.getenv("REDIS_URL", "redis://localhost:6379"), decode_responses=True
)


# Level 2: Semantic Cache (using existing semantic_cache.py)
semantic_cache = SemanticCache()

# Level 3: Prefix Cache (Redis with key prefixes)
prefix_cache = redis.from_url(
    os.getenv("REDIS_URL", "redis://localhost:6379"), decode_responses=True
)


# Level 4: Session Cache (In-memory LRU cache per worker)
@lru_cache(maxsize=1000)
def get_session_cache(session_id: str, prompt: str) -> str | None:
    """In-memory session cache - returns None if not found"""
    # This is a simplified implementation
    return None


# Level 5: AI Model Call (fallback)


class MultiLayerCache:
    """5-Layer Aggressive Caching System for SupremeAI 2.0"""

    def __init__(self):
        self.local_cache_hits = 0
        self.local_cache_misses = 0

    async def get(
        self, prompt: str, model_name: str, session_id: str | None = None
    ) -> dict[str, Any] | None:
        """
        Check all 5 cache layers in order. Return cached response if found.
        Returns None if all layers miss.
        """
        # Layer 1: Exact Match Cache (Redis)
        exact_cache_key = (
            f"exact:{hashlib.sha256(f'{prompt}:{model_name}'.encode()).hexdigest()}"
        )
        cached_response = await exact_match_cache.get(exact_cache_key)
        if cached_response:
            logger.info("✅ L1 CACHE HIT: Exact Match")
            return {
                "response": cached_response,
                "source": "L1_EXACT_CACHE",
                "latency_ms": 1,  # Simulated low latency
            }

        # Layer 2: Semantic Cache (FAISS/ChromaDB via SemanticCache)
        semantic_result = await semantic_cache.query_similar(prompt)
        if semantic_result:
            logger.info("✅ L2 CACHE HIT: Semantic Match")
            return {
                "response": semantic_result.response,
                "source": "L2_SEMANTIC_CACHE",
                "latency_ms": 5,  # Simulated low latency
            }

        # Layer 3: Prefix Cache (Redis with key prefixes)
        words = prompt.split()
        for i in range(len(words) - 1, 0, -1):  # Check longest prefixes first
            prefix = " ".join(words[:i])
            prefix_cache_key = f"prefix:{hashlib.sha256(f'{prefix}:{model_name}'.encode()).hexdigest()}"
            cached_response = await prefix_cache.get(prefix_cache_key)
            if cached_response:
                logger.info("✅ L3 CACHE HIT: Prefix Match")
                # In a real implementation, we'd reconstruct the response based on prefix
                return {
                    "response": cached_response,  # Simplified - would need smarter reconstruction
                    "source": "L3_PREFIX_CACHE",
                    "latency_ms": 10,  # Simulated low latency
                }

        # Layer 4: Session Cache (In-memory)
        if session_id:
            session_response = get_session_cache(session_id, prompt)
            if session_response:
                logger.info("✅ L4 CACHE HIT: Session Match")
                self.local_cache_hits += 1
                return {
                    "response": session_response,
                    "source": "L4_SESSION_CACHE",
                    "latency_ms": 0.1,  # In-memory is fastest
                }
            else:
                self.local_cache_misses += 1

        # Layer 5: AI Model Call (fallback)
        logger.info("❌ ALL CACHE LAYERS MISS - Calling AI Model")
        return None  # Indicates we need to call the AI model

    async def set(
        self, prompt: str, response: str, model_name: str, session_id: str | None = None
    ):
        """Store response in all relevant cache layers"""
        # Layer 1: Exact Match Cache
        exact_cache_key = (
            f"exact:{hashlib.sha256(f'{prompt}:{model_name}'.encode()).hexdigest()}"
        )
        await exact_match_cache.setex(exact_cache_key, 3600, response)  # 1 hour TTL

        # Layer 2: Semantic Cache
        await semantic_cache.set(prompt, response, task_type="general")

        # Layer 3: Prefix Cache (cache all prefixes)
        words = prompt.split()
        for i in range(1, len(words) + 1):
            prefix = " ".join(words[:i])
            prefix_cache_key = f"prefix:{hashlib.sha256(f'{prefix}:{model_name}'.encode()).hexdigest()}"
            await prefix_cache.setex(
                prefix_cache_key, 1800, response
            )  # 30 min TTL for prefixes

        # Layer 4: Session Cache
        if session_id:
            # Note: lru_cache doesn't have a direct setter, we'd need to manage this differently
            # For demonstration, we'll just note that session caching would happen here
            pass

        logger.info(
            f"💾 Response cached in all applicable layers for model {model_name}"
        )


# Global instance
multi_layer_cache = MultiLayerCache()
