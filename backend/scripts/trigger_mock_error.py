import asyncio
import logging

from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext

# Configure logger to output to terminal
logging.basicConfig(level=logging.WARNING)

from core.messaging.event_bus import ErrorEvent, error_event_bus


@with_error_bus("main")
async def main():
    print("Mocking an error trigger...")
    event = ErrorEvent(
        module="mock.module",
        error_type="MockError",
        message="This is a mock error to verify EventBus routing",
        severity="WARNING",
        structured_context=ErrorContext(module="auto_fixed"),
        context={"task_id": "mock_task_123"},
    )

    # Fire the event bus
    await error_event_bus.emit_async(event)

    # Wait a bit for the async listener to finish
    await asyncio.sleep(0.5)
    print("Mock error triggered successfully.")


if __name__ == "__main__":
    asyncio.run(main())
