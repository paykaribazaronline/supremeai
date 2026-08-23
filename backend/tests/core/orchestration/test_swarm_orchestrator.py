import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from core.orchestration.swarm_orchestrator import SwarmOrchestrator, ExecutionResult
from core.resilience.circuit_breaker import CircuitBreakerOpenError
from models.shared_workspace import SharedWorkspace

@pytest.fixture(autouse=True)
def mock_external_calls():
    """Mock LLM calls, MCP, and external skill calls so agents execute logic but don't hit the network."""
    with patch("core.llm.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock) as mock_acompletion:
        mock_acompletion.return_value = {
            "choices": [{"message": {"content": "mock response"}}],
            "text": "mock text",
            "success": True
        }
        
        with patch("core.mcp_client.MCPRegistryClient.discover_tools", new_callable=AsyncMock) as mock_discover:
            mock_discover.return_value = [{"name": "mock_tool"}]
            
            with patch("core.agent_factory.DynamicAgentFactory.create_specialized_agent", new_callable=AsyncMock) as mock_factory:
                mock_factory.return_value = {"agent_name": "mocked_agent", "script": "print('hello')"}
                
                with patch("core.orchestration.agent_orchestrator.budget_aware_route") as mock_route:
                    mock_route.return_value = {"intent": "general", "tier": "free", "best_provider": "mock"}
                    
                    # We also mock the skill executions to prevent agents from attempting real integrations
                    with patch("core.orchestration.crew_departments.SwarmAgentBase._safe_skill_run", new_callable=AsyncMock) as mock_skill:
                        mock_skill.return_value = "mock_skill_output"
                        
                        yield {
                            "acompletion": mock_acompletion,
                            "discover_tools": mock_discover,
                            "create_specialized_agent": mock_factory,
                            "budget_aware_route": mock_route,
                            "safe_skill_run": mock_skill
                        }

@pytest.fixture
def orchestrator():
    return SwarmOrchestrator()

@pytest.mark.asyncio
async def test_execute_task_general_intent(orchestrator, mock_external_calls):
    """Test full execution flow for a general intent."""
    result = await orchestrator.execute_task("Do a general task", "user_1")
    
    assert isinstance(result, ExecutionResult)
    assert result.status == "success"
    assert result.workspace.intent == "general_task"
    assert "mock_tool" in [t.get("name") for t in result.workspace.work_product.get("available_tools", [])]
    
    # Check that execution logs record the start and successful completion
    assert any("Initialized swarm DAG" in log for log in result.workspace.execution_logs)
    assert any("Multi-Agent DAG execution completed successfully" in log for log in result.workspace.execution_logs)

@pytest.mark.asyncio
async def test_execute_task_code_generation_intent(orchestrator, mock_external_calls):
    """Test the specialized code_generation intent which has a refinement loop."""
    mock_external_calls["budget_aware_route"].return_value = {"intent": "coding", "tier": "free"}
    
    # Mock the guardian validation to approve on the first try
    with patch.object(orchestrator.agents["guardian"], "validate", new_callable=AsyncMock) as mock_validate:
        mock_validate.return_value = (True, "Looks good")
        
        result = await orchestrator.execute_task("Write a python script", "user_1")
        
        assert result.workspace.intent == "code_generation"
        assert result.status == "success"
        
        # Ensure guardian was called
        mock_validate.assert_called_once()
        assert any("Code APPROVED by Guardian" in log for log in result.workspace.execution_logs)

@pytest.mark.asyncio
async def test_circuit_breaker_integration(orchestrator, mock_external_calls):
    """Test that if an agent repeatedly fails, the circuit breaker opens and execution is halted gracefully."""
    # Force the executor agent to raise an error
    with patch.object(orchestrator.agents["executor"], "run", new_callable=AsyncMock) as mock_run:
        mock_run.side_effect = RuntimeError("Executor failed catastrophically")
        
        # First execution (should fail and increment circuit breaker)
        result1 = await orchestrator.execute_task("Task 1", "user_1")
        assert result1.status == "error"
        assert len(result1.errors) > 0
        
        # Second execution
        result2 = await orchestrator.execute_task("Task 2", "user_1")
        
        # Third execution
        result3 = await orchestrator.execute_task("Task 3", "user_1")
        
        # By the fourth execution, the circuit breaker should be OPEN
        result4 = await orchestrator.execute_task("Task 4", "user_1")
        
        assert result4.status == "error"
        assert orchestrator.circuit_breaker.state == "OPEN"
        assert any("Circuit breaker OPEN" in log for log in result4.workspace.execution_logs)

@pytest.mark.asyncio
async def test_synthesize_tool_fallback(orchestrator, mock_external_calls):
    """Test that if discover_tools returns nothing, _synthesize_tool is called."""
    mock_external_calls["discover_tools"].return_value = []
    
    result = await orchestrator.execute_task("Do something obscure", "user_1")
    
    # create_specialized_agent should have been called
    mock_external_calls["create_specialized_agent"].assert_called_once()
    
    # The synthesized capability should be in available tools
    available_tools = result.workspace.work_product.get("available_tools", [])
    assert any(t.get("agent_name") == "mocked_agent" for t in available_tools)

@pytest.mark.asyncio
async def test_dynamic_dag_routing(orchestrator):
    """Test that the correct DAG is returned for different intents."""
    dag_code = await orchestrator._get_dag_for_intent("code_generation")
    assert "coder" in dag_code
    assert "architect" in dag_code["coder"]
    
    dag_research = await orchestrator._get_dag_for_intent("research_analysis")
    assert "researcher" in dag_research
    assert "reflection" in dag_research
    
    dag_sync = await orchestrator._get_dag_for_intent("sync_to_github")
    assert "integration" in dag_sync
    
    dag_general = await orchestrator._get_dag_for_intent("unknown_intent")
    assert "executor" in dag_general
    assert "researcher" in dag_general
    assert "executor" in dag_general["researcher"]
