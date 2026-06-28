import asyncio
from typing import Any

from fastapi import APIRouter
from fastapi import WebSocket
from fastapi.websockets import WebSocketDisconnect
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
    return {"status": "approved", "task": task.model_dump()}


@router.post("/reject/{task_id}")
def reject_task(task_id: str, req: ApproveRequest):
    task = update_task_status(task_id, TaskStatus.REJECTED, req.resolved_by, req.reason)
    if not task:
        return {"status": "error", "detail": "not_found"}
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
