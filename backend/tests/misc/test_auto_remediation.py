import os
from unittest.mock import AsyncMock, MagicMock, mock_open, patch

import pytest

from core.resilience.auto_remediation import AutoRemediation


@pytest.mark.anyio
async def test_process_security_alert_file_not_found(monkeypatch):
    ar = AutoRemediation(gemini_api_key="")

    # Make path validation deterministic; this test focuses on exists() branch.
    monkeypatch.setattr(
        ar,
        "_validate_file_path",
        lambda p: os.path.abspath("backend"),
        raising=False,
    )

    with patch("core.resilience.auto_remediation.os.path.exists", return_value=False):
        result = await ar.process_security_alert(
            file_path="backend/core/resilience/auto_remediation.py",
            line_number=12,
            issue="XSS issue",
            severity="high",
            tenant_id="t1",
        )

    assert result["success"] is False
    assert "not found" in result["error"].lower()


@pytest.mark.anyio
async def test_process_security_alert_pipeline_rejects(monkeypatch):
    ar = AutoRemediation(gemini_api_key="")
    safe_path = os.path.abspath("backend/core/resilience/auto_remediation.py")

    m = mock_open(read_data="print('original')\n")

    pipeline = MagicMock()
    pipeline.submit = AsyncMock(return_value="reject: unsafe patch")

    monkeypatch.setattr(ar, "_validate_file_path", lambda p: safe_path)

    with (
        patch("core.resilience.auto_remediation.os.path.exists", return_value=True),
        patch("builtins.open", m),
        patch.object(
            ar,
            "_get_ai_patch",
            new=AsyncMock(return_value="# fixed\n"),
        ),
        patch(
            "core.health.self_healer.RemediationPipeline",
            return_value=pipeline,
        ),
    ):
        result = await ar.process_security_alert(
            file_path="backend/core/resilience/auto_remediation.py",
            line_number=1,
            issue="test",
            severity="critical",
            tenant_id="t1",
        )

    assert result["success"] is False
    assert "patch rejected" in result["error"].lower()


@pytest.mark.anyio
async def test_process_security_alert_happy_path(monkeypatch):
    ar = AutoRemediation(gemini_api_key="")
    safe_path = os.path.abspath("backend/core/resilience/auto_remediation.py")

    m = mock_open(read_data="print('original')\n")

    class DummyPipeline:
        async def submit(self, tenant_id, issue, fixed_code, impact_score, dependency_tree):
            assert tenant_id == "t1"
            assert "fixed" in fixed_code.lower() or fixed_code.startswith("#")
            assert 0.0 <= impact_score <= 1.0
            return "accepted:remediation-id-123"

    monkeypatch.setattr(ar, "_validate_file_path", lambda p: safe_path)

    with (
        patch("core.resilience.auto_remediation.os.path.exists", return_value=True),
        patch("builtins.open", m),
        patch.object(
            ar,
            "_get_ai_patch",
            new=AsyncMock(return_value="# fixed\nprint('ok')\n"),
        ),
        patch(
            "core.health.self_healer.RemediationPipeline",
            DummyPipeline,
        ),
    ):
        result = await ar.process_security_alert(
            file_path="backend/core/resilience/auto_remediation.py",
            line_number=42,
            issue="command injection",
            severity="high",
            tenant_id="t1",
        )

    assert result["success"] is True
    assert result["patch_applied"] is True
    assert result["branch"] == "supremeai-improvements"
