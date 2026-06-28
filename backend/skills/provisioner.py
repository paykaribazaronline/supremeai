from typing import Any

from loguru import logger

from skills.skill_registry import SkillRegistry


class SkillProvisioner:
    async def provision(self, skill_id: str, user_cloud: dict[str, Any]) -> dict[str, Any]:
        skill = SkillRegistry.get(skill_id)
        if not skill:
            return {"status": "error", "detail": "skill_not_found"}
        logger.info(f"Provisioning skill {skill_id} for user {user_cloud.get('user_id')}")
        return {"status": "provisioned", "skill_id": skill_id, "terraform": True}
