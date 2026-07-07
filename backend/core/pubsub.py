import asyncio
from typing import Dict, Set

class PubSub:
    def __init__(self):
        self.subscribers: Dict[str, Set[asyncio.Queue]] = {}

    def subscribe(self, channel: str) -> asyncio.Queue:
        if channel not in self.subscribers:
            self.subscribers[channel] = set()
        queue = asyncio.Queue()
        self.subscribers[channel].add(queue)
        return queue

    def unsubscribe(self, channel: str, queue: asyncio.Queue):
        if channel in self.subscribers:
            self.subscribers[channel].discard(queue)
            if not self.subscribers[channel]:
                del self.subscribers[channel]

    async def publish(self, channel: str, message: dict):
        if channel in self.subscribers:
            for queue in self.subscribers[channel]:
                await queue.put(message)

# Global Instance
global_pubsub = PubSub()
