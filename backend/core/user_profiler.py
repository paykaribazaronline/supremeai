from dataclasses import dataclass
from dataclasses import field
from enum import Enum
from typing import Any

from loguru import logger


class UserMode(str, Enum):
    FAST_TRACK = "FAST_TRACK"
    LEARNING = "LEARNING"
    PRODUCTION = "PRODUCTION"


@dataclass
class UserProfile:
    user_id: str
    mode: UserMode = UserMode.FAST_TRACK
    goals: list[str] = field(default_factory=list)
    preferences: dict[str, Any] = field(default_factory=dict)
    history: list[dict[str, Any]] = field(default_factory=list)


class UserProfiler:
    MODES = [m.value for m in UserMode]

    async def classify_user(self, user_id: str) -> UserProfile:
        return UserProfile(user_id=user_id)

    async def update_from_history(self, user_id: str, task: dict[str, Any]) -> None:
        logger.debug(f"Updating user profile for {user_id} from task")
