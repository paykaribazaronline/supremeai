# 📄 ফাইল: backend/api/routes/session_stream.py

**প্রকার:** .py  
**সাইজ:** 1,947 বাইট  
**আপডেট:** 2026-07-08T02:42:51.216394

---

## কোড

```py
import asyncio
import json

from fastapi import APIRouter
from fastapi import Path
from fastapi import Request
from sse_starlette.sse import EventSourceResponse

from core.log_batcher import batcher


router = APIRouter()

@router.get("/session/{session_id}/stream")
async def stream_session(
    request: Request,
    session_id: str = Path(..., title="The ID of the session to stream")
):
    """
    SSE endpoint for multiplexed session logs, state changes, and filetree diffs.
    Heartbeat every 15 seconds.
    """
    async def event_generator():
        queue = batcher.subscribe(session_id)
        try:
            # Send initial state or connection confirmed
            yield {
                "event": "connected",
                "data": json.dumps({"channel": "system", "data": "connected to stream"})
            }

            while True:
                if await request.is_disconnected():
                    break

                try:
                    # Wait for log event or 15s heartbeat timeout
                    item = await asyncio.wait_for(queue.get(), timeout=15.0)

                    # Decide channel based on item schema
                    channel = "logs"
                    if item.get("log_type") == "state_change":
                        channel = "state"
                    elif item.get("log_type") in ("file_write", "file_delete"):
                        channel = "filetree"

                    yield {
                        "event": "message",
                        "data": json.dumps({"channel": channel, "data": item})
                    }
                except TimeoutError:
                    # Heartbeat
                    yield {
                        "event": "ping",
                        "data": json.dumps({"channel": "heartbeat"})
                    }
        finally:
            batcher.unsubscribe(session_id, queue)

    return EventSourceResponse(event_generator())

```