import sys

import pytest

sys.path.append("../..")
from byoc.container_orchestrator import ContainerOrchestrator


class TestContainerOrchestrator:
    @pytest.mark.asyncio
    async def test_deploy(self):
        orchestrator = ContainerOrchestrator()
        result = await orchestrator.deploy("user1", "skill_v1")
        assert result["status"] == "deployed"
        assert result["user_id"] == "user1"
        assert result["skill"] == "skill_v1"

    @pytest.mark.asyncio
    async def test_rollback(self):
        orchestrator = ContainerOrchestrator()
        result = await orchestrator.rollback("deploy_abc")
        assert result["status"] == "rolled_back"
        assert result["deployment_id"] == "deploy_abc"
