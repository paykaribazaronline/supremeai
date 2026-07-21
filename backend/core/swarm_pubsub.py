from core.messaging.event_bus import ErrorContext

"""This module provides a robust, Redis-backed Publish/Subscribe (PubSub) system, `SwarmPubSub`, designed to facilitate real-time event streaming and communication across the SupremeAI ecosystem. It offers a multi-worker safe mechanism for broadcasting and subscribing to a central "swarm_stream" channel, ensuring scalable and decoupled event propagation, with lazy Redis client initialization and comprehensive error handling integrated with the project's central event bus.

Key Components:
- `SwarmPubSub`: Manages the Redis PubSub client, handling lazy connection, subscription to the "swarm_stream" channel, and broadcasting messages to all active subscribers.
- `get_swarm_streamer()`: Returns a singleton instance of the `SwarmPubSub` class, ensuring a single, globally accessible point of control for swarm-wide communication.
- `swarm_streamer`: The globally accessible singleton instance of `SwarmPubSub`, initialized upon module import.

Dependencies:
- `asyncio`: For asynchronous programming constructs and managing concurrent operations.
- `json`: For serializing and deserializing event payloads to and from JSON format.
- `loguru`: For structured logging of operational events and errors within the module.
- `redis.asyncio`: The asynchronous Redis client library used for PubSub operations.
- `core.config`: To retrieve application settings, specifically the Redis connection URL.
- `core.messaging.event_bus`: For emitting structured error events to the central application event bus."""

import asyncio  # noqa: E402
import json  # noqa: E402
from collections.abc import AsyncGenerator  # noqa: E402

from core.messaging.event_bus import ErrorEvent  # noqa: E402
from core.messaging.event_bus import error_event_bus  # noqa: E402
from loguru import logger  # noqa: E402

# বাংলা মন্তব্য: module-level redis.from_url("redis://localhost") সম্পূর্ণ নিষিদ্ধ।
# RedisURL এখন settings থেকে আসে, hardcode নয়।


