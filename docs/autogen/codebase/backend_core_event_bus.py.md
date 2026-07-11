# 📄 ফাইল: backend/core/event_bus.py

**প্রকার:** .py  
**সাইজ:** 10,890 বাইট  
**আপডেট:** 2026-07-11T11:32:06.961064

---

## কোড

```py
# backend/core/event_bus.py
# বাংলা মন্তব্য: সম্পূর্ণ রি-ফ্যাক্টর — Observable Anti-Suppression Error Pipeline।
# প্রতিটি error structured context (user_id, task_id, request_id) সহ emit হয়।
# CancelledError কখনো suppress হয় না — সবসময় re-raise।
# Dead Letter Queue bounded (maxsize=1000) — unbounded growth নিষিদ্ধ।
# Listener failure → DLQ — silent drop সম্পূর্ণ নিষিদ্ধ।

import asyncio
import logging
from collections.abc import Callable
from datetime import UTC
from datetime import datetime
from typing import Any

from pydantic import BaseModel
from pydantic import Field


logger = logging.getLogger("supremeai.event_bus")


class ErrorContext(BaseModel):
    """
    বাংলা মন্তব্য: প্রতিটি error event-এ এই structured context থাকবে।
    Generic 'module' মাত্র নয় — user_id, task_id, request_id সহ।
    এটি production debugging-এ correlation করার জন্য অপরিহার্য।
    """

    module: str
    # বাংলা মন্তব্য: Correlation IDs — production debugging-এ অপরিহার্য
    user_id: str | None = None
    task_id: str | None = None
    request_id: str | None = None
    # বাংলা মন্তব্য: কোন env-এ ঘটলো — staging vs production আলাদাভাবে alert হবে
    env: str = "unknown"
    extra: dict[str, Any] = Field(default_factory=dict)


class ErrorEvent(BaseModel):
    """
    বাংলা মন্তব্য: ErrorEventBus-এর primary data model।
    severity levels: CRITICAL > ERROR > WARNING > INFO
    structured_context দিয়ে correlation ID track করা যায়।
    """

    module: str
    error_type: str
    message: str
    severity: str  # CRITICAL, ERROR, WARNING, INFO
    context: dict[str, Any] = Field(default_factory=dict)
    # বাংলা মন্তব্য: structured context — flat dict-এর পাশাপাশি type-safe correlation
    structured_context: ErrorContext | None = None
    timestamp: datetime = Field(default_factory=lambda: datetime.now(UTC))


class DeadLetterQueueItem(BaseModel):
    """বাংলা মন্তব্য: Handler failure-এ এই item DLQ-তে যায়। Silent drop নিষিদ্ধ।"""

    event_type: str
    handler_name: str
    error: str
    timestamp: datetime
    retry_count: int = 0
    original_event: ErrorEvent | None = None


class ErrorEventBus:
    """
    বাংলা মন্তব্য: Central Observable Error Pipeline।
    কোনো silent drop নেই। প্রতিটি failure DLQ-তে যায়।
    CancelledError সবসময় re-raise হয়।
    Structured logging — severity অনুযায়ী log level।
    Bounded DLQ — maxsize=1000 (অতিরিক্ত item-এ critical alert)।
    """

    def __init__(self) -> None:
        self._listeners: list[Callable[[ErrorEvent], Any]] = []
        # বাংলা মন্তব্য: bounded queue — unbounded growth prevent করা হলো
        self._dlq: asyncio.Queue[DeadLetterQueueItem] = asyncio.Queue(maxsize=1000)
        self._dead_letter_handlers: list[Callable[[DeadLetterQueueItem], Any]] = []
        self._total_emitted: int = 0
        self._total_dlq_items: int = 0

    def register_listener(self, listener: Callable[[ErrorEvent], Any]) -> None:
        """বাংলা মন্তব্য: Error event listener register করুন।"""
        self._listeners.append(listener)
        logger.debug(f"[ErrorEventBus] Registered listener: {getattr(listener, '__name__', str(listener))}")

    def register_dead_letter_handler(self, handler: Callable[[DeadLetterQueueItem], Any]) -> None:
        """বাংলা মন্তব্য: DLQ handler register করুন — handler failure alert পাঠাতে।"""
        self._dead_letter_handlers.append(handler)

    def emit(self, event: ErrorEvent) -> None:
        """
        বাংলা মন্তব্য: Synchronous emit — async context ছাড়াও call করা যাবে।
        Structured log সবসময় emit হয় — listener failure-তেও।
        """
        # বাংলা মন্তব্য: listener failure নির্বিশেষে structured log সবসময় emit হয়
        self._log_event(event)
        self._total_emitted += 1

        try:
            loop = asyncio.get_running_loop()
            loop.create_task(self._dispatch_async(event))
        except RuntimeError:
            # বাংলা মন্তব্য: running loop নেই — sync context (tests, scripts)।
            # নতুন loop তৈরি করা হয় না — thread safety issue এড়াতে।
            logger.debug(f"[ErrorEventBus] No running loop for async dispatch of '{event.error_type}'. Sync log completed.")

    async def emit_async(self, event: ErrorEvent) -> None:
        """বাংলা মন্তব্য: Async context-এ সরাসরি call করার জন্য।"""
        self._log_event(event)
        self._total_emitted += 1
        await self._dispatch_async(event)

    async def _dispatch_async(self, event: ErrorEvent) -> None:
        """বাংলা মন্তব্য: সব listeners-এ concurrent dispatch। Individual failure isolation।"""
        if not self._listeners:
            return

        results = await asyncio.gather(
            *[self._safe_invoke(handler, event) for handler in self._listeners],
            return_exceptions=True,
        )

        for handler, result in zip(self._listeners, results, strict=False):
            if isinstance(result, BaseException):
                handler_name = getattr(handler, "__name__", str(handler))
                dlq_item = DeadLetterQueueItem(
                    event_type=event.error_type,
                    handler_name=handler_name,
                    error=str(result),
                    timestamp=datetime.now(UTC),
                    original_event=event,
                )
                try:
                    self._dlq.put_nowait(dlq_item)
                    self._total_dlq_items += 1
                except asyncio.QueueFull:
                    # বাংলা মন্তব্য: DLQ full হলে drop এবং critical log — silent নয়
                    logger.critical(
                        f"[ErrorEventBus] DLQ full! Dropping item for handler: '{handler_name}'. DLQ capacity exceeded — check stuck listeners."
                    )

                logger.error(f"[ErrorEventBus] Handler '{handler_name}' failed for event '{event.error_type}': {result}")

                for dl_handler in self._dead_letter_handlers:
                    try:
                        await dl_handler(dlq_item)
                    except asyncio.CancelledError:
                        # বাংলা মন্তব্য: CancelledError কখনো suppress করা যাবে না
                        raise
                    except Exception as dl_exc:  # noqa: BLE001
                        logger.error(f"[ErrorEventBus] Dead letter handler failed: {dl_exc}")

    async def _safe_invoke(self, handler: Callable, event: ErrorEvent) -> Any:
        """
        বাংলা মন্তব্য: প্রতিটি handler individually isolate করা।
        CancelledError re-raise — graceful shutdown নিশ্চিত।
        """
        try:
            result = handler(event)
            if asyncio.iscoroutine(result):
                return await result
            return result
        except asyncio.CancelledError:
            logger.warning(f"[ErrorEventBus] CancelledError in handler '{getattr(handler, '__name__', str(handler))}' — re-raising.")
            raise  # CRITICAL: CancelledError কখনো suppress করা যাবে না
        except Exception as exc:  # noqa: BLE001
            return exc  # exception return করা হচ্ছে, suppress নয়

    def _log_event(self, event: ErrorEvent) -> None:
        """
        বাংলা মন্তব্য: Structured logging — severity অনুযায়ী log level।
        context সহ — user_id, task_id, request_id correlation।
        """
        ctx = event.context.copy()
        if event.structured_context:
            ctx.update(
                {
                    "user_id": event.structured_context.user_id,
                    "task_id": event.structured_context.task_id,
                    "request_id": event.structured_context.request_id,
                    "env": event.structured_context.env,
                }
            )

        log_msg = f"[{event.module}] {event.error_type}: {event.message[:500]} | ctx={ctx}"

        if event.severity == "CRITICAL":
            logger.critical(log_msg)
        elif event.severity == "ERROR":
            logger.error(log_msg)
        elif event.severity == "WARNING":
            logger.warning(log_msg)
        else:
            logger.info(log_msg)

    @property
    def dead_letter_queue_size(self) -> int:
        return self._dlq.qsize()

    @property
    def stats(self) -> dict[str, int]:
        """বাংলা মন্তব্য: Event bus statistics — admin dashboard-এ expose করা যাবে।"""
        return {
            "total_emitted": self._total_emitted,
            "total_dlq_items": self._total_dlq_items,
            "dlq_current_size": self._dlq.qsize(),
            "registered_listeners": len(self._listeners),
        }

    async def process_dead_letter_queue(self, max_items: int = 10) -> list[DeadLetterQueueItem]:
        """
        বাংলা মন্তব্য: DLQ থেকে items process করা।
        max_items bounded — unbounded processing prevent।
        """
        processed: list[DeadLetterQueueItem] = []
        for _ in range(min(max_items, self._dlq.qsize())):
            try:
                item = self._dlq.get_nowait()
                item.retry_count += 1
                processed.append(item)
                logger.warning(f"[ErrorEventBus] DLQ processed: '{item.handler_name}' (retry #{item.retry_count}) | event: {item.event_type}")
            except asyncio.QueueEmpty:
                break
        return processed


# বাংলা মন্তব্য: Module-level singleton — সিস্টেমে একটিই ErrorEventBus instance থাকবে।
error_event_bus: ErrorEventBus = ErrorEventBus()

```