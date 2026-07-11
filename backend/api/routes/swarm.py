from fastapi import APIRouter
from fastapi import Request
from sse_starlette.sse import EventSourceResponse

from core.swarm_pubsub import swarm_streamer


router = APIRouter(tags=["Swarm"])


@router.get("/stream")
async def stream_swarm_health(request: Request):
    """
    SSE Endpoint for Real-time Swarm Health & Logs.
    URL: /api/v1/swarm/stream
    """

    async def event_generator():
        try:
            async for message in swarm_streamer.subscribe():
                # যদি রিকোয়েস্ট ড্রপ হয় (যেমন ইউজার ট্যাব ক্লোজ করেছে)
                if await request.is_disconnected():
                    break
                # SSE ফরম্যাটে ডেটা পাঠানো হচ্ছে
                yield {"data": message}
        except Exception:
            pass  # Handle graceful shutdown

    return EventSourceResponse(event_generator())
