import asyncio
from typing import Any

from fastapi import APIRouter
from fastapi import WebSocket
from fastapi.websockets import WebSocketDisconnect
from loguru import logger
from pydantic import BaseModel

from models.pending_tasks import TaskStatus
from models.pending_tasks import list_pending
from models.pending_tasks import update_task_status


router = APIRouter()

_connections: list[WebSocket] = []


class ApproveRequest(BaseModel):
    resolved_by: str
    reason: str | None = None


@router.get("/pending")
def get_pending() -> list[dict[str, Any]]:
    return [t.model_dump() for t in list_pending()]


@router.post("/approve/{task_id}")
def approve_task(task_id: str, req: ApproveRequest):
    task = update_task_status(task_id, TaskStatus.APPROVED, req.resolved_by, req.reason)
    if not task:
        return {"status": "error", "detail": "not_found"}
    
    # ── Execute Task Logic ──
    # বাংলা মন্তব্য: অনুমোদিত হওয়ার পর কাজটির ধরণ অনুযায়ী সংশ্লিষ্ট স্কিল বা অ্যাকশন এক্সিকিউট করা হচ্ছে
    if task.task_type == "SKILL_GENERATION":
        try:
            import os
            skill_name = task.payload.get("skill_name")
            code = task.payload.get("generated_code")
            if skill_name and code:
                # Resolve paths
                # বাংলা মন্তব্য: backend root থেকে skills ডিরেক্টরি সঠিক পথে রাইট করা হচ্ছে
                backend_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
                skills_dir = os.path.join(backend_dir, "skills")
                os.makedirs(skills_dir, exist_ok=True)
                path = os.path.join(skills_dir, f"{skill_name}.py")
                with open(path, "w", encoding="utf-8") as f:
                    f.write(code)
                logger.info(f"✅ Approved skill '{skill_name}' successfully written to {path}")
        except Exception as e:
            logger.error(f"Failed to execute approved skill generation: {e}")
            return {"status": "execution_failed", "detail": str(e), "task": task.model_dump()}

    return {"status": "approved", "task": task.model_dump()}


@router.post("/reject/{task_id}")
def reject_task(task_id: str, req: ApproveRequest):
    task = update_task_status(task_id, TaskStatus.REJECTED, req.resolved_by, req.reason)
    if not task:
        return {"status": "error", "detail": "not_found"}
    # If rejected, we simply update the status and drop the execution
    logger.info(f"❌ Task {task_id} rejected by {req.resolved_by}. Reason: {req.reason}")
    return {"status": "rejected", "task": task.model_dump()}


@router.websocket("/ws/hitl")
async def hitl_ws(ws: WebSocket):
    await ws.accept()
    _connections.append(ws)
    try:
        while True:
            await asyncio.sleep(1)
    except WebSocketDisconnect:
        _connections.remove(ws)
