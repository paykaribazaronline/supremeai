import asyncio
import json
from typing import Any

from fastapi import APIRouter
from fastapi import WebSocket
from fastapi.websockets import WebSocketDisconnect
from pydantic import BaseModel

from models.pending_tasks import PendingTask
from models.pending_tasks import TaskStatus


router = APIRouter()

_pending: dict[str, PendingTask] = {}
_connections: list[WebSocket] = []


class ApproveRequest(BaseModel):
    resolved_by: str


@router.get("/pending")
def get_pending() -> list[dict[str, Any]]:
    return [t.model_dump() for t in _pending.values() if t.status == TaskStatus.PENDING]


@router.post("/approve/{task_id}")
def approve_task(task_id: str, req: ApproveRequest):
    task = _pending.get(task_id)
    if not task:
        return {"status": "error", "detail": "not_found"}
    task.status = TaskStatus.APPROVED
    task.resolved_by = req.resolved_by
    task.resolved_at = json.dumps({"ts": __import__("datetime").datetime.now(__import__("datetime").timezone.utc).isoformat()})
    return {"status": "approved", "task": task.model_dump()}


@router.post("/reject/{task_id}")
def reject_task(task_id: str, req: ApproveRequest):
    task = _pending.get(task_id)
    if not task:
        return {"status": "error", "detail": "not_found"}
    task.status = TaskStatus.REJECTED
    task.resolved_by = req.resolved_by
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
