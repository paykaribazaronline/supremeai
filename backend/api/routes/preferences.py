import asyncio
import json

from fastapi import APIRouter, Depends, HTTPException, Query, Request
from loguru import logger
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

from api.dependencies import get_current_user_token
from core.messaging.pubsub import global_pubsub as theme_pubsub
from database.supabase_client import db

router = APIRouter(
    prefix="/preferences",
    tags=["preferences"],
    dependencies=[Depends(get_current_user_token)],
)


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
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.post("/")
async def upsert_preferences(payload: PreferenceUpdate, user_id: str = Query(default="default")):
    data = payload.dict(exclude_none=True)
    if not data:
        raise HTTPException(status_code=400, detail="No fields to update")

    # ADVANCED: Record preference change signal in AdaptiveEngine LearningLoop
    suggestions: list[dict] = []
    try:
        from adaptive_engine.intent_parser import IntentParser
        from adaptive_engine.learning_loop import LearningLoop

        loop = LearningLoop.get_instance()
        context = await IntentParser.extract_context(payload)
        await loop.record_signal(
            user_id=user_id,
            signal_type="preference_change",
            payload=data,
            context=context,
        )
        suggestions = await loop.suggest(user_id=user_id)
    except Exception as e:
        logger.warning(f"[Preferences] AdaptiveEngine signal recording skipped: {e}")

    if not db.client:
        # For offline/local mode, still broadcast the theme
        if payload.theme:
            await theme_pubsub.publish(user_id, {"theme": payload.theme})
        return {
            "status": "success",
            "preferences": data,
            "adaptive_suggestions": suggestions,
        }

    data["user_id"] = user_id
    try:
        res = db.client.table("user_preferences").upsert(data).execute()
        if payload.theme:
            await theme_pubsub.publish(user_id, {"theme": payload.theme})
        pref_res = res.data[0] if res.data else data
        return {
            "status": "success",
            "preferences": pref_res,
            "adaptive_suggestions": suggestions,
        }
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.get("/{user_id}/stream")
async def stream_preferences(request: Request, user_id: str):
    """
    SSE endpoint to listen for real-time theme and preference updates for a specific user.
    """

    async def event_generator():
        queue = await theme_pubsub.subscribe(user_id)
        try:
            # Yield connection success
            yield {
                "event": "connected",
                "data": json.dumps({"status": "connected to theme stream"}),
            }

            while True:
                if await request.is_disconnected():
                    break
                try:
                    # Wait for theme change or 15s heartbeat
                    item = await asyncio.wait_for(queue.get(), timeout=15.0)
                    yield {"event": "message", "data": json.dumps(item)}
                except TimeoutError:
                    # Heartbeat ping
                    yield {
                        "event": "ping",
                        "data": json.dumps({"channel": "heartbeat"}),
                    }
        finally:
            await theme_pubsub.unsubscribe(user_id, queue)

    return EventSourceResponse(event_generator())
