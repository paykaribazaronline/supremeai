# 📄 ফাইল: backend/api/routes/events.py

**প্রকার:** .py  
**সাইজ:** 2,492 বাইট  
**আপডেট:** 2026-07-07T21:54:36.130786

---

## কোড

```py
import asyncio
import json
from fastapi import APIRouter, Request
from sse_starlette.sse import EventSourceResponse
from core.pubsub import global_pubsub

router = APIRouter(tags=["Events"])

@router.get("/dashboard/stream")
async def dashboard_stream(request: Request):
    """
    SSE endpoint for dashboard metrics and events.
    Yields data when published to 'dashboard_events' channel.
    Maintains connection with a 20s heartbeat.
    """
    async def event_generator():
        # Subscribe to the required channels
        dashboard_queue = global_pubsub.subscribe("dashboard_events")
        metrics_queue = global_pubsub.subscribe("metrics_events")
        tasks_queue = global_pubsub.subscribe("browser_tasks")
        
        try:
            while True:
                # Wait for an event or a heartbeat timeout (20s)
                # Using asyncio.wait to race between the queues and the heartbeat timeout
                dashboard_task = asyncio.create_task(dashboard_queue.get())
                metrics_task = asyncio.create_task(metrics_queue.get())
                tasks_task = asyncio.create_task(tasks_queue.get())
                
                done, pending = await asyncio.wait(
                    [dashboard_task, metrics_task, tasks_task],
                    timeout=20,
                    return_when=asyncio.FIRST_COMPLETED
                )
                
                if not done:
                    # Heartbeat
                    yield {
                        "event": "ping",
                        "data": ""
                    }
                else:
                    for task in done:
                        result = task.result()
                        # Assuming the result is a dict with 'type' and 'payload'
                        yield {
                            "event": result.get("type", "message"),
                            "data": json.dumps(result.get("payload", {}))
                        }
                        
                for t in pending:
                    t.cancel()
                    
                # If client disconnected, break
                if await request.is_disconnected():
                    break
        finally:
            global_pubsub.unsubscribe("dashboard_events", dashboard_queue)
            global_pubsub.unsubscribe("metrics_events", metrics_queue)
            global_pubsub.unsubscribe("browser_tasks", tasks_queue)

    return EventSourceResponse(event_generator())

```