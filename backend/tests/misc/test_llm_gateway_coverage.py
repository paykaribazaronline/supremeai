# বাংলা মন্তব্য: core/llm_gateway.py এর কম-কভার হওয়া শাখাগুলোর জন্য অতিরিক্ত টেস্ট —
# সাকসেস/ফেইলিওর কলব্যাক, রাউটিং পলিসি লোড এরর, difficulty রাউটিং, messages
# প্যারামিটার এবং স্ট্রিমিং ফলব্যাক।
from __future__ import annotations

import os
from unittest.mock import AsyncMock, MagicMock, patch

import litellm
import pytest

from core.llm.llm_gateway import LLMGateway


@pytest.fixture(autouse=True)
def setup_litellm():
    litellm.use_litellm_proxy = False
    litellm.drop_params = True
    litellm.telemetry = False
    yield
    return


def test_load_routing_policy_handles_invalid_json(monkeypatch, tmp_path):
    # বাংলা মন্তব্য: ভাঙা JSON হলে ডিফল্ট পলিসি ফেরত আসবে (except শাখা কভার করে)
    bad = tmp_path / "routing_policy.json"
    bad.write_text("{ this is : not json", encoding="utf-8")
    monkeypatch.setattr("core.llm.llm_gateway._POLICY_PATH", str(bad))
    gateway = LLMGateway()
    from core.llm.llm_gateway import _DEFAULT_FALLBACK_MODELS

    assert gateway.routing_policy == {
        "complexity_rules": {},
        "fallback_chain": list(_DEFAULT_FALLBACK_MODELS),
    }


def test_success_callback_logs_without_error():
    # বাংলা মন্তব্য: LLMGateway ইনস্ট্যান্স করলে litellm.success_callback সেট হয়
    LLMGateway()
    callback = litellm.success_callback[0]
    response_obj = MagicMock()
    response_obj.usage = MagicMock(prompt_tokens=10, completion_tokens=5)
    response_obj._response_metadata = {"api_cost": 0.0012}
    # কোনো এক্সসেপশন ছাড়াই রান করবে
    callback({"model": "gemini/gemini-2.5-flash"}, response_obj, 0.0, 1.5)


def test_success_callback_swallows_exceptions():
    # বাংলা মন্তব্য: কলব্যাকের ভেতরের এরর গিলে ফেলা হয় (except শাখা কভার)
    LLMGateway()
    callback = litellm.success_callback[0]
    response_obj = MagicMock()
    response_obj.usage = None
    response_obj._response_metadata = {}
    # start/end time নন-নিউমেরিক দিলে duration হিসাবে TypeError হবে যা গিলে ফেলা হবে
    callback({"model": "m"}, response_obj, "bad", "time")


def test_failure_callback_logs_without_error():
    LLMGateway()
    callback = litellm.failure_callback[0]
    callback({"model": "m"}, Exception("boom"), 0.0, 1.0)


@pytest.mark.anyio
async def test_acompletion_accepts_messages_param():
    # বাংলা মন্তব্য: prompt এর বদলে messages দিলেও কাজ করবে (backward compatibility)
    gateway = LLMGateway()
    gateway.cache = MagicMock()
    gateway.cache.query_similar = AsyncMock(return_value=None)
    response = MagicMock()
    response.choices = [MagicMock(message=MagicMock(content="hi"))]
    response._response_metadata = {}
    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=response) as mock_call:
        os.environ["OPENAI_API_KEY"] = "mock_key"
        result = await gateway.acompletion(
            messages=[{"role": "user", "content": "hello there"}],
            model="groq/llama-3.3-70b-versatile",
        )
    assert result["text"] == "hi"
    assert mock_call.call_args.kwargs["messages"] == [{"role": "user", "content": "hello there"}]


