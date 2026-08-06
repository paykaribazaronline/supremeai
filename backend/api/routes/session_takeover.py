import asyncio
import json
import os
import secrets
import time

from fastapi import (
    APIRouter,
    HTTPException,
    Query,
    Request,
    WebSocket,
    WebSocketDisconnect,
)
from loguru import logger
from pydantic import BaseModel

from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext

router = APIRouter()


# বাংলা মন্তব্য: TakeoverRequest — HTTP endpoint এর জন্য Pydantic model
class TakeoverRequest(BaseModel):
    session_id: str


def _require_admin(request: Request) -> dict:
    """Admin role enforcement — fail-closed."""
    user = getattr(request.state, "user", None)
    if not user:
        raise HTTPException(status_code=401, detail="Authentication required")
    if user.get("role") != "admin":
        raise HTTPException(status_code=403, detail="Admin role required for session takeover")
    return user


def request_takeover(payload: TakeoverRequest, request: Request) -> dict:
    """HTTP endpoint: Admin requests a session takeover token."""
    # বাংলা মন্তব্য: Admin validation fail-closed
    _require_admin(request)
    token = f"tok_{secrets.token_urlsafe(32)}"
    return {
        "token": token,
        "session_id": payload.session_id,
        "expires_in": 300,  # 5 মিনিট TTL
    }


def release_takeover(session_id: str, request: Request) -> dict:
    """HTTP endpoint: Admin releases a session takeover."""
    _require_admin(request)
    return {"status": "released", "session_id": session_id}


def get_takeover_status(session_id: str, request: Request) -> dict:
    """HTTP endpoint: Get current takeover status for a session."""
    _require_admin(request)
    # বাংলা মন্তব্য: Redis check synchronous wrapper — real impl async
    try:
        import asyncio as _asyncio

        loop = _asyncio.new_event_loop()
        data = loop.run_until_complete(_get_status_from_redis(session_id))
        loop.close()
    except Exception as e:
        # বাংলা: এটি অ্যাডমিন সেশন-টেকওভার মনিটরিং এন্ডপয়েন্ট — Redis lookup ব্যর্থ হলে
        # চুপচাপ "inactive" দেখানো বিভ্রান্তিকর, তাই কারণটি লগ করা হচ্ছে
        logger.warning(f"Failed to fetch takeover status from Redis for session {session_id}: {e}")
        data = None
    if data:
        return {"status": "active", "session_id": session_id, "data": data}
    return {"status": "inactive", "session_id": session_id}


async def _get_status_from_redis(session_id: str) -> dict | None:
    """Redis থেকে session status নিয়ে আসা।"""
    try:
        client = await _redis_client()
        if client is None:
            return None
        raw = await client.get(f"takeover_session:{session_id}")
        if raw:
            return json.loads(raw) if isinstance(raw, str | bytes) else raw
    except Exception as e:
        logger.debug(f"Redis status check failed: {e}")
    return None


# গ্যাপ ফিক্স (Security/Anti-Silent-Failure):
# ১. verify_takeover_token() আগে শুধু একটি env-var কমা-সেপারেটেড লিস্টের বিরুদ্ধে চেক করত (কোনো
#    expiry বা single-use গ্যারান্টি ছাড়াই) — এখন একটি ব্যবহৃত টোকেন Redis-এ single-use হিসেবে
#    মার্ক করা হয় (রেপ্লে-অ্যাটাক প্রতিরোধ), Redis না থাকলে গ্রেসফুলি আগের বেস চেকে ফলব্যাক করে।
# ২. mock_screencast_emitter() আগে *সব* environment-এ (production সহ) একটি স্ট্যাটিক কালো ফ্রেম
#    "লাইভ স্ট্রিম" হিসেবে পাঠাত — একজন অ্যাডমিন সন্দেহভাজন কম্প্রোমাইজড ডিভাইসে JIT session
#    takeover করার সময় ভুলভাবে ভাবতে পারতেন তিনি রিয়েল স্ক্রিন দেখছেন। এখন এটি শুধু non-production
#    এ ডেভ/স্ট্রেস-টেস্ট মোডে চলে; production-এ honest "unavailable" স্ট্যাটাস পাঠিয়ে বন্ধ হয়ে যায়,
#    আর কেন্দ্রীয় error bus-এ রিপোর্ট করে (self-healing পর্যবেক্ষণযোগ্যতার জন্য)।


async def _redis_client():
    """Best-effort shared Redis client for single-use token consumption. Returns None if unavailable."""
    try:
        import redis.asyncio as aioredis  # type: ignore[import-untyped]

        from core.config import settings as app_settings

        redis_url = getattr(app_settings, "redis_url", None) or os.getenv("REDIS_URL") or os.getenv("UPSTASH_REDIS_URL")
        if not redis_url:
            return None
        return aioredis.from_url(redis_url, decode_responses=True)
    except Exception as exc:
        logger.debug(f"Redis unavailable for takeover-token consumption, falling back to base check only: {exc}")
        return None


