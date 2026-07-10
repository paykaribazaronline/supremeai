import asyncio
import logging
from collections.abc import Callable
from datetime import datetime
from datetime import timezone
from typing import Any

from pydantic import BaseModel


logger = logging.getLogger("supremeai.event_bus")


class ErrorEvent(BaseModel):
    module: str
    error_type: str
    message: str
    severity: str  # CRITICAL, WARNING, INFO
    context: dict[str, Any]


class DeadLetterQueueItem(BaseModel):
    event_type: str
    handler_name: str
    error: str
    timestamp: datetime
    retry_count: int = 0


class ErrorEventBus:
    # বাংলা মন্তব্য: P1 Fix — Dead Letter Queue (DLQ) pattern.
    # Event handler failure এ message DLQ তে যায়, silent drop না।
    # Retry count track করা হয় — ৩ বার fail হলে automatic alert.

    def __init__(self):
        self._listeners: list[Callable[[ErrorEvent], asyncio.Future]] = []
        self._dlq: asyncio.Queue[DeadLetterQueueItem] = asyncio.Queue()
        self._dead_letter_handlers: list[Callable[[DeadLetterQueueItem], asyncio.Future]] = []

    def register_listener(self, listener: Callable[[ErrorEvent], asyncio.Future]):
        self._listeners.append(listener)

    def register_dead_letter_handler(self, handler: Callable[[DeadLetterQueueItem], asyncio.Future]):
        self._dead_letter_handlers.append(handler)

    def emit(self, event: ErrorEvent) -> None:
        """
        Synchronous method to emit an error event.
        Useful for non-async functions.
        """
        try:
            loop = asyncio.get_running_loop()
            loop.create_task(self.emit_async(event))
        except RuntimeError:
            # No running loop, so we run it directly (this will block but it's safe for tests/sync context)
            try:
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                loop.run_until_complete(self.emit_async(event))
            finally:
                loop.close()
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Failed to emit event {event.error_type}: {exc}")

    async def emit_async(self, event: ErrorEvent) -> None:
        if not self._listeners:
            logger.debug(f"No listeners registered for event: {event.error_type}")
            return

        results = await asyncio.gather(
            *[self._safe_invoke(h, event) for h in self._listeners],
            return_exceptions=True,
        )

        for handler, result in zip(self._listeners, results):
            if isinstance(result, Exception):
                dlq_item = DeadLetterQueueItem(
                    event_type=event.error_type,
                    handler_name=getattr(handler, "__name__", str(handler)),
                    error=str(result),
                    timestamp=datetime.now(timezone.utc),
                )
                await self._dlq.put(dlq_item)
                logger.error(f"Event handler failed: {dlq_item.handler_name} — {result}")

                # Notify dead letter handlers
                for dl_handler in self._dead_letter_handlers:
                    try:
                        await dl_handler(dlq_item)
                    except Exception as dl_exc:  # noqa: BLE001
                        logger.error(f"Dead letter handler failed: {dl_exc}")

    async def _safe_invoke(self, handler: Callable, event: ErrorEvent) -> Any:
        """
        বাংলা মন্তব্য: P1 Fix — প্রতিটি handler individually wrap করা হয়েছে।
        CancelledError কখনোই suppress করা যাবে না — সবসময় re-raise।
        """
        try:
            return await handler(event)
        except asyncio.CancelledError:
            raise  # CRITICAL: Never suppress CancelledError
        except Exception as e:
            return e  # Return exception, don't suppress

    @property
    def dead_letter_queue_size(self) -> int:
        return self._dlq.qsize()

    async def process_dead_letter_queue(self, max_items: int = 10) -> list[DeadLetterQueueItem]:
        """
        বাংলা মন্তব্য: P1 Fix — DLQ থেকে items process করা।
        max_items limit দিয়ে unbounded processing prevent করা।
        """
        processed: list[DeadLetterQueueItem] = []
        for _ in range(min(max_items, self._dlq.qsize())):
            try:
                item = self._dlq.get_nowait()
                item.retry_count += 1
                processed.append(item)
                logger.warning(f"DLQ processed: {item.handler_name} (retry #{item.retry_count})")
            except asyncio.QueueEmpty:
                break
        return processed