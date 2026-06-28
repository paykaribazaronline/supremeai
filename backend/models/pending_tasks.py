import uuid
from datetime import datetime
from datetime import timezone
from enum import Enum

from pydantic import BaseModel


class TaskType(str, Enum):
    CODE_PUSH = "CODE_PUSH"
    NEW_SITE_VISIT = "NEW_SITE_VISIT"
    SKILL_GENERATION = "SKILL_GENERATION"
    VPN_SWITCH = "VPN_SWITCH"
    AUTO_EVOLUTION_PATCH = "AUTO_EVOLUTION_PATCH"


class TaskStatus(str, Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    EXECUTED = "EXECUTED"
    CANCELLED = "CANCELLED"


class PendingTask(BaseModel):
    task_id: str
    task_type: TaskType
    payload: dict
    status: TaskStatus
    created_at: str
    resolved_by: str | None = None
    resolved_at: str | None = None
    reason: str | None = None


def create_pending_task(task_type: TaskType, payload: dict, created_by: str = "system") -> PendingTask:
    return PendingTask(
        task_id=str(uuid.uuid4()),
        task_type=task_type,
        payload=payload,
        status=TaskStatus.PENDING,
        created_at=datetime.now(timezone.utc).isoformat(),
    )
