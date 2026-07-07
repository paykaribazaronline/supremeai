# 📄 ফাইল: backend/scripts/trigger_mock_error.py

**প্রকার:** .py  
**সাইজ:** 721 বাইট  
**আপডেট:** 2026-07-07T21:54:36.125183

---

## কোড

```py
import asyncio
import logging

# Configure logger to output to terminal
logging.basicConfig(level=logging.WARNING)

from core.event_bus import error_event_bus, ErrorEvent

async def main():
    print("Mocking an error trigger...")
    event = ErrorEvent(
        module="mock.module",
        error_type="MockError",
        message="This is a mock error to verify EventBus routing",
        severity="WARNING",
        context={"task_id": "mock_task_123"}
    )
    
    # Fire the event bus
    await error_event_bus.emit_async(event)
    
    # Wait a bit for the async listener to finish
    await asyncio.sleep(0.5)
    print("Mock error triggered successfully.")

if __name__ == "__main__":
    asyncio.run(main())

```