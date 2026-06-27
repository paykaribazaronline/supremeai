import json
from typing import Any

from fastapi import APIRouter
from fastapi import WebSocket
from fastapi import WebSocketDisconnect
from loguru import logger


router = APIRouter(prefix="/collaborate", tags=["collaborative-editor"])


class CollaborativeEditor:
    def __init__(self):
        self.active_sessions: dict[str, dict[str, Any]] = {}
        logger.info("Initialized CollaborativeEditor")

    async def connect_client(self, session_id: str, client_id: str, websocket: WebSocket):
        await websocket.accept()
        if session_id not in self.active_sessions:
            self.active_sessions[session_id] = {
                "clients": {},
                "document_state": "",
                "ai_cursor": {"position": 0, "status": "idle"},
            }
        self.active_sessions[session_id]["clients"][client_id] = websocket
        logger.info(f"Client {client_id} connected to session {session_id}")

    async def disconnect_client(self, session_id: str, client_id: str):
        if session_id in self.active_sessions:
            clients = self.active_sessions[session_id]["clients"]
            if client_id in clients:
                del clients[client_id]
                logger.info(f"Client {client_id} disconnected from session {session_id}")
            if not clients:
                del self.active_sessions[session_id]
                logger.info(f"Session {session_id} closed as last client disconnected")

    async def broadcast(self, session_id: str, message: dict, sender_id: str | None = None):
        session = self.active_sessions.get(session_id)
        if not session:
            return

        disconnected_clients = []
        for client_id, websocket in session["clients"].items():
            if client_id == sender_id:
                continue
            try:
                await websocket.send_json(message)
            except Exception as e:
                logger.error(f"Failed to send message to {client_id}: {e}")
                disconnected_clients.append(client_id)

        for client_id in disconnected_clients:
            await self.disconnect_client(session_id, client_id)

    async def broadcast_delta(self, session_id: str, delta: dict[str, Any], sender_id: str | None = None):
        session = self.active_sessions.get(session_id)
        if not session:
            logger.warning(f"Session {session_id} not found for broadcast.")
            return

        # Basic state update for mock CRDT
        if "insert" in delta:
            pos = delta.get("position", len(session["document_state"]))
            session["document_state"] = session["document_state"][:pos] + delta["insert"] + session["document_state"][pos:]

        logger.debug(f"Broadcasting delta in {session_id}")
        await self.broadcast(session_id, {"type": "delta", "delta": delta}, sender_id)

    async def trigger_ai_edit(self, session_id: str, prompt: str, sender_id: str | None = None):
        logger.info(f"AI edit triggered in {session_id} with prompt: {prompt}")
        session = self.active_sessions.get(session_id)
        if not session:
            return {"status": "error", "error": "Session not found"}

        session["ai_cursor"]["status"] = "generating"
        await self.broadcast(session_id, {"type": "ai_cursor", "cursor": session["ai_cursor"]}, sender_id)

        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            result = await router.async_route_and_generate(prompt, task_type="coding", max_cost=0.03)
            text = result.get("text", "") if isinstance(result, dict) else ""
            if not text:
                text = "// AI generated no output"

            delta = {"insert": text, "position": len(session["document_state"])}
            session["document_state"] += text
            session["ai_cursor"]["status"] = "idle"

            await self.broadcast_delta(session_id, delta, sender_id)
            await self.broadcast(
                session_id,
                {"type": "ai_cursor", "cursor": session["ai_cursor"]},
                sender_id,
            )
            return {"status": "success", "delta": delta}
        except Exception as exc:
            session["ai_cursor"]["status"] = "idle"
            await self.broadcast(
                session_id,
                {"type": "ai_cursor", "cursor": session["ai_cursor"]},
                sender_id,
            )
            logger.error(f"AI edit failed: {exc}")
            return {"status": "error", "error": str(exc)}


editor_manager = CollaborativeEditor()


@router.websocket("/ws/{session_id}/{client_id}")
async def collaborate_ws(websocket: WebSocket, session_id: str, client_id: str):
    await editor_manager.connect_client(session_id, client_id, websocket)
    try:
        while True:
            data = await websocket.receive_text()
            try:
                message = json.loads(data)
                msg_type = message.get("type")

                if msg_type == "delta":
                    await editor_manager.broadcast_delta(session_id, message.get("delta", {}), client_id)
                elif msg_type == "ai_request":
                    prompt = message.get("prompt", "")
                    await editor_manager.trigger_ai_edit(session_id, prompt, client_id)
                elif msg_type == "cursor":
                    # Broadcast cursor position
                    await editor_manager.broadcast(
                        session_id,
                        {
                            "type": "cursor",
                            "client_id": client_id,
                            "position": message.get("position", {}),
                        },
                        client_id,
                    )
            except json.JSONDecodeError:
                logger.warning(f"Invalid JSON received from client {client_id}")
    except WebSocketDisconnect:
        await editor_manager.disconnect_client(session_id, client_id)
    except Exception as e:
        logger.error(f"WebSocket error in session {session_id} for client {client_id}: {e}")
        await editor_manager.disconnect_client(session_id, client_id)
