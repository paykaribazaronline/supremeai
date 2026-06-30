import pytest
from unittest.mock import patch, AsyncMock
from core.swarm_orchestrator import SwarmOrchestrator

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
