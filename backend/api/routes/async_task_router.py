from typing import Any

from fastapi import APIRouter
from pydantic import BaseModel

from core.agent_orchestrator import async_task_manager


router = APIRouter(prefix="/api/task", tags=["async-task"])


class TaskResponse(BaseModel):
    task_id: str
    status: str
    progress: int = 0
    result: str | None = None
    error: str | None = None


@router.get("/{task_id}")
def get_task_status(task_id: str) -> TaskResponse:
    task = async_task_manager.get_task(task_id)
    if task:
        return TaskResponse(
            task_id=task["task_id"],
            status=task["status"],
            progress=task.get("progress", 0),
            result=task.get("result"),
            error=task.get("error"),
        )
    return TaskResponse(task_id=task_id, status="not_found", progress=0)


@router.get("/_stats")
def get_task_stats() -> dict[str, Any]:
    return async_task_manager.get_stats()
