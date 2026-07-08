import asyncio
import logging
from collections.abc import Callable
from typing import Any

from pydantic import BaseModel


logger = logging.getLogger("supremeai.event_bus")


class ErrorEvent(BaseModel):
    module: str
    error_type: str
    message: str
    severity: str  # CRITICAL, WARNING, INFO
    context: dict[str, Any]


class ErrorEventBus:
    def __init__(self):
        self._listeners: list[Callable[[ErrorEvent], asyncio.Future]] = []

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
        except asyncio.CancelledError:
            # বাংলা মন্তব্য: P1 Fix — CancelledError properly re-raised, not swallowed।
            # আগে: Exception base class-তে catch হওয়ায় CancelledError silently ignored হতো।
            raise
        except Exception as listener_exc:  # noqa: BLE001
            logger.critical(f"🔥 EventBus Listener Failed: {listener_exc}")
            # বাংলা মন্তব্য: P1 Fix — listener failure-এ backup EventBus emit করা হলো।
            # Infinite loop এড়াতে max depth check করা হলো।
            if event.module != "event_bus" and event.severity != "CRITICAL":
                try:
                    backup_event = ErrorEvent(
                        module="event_bus",
                        error_type="LISTENER_FAILURE",
                        message=f"Listener in {event.module} failed: {str(listener_exc)[:200]}",
                        severity="WARNING",
                        context={"original_module": event.module, "original_error": str(listener_exc)[:200]},
                    )
                    asyncio.create_task(self.emit_async(backup_event))
                except Exception:  # noqa: BLE001
                    pass  # Ultimate fallback — no infinite recursion


# Global Instance
error_event_bus = ErrorEventBus()
