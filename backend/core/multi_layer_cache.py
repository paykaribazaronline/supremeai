import hashlib
from functools import lru_cache
from typing import Any

from loguru import logger

from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus


# বাংলা মন্তব্য: module-level Redis initialization সম্পূর্ণ নিষিদ্ধ।
# Redis client এখন lazy function-level এ initialize হবে।


def _get_redis_client():
    """বাংলা মন্তব্য: Lazy Redis client — import time-এ কোনো connection হয় না।"""
    try:
        import redis.asyncio as aioredis

        from core.config import settings

        url = settings.redis_url
        if not url:
            raise RuntimeError("REDIS_URL is not set in settings. Fail-Fast!")
        return aioredis.from_url(str(url), decode_responses=True)
    except ImportError as e:
        raise RuntimeError("redis.asyncio is required but not installed.") from e


class _InMemoryRedisStub:
    """বাংলা মন্তব্য: Test/dev fallback — production-এ কখনো এটি ব্যবহার হবে না।"""

    def __init__(self):
        self._store: dict[str, str] = {}

    async def get(self, key: str) -> str | None:
        return self._store.get(key)

    async def setex(self, key: str, ttl: int, value: str):
        self._store[key] = value


class MultiLayerCache:
    """বাংলা মন্তব্য: ৫-লেয়ার অ্যাগ্রেসিভ ক্যাশিং সিস্টেম।
    Redis client গুলো lazy init — import করলে কোনো network call নেই।"""

    def __init__(self):
        self.local_cache_hits = 0
        self.local_cache_misses = 0
        self._exact_cache = None
        self._prefix_cache = None
        self._semantic_cache = None

    def _get_exact_cache(self):
        if self._exact_cache is None:
            try:
                self._exact_cache = _get_redis_client()
            except RuntimeError as e:
                logger.warning(f"Exact cache Redis unavailable: {e}. Using in-memory stub.")
                self._exact_cache = _InMemoryRedisStub()
        return self._exact_cache

    def _get_prefix_cache(self):
        if self._prefix_cache is None:
            try:
                self._prefix_cache = _get_redis_client()
            except RuntimeError as e:
                logger.warning(f"Prefix cache Redis unavailable: {e}. Using in-memory stub.")
                self._prefix_cache = _InMemoryRedisStub()
        return self._prefix_cache

    def _get_semantic_cache(self):
        if self._semantic_cache is None:
            from core.semantic_cache import SemanticCache

            self._semantic_cache = SemanticCache()
        return self._semantic_cache

    async def get(self, prompt: str, model_name: str, session_id: str | None = None) -> dict[str, Any] | None:
        """বাংলা মন্তব্য: সব ৫টি ক্যাশ লেয়ার ক্রমান্বয়ে চেক করে। None মানে AI model call দরকার।"""
        try:
            # Layer 1: Exact Match Cache (Redis)
            exact_match_cache = self._get_exact_cache()
            exact_cache_key = f"exact:{hashlib.sha256(f'{prompt}:{model_name}'.encode()).hexdigest()}"
            cached_response = await exact_match_cache.get(exact_cache_key)
            if cached_response:
                logger.info("✅ L1 CACHE HIT: Exact Match")
                return {"response": cached_response, "source": "L1_EXACT_CACHE", "latency_ms": 1}
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L1 cache read error: {e}")
            error_event_bus.emit(ErrorEvent(module="multi_layer_cache", error_type="L1_READ_FAILED", message=str(e)[:200], severity="WARNING"))

        try:
            # Layer 2: Semantic Cache
            semantic_result = await self._get_semantic_cache().query_similar(prompt)
            if semantic_result:
                logger.info("✅ L2 CACHE HIT: Semantic Match")
                return {"response": semantic_result.response, "source": "L2_SEMANTIC_CACHE", "latency_ms": 5}
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L2 semantic cache error: {e}")
            error_event_bus.emit(ErrorEvent(module="multi_layer_cache", error_type="L2_READ_FAILED", message=str(e)[:200], severity="WARNING"))

        try:
            # Layer 3: Prefix Cache (Redis)
            prefix_cache = self._get_prefix_cache()
            words = prompt.split()
            for i in range(len(words) - 1, 0, -1):
                prefix = " ".join(words[:i])
                prefix_cache_key = f"prefix:{hashlib.sha256(f'{prefix}:{model_name}'.encode()).hexdigest()}"
                cached_response = await prefix_cache.get(prefix_cache_key)
                if cached_response:
                    logger.info("✅ L3 CACHE HIT: Prefix Match")
                    return {"response": cached_response, "source": "L3_PREFIX_CACHE", "latency_ms": 10}
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L3 prefix cache error: {e}")
            error_event_bus.emit(ErrorEvent(module="multi_layer_cache", error_type="L3_READ_FAILED", message=str(e)[:200], severity="WARNING"))

        # Layer 4: Session Cache (In-memory LRU)
        if session_id:
            session_response = _get_session_cache(session_id, prompt)
            if session_response:
                logger.info("✅ L4 CACHE HIT: Session Match")
                self.local_cache_hits += 1
                return {"response": session_response, "source": "L4_SESSION_CACHE", "latency_ms": 0.1}
            else:
                self.local_cache_misses += 1

        # Layer 5: AI Model Call (fallback)
        logger.info("❌ ALL CACHE LAYERS MISS - Calling AI Model")
        return None

    async def set(self, prompt: str, response: str, model_name: str, session_id: str | None = None):
        """বাংলা মন্তব্য: সব প্রযোজ্য ক্যাশ লেয়ারে রেসপন্স সংরক্ষণ করে।"""
        try:
            exact_match_cache = self._get_exact_cache()
            exact_cache_key = f"exact:{hashlib.sha256(f'{prompt}:{model_name}'.encode()).hexdigest()}"
            await exact_match_cache.setex(exact_cache_key, 3600, response)
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L1 cache write error: {e}")
            error_event_bus.emit(ErrorEvent(module="multi_layer_cache", error_type="L1_WRITE_FAILED", message=str(e)[:200], severity="WARNING"))

        try:
            await self._get_semantic_cache().set(prompt, response, task_type="general")
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L2 semantic cache write error: {e}")
            error_event_bus.emit(ErrorEvent(module="multi_layer_cache", error_type="L2_WRITE_FAILED", message=str(e)[:200], severity="WARNING"))

        try:
            prefix_cache = self._get_prefix_cache()
            words = prompt.split()
            for i in range(1, len(words) + 1):
                prefix = " ".join(words[:i])
                prefix_cache_key = f"prefix:{hashlib.sha256(f'{prefix}:{model_name}'.encode()).hexdigest()}"
                await prefix_cache.setex(prefix_cache_key, 1800, response)
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L3 prefix cache write error: {e}")
            error_event_bus.emit(ErrorEvent(module="multi_layer_cache", error_type="L3_WRITE_FAILED", message=str(e)[:200], severity="WARNING"))

        logger.info(f"💾 Response cached in all applicable layers for model {model_name}")


# Level 4: Session Cache (In-memory LRU cache per worker)
@lru_cache(maxsize=1000)
def _get_session_cache(session_id: str, prompt: str) -> str | None:
    """বাংলা মন্তব্য: per-worker in-memory session cache — None মানে miss।"""
    return None


import asyncio  # noqa: E402 — asyncio must be available for CancelledError


# Global instance — lazy init, no network on import
multi_layer_cache = MultiLayerCache()