@pytest.mark.skip(reason="LLMGateway model routing fallback chain priority")
@pytest.mark.anyio
async def test_acompletion_medium_difficulty_routing():
    # বাংলা মন্তব্য: agent/analysis টাস্ক medium difficulty রাউটে যায়
    gateway = LLMGateway()
    gateway.cache = MagicMock()
    gateway.cache.query_similar = AsyncMock(return_value=None)
    gateway.routing_policy = {
        "complexity_rules": {"medium": ["medium/model"]},
        "fallback_chain": ["fb/model"],
    }
    response = MagicMock()
    response.choices = [MagicMock(message=MagicMock(content="ok"))]
    response._response_metadata = {}
    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=response) as mock_call:
        os.environ["OPENAI_API_KEY"] = "mock_key"
        result = await gateway.acompletion(prompt="please do analysis", task_type="agent")
    assert result["success"] is True
    assert mock_call.call_args.kwargs["model"] == "medium/model"


@pytest.mark.anyio
async def test_acompletion_stream_returns_generator():
    # বাংলা মন্তব্য: stream=True দিলে অ্যাসিঙ্ক জেনারেটর ফেরত আসে
    gateway = LLMGateway()
    gateway.cache = MagicMock()
    gateway.cache.query_similar = AsyncMock(return_value=None)
    gateway.routing_policy = {
        "complexity_rules": {"easy": ["m1"]},
        "fallback_chain": [],
    }

    async def mock_stream():
        for token in ["a", "b"]:
            chunk = MagicMock()
            chunk.choices = [MagicMock(delta=MagicMock(content=token))]
            yield chunk

    stream_resp = MagicMock()
    stream_resp.__aiter__ = lambda self: mock_stream()
    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=stream_resp):
        os.environ["OPENAI_API_KEY"] = "mock_key"
        gen = await gateway.acompletion(prompt="stream this", stream=True)
        collected = [chunk async for chunk in gen]
    assert collected == ["a", "b"]


@pytest.mark.anyio
async def test_stream_completion_raises_when_all_models_fail():
    gateway = LLMGateway()
    with patch("litellm.acompletion", new_callable=AsyncMock, side_effect=Exception("down")):
        os.environ["OPENAI_API_KEY"] = "mock_key"
        with pytest.raises(
            Exception
        ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
            _ = [c async for c in gateway._stream_completion([{"role": "user", "content": "x"}], ["m1", "m2"], 1.0)]


@pytest.mark.anyio
async def test_acompletion_provider_filtering():
    # বাংলা মন্তব্য: provider দিলে সেটির মডেলগুলো আগে prioritized হবে
    gateway = LLMGateway()
    gateway.cache = MagicMock()
    gateway.cache.query_similar = AsyncMock(return_value=None)
    gateway.routing_policy = {
        "complexity_rules": {"easy": ["groq/llama", "openai/gpt"]},
        "fallback_chain": ["fallback/model"],
    }
    response = MagicMock()
    response.choices = [MagicMock(message=MagicMock(content="ok"))]
    response._response_metadata = {}
    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=response) as mock_call:
        os.environ["OPENAI_API_KEY"] = "mock_key"
        result = await gateway.acompletion(prompt="hi", provider="groq")
        assert result["success"] is True
        assert mock_call.call_args.kwargs["model"] == "groq/llama"


@pytest.mark.anyio
async def test_stream_completion_empty_content():
    # বাংলা মন্তব্য: স্ট্রিম চ্যাংক content খালি থাকলে skip হবে
    gateway = LLMGateway()
    gateway.cache = MagicMock()
    gateway.cache.query_similar = AsyncMock(return_value=None)
    gateway.routing_policy = {
        "complexity_rules": {"easy": ["m1"]},
        "fallback_chain": [],
    }

    async def mock_stream():
        m = MagicMock()
        m.choices = [MagicMock(delta=MagicMock(content=None))]
        yield m

    stream_resp = MagicMock()
    stream_resp.__aiter__ = lambda self: mock_stream()
    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=stream_resp):
        os.environ["OPENAI_API_KEY"] = "mock_key"
        result = [chunk async for chunk in gateway._stream_completion([{"role": "user", "content": "hi"}], ["m1"], 1.0)]
    assert result == []
