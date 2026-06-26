from __future__ import annotations

from types import SimpleNamespace
from unittest.mock import patch

import pytest
from fastapi import HTTPException

from api.routes.chat import ChatPayload, get_completion, stream_chat


class FakeCache:
    def __init__(self, value=None):
        self.value = value
        self.saved = None

    async def get(self, prompt: str, model_name: str, session_id: str):
        return self.value

    async def set(self, prompt: str, response: str, model_name: str, session_id: str):
        self.saved = {
            "prompt": prompt,
            "response": response,
            "model_name": model_name,
            "session_id": session_id,
        }


class FakeModel:
    def __init__(self, model_name: str):
        self.model_name = model_name

    async def generate_content_async(self, prompt: str, stream: bool = False):
        if prompt == "raise-error":
            raise RuntimeError("boom")
        return SimpleNamespace(text=f"generated:{prompt}")


@pytest.mark.asyncio
async def test_get_completion_returns_cached_result(monkeypatch):
    fake_cache = FakeCache(value={
        "success": True,
        "response": "cached-response",
        "source": "L5_CACHE",
        "latency_ms": 10,
    })
    monkeypatch.setattr("api.routes.chat.multi_layer_cache", fake_cache)
    request = SimpleNamespace(headers={"X-Session-ID": "session-1"})
    payload = ChatPayload(prompt="hello")
    result = await get_completion(request, payload, db=SimpleNamespace(tenant_id="tenant-1"))

    assert result["cached"] is True
    assert result["response"] == "cached-response"
    assert result["cache_source"] == "L5_CACHE"


@pytest.mark.asyncio
async def test_get_completion_generates_response_and_saves_cache(monkeypatch):
    fake_cache = FakeCache(value=None)
    monkeypatch.setattr("api.routes.chat.multi_layer_cache", fake_cache)
    monkeypatch.setattr("api.routes.chat.genai", SimpleNamespace(GenerativeModel=FakeModel))

    request = SimpleNamespace(headers={"X-Session-ID": "session-2"})
    payload = ChatPayload(prompt="live-prompt")
    result = await get_completion(request, payload, db=SimpleNamespace(tenant_id="tenant-2"))

    assert result["cached"] is False
    assert result["response"] == "generated:live-prompt"
    assert fake_cache.saved is not None
    assert fake_cache.saved["model_name"] == "gemini-1.5-pro"


@pytest.mark.asyncio
async def test_get_completion_raises_http_exception_on_model_failure(monkeypatch):
    fake_cache = FakeCache(value=None)
    monkeypatch.setattr("api.routes.chat.multi_layer_cache", fake_cache)
    monkeypatch.setattr("api.routes.chat.genai", SimpleNamespace(GenerativeModel=FakeModel))

    request = SimpleNamespace(headers={"X-Session-ID": "session-3"})
    payload = ChatPayload(prompt="raise-error")

    with pytest.raises(HTTPException):
        await get_completion(request, payload, db=SimpleNamespace(tenant_id="tenant-3"))


@pytest.mark.asyncio
async def test_stream_chat_yields_sse_chunks(monkeypatch):
    async def fake_generate_content_async(prompt: str, stream: bool = False):
        class Response:
            async def __aiter__(self):
                yield SimpleNamespace(text="chunk-one")
                yield SimpleNamespace(text="chunk-two")

        return Response()

    class FakeStreamModel:
        def __init__(self, model_name: str):
            self.model_name = model_name

        async def generate_content_async(self, prompt: str, stream: bool = False):
            return await fake_generate_content_async(prompt, stream=stream)

    monkeypatch.setattr("api.routes.chat.genai", SimpleNamespace(GenerativeModel=FakeStreamModel))
    request_payload = ChatPayload(prompt="stream-prompt")
    response = stream_chat(request_payload, db=SimpleNamespace(tenant_id="tenant-4"))

    assert response.media_type == "text/event-stream"

    body = b""
    async for chunk in response.body_iterator:
        body += chunk

    assert b"chunk-one" in body
    assert b"chunk-two" in body
