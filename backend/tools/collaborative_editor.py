import asyncio
import json
import uuid
from typing import Dict, Any, List
from loguru import logger

class CollaborativeEditor:
    def __init__(self):
        self.active_sessions: Dict[str, Dict[str, Any]] = {}
        logger.info("Initialized CollaborativeEditor")

    async def connect_client(self, session_id: str, client_id: str, websocket: Any):
        if session_id not in self.active_sessions:
            self.active_sessions[session_id] = {
                "clients": [],
                "document_state": "",
                "ai_cursor": {"position": 0, "status": "idle"},
            }
        if client_id not in self.active_sessions[session_id]["clients"]:
            self.active_sessions[session_id]["clients"].append(client_id)
        logger.info(f"Client {client_id} connected to session {session_id}")

    async def broadcast_delta(self, session_id: str, delta: Dict[str, Any], sender_id: str | None = None):
        session = self.active_sessions.get(session_id)
        if not session:
            logger.warning(f"Session {session_id} not found for broadcast.")
            return
        if "insert" in delta:
            pos = delta.get("position", len(session["document_state"]))
            session["document_state"] = (
                session["document_state"][:pos]
                + delta["insert"]
                + session["document_state"][pos:]
            )
        logger.debug(f"Broadcasting delta in {session_id}: {delta}")
        for client_id in session.get("clients", []):
            if client_id == sender_id:
                continue
            logger.debug(f"Would send delta to client {client_id}")

    async def trigger_ai_edit(self, session_id: str, prompt: str, sender_id: str | None = None):
        logger.info(f"AI edit triggered in {session_id} with prompt: {prompt}")
        session = self.active_sessions.get(session_id)
        if not session:
            return {"status": "error", "error": "Session not found"}
        session["ai_cursor"]["status"] = "generating"
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            result = router.async_route_and_generate(prompt, task_type="coding", max_cost=0.03)
            text = result.get("text", "") if isinstance(result, dict) else ""
            if not text:
                text = "// AI generated no output"
            delta = {"insert": text, "position": len(session["document_state"])}
            session["document_state"] += text
            session["ai_cursor"]["status"] = "idle"
            await self.broadcast_delta(session_id, delta, sender_id)
            return {"status": "success", "delta": delta}
        except Exception as exc:
            session["ai_cursor"]["status"] = "idle"
            logger.error(f"AI edit failed: {exc}")
            return {"status": "error", "error": str(exc)}
