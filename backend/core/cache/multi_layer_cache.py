from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext

"""Implements a robust, multi-layered caching system for the SupremeAI project.

This module provides the `MultiLayerCache` class, which orchestrates a five-tier caching strategy
to optimize response times and reduce AI model inference costs. It integrates Redis-based
exact and prefix matching caches, a semantic cache, and an in-memory LRU session cache.
Redis clients are initialized lazily to prevent network calls during module import,
with an in-memory stub fallback for development or unavailable Redis instances.
The system prioritizes cache hits across layers before falling back to AI model

"""

import asyncio
import hashlib
import json
import threading
import time  # - Added for performance metrics
from typing import Any

try:
    from cachetools import TTLCache
except ImportError:
    TTLCache = dict  # fallback for lightweight environments lacking cachetools

from loguru import logger

from core.messaging.event_bus import ErrorEvent, error_event_bus
from core.metrics_collector import metrics_collector, record_cache_access
from core.swarm_pubsub import swarm_streamer

# বাংলা মন্তব্ব্য: module-level Redis initialization সম্পূর্ণ নিষিদ্ধ।
# Redis client এখন lazy function-level এ initialize হবে।


def _get_redis_client():
    """বাংলা মন্তব্ব্য: Lazy Redis client — import time-এ কোনো connection হয় না।"""
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
    """বাংলা মন্তব্ব্য: Test/dev fallback — production-এ কখনো এটি ব্যবহার হবে না।"""

    def __init__(self):
        self._store: dict[str, str] = {}

    async def get(self, key: str) -> str | None:
        return self._store.get(key)

    async def setex(self, key: str, ttl: int, value: str):
        self._store[key] = value

    async def mget(self, keys: list[str]) -> list[str | None]:
        # বাংলা মন্তব্ব্য: ব্যাচ রিড সাপোর্ট করার জন্য স্টাব ক্লাসে mget মেথড যোগ করা হলো।
        return [self._store.get(k) for k in keys]

    async def pipeline(self, transaction=False):
        """Mock pipeline for stub implementation."""
        return _PipelineStub(self._store)


class _PipelineStub:
    """Mock pipeline for stub implementation."""

    def __init__(self, store):
        self.store = store
        self.commands = []

    async def setex(self, key: str, ttl: int, value: str):
        self.store[key] = value
        return self

    async def execute(self):
        results = []
        for cmd in self.commands:
            results.append(cmd())
        return results

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        pass


# বাংলা মন্তব্ব্য: প্রিফিক্স ল্যাটেরাল রাউন্ড-ট্রিপ ক্যাপ করার কনস্ট্যান্ট।
_MAX_PREFIX_CANDIDATES = 8


