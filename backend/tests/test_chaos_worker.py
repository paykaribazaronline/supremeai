from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from workers.chaos_worker import NightlyChaosAuditor


def make_auditor():
    auditor = NightlyChaosAuditor()
    auditor.db = None
    auditor.gate_ref = MagicMock()
    return auditor


@pytest.mark.asyncio
async def test_execute_audit_sequence_all_pass():
    auditor = make_auditor()
    mock_payloads = [("safe_code_1", "desc1"), ("safe_code_2", "desc2")]

    mock_response = MagicMock()
    mock_response.status_code = 200

    mock_client = AsyncMock()
    mock_client.post.return_value = mock_response

    with (
        patch(
            "workers.chaos_worker.generate_fuzz_payloads", return_value=mock_payloads
        ),
        patch("workers.chaos_worker.run_sandbox_ast_check", return_value=False),
        patch("httpx.AsyncClient") as mock_client_cls,
    ):
        mock_client_cls.return_value.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client_cls.return_value.__aexit__ = AsyncMock(return_value=False)

        result = await auditor.execute_audit_sequence()

        assert result is True
        auditor.gate_ref.set.assert_called_once()


@pytest.mark.asyncio
async def test_execute_audit_sequence_fuzz_failure():
    auditor = make_auditor()
    mock_payloads = [("malicious_code", "desc1"), ("safe_code", "desc2")]

    def fake_check(code):
        return code == "malicious_code"

    mock_response = MagicMock()
    mock_response.status_code = 200

    mock_client = AsyncMock()
    mock_client.post.return_value = mock_response

    with (
        patch(
            "workers.chaos_worker.generate_fuzz_payloads", return_value=mock_payloads
        ),
        patch("workers.chaos_worker.run_sandbox_ast_check", side_effect=fake_check),
        patch("httpx.AsyncClient") as mock_client_cls,
    ):
        mock_client_cls.return_value.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client_cls.return_value.__aexit__ = AsyncMock(return_value=False)

        result = await auditor.execute_audit_sequence()

        assert result is False
        auditor.gate_ref.set.assert_called_once()
        call_args = auditor.gate_ref.set.call_args[0][0]
        assert call_args["status"] == "LOCKED"


@pytest.mark.asyncio
async def test_execute_audit_sequence_network_failure():
    auditor = make_auditor()
    mock_payloads = []

    mock_client = AsyncMock()
    mock_client.post.side_effect = Exception("Network error")

    with (
        patch(
            "workers.chaos_worker.generate_fuzz_payloads", return_value=mock_payloads
        ),
        patch("httpx.AsyncClient") as mock_client_cls,
    ):
        mock_client_cls.return_value.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client_cls.return_value.__aexit__ = AsyncMock(return_value=False)

        result = await auditor.execute_audit_sequence()

        # 5 stress-test HTTP requests all raise Exception("Network error")
        # → failures == 5, so auditor locks the deploy gate and returns False
        assert result is False
        auditor.gate_ref.set.assert_called_once()
        call_args = auditor.gate_ref.set.call_args[0][0]
        assert call_args["status"] == "LOCKED"
