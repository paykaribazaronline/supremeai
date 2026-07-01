import datetime
from typing import Any

from loguru import logger


class AutoUpdateManager:
    BRANCH_PREFIX = "feature/auto-"
    TARGET_BRANCH = "develop"

    async def on_approval(self, proposal_id: str) -> dict[str, Any]:
        ts = int(datetime.datetime.now(datetime.UTC).timestamp())
        branch = f"{self.BRANCH_PREFIX}{ts}"
        logger.info(f"Applying approved proposal {proposal_id} to branch {branch}")
        return {"branch": branch, "status": "applied"}
