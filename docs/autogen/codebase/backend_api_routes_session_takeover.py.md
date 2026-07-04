# 📄 ফাইল: backend/api/routes/session_takeover.py

**প্রকার:** .py  
**সাইজ:** 3,222 বাইট  
**আপডেট:** 2026-07-04T23:38:49.184691

---

## কোড

```py
import asyncio
import base64

from fastapi import APIRouter, Query, WebSocket, WebSocketDisconnect
from loguru import logger

from database.session import get_db_session

router = APIRouter()

# Note: In production, tokens would be verified against Redis/DB
def verify_takeover_token(token: str) -> bool:
    return token.startswith("tok_")

# A 1x1 black JPEG pixel encoded in base64
MOCK_FRAME_B64 = (
    "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
)

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
            await websocket.send_json({
                "channel": "screencast",
                "data": MOCK_FRAME_B64
            })
    except asyncio.CancelledError:
        pass
    except Exception as e:
        logger.debug(f"Mock screencast emitter closed for session {session_id}: {e}")

@router.websocket("/ws/session/{session_id}/takeover")
async def takeover_session_websocket(
    websocket: WebSocket,
    session_id: str,
    token: str = Query(...)
):
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
    except Exception as e:
        logger.error(f"WebSocket takeover error: {e}")
    finally:
        emitter_task.cancel()
        if not websocket.client_state.name == "DISCONNECTED":
            await websocket.close()

```