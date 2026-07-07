# 📄 ফাইল: backend/core/event_bus.py

**প্রকার:** .py  
**সাইজ:** 2,394 বাইট  
**আপডেট:** 2026-07-07T21:29:49.045460

---

## কোড

```py
import asyncio
import logging
from pydantic import BaseModel
from typing import Dict, Any, Callable, List

logger = logging.getLogger("supremeai.event_bus")

class ErrorEvent(BaseModel):
    module: str
    error_type: str
    message: str
    severity: str  # CRITICAL, WARNING, INFO
    context: Dict[str, Any]

class ErrorEventBus:
    def __init__(self):
        self._listeners: List[Callable[[ErrorEvent], asyncio.Future]] = []

    def register_listener(self, listener: Callable[[ErrorEvent], asyncio.Future]):
        self._listeners.append(listener)

    def emit(self, event: ErrorEvent) -> None:
        """
        Synchronous method to emit an error event.
        Useful for non-async functions.
        """
        try:
            loop = asyncio.get_running_loop()
            loop.create_task(self.emit_async(event))
        except RuntimeError:
            # No running loop, so we run it directly (this will block but it's safe for tests/sync)
            asyncio.run(self.emit_async(event))

    async def emit_async(self, event: ErrorEvent) -> None:
        """
        সিস্টেমের যেকোনো প্রান্ত থেকে এরর ইভেন্ট ফায়ার করার সেন্ট্রাল মেথড।
        এটি সম্পূর্ণ অ-ব্লকিং (Non-blocking) উপায়ে ব্যাকগ্রাউন্ডে লিসেনারদের এক্সিকিউট করবে।
        """
        logger.warning(f"🚨 [EventBus] New Error Event emitted from {event.module} ({event.severity})")
        
        # ব্যাকগ্রাউন্ড টাস্ক হিসেবে লিসেনারদের ফায়ার করা হচ্ছে যাতে মেইন থ্রেড ব্লক না হয়
        for listener in self._listeners:
            asyncio.create_task(self._safe_execute_listener(listener, event))

    async def _safe_execute_listener(self, listener: Callable[[ErrorEvent], Any], event: ErrorEvent):
        try:
            if asyncio.iscoroutinefunction(listener):
                await listener(event)
            else:
                listener(event)
        except Exception as listener_exc:
            logger.critical(f"🔥 EventBus Listener Failed: {listener_exc}")

# Global Instance
error_event_bus = ErrorEventBus()

```