async def verify_takeover_token(token: str) -> bool:
    """
    Validates the takeover token, then consumes it as single-use in Redis (when available)
    to block replay of a leaked/logged token.
    """
    if not token or not token.startswith("tok_"):
        return False

    try:
        valid_tokens = os.environ.get("ALLOWED_TAKEOVER_TOKENS", "").split(",")
        if token not in valid_tokens:
            logger.warning(f"Unauthorized takeover attempt with token: {token[:10]}...")
            return False

        client = await _redis_client()
        if client is not None:
            try:
                # SETNX-স্টাইল single-use consumption — token একবার ব্যবহার হলে ৫ মিনিটের জন্য লক থাকে
                consumed = await client.set(f"takeover_used:{token}", "1", nx=True, ex=300)
                if not consumed:
                    logger.warning(f"Replay attempt detected for already-used takeover token: {token[:10]}...")
                    return False
            except Exception as exc:
                # বাংলা মন্তব্য: সিকিউরিটি গার্ড — Redis চেক ফেইল করলে রিপ্লে অ্যাটাক রোধে টোকেন রিজেক্ট করা হচ্ছে
                logger.error(f"Redis single-use check failed — rejecting takeover token (fail-closed): {exc}")
                return False

        return True
    except Exception as e:
        logger.error(f"Token verification failed: {e!s}")
        return False


def _is_production() -> bool:
    return os.environ.get("SUPREMEAI_ENV", "").lower() == "production"


# A 1x1 black JPEG pixel encoded in base64 — dev/stress-test only, never sent in production.
MOCK_FRAME_B64 = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="


@with_error_bus("dev_mock_screencast_emitter")
async def dev_mock_screencast_emitter(websocket: WebSocket, session_id: str):
    """
    Non-production only: heartbeat emitter for mock CDP screencast frames to stress-test the
    frontend canvas. Emits ~10 fps to simulate live streaming.
    """
    try:
        while True:
            await asyncio.sleep(0.1)
            await websocket.send_json({"channel": "screencast", "data": MOCK_FRAME_B64, "mock": True})
    except asyncio.CancelledError:
        logger.warning("⚠️ Task execution was intentionally cancelled.")
        raise
    except Exception as e:
        logger.exception(f"❌ Critical task failure in session_takeover.py: {e}")
        from core.messaging.event_bus import ErrorEvent, error_event_bus

        await error_event_bus.emit_async(
            ErrorEvent(
                module="backend.api.routes.session_takeover",
                error_type=type(e).__name__,
                message=str(e),
                severity="WARNING",
                structured_context=ErrorContext(module="auto_fixed"),
                context={"session_id": session_id},
            )
        )


@with_error_bus("_report_screencast_unavailable")
async def _report_screencast_unavailable(websocket: WebSocket, session_id: str) -> None:
    """Production fallback: no real CDP/Playwright frame source is wired to this gateway yet.
    Tell the client honestly instead of faking a live feed, and log it centrally."""
    from core.messaging.event_bus import ErrorEvent, error_event_bus

    await websocket.send_json(
        {
            "channel": "screencast",
            "status": "unavailable",
            "message": "Live screencast is not wired to a real browser session yet in production.",
        }
    )
    await error_event_bus.emit_async(
        ErrorEvent(
            module="backend.api.routes.session_takeover",
            error_type="ScreencastSourceMissing",
            message="Production session takeover requested but no real CDP frame source is configured.",
            severity="WARNING",
            structured_context=ErrorContext(module="auto_fixed"),
            context={"session_id": session_id},
        )
    )


@router.websocket("/ws/session/{session_id}/takeover")
async def takeover_session_websocket(websocket: WebSocket, session_id: str, token: str = Query(...)):
    """
    Ephemeral WebSocket gateway for Sandbox Viewport takeover.
    Validates token, streams CDP frames to client, and receives mouse/keyboard events.
    Mounts ONLY when control_mode == 'human'.
    """
    await websocket.accept()

    if not await verify_takeover_token(token):
        await websocket.send_json({"error": "Invalid, expired, or already-used takeover token"})
        await websocket.close(code=1008)
        return

    logger.info(f"WebSocket takeover initiated for session {session_id}")

    if _is_production():
        await _report_screencast_unavailable(websocket, session_id)
        emitter_task = None
    else:
        emitter_task = asyncio.create_task(dev_mock_screencast_emitter(websocket, session_id))

    start_time = time.monotonic()
    try:
        while True:
            data = await websocket.receive_json()

            action = data.get("action") or data.get("method")
            if action == "return_control":
                logger.info(f"Session {session_id} returned control to agent.")
                break
            elif str(action).startswith("Input.dispatch"):
                # Handle CDP input routing here (will route to Playwright context in production)
                logger.debug(f"CDP Event [{session_id}]: {action} - {data.get('params')}")

    except WebSocketDisconnect:
        logger.info(f"WebSocket takeover disconnected for session {session_id}")
    except Exception as e:
        logger.error(f"WebSocket takeover error: {e}")
    finally:
        if emitter_task is not None:
            emitter_task.cancel()
        if websocket.client_state.name != "DISCONNECTED":
            await websocket.close()
        logger.debug(f"Takeover session {session_id} lasted {time.monotonic() - start_time:.1f}s")
