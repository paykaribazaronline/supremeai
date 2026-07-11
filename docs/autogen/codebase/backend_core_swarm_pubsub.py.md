# 📄 ফাইল: backend/core/swarm_pubsub.py

**প্রকার:** .py  
**সাইজ:** 1,697 বাইট  
**আপডেট:** 2026-07-11T14:23:58.573400

---

## কোড

```py
import asyncio
import json
import logging
from collections.abc import AsyncGenerator

import redis.asyncio as redis


logger = logging.getLogger(__name__)


class SwarmPubSub:
    def __init__(self):
        # Use redis.asyncio for modern redis-py
        self.redis = redis.from_url("redis://localhost")

    async def subscribe(self) -> AsyncGenerator[str, None]:
        """নতুন ক্লায়েন্টের জন্য Redis চ্যানেল সাবস্ক্রাইব করবে (Multi-Worker Safe)"""
        pubsub = self.redis.pubsub()
        await pubsub.subscribe("swarm_stream")
        logger.info("New client subscribed to Redis Swarm Stream.")

        try:
            while True:
                message = await pubsub.get_message(ignore_subscribe_messages=True, timeout=1.0)
                if message is not None:
                    yield message["data"].decode("utf-8")
                await asyncio.sleep(0.01)
        except asyncio.CancelledError:
            logger.info("Client disconnected from Redis Swarm Stream.")
            await pubsub.unsubscribe("swarm_stream")
            await pubsub.close()
            raise

    async def broadcast(self, event_type: str, payload: dict):
        """সকল অ্যাক্টিভ ক্লায়েন্টকে Redis চ্যানেলে ডেটা পুশ করবে"""
        message = json.dumps({"type": event_type, "data": payload})
        await self.redis.publish("swarm_stream", message)


# গ্লোবাল ইন্সট্যান্স যা পুরো অ্যাপ জুড়ে ব্যবহার হবে
swarm_streamer = SwarmPubSub()

```