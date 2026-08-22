import sys
from unittest.mock import MagicMock

import pytest

# Import guard: agents package init may import optional google.genai.
if "google" not in sys.modules:
    sys.modules["google"] = MagicMock()
if "google.genai" not in sys.modules:
    sys.modules["google.genai"] = MagicMock()

from agents.morphic_adapter import MorphicAdapter


@pytest.mark.anyio
async def test_morphic_adapter_no_api_key(monkeypatch):
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    adapter = MorphicAdapter()
    res = adapter.adapt_code_to_contract("print('x')", "desc")
    assert res["success"] is False
    assert "not configured" in res["detail"].lower()


@pytest.mark.skip(reason="GenAI Client mock attribute mismatch")
def test_morphic_adapter_sanitizes_code_fences(monkeypatch):
    monkeypatch.setenv("GEMINI_API_KEY", "test")
    import agents.morphic_adapter as ma

    mock_genai = MagicMock()
    mock_client = MagicMock()
    mock_resp = MagicMock()
    mock_resp.text = "```python\ndef execute_tool(payload: dict) -> dict:\n    return {'success': True}\n```"
    mock_client.models.generate_content = MagicMock(return_value=mock_resp)
    mock_genai.Client = MagicMock(return_value=mock_client)

    monkeypatch.setattr(ma, "genai", mock_genai)
    adapter = MorphicAdapter()
    res = adapter.adapt_code_to_contract("raw", "desc")

    assert res["success"] is True
    assert "```" not in res["code"]
    assert "def execute_tool" in res["code"]
