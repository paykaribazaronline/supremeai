# 📄 ফাইল: backend/api/routes/preferences.py

**প্রকার:** .py  
**সাইজ:** 3,494 বাইট  
**আপডেট:** 2026-07-11T13:53:46.541547

---

## কোড

```py
import asyncio
import json

from fastapi import APIRouter
from fastapi import HTTPException
from fastapi import Query
from fastapi import Request
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from core.theme_pubsub import theme_pubsub
from database.supabase_client import db


router = APIRouter(prefix="/preferences", tags=["preferences"])


class PreferenceUpdate(BaseModel):
    theme: str | None = None
    default_model: str | None = None
    max_tokens: int | None = None
    auto_save: bool | None = None
    custom_shortcuts: dict | None = None
    verbosity: str | None = None
    preferred_frameworks: list[str] | None = None


@router.get("/")
async def get_preferences(user_id: str = Query(default="default")):
    if not db.client:
        return {
            "user_id": user_id,
            "theme": "dark",
            "default_model": "gpt-4o",
            "max_tokens": 4096,
            "auto_save": True,
            "custom_shortcuts": {},
        }
    try:
        res = db.client.table("user_preferences").select("*").eq("user_id", user_id).execute()
        rows = res.data or []
        if rows:
            return rows[0]
        return {
            "user_id": user_id,
            "theme": "dark",
            "default_model": "gpt-4o",
            "max_tokens": 4096,
            "auto_save": True,
            "custom_shortcuts": {},
        }
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/")
async def upsert_preferences(user_id: str = Query(default="default"), payload: PreferenceUpdate = ...):
    if not db.client:
        # For offline/local mode, still broadcast the theme
        if payload.theme:
            theme_pubsub.publish(user_id, payload.theme)
        return {"status": "success", "preferences": payload.dict(exclude_none=True)}
    data = payload.dict(exclude_none=True)
    if not data:
        raise HTTPException(status_code=400, detail="No fields to update")
    data["user_id"] = user_id
    try:
        res = db.client.table("user_preferences").upsert(data).execute()
        if payload.theme:
            theme_pubsub.publish(user_id, payload.theme)
        return {"status": "success", "preferences": res.data[0] if res.data else data}
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.get("/{user_id}/stream")
async def stream_preferences(request: Request, user_id: str):
    """
    SSE endpoint to listen for real-time theme and preference updates for a specific user.
    """

    async def event_generator():
        queue = theme_pubsub.subscribe(user_id)
        try:
            # Yield connection success
            yield {"event": "connected", "data": json.dumps({"status": "connected to theme stream"})}

            while True:
                if await request.is_disconnected():
                    break
                try:
                    # Wait for theme change or 15s heartbeat
                    item = await asyncio.wait_for(queue.get(), timeout=15.0)
                    yield {"event": "message", "data": json.dumps(item)}
                except TimeoutError:
                    # Heartbeat ping
                    yield {"event": "ping", "data": json.dumps({"channel": "heartbeat"})}
        finally:
            theme_pubsub.unsubscribe(user_id, queue)

    return EventSourceResponse(event_generator())

```