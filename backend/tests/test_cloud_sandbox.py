from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from tools.cloud_sandbox_orchestrator import CloudSandboxOrchestrator


@pytest.mark.anyio
async def test_sandbox_local_flow():
    orchestrator = CloudSandboxOrchestrator(provider="runpod")
    res = await orchestrator.create_sandbox(spec={})
    assert res is not None
    assert res["id"] == "mock-sandbox-id-12345"
    assert res["status"] == "running"

    cmd_res = await orchestrator.run_command(res["id"], "ls -la")
    assert cmd_res["exitCode"] == 0
    assert "Mock output" in cmd_res["stdout"]

    term_res = await orchestrator.destroy_sandbox(res["id"])
    assert term_res is True


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
        res = await orchestrator.create_sandbox(spec={"imageName": "ubuntu"})
        assert res is not None
        assert res["id"] == "pod-12345"

        # Test command mock fallback/execution
        mock_cmd_resp = MagicMock()
        mock_cmd_resp.status_code = 200
        mock_cmd_resp.json.return_value = {"status": "COMPLETED", "exitCode": 0}
        mock_post.return_value = mock_cmd_resp

        cmd_res = await orchestrator.run_command(res["id"], "echo 'hello'")
        assert cmd_res["exitCode"] == 0

        # Cleanup
        mock_destroy_resp = MagicMock()
        mock_destroy_resp.status_code = 200
        mock_post.return_value = mock_destroy_resp
        await orchestrator.destroy_sandbox(res["id"])
