import asyncio
import traceback

from .event_bus import ErrorEvent
from .event_bus import ErrorEventBus


class SelfHealerService:
    def __init__(self, db=None):
        self.event_bus = ErrorEventBus()
        self.db = db

    async def self_heal(self, coro, timeout: float = 30.0):
        try:
            async with asyncio.timeout(timeout):
                return await coro
        except TimeoutError:
            await self.event_bus.emit(
                ErrorEvent(
                    module="self_healer",
                    error_type="TIMEOUT",
                    message=f"Coroutine {coro.__name__} timed out after {timeout}s",
                    severity="WARNING",
                    context={"coroutine": coro.__name__, "timeout": timeout},
                )
            )
            raise
        except asyncio.CancelledError:
            await self.event_bus.emit(
                ErrorEvent(
                    module="self_healer",
                    error_type="CANCELLED",
                    message=f"Coroutine {coro.__name__} was cancelled",
                    severity="WARNING",
                    context={"coroutine": coro.__name__},
                )
            )
            raise
        except (RuntimeError, ValueError, TypeError, ConnectionError, OSError) as e:
            # সাধারণ এক্সেপশনের বদলে সুনির্দিষ্ট ত্রুটি ক্যাচ করে ইভেন্ট বাসে পাঠানো হলো
            await self.event_bus.emit(
                ErrorEvent(
                    module="self_healer",
                    error_type="ERROR",
                    message=str(e),
                    severity="ERROR",
                    context={"coroutine": coro.__name__, "traceback": traceback.format_exc()},
                )
            )
            raise
