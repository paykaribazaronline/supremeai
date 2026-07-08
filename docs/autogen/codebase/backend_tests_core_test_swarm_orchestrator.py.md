# 📄 ফাইল: backend/tests/core/test_swarm_orchestrator.py

**প্রকার:** .py  
**সাইজ:** 1,653 বাইট  
**আপডেট:** 2026-07-08T00:29:13.898046

---

## কোড

```py
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
    mock_qa.verify = AsyncMock()

    with patch("core.swarm_orchestrator.ArchitectureAgent", return_value=mock_architect), \
         patch("core.swarm_orchestrator.CodeGeneratorAgent", return_value=mock_coder), \
         patch("core.swarm_orchestrator.QAAgent", return_value=mock_qa):
        from core.swarm_orchestrator import SwarmOrchestrator

        yield {
            "orchestrator_class": SwarmOrchestrator,
            "architect": mock_architect,
            "coder": mock_coder,
            "qa": mock_qa,
        }


def test_swarm_orchestrator_initializes_agents(mock_agents):
    orchestrator = mock_agents["orchestrator_class"]()
    assert orchestrator.architect is not None
    assert orchestrator.coder is not None
    assert orchestrator.qa is not None


@pytest.mark.anyio
async def test_swarm_orchestrator_execute_task(mock_agents):
    orchestrator = mock_agents["orchestrator_class"]()
    workspace = await orchestrator.execute_task("do something", user_id="user1")
    assert isinstance(workspace, SharedWorkspace)
    mock_agents["architect"].design.assert_awaited_once()
    mock_agents["coder"].generate_code.assert_awaited_once()
    mock_agents["qa"].verify.assert_awaited_once()
    assert workspace.task_id is not None
    assert workspace.original_prompt == "do something"

```