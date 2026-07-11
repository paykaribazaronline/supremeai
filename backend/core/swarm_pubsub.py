import asyncio
import json
from collections.abc import AsyncGenerator

from loguru import logger

from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus


# বাংলা মন্তব্য: module-level redis.from_url("redis://localhost") সম্পূর্ণ নিষিদ্ধ।
# RedisURL এখন settings থেকে আসে, hardcode নয়।


class SwarmPubSub:
    """বাংলা মন্তব্য: Redis PubSub-ভিত্তিক Swarm Event Stream।
    Redis client lazy init — import করলে কোনো connection attempt নেই।"""

    def __init__(self):
        self._redis = None

    def _get_redis(self):
        if self._redis is None:
            import redis.asyncio as aioredis

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
                )
            )
            raise

        pubsub = redis_client.pubsub()
        await pubsub.subscribe("swarm_stream")
        logger.info("New client subscribed to Redis Swarm Stream.")

        try:
            while True:
                message = await pubsub.get_message(ignore_subscribe_messages=True, timeout=1.0)
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
                    )
                )
            raise
        except Exception as e:
            logger.error(f"SwarmPubSub subscription error: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="swarm_pubsub",
                    error_type="SUBSCRIPTION_ERROR",
                    message=str(e)[:200],
                    severity="ERROR",
                )
            )
            raise

    async def broadcast(self, event_type: str, payload: dict):
        """বাংলা মন্তব্য: সকল অ্যাক্টিভ ক্লায়েন্টকে Redis চ্যানেলে ডেটা পুশ করবে।"""
        try:
            redis_client = self._get_redis()
            message = json.dumps({"type": event_type, "data": payload})
            await redis_client.publish("swarm_stream", message)
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"SwarmPubSub broadcast failed: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="swarm_pubsub",
                    error_type="BROADCAST_FAILED",
                    message=str(e)[:200],
                    severity="ERROR",
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
