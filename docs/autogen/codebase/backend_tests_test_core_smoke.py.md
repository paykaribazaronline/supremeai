# 📄 ফাইল: backend/tests/test_core_smoke.py

**প্রকার:** .py  
**সাইজ:** 1,494 বাইট  
**আপডেট:** 2026-07-08T04:17:37.558604

---

## কোড

```py
import importlib
import sys
import types
import asyncio

import pytest
from unittest.mock import patch, AsyncMock


def test_setup_logging_runs():
    from core.logging_config import setup_logging

    # Should not raise
    setup_logging()


def test_config_validators_basic():
    from core.config import Settings

    s = Settings(env="test", cors_origins='["http://127.0.0.1:3000"]')
    assert "127.0.0.1" in " ".join(s.cors_origins)
    # ensure debug remains a bool
    assert isinstance(s.debug, bool)


@pytest.mark.anyio
async def test_llm_gateway_acompletion_monkeypatched(monkeypatch, tmp_path):
    class FakeChoiceMessage:
        def __init__(self, content):
            self.content = content

    class FakeChoice:
        def __init__(self, msg):
            self.message = FakeChoiceMessage(msg)

    class FakeResponse:
        def __init__(self, text):
            self.choices = [FakeChoice(text)]
            self._response_metadata = {"api_cost": 0.001}

    async def fake_acompletion(*args, **kwargs):
        return FakeResponse("mocked-response")

    from core.llm_gateway import LLMGateway
    
    with patch("core.llm_gateway.litellm.acompletion", new=fake_acompletion):
        with patch("core.semantic_cache.SemanticCache.query_similar", new=AsyncMock(return_value=None)):
            gateway = LLMGateway()
            res = await gateway.acompletion(prompt="hi")
            assert res["success"] is True
            assert res["text"] == "mocked-response"

```