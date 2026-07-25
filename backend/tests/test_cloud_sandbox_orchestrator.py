from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from core.orchestration.cloud_sandbox_orchestrator import \
    CloudSandboxOrchestrator


@pytest.mark.anyio
async def test_delegate_to_freebuff_missing_binary(monkeypatch):
    orch = CloudSandboxOrchestrator(provider="runpod")

    # Ensure it tries to run freebuff cmd and it is missing
    with patch.object(orch, "api_key", None, create=True):
        with patch(
            "core.orchestration.cloud_sandbox_orchestrator.asyncio.create_subprocess_exec",
            side_effect=FileNotFoundError(),
        ):
            res = await orch.delegate_to_freebuff("print('hi')", working_dir=".")

    assert res["status"] == "error"
    assert (
        "not installed" in res["error"].lower()
        or "freebuff cli not found" in res["error"].lower()
    )


@pytest.mark.anyio
async def test_delegate_to_freebuff_success(monkeypatch):
    orch = CloudSandboxOrchestrator(provider="runpod")

    mock_proc = MagicMock()
    mock_proc.communicate = AsyncMock(return_value=(b"ok", b""))
    mock_proc.returncode = 0

    with patch(
        "core.orchestration.cloud_sandbox_orchestrator.asyncio.create_subprocess_exec",
        new_callable=AsyncMock,
        return_value=mock_proc,
    ) as create_mock:
        res = await orch.delegate_to_freebuff("do thing", working_dir=".")

    assert res["status"] == "success"
    assert "ok" in res["output"]
    assert create_mock.called
