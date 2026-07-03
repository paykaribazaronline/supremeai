# 📄 ফাইল: backend/tests/test_core_smoke.py

**প্রকার:** .py  
**সাইজ:** 2,043 বাইট  
**আপডেট:** 2026-07-03T15:56:22.645888

---

## কোড

```py
import importlib
import sys
import types
import asyncio

import pytest


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
    # Prepare a fake litellm module before importing core.llm_gateway
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

    fake_litellm = types.SimpleNamespace(
        acompletion=fake_acompletion, success_callback=[], failure_callback=[]
    )

    # Fake semantic_cache with a simple query_similar returning None
    fake_semantic_cache_mod = types.ModuleType("core.semantic_cache")

    class SemanticCache:
        async def query_similar(self, prompt, task_type=None):
            return None

    fake_semantic_cache_mod.SemanticCache = SemanticCache

    # Insert fakes into sys.modules before importing
    sys.modules["litellm"] = fake_litellm
    sys.modules["core.semantic_cache"] = fake_semantic_cache_mod

    # Ensure module is reloaded cleanly
    if "core.llm_gateway" in sys.modules:
        del sys.modules["core.llm_gateway"]

    llm_mod = importlib.import_module("core.llm_gateway")
    gw = llm_mod.llm_gateway

    res = await gw.acompletion("hello world")
    assert res["success"] is True
    assert res["text"] == "mocked-response"

```