# 📄 ফাইল: backend/core/theme_pubsub.py

**প্রকার:** .py  
**সাইজ:** 1,315 বাইট  
**আপডেট:** 2026-07-11T11:32:06.960913

---

## কোড

```py
import asyncio
import contextlib

from loguru import logger


class ThemePubSub:
    """
    In-memory PubSub for synchronizing theme preferences across connected clients (Web, Mobile).
    """

    def __init__(self):
        # user_id -> list of asyncio.Queue
        self._subscribers: dict[str, list[asyncio.Queue]] = {}

    def subscribe(self, user_id: str) -> asyncio.Queue:
        if user_id not in self._subscribers:
            self._subscribers[user_id] = []
        q = asyncio.Queue()
        self._subscribers[user_id].append(q)
        return q

    def unsubscribe(self, user_id: str, q: asyncio.Queue):
        if user_id in self._subscribers:
            with contextlib.suppress(ValueError):
                self._subscribers[user_id].remove(q)
            if not self._subscribers[user_id]:
                del self._subscribers[user_id]

    def publish(self, user_id: str, theme: str):
        """Publish a theme change to all connected clients for a specific user."""
        if user_id in self._subscribers:
            logger.info(f"Publishing theme update '{theme}' for user '{user_id}' to {len(self._subscribers[user_id])} clients.")
            for q in self._subscribers[user_id]:
                q.put_nowait({"event": "theme_changed", "theme": theme})


theme_pubsub = ThemePubSub()

```