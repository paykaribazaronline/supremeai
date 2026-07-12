import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from models.shared_workspace import SharedWorkspace


@pytest.fixture(autouse=True)
def mock_agents():
    mock_architect = MagicMock()
    mock_architect.design = AsyncMock()
    mock_coder = MagicMock()
    mock_coder.generate_code = AsyncMock()
    mock_qa = MagicMock()
    mock_qa.run = AsyncMock()
    mock_guardian = MagicMock()
    mock_guardian.run = AsyncMock(return_value=(True, "APPROVED"))
    mock_reflection = MagicMock()
    mock_reflection.run = AsyncMock()

    with (
        patch("core.swarm_orchestrator.ArchitectureAgent", return_value=mock_architect),
        patch("core.swarm_orchestrator.CodeGeneratorAgent", return_value=mock_coder),
        patch("core.swarm_orchestrator.QAAgent", return_value=mock_qa),
        patch("core.swarm_orchestrator.GuardianAgent", return_value=mock_guardian),
        patch("core.swarm_orchestrator.ReflectionAgent", return_value=mock_reflection),
    ):
        from core.swarm_orchestrator import MorphicOrchestrator

        yield {
            "orchestrator_class": MorphicOrchestrator,
            "architect": mock_architect,
            "coder": mock_coder,
            "qa": mock_qa,
            "guardian": mock_guardian,
            "reflection": mock_reflection,
        }


def test_swarm_orchestrator_initializes_agents(mock_agents):
    orchestrator = mock_agents["orchestrator_class"]()
    assert orchestrator.agents.get("architect") is not None
    assert orchestrator.agents.get("coder") is not None
    assert orchestrator.agents.get("qa") is not None


@pytest.mark.anyio
async def test_swarm_orchestrator_execute_task(mock_agents):
    orchestrator = mock_agents["orchestrator_class"]()
    workspace = await orchestrator.execute_task("do something", user_id="user1")
    assert isinstance(workspace, SharedWorkspace)
    mock_agents["architect"].run.assert_awaited_once()
    mock_agents["coder"].run.assert_awaited_once()
    mock_agents["guardian"].run.assert_awaited_once()
    mock_agents["reflection"].run.assert_awaited_once()
    assert workspace.task_id is not None
    assert workspace.original_prompt == "do something"
