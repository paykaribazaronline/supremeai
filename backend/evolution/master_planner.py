import importlib.util
from typing import Any


HAS_CREWAI = importlib.util.find_spec("crewai") is not None


class MasterPlanner:
    async def run_evolution_cycle(self) -> list[dict[str, Any]]:
        return []

    async def submit_for_hitl_review(self, proposal: dict[str, Any]) -> str:
        import uuid
        return str(uuid.uuid4())