class SwarmPubSub:
    """বাংলা মন্তব্য: Redis PubSub-ভিত্তিক Swarm Event Stream।
    Redis client lazy init — import করলে কোনো connection attempt নেই।"""

    def __init__(self):
        self._redis = None

    @property
    def redis(self):
        """বাংলা মন্তব্য: Public accessor for lazy-initialized Redis client."""
        return self._get_redis()

    @redis.setter
    def redis(self, value):
        """বাংলা মন্তব্য: Allow tests to inject mock Redis client."""
        self._redis = value

    def _get_redis(self):
        if self._redis is not None:
            return self._redis
        import redis.asyncio as aioredis  # type: ignore[import-untyped]
        from core.config import settings

        url = str(settings.redis_url)
        if not url:
            raise RuntimeError("REDIS_URL is not configured in settings. Fail-Fast!")
        self._redis = aioredis.from_url(url)
        return self._redis

    async def subscribe(self) -> AsyncGenerator[str, None]:
        """বাংলা মন্তব্য: নতুন ক্লায়েন্টের জন্য Redis চ্যানেল সাবস্ক্রাইব করবে (Multi-Worker Safe)।"""
        try:
            redis_client = self._get_redis()
        except RuntimeError as e:
            logger.error(f"SwarmPubSub: Cannot subscribe, Redis unavailable: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="swarm_pubsub",
                    error_type="REDIS_UNAVAILABLE",
                    message=str(e)[:200],
                    severity="CRITICAL",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )
            raise

        pubsub = redis_client.pubsub()
        await pubsub.subscribe("swarm_stream")
        logger.info("New client subscribed to Redis Swarm Stream.")

        try:
            while True:
                message = await pubsub.get_message(
                    ignore_subscribe_messages=True, timeout=1.0
                )
                if message is not None:
                    yield message["data"].decode("utf-8")
                await asyncio.sleep(0.01)
        except asyncio.CancelledError:
            # বাংলা মন্তব্য: CancelledError suppress নিষিদ্ধ — cleanup করে re-raise করতেই হবে।
            logger.info("Client disconnected from Redis Swarm Stream.")
            try:
                await pubsub.unsubscribe("swarm_stream")
                await pubsub.close()
            except Exception as cleanup_err:  # noqa: BLE001
                logger.error(f"SwarmPubSub cleanup error: {cleanup_err}")
                error_event_bus.emit(
                    ErrorEvent(
                        module="swarm_pubsub",
                        error_type="CLEANUP_FAILED",
                        message=str(cleanup_err)[:200],
                        severity="WARNING",
                        structured_context=ErrorContext(module="auto_fixed"),
                    )
                )
            raise
        except Exception as e:  # noqa: BLE001
            logger.error(f"SwarmPubSub subscription error: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="swarm_pubsub",
                    error_type="SUBSCRIPTION_ERROR",
                    message=str(e)[:200],
                    severity="ERROR",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )
            raise

    async def set_halt(self, reason: str = "manual_emergency_stop") -> None:
        """বাংলা মন্তব্য: গ্লোবাল ইমার্জেন্সি-স্টপ ফ্ল্যাগ সেট করে (Redis-backed, multi-worker safe)।
        মোবাইল অ্যাপের 'Hold to Kill' বাটন থেকে আসা একমাত্র সত্যিকারের হল্ট সিগন্যাল —
        TTL সহ, যাতে কোনো কারণে clear_halt() না চললেও সিস্টেম চিরস্থায়ীভাবে আটকে না থাকে।
        """
        try:
            redis_client = self._get_redis()
            await redis_client.set("swarm:halt:global", reason, ex=3600)
        except Exception as e:  # noqa: BLE001
            logger.error(f"SwarmPubSub: failed to set halt flag: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="swarm_pubsub",
                    error_type="HALT_FLAG_SET_FAILED",
                    message=str(e)[:200],
                    severity="CRITICAL",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )
            raise

    async def clear_halt(self) -> None:
        """বাংলা মন্তব্য: ইমার্জেন্সি-স্টপ ফ্ল্যাগ ক্লিয়ার করে, সোয়ার্ম আবার এক্সিকিউশন শুরু করতে পারবে।"""
        try:
            redis_client = self._get_redis()
            await redis_client.delete("swarm:halt:global")
        except Exception as e:  # noqa: BLE001
            logger.error(f"SwarmPubSub: failed to clear halt flag: {e}")
            raise

    async def is_halted(self) -> bool:
        """বাংলা মন্তব্য: এক্সিকিউশন লুপে চেক করার জন্য — গ্লোবাল হল্ট চালু আছে কিনা।
        Redis সাময়িকভাবে আনরিচেবল হলে fail-open (halted=False) থাকবে, কারণ একটি
        flaky Redis-এর কারণে পুরো সিস্টেমের সব টাস্ক আটকে যাওয়া (নতুন outage vector)
        চাওয়া হয় না — বরং এরর ইভেন্ট এমিট করে অবজার্ভেবিলিটির মাধ্যমে সতর্ক করা হয়।
        """
        try:
            redis_client = self._get_redis()
            value = await redis_client.get("swarm:halt:global")
            return value is not None
        except Exception as e:  # noqa: BLE001
            logger.error(
                f"SwarmPubSub: halt-flag check failed, defaulting to NOT halted: {e}"
            )
            error_event_bus.emit(
                ErrorEvent(
                    module="swarm_pubsub",
                    error_type="HALT_FLAG_CHECK_FAILED",
                    message=str(e)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )
            return False

    async def broadcast(self, event_type: str, payload: dict):
        """বাংলা মন্তব্য: সকল অ্যাক্টিভ ক্লায়েন্টকে Redis চ্যানেলে ডেটা পুশ করবে।"""
        try:
            redis_client = self._get_redis()
            message = json.dumps({"type": event_type, "data": payload})
            await redis_client.publish("swarm_stream", message)
        except asyncio.CancelledError:
            raise
        except Exception as e:  # noqa: BLE001
            logger.error(f"SwarmPubSub broadcast failed: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="swarm_pubsub",
                    error_type="BROADCAST_FAILED",
                    message=str(e)[:200],
                    severity="ERROR",
                    structured_context=ErrorContext(module="auto_fixed"),
                )
            )
            raise


# বাংলা মন্তব্য: Lazy singleton — module import করলে কোনো Redis connection হয় না।
_swarm_streamer_instance: SwarmPubSub | None = None


def get_swarm_streamer() -> SwarmPubSub:
    global _swarm_streamer_instance
    if _swarm_streamer_instance is None:
        _swarm_streamer_instance = SwarmPubSub()
    return _swarm_streamer_instance


swarm_streamer = get_swarm_streamer()