class MultiLayerCache:
    """বাংলা মন্তব্ব্য: ৫-লেয়ার অ্যাগ্রেসিভ ক্যাশিং সিস্টেম।
    Redis client গুলো lazy init — import করলে কোনো network call নেই।"""

    def __init__(self):
        self.local_cache_hits = 0
        self.local_cache_misses = 0
        self._exact_cache = None
        self._prefix_cache = None
        self._semantic_cache = None
        # Performance metrics
        self.cache_stats = {"exact_hits": 0, "semantic_hits": 0, "prefix_hits": 0, "session_hits": 0, "misses": 0}

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
            from core.cache.semantic_cache import SemanticCache

            self._semantic_cache = SemanticCache()
        return self._semantic_cache

    @with_error_bus("get")
    async def get(self, prompt: str, model_name: str, session_id: str | None = None) -> dict[str, Any] | None:
        """বাংলা মন্তব্ব্য: সব ৫টি ক্যাশ লেয়ার ক্রমান্বয়ে চেক করে। None মানে AI model call দরকার।"""
        start_time = time.time()
        try:
            # Layer 1: Exact Match Cache (Redis)
            exact_match_cache = self._get_exact_cache()
            exact_cache_key = f"exact:{hashlib.sha256(f'{prompt}:{model_name}'.encode()).hexdigest()}"
            cached_response = await exact_match_cache.get(exact_cache_key)
            if cached_response:
                logger.info("✅ L1 CACHE HIT: Exact Match")
                self.cache_stats["exact_hits"] += 1
                await record_cache_access(True)  # Record cache hit
                await metrics_collector.observe_histogram(
                    "cache_access_duration_seconds", time.time() - start_time, {"layer": "exact", "result": "hit"}
                )
                return {
                    "response": cached_response,
                    "source": "L1_EXACT_CACHE",
                    "latency_ms": 1,
                }
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L1 cache read error: {e}")
            await record_cache_access(False)  # Record cache miss/error
            error_event_bus.emit(
                ErrorEvent(
                    module="multi_layer_cache",
                    error_type="L1_READ_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )

        try:
            # Layer 2: Semantic Cache
            semantic_start = time.time()
            semantic_result = await self._get_semantic_cache().query_similar(prompt)
            semantic_duration = time.time() - semantic_start
            if semantic_result:
                logger.info("✅ L2 CACHE HIT: Semantic Match")
                self.cache_stats["semantic_hits"] += 1
                await record_cache_access(True)  # Record cache hit
                await metrics_collector.observe_histogram(
                    "cache_access_duration_seconds", semantic_duration, {"layer": "semantic", "result": "hit"}
                )
                return {
                    "response": semantic_result.response,
                    "source": "L2_SEMANTIC_CACHE",
                    "latency_ms": int(semantic_duration * 1000),
                }
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L2 semantic cache error: {e}")
            await record_cache_access(False)  # Record cache miss/error
            error_event_bus.emit(
                ErrorEvent(
                    module="multi_layer_cache",
                    error_type="L2_READ_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )

        try:
            # Layer 3: Prefix Cache (Redis) — এখন একটাই batched round-trip
            prefix_start = time.time()
            prefix_cache = self._get_prefix_cache()
            words = prompt.split()
            # বাংলা মন্তব্ব্য: O(n) রাউন্ড-ট্রিপ এড়াতে এবং দীর্ঘতম প্রিফিক্সে অগ্রাধিকার দিতে ক্যান্ডিডেট সংখ্যা ক্যাপ করা হলো।
            candidate_lengths = sorted(
                {max(1, len(words) - step) for step in range(0, min(len(words), _MAX_PREFIX_CANDIDATES))},
                reverse=True,
            )
            prefix_keys = []
            for i in candidate_lengths:
                prefix = " ".join(words[:i])
                prefix_keys.append(f"prefix:{hashlib.sha256(f'{prefix}:{model_name}'.encode()).hexdigest()}")

            if prefix_keys:
                # বাংলা মন্তব্ব্য: mget দিয়ে সম্পূর্ণ প্রিফিক্স ক্যান্ডিডেটগুলোর ডাটা একটাই নেটওয়ার্ক রাউন্ড-ট্রিপে আনা হচ্ছে।
                results = await prefix_cache.mget(prefix_keys)
                for cached_response in results:
                    if cached_response:
                        logger.info("✅ L3 CACHE HIT: Prefix Match")
                        self.cache_stats["prefix_hits"] += 1
                        await record_cache_access(True)  # Record cache hit
                        prefix_duration = time.time() - prefix_start
                        await metrics_collector.observe_histogram(
                            "cache_access_duration_seconds", prefix_duration, {"layer": "prefix", "result": "hit"}
                        )
                        return {
                            "response": cached_response,
                            "source": "L3_PREFIX_CACHE",
                            "latency_ms": int(prefix_duration * 1000),
                        }
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L3 prefix cache error: {e}")
            await record_cache_access(False)  # Record cache miss/error
            error_event_bus.emit(
                ErrorEvent(
                    module="multi_layer_cache",
                    error_type="L3_READ_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )

        # Layer 4: Session Cache (In-memory LRU)
        if session_id:
            session_start = time.time()
            session_response = _get_session_cache(session_id, prompt)
            session_duration = time.time() - session_start
            if session_response:
                logger.info("✅ L4 CACHE HIT: Session Match")
                self.local_cache_hits += 1
                self.cache_stats["session_hits"] += 1
                await record_cache_access(True)  # Record cache hit
                await metrics_collector.observe_histogram(
                    "cache_access_duration_seconds", session_duration, {"layer": "session", "result": "hit"}
                )
                return {
                    "response": session_response,
                    "source": "L4_SESSION_CACHE",
                    "latency_ms": int(session_duration * 100),
                }
            else:
                self.local_cache_misses += 1

        # Layer 5: AI Model Call (fallback)
        total_duration = time.time() - start_time
        self.cache_stats["misses"] += 1
        await record_cache_access(False)  # Record cache miss
        await metrics_collector.observe_histogram(
            "cache_access_duration_seconds", total_duration, {"layer": "all", "result": "miss"}
        )
        logger.info("❌ ALL CACHE LAYERS MISS - Calling AI Model")
        return None

    @with_error_bus("set")
    async def set(self, prompt: str, response: str, model_name: str, session_id: str | None = None):
        """বাংলা মন্তব্ব্য: সব প্রযোজ্য ক্যাশ লেয়ারে রেসপন্স সংরক্ষণ করে।"""
        cache_set_start = time.time()

        try:
            exact_match_cache = self._get_exact_cache()
            exact_cache_key = f"exact:{hashlib.sha256(f'{prompt}:{model_name}'.encode()).hexdigest()}"
            await exact_match_cache.setex(exact_cache_key, 3600, response)
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L1 cache write error: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="multi_layer_cache",
                    error_type="L1_WRITE_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )

        try:
            semantic_start = time.time()
            await self._get_semantic_cache().set(prompt, response, task_type="general")
            semantic_duration = time.time() - semantic_start
            await metrics_collector.observe_histogram(
                "cache_write_duration_seconds", semantic_duration, {"layer": "semantic"}
            )
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L2 semantic cache write error: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="multi_layer_cache",
                    error_type="L2_WRITE_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )

        try:
            prefix_start = time.time()
            prefix_cache = self._get_prefix_cache()
            words = prompt.split()
            # বাংলা মন্তব্ব্য: O(n) রাইট এড়াতে এবং স্টোরেজ অপটিমাইজেশনের জন্য প্রিফিক্স ক্যান্ডিডেট ক্যাপ করা হচ্ছে।
            candidate_lengths = sorted(
                {max(1, len(words) - step) for step in range(0, min(len(words), _MAX_PREFIX_CANDIDATES))},
                reverse=True,
            )
            # pipelined write if pipeline method exists
            if hasattr(prefix_cache, "pipeline"):
                async with prefix_cache.pipeline(transaction=False) as pipe:
                    for i in candidate_lengths:
                        prefix = " ".join(words[:i])
                        prefix_cache_key = f"prefix:{hashlib.sha256(f'{prefix}:{model_name}'.encode()).hexdigest()}"
                        pipe.setex(prefix_cache_key, 1800, response)
                    await pipe.execute()
            else:
                for i in candidate_lengths:
                    prefix = " ".join(words[:i])
                    prefix_cache_key = f"prefix:{hashlib.sha256(f'{prefix}:{model_name}'.encode()).hexdigest()}"
                    await prefix_cache.setex(prefix_cache_key, 1800, response)
            prefix_duration = time.time() - prefix_start
            await metrics_collector.observe_histogram(
                "cache_write_duration_seconds", prefix_duration, {"layer": "prefix"}
            )
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"L3 prefix cache write error: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="multi_layer_cache",
                    error_type="L3_WRITE_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )

        if session_id:
            _set_session_cache(session_id, prompt, response)

        cache_set_duration = time.time() - cache_set_start
        await metrics_collector.observe_histogram("cache_total_set_duration_seconds", cache_set_duration, {})
        logger.info(f"💾 Response cached in all applicable layers for model {model_name}")

    async def get_cache_statistics(self) -> dict[str, Any]:
        """Get cache performance statistics."""
        total_accesses = sum(self.cache_stats.values())
        hit_rate = 0
        if total_accesses > 0:
            hits = total_accesses - self.cache_stats["misses"]
            hit_rate = hits / total_accesses * 100

        return {
            "total_accesses": total_accesses,
            "hit_rate_percentage": hit_rate,
            "exact_hits": self.cache_stats["exact_hits"],
            "semantic_hits": self.cache_stats["semantic_hits"],
            "prefix_hits": self.cache_stats["prefix_hits"],
            "session_hits": self.cache_stats["session_hits"],
            "misses": self.cache_stats["misses"],
            "local_cache_hits": self.local_cache_hits,
            "local_cache_misses": self.local_cache_misses,
        }


# Level 4: Session Cache (In-memory TTLCache per worker)
_session_cache: TTLCache = TTLCache(maxsize=2000, ttl=600)  # 10 minutes, per-worker
_session_lock = threading.Lock()


def _session_key(session_id: str, prompt: str) -> str:
    return f"{session_id}:{hashlib.sha256(prompt.encode()).hexdigest()[:16]}"


def _get_session_cache(session_id: str, prompt: str) -> str | None:
    """বাংলা মন্তব্ব্য: per-worker in-memory session cache — None মানে miss।"""
    with _session_lock:
        return _session_cache.get(_session_key(session_id, prompt))


def _set_session_cache(session_id: str, prompt: str, response: str) -> None:
    with _session_lock:
        _session_cache[_session_key(session_id, prompt)] = response


def _cache_invalidation_listener(event: ErrorEvent) -> None:
    """বাংলা মন্তব্ব্য: Event-Sourced Cache Invalidation
    ErrorEventBus থেকে ইভেন্ট রিসিভ করে ক্যাশ ক্লিয়ার করে।
    """
    if event.error_type in ["CIRCUIT_OPEN", "LLM_DOWN", "RATE_LIMIT_EXCEEDED"]:
        tenant_id = event.context.get("tenant_id") or (
            event.structured_context.env if event.structured_context else None
        )
        # Attempt to get tenant from context

        with _session_lock:
            if tenant_id:
                # Filter and clear only keys associated with this tenant
                keys_to_delete = [k for k in _session_cache if tenant_id in str(k)]
                for k in keys_to_delete:
                    del _session_cache[k]
                logger.info(
                    f"🧹 Event-Sourced Cache: Invalidated {len(keys_to_delete)} session cache keys for tenant {tenant_id} due to {event.error_type}."
                )
            else:
                # Fallback to clear all if tenant_id is not explicitly provided in the error context
                _session_cache.clear()
                logger.info(f"🧹 Event-Sourced Cache: Invalidated entire session cache due to {event.error_type}.")


error_event_bus.register_listener("*", _cache_invalidation_listener)


async def start_swarm_cache_invalidator():
    """বাংলা মন্তব্ব্য: SwarmPubSub থেকে domain ইভেন্ট শুনে ক্যাশ ক্লিয়ার করা।"""
    try:
        async for message_str in swarm_streamer.subscribe():
            try:
                message = json.loads(message_str)
                event_type = message.get("type")
                payload = message.get("data", {})

                target_events = [
                    "KNOWLEDGE_BASE_UPDATED",
                    "TENANT_CONFIG_CHANGED",
                    "TENANT_DELETED",
                    "SYSTEM_CIRCUIT_OPEN",
                    "CACHE_INVALIDATE_REQUESTED",
                ]

                if event_type in target_events:
                    tenant_id = payload.get("tenant_id")

                    with _session_lock:
                        if tenant_id:
                            keys_to_delete = [k for k in _session_cache if tenant_id in str(k)]
                            for k in keys_to_delete:
                                del _session_cache[k]
                            logger.info(
                                f"🧹 Swarm Event Cache Invalidation: Cleared {len(keys_to_delete)} keys for tenant {tenant_id} due to {event_type}."
                            )
                        else:
                            _session_cache.clear()
                            logger.info(
                                f"🧹 Swarm Event Cache Invalidation: Cleared entire session cache due to {event_type}."
                            )
            except json.JSONDecodeError as json_err:
                # বাংলা মন্তব্ব্য: malformed মেসেজ পেলে তা ড্রপ করার সময় সতর্কতা লগ করা হচ্ছে
                logger.warning(
                    f"🧹 Swarm Event Cache Invalidation: Malformed message payload dropped. Error: {json_err}"
                )
            except Exception as e:
                logger.error(f"Error in swarm cache invalidator processing: {e}")
    except asyncio.CancelledError:
        logger.info("Swarm cache invalidator task cancelled.")
    except Exception as e:
        logger.error(f"Swarm cache invalidator crashed: {e}")


# Global instance — lazy init, no network on import
multi_layer_cache = MultiLayerCache()
