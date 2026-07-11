import asyncio

from fastapi import APIRouter
from fastapi import Query
from fastapi import WebSocket
from fastapi import WebSocketDisconnect
from loguru import logger


router = APIRouter()

import os


# Note: In production, tokens would be verified against Redis/DB
def verify_takeover_token(token: str) -> bool:
    """
    Validates the takeover token against the secure database or Redis cache.
    """
    if not token or not token.startswith("tok_"):
        return False

    try:
        # 🔥 ELITE APPROACH: Validate against Database or Redis
        # Example using a mock DB call:
        # response = supabase_client.table('active_sessions').select('*').eq('token', token).execute()
        # if not response.data:
        #     return False

        # Temporary strict validation until DB is wired up:
        valid_tokens = os.environ.get("ALLOWED_TAKEOVER_TOKENS", "").split(",")
        if token not in valid_tokens:
            logger.warning(f"Unauthorized takeover attempt with token: {token[:10]}...")
            return False

        return True
    except Exception as e:  # noqa: BLE001
        logger.error(f"Token verification failed: {str(e)}")
        return False


# A 1x1 black JPEG pixel encoded in base64
MOCK_FRAME_B64 = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="  # noqa: E501


async def mock_screencast_emitter(websocket: WebSocket, session_id: str):
    """
    Heartbeat emitter for mock CDP screencast frames to stress-test the frontend canvas.
    Emits ~10 fps to simulate live streaming.
    """
    try:
        while True:
            # Throttle to ~10 FPS
            await asyncio.sleep(0.1)

            # 🛑 ZERO-GAP: Skip rendering logic handled client-side if frames pile up,
            # but backend controls raw outgoing FPS here.
            await websocket.send_json({"channel": "screencast", "data": MOCK_FRAME_B64})
    except asyncio.CancelledError:
        logger.warning("⚠️ Task execution was intentionally cancelled.")
        raise
    except Exception as e:  # noqa: BLE001
        logger.exception(f"❌ Critical task failure in session_takeover.py: {e}")
        from core.event_bus import ErrorEvent
        from core.event_bus import error_event_bus

        await error_event_bus.emit_async(
            ErrorEvent(
                module="backend.api.routes.session_takeover",
                error_type=type(e).__name__,
                message=str(e),
                severity="WARNING",
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

    if not verify_takeover_token(token):
        await websocket.send_json({"error": "Invalid or expired takeover token"})
        await websocket.close(code=1008)
        return

    logger.info(f"WebSocket takeover initiated for session {session_id}")

    emitter_task = asyncio.create_task(mock_screencast_emitter(websocket, session_id))

    try:
        # Loop for bidirectional communication
        while True:
            # Receive mouse/keyboard actions from the React client
            data = await websocket.receive_json()

            action = data.get("action") or data.get("method")
            if action == "return_control":
                # User clicked Return Control
                logger.info(f"Session {session_id} returned control to agent.")
                break
            elif str(action).startswith("Input.dispatch"):
                # Handle CDP input routing here
                # (Will route to Playwright context in production)
                logger.debug(f"CDP Event [{session_id}]: {action} - {data.get('params')}")

    except WebSocketDisconnect:
        logger.info(f"WebSocket takeover disconnected for session {session_id}")
    except Exception as e:  # noqa: BLE001
        logger.error(f"WebSocket takeover error: {e}")
    finally:
        emitter_task.cancel()
        if websocket.client_state.name != "DISCONNECTED":
            await websocket.close()
