import pytest
from unittest.mock import patch, MagicMock
from tools.cloud_sandbox_orchestrator import CloudSandboxOrchestrator

@pytest.mark.anyio
async def test_sandbox_local_flow():
    orchestrator = CloudSandboxOrchestrator(provider="local")
    session_id = await orchestrator.create_session()
    assert session_id is not None
    assert session_id in orchestrator.active_sessions
    
    cmd_res = await orchestrator.run_command(session_id, "ls -la")
    assert cmd_res["exit_code"] == 0
    assert "Mock output" in cmd_res["stdout"]
    
    term_res = await orchestrator.terminate_session(session_id)
    assert term_res is True
    assert session_id not in orchestrator.active_sessions

import os

@pytest.mark.anyio
@patch("httpx.AsyncClient.post")
async def test_sandbox_runpod_flow(mock_post):
    # Mock RunPod pod creation API
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.json.return_value = {"id": "pod-12345"}
    mock_post.return_value = mock_resp

    with patch.dict(os.environ, {"RUNPOD_API_KEY": "test-api-key"}):
        orchestrator = CloudSandboxOrchestrator(provider="runpod")
        session_id = await orchestrator.create_session()
        assert session_id is not None
        assert orchestrator.active_sessions[session_id]["pod_id"] == "pod-12345"
        
        # Test command mock fallback/execution
        cmd_res = await orchestrator.run_command(session_id, "echo 'hello'")
        assert cmd_res["exit_code"] == 0
        
        # Cleanup
        await orchestrator.terminate_session(session_id)
