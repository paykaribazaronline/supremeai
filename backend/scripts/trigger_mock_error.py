import asyncio
import logging


# Configure logger to output to terminal
logging.basicConfig(level=logging.WARNING)

from core.messaging.event_bus import ErrorEvent  # noqa: E402
from core.messaging.event_bus import error_event_bus  # noqa: E402


async def main():
    print("Mocking an error trigger...")  # noqa: T201
    event = ErrorEvent(
        module="mock.module",
        error_type="MockError",
        message="This is a mock error to verify EventBus routing",
        severity="WARNING",
        context={"task_id": "mock_task_123"},
    )

    # Fire the event bus
    await error_event_bus.emit_async(event)

    # Wait a bit for the async listener to finish
    await asyncio.sleep(0.5)
    print("Mock error triggered successfully.")  # noqa: T201


if __name__ == "__main__":
    asyncio.run(main())
