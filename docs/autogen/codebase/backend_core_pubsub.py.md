# 📄 ফাইল: backend/core/pubsub.py

**প্রকার:** .py  
**সাইজ:** 846 বাইট  
**আপডেট:** 2026-07-08T03:11:56.323537

---

## কোড

```py
import asyncio


class PubSub:
    def __init__(self):
        self.subscribers: dict[str, set[asyncio.Queue]] = {}

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

```