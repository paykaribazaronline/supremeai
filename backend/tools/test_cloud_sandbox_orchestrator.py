import os
from unittest.mock import AsyncMock
from unittest.mock import patch

import pytest
import respx
from httpx import Response

from backend.tools.cloud_sandbox_orchestrator import CloudSandboxOrchestrator


@pytest.fixture
def mock_env_runpod():
    with patch.dict(os.environ, {"RUNPOD_API_KEY": "test-key"}, clear=True):
        yield


@pytest.fixture
def mock_env_docker():
    with patch.dict(os.environ, {"DOCKER_HOST": "unix:///var/run/docker.sock"}, clear=True):
        yield


@pytest.mark.asyncio
async def test_create_session_docker_success(mock_env_docker):
    orchestrator = CloudSandboxOrchestrator(provider="docker")

    with patch("docker.from_env") as mock_from_env:
        mock_container = AsyncMock()
        mock_container.id = "test_container_id"
        mock_docker_client = AsyncMock()
        mock_docker_client.containers.run.return_value = mock_container
        mock_from_env.return_value = mock_docker_client

        orchestrator._docker_client = mock_docker_client

        session_id = await orchestrator.create_session()

        assert session_id is not None
        assert session_id.startswith("sandbox-")
        assert orchestrator.active_sessions[session_id]["provider"] == "docker"
        assert orchestrator.active_sessions[session_id]["container_id"] == "test_container_id"


@pytest.mark.asyncio
@respx.mock
async def test_create_session_runpod_success(mock_env_runpod):
    # Mock the RunPod API endpoint
    respx.post("https://api.runpod.io/v1/user/pod").mock(return_value=Response(200, json={"id": "test_pod_id", "status": "creating"}))

    orchestrator = CloudSandboxOrchestrator(provider="runpod")
    session_id = await orchestrator.create_session()

    assert session_id is not None
    assert session_id.startswith("sandbox-")
    assert orchestrator.active_sessions[session_id]["provider"] == "runpod"
    assert orchestrator.active_sessions[session_id]["pod_id"] == "test_pod_id"


@pytest.mark.asyncio
@respx.mock
async def test_create_session_auto_fallback_to_runpod(mock_env_runpod):
    """
    Test that when provider is 'auto' and Docker fails, it falls back to RunPod.
    """
    # Mock the RunPod API endpoint for success
    respx.post("https://api.runpod.io/v1/user/pod").mock(return_value=Response(200, json={"id": "test_pod_id_fallback", "status": "creating"}))

    orchestrator = CloudSandboxOrchestrator(provider="auto")

    # Mock Docker to fail
    with patch("docker.from_env", side_effect=Exception("Docker daemon not running")):
        session_id = await orchestrator.create_session()

    assert session_id is not None
    assert orchestrator.active_sessions[session_id]["provider"] == "runpod"
    assert orchestrator.active_sessions[session_id]["pod_id"] == "test_pod_id_fallback"


@pytest.mark.asyncio
async def test_create_session_all_fail_fallback_to_mock():
    """
    Test that if Docker and RunPod (no API key) fail, it falls back to a mock session.
    """
    # No RUNPOD_API_KEY in env
    orchestrator = CloudSandboxOrchestrator(provider="auto")

    # Mock Docker to fail
    with patch("docker.from_env", side_effect=Exception("Docker daemon not running")):
        session_id = await orchestrator.create_session()

    assert session_id is not None
    assert orchestrator.active_sessions[session_id]["provider"] == "auto"
    assert "container_id" not in orchestrator.active_sessions[session_id]
    assert "pod_id" not in orchestrator.active_sessions[session_id]


@pytest.mark.asyncio
async def test_run_command_docker(mock_env_docker):
    orchestrator = CloudSandboxOrchestrator(provider="docker")

    with patch("docker.from_env") as mock_from_env:
        # Setup mock container and exec_run result
        mock_exec_result = AsyncMock()
        mock_exec_result.exit_code = 0
        mock_exec_result.output = b"hello world"

        mock_container = AsyncMock()
        mock_container.exec_run.return_value = mock_exec_result

        mock_docker_client = AsyncMock()
        mock_docker_client.containers.get.return_value = mock_container
        mock_from_env.return_value = mock_docker_client
        orchestrator._docker_client = mock_docker_client

        # Create a fake active session
        session_id = "sandbox-test"
        orchestrator.active_sessions[session_id] = {
            "provider": "docker",
            "container_id": "fake_id",
        }

        result = await orchestrator.run_command(session_id, "echo 'hello world'")

        assert result["exit_code"] == 0
        assert "hello world" in result["stdout"]
