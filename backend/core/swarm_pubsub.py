import asyncio
import json
from typing import AsyncGenerator
import logging

logger = logging.getLogger(__name__)

class SwarmPubSub:
    def __init__(self):
        # প্রতিটি ক্লায়েন্টের জন্য একটি করে asyncio.Queue
        self.active_connections: list[asyncio.Queue] = []

    async def subscribe(self) -> AsyncGenerator[str, None]:
        """নতুন ক্লায়েন্টের জন্য একটি কিউ তৈরি করে স্ট্রিম শুরু করবে"""
        queue = asyncio.Queue()
        self.active_connections.append(queue)
        logger.info(f"New client connected to Swarm Stream. Total: {len(self.active_connections)}")
        
        try:
            while True:
                # কিউ থেকে ডেটা নিয়ে ইয়েল্ড (yield) করবে
                data = await queue.get()
                yield data
        except asyncio.CancelledError:
            # ক্লায়েন্ট ডিসকানেক্ট করলে কিউ রিমুভ করে মেমরি ক্লিনআপ করবে
            self.active_connections.remove(queue)
            logger.info(f"Client disconnected. Total: {len(self.active_connections)}")
            raise

    async def broadcast(self, event_type: str, payload: dict):
        """সকল অ্যাক্টিভ ক্লায়েন্টকে রিয়েল-টাইম ডেটা পুশ করবে"""
        if not self.active_connections:
            return # কোনো ক্লায়েন্ট না থাকলে অযথাই ইভেন্ট পুশ করবে না
            
        message = json.dumps({"type": event_type, "data": payload})
        for queue in self.active_connections:
            await queue.put(message)

# গ্লোবাল ইন্সট্যান্স যা পুরো অ্যাপ জুড়ে ব্যবহার হবে
swarm_streamer = SwarmPubSub()
