"""Approval Manager — Human-in-the-Loop approval workflow with secure file operations.

বাংলা: মানবাধীন অনুমোদন কর্মকাণ্ড ও নিরাপদ ফাইল অপারেশন সহ।
"""

import asyncio
import os
from typing import Any

from core.code_validator import AICodeValidator
from core.security.auth_middleware import verify_admin_session_fail_closed
from fastapi import APIRouter, Depends, HTTPException, WebSocket
from fastapi.websockets import WebSocketDisconnect
from loguru import logger
from models.pending_tasks import TaskStatus, list_pending, update_task_status
from pydantic import BaseModel

router = APIRouter()

_connections: list[WebSocket] = []

# Path validation for skill generation
_ALLOWED_SKILLS_DIR = None


def _get_allowed_skills_dir() -> str:
    """Get canonical skills directory path once."""
    global _ALLOWED_SKILLS_DIR
    if _ALLOWED_SKILLS_DIR is None:
        backend_dir = os.path.dirname(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        )
        _ALLOWED_SKILLS_DIR = os.path.join(backend_dir, "skills")
    return _ALLOWED_SKILLS_DIR


class ApproveRequest(BaseModel):
    resolved_by: str
    reason: str | None = None


@router.get("/pending")
def get_pending(
    _: dict = Depends(verify_admin_session_fail_closed),
) -> list[dict[str, Any]]:
    """Get all pending tasks - REQUIRES admin authentication."""
    return [t.model_dump() for t in list_pending()]


@router.post("/approve/{task_id}")
def approve_task(
    task_id: str,
    req: ApproveRequest,
    _: dict = Depends(verify_admin_session_fail_closed),
) -> dict[str, Any]:
    """Approve a pending task - REQUIRES admin authentication."""
    task = update_task_status(task_id, TaskStatus.APPROVED, req.resolved_by, req.reason)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    if task.task_type == "SKILL_GENERATION":
        try:
            skill_name = task.payload.get("skill_name")
            code = task.payload.get("generated_code")

            if not skill_name or not code:
                raise HTTPException(
                    status_code=400, detail="Missing skill_name or generated_code"
                )

            if not skill_name.replace("_", "").replace("-", "").isalnum():
                raise HTTPException(status_code=400, detail="Invalid skill name format")

            # বাংলা মন্তব্য: রেন্ডার ডকার লেআউটের জন্য সঠিক AICodeValidator ক্লাস এবং can_use ভ্যালিডেশন কী ব্যবহার করা হলো
            validator = AICodeValidator()
            validation_result = validator.validate_before_use(code)
            if not validation_result.get("can_use", False):
                raise HTTPException(
                    status_code=400,
                    detail=f"Code validation failed: {validation_result.get('checks', {})}",
                )

            skills_dir = _get_allowed_skills_dir()
            os.makedirs(skills_dir, exist_ok=True)
            path = os.path.join(skills_dir, f"{skill_name}.py")

            real_path = os.path.realpath(path)
            if not real_path.startswith(os.path.realpath(skills_dir)):
                raise HTTPException(
                    status_code=403, detail="Path traversal attempt blocked"
                )

            with open(path, "w", encoding="utf-8") as f:
                f.write(code)
            logger.info(
                f"✅ Approved skill '{skill_name}' successfully written to {path}"
            )

        except HTTPException:
            raise
        except Exception as e:
            logger.error(f"Failed to execute approved skill generation: {e}")
            raise HTTPException(status_code=500, detail=f"Execution failed: {e}") from e

    return {"status": "approved", "task": task.model_dump()}


@router.post("/reject/{task_id}")
def reject_task(
    task_id: str,
    req: ApproveRequest,
    _: dict = Depends(verify_admin_session_fail_closed),
) -> dict[str, Any]:
    """Reject a pending task - REQUIRES admin authentication."""
    task = update_task_status(task_id, TaskStatus.REJECTED, req.resolved_by, req.reason)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    logger.info(
        f"❌ Task {task_id} rejected by {req.resolved_by}. Reason: {req.reason}"
    )
    return {"status": "rejected", "task": task.model_dump()}


@router.websocket("/ws/hitl")
async def hitl_ws(ws: WebSocket):
    """WebSocket endpoint for HITL notifications - no auth (notifications only)."""
    await ws.accept()
    _connections.append(ws)
    try:
        while True:
            await asyncio.sleep(1)
    except WebSocketDisconnect:
        _connections.remove(ws)
    except asyncio.CancelledError:
        _connections.remove(ws)
        raise
