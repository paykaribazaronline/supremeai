# tests/test_local_model_handler_full.py
"""Unit tests for LocalModelHandler (Ollama / Local Inference).

বাংলা মন্তব্য: respx এর পরিবর্তে unittest.mock.patch ব্যবহার করা হয়েছে।
Full suite চলার সময় অন্য টেস্ট ফাইল httpx.AsyncClient প্যাচ করার কারণে
respx ইন্টারসেপশন লেয়ার বাইপাস হয়ে যেত। patch() দিয়ে সরাসরি মক করা
সম্পূর্ণ আইসোলেটেড এবং সুইট-ইন্ডিপেন্ডেন্ট।
"""

from __future__ import annotations

import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from models.local_model_handler import LocalModelHandler


def _make_mock_response(status_code: int, body: dict) -> MagicMock:
    """httpx.Response মক তৈরি করার হেল্পার ফাংশন।"""
    mock_resp = MagicMock()
    mock_resp.status_code = status_code
    mock_resp.json.return_value = body
    mock_resp.text = json.dumps(body)
    return mock_resp


@pytest.mark.asyncio
async def test_health_check_success():
    """HTTP 200 হলে health_check True রিটার্ন করবে।"""
    handler = LocalModelHandler("http://localhost:11434")
    mock_resp = _make_mock_response(200, {"models": []})

    mock_client = AsyncMock()
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)
    mock_client.get = AsyncMock(return_value=mock_resp)

    with patch("models.local_model_handler.httpx.AsyncClient", return_value=mock_client):
        result = await handler.health_check()

    assert result is True


@pytest.mark.asyncio
async def test_health_check_failure():
    """HTTP 500 হলে health_check False রিটার্ন করবে।"""
    handler = LocalModelHandler("http://localhost:11434")
    mock_resp = _make_mock_response(500, {})

    mock_client = AsyncMock()
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)
    mock_client.get = AsyncMock(return_value=mock_resp)

    with patch("models.local_model_handler.httpx.AsyncClient", return_value=mock_client):
        result = await handler.health_check()

    assert result is False


@pytest.mark.asyncio
async def test_list_models_success():
    """API সফল রেসপন্স দিলে মডেল নামের তালিকা রিটার্ন করবে।"""
    handler = LocalModelHandler("http://localhost:11434")
    mock_resp = _make_mock_response(
        200,
        {"models": [{"name": "llama3:latest"}, {"name": "mistral:latest"}]},
    )

    mock_client = AsyncMock()
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)
    mock_client.get = AsyncMock(return_value=mock_resp)

    with patch("models.local_model_handler.httpx.AsyncClient", return_value=mock_client):
        models = await handler.list_models()

    assert models == ["llama3:latest", "mistral:latest"]


@pytest.mark.asyncio
async def test_infer_success_and_caching():
    """ইনফারেন্স সফল হলে status=success এবং দ্বিতীয় কলে cache hit হবে।"""
    handler = LocalModelHandler("http://localhost:11434")
    mock_resp = _make_mock_response(
        200,
        {"response": "Hello from Ollama!", "eval_count": 12},
    )

    call_count = 0

    async def mock_post(*args, **kwargs):
        nonlocal call_count
        call_count += 1
        return mock_resp

    mock_client = AsyncMock()
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)
    mock_client.post = mock_post

    with patch("models.local_model_handler.httpx.AsyncClient", return_value=mock_client):
        res1 = await handler.infer("llama3", "Hi")
        assert res1["status"] == "success"
        assert res1["text"] == "Hello from Ollama!"
        assert res1["cached"] is False
        assert call_count == 1

        # Second call should use in-memory cache (no HTTP call)
        res2 = await handler.infer("llama3", "Hi")
        assert res2["status"] == "success"
        assert res2["text"] == "Hello from Ollama!"
        assert res2["cached"] is True
        assert call_count == 1  # HTTP call হয়নি
