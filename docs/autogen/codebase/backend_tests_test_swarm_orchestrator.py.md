# 📄 ফাইল: backend/tests/test_swarm_orchestrator.py

**প্রকার:** .py  
**সাইজ:** 1,826 বাইট  
**আপডেট:** 2026-07-04T05:52:57.774130

---

## কোড

```py
import pytest
from unittest.mock import patch, AsyncMock
import litellm
from core.swarm_orchestrator import SwarmOrchestrator


@pytest.fixture(autouse=True)
def setup_litellm():
    """কনফিগার করুন litellm সেটিংস প্রতিটি টেস্টের আগে"""
    # বাংলা মন্তব্য: লিটেলএলএম প্রক্সি সেটিংস নিশ্চিত করা
    litellm.use_litellm_proxy = False
    litellm.drop_params = True
    litellm.telemetry = False
    yield


def test_swarm_orchestrator_initializes_agents():
    orchestrator = SwarmOrchestrator()
    assert orchestrator.architect is not None
    assert orchestrator.coder is not None
    assert orchestrator.qa is not None


@pytest.mark.anyio
async def test_swarm_orchestrator_runs_entire_graph():
    orchestrator = SwarmOrchestrator()
    
    # Mock LLM Gateway response to bypass actual internet LLM calls during pytest
    mock_response = {
        "choices": [
            {
                "message": {
                    "content": "class TestModel: pass"
                }
            }
        ]
    }
    
    with patch("core.llm_gateway.llm_gateway.acompletion", new_callable=AsyncMock, return_value=mock_response) as mock_call:
        workspace = await orchestrator.execute_task(
            prompt="Build a simple key-value database model class in Python",
            user_id="default_user_session"
        )
        
        # Verify all agents were called
        assert mock_call.call_count == 3
        assert workspace.architecture_design == "class TestModel: pass"
        assert workspace.generated_code["main.py"] == "class TestModel: pass"
        assert len(workspace.execution_logs) > 0
        assert workspace.test_results["safe"] is True

```