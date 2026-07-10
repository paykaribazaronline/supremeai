import asyncio
import traceback

from .event_bus import ErrorEvent
from .event_bus import ErrorEventBus


class SelfHealerService:
    def __init__(self):
        self.event_bus = ErrorEventBus()

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
        except Exception as e:  # noqa: BLE001
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
