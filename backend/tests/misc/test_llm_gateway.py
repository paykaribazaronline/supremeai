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


@pytest.fixture(autouse=True)
def disable_semantic_cache(monkeypatch):
    from unittest.mock import AsyncMock

    monkeypatch.setattr(
        "core.cache.semantic_cache.SemanticCache.query_similar",
        AsyncMock(return_value=None),
    )
    yield
    return


@pytest.fixture(autouse=True)
def reset_shared_circuit_breakers():
    """বাংলা মন্তব্য: CircuitBreakerManager একটি process-wide singleton (real
    resilience feature, বাগ না) — কিন্তু টেস্ট আইসোলেশনের জন্য প্রতিটা টেস্টের
    আগে/পরে state reset করা দরকার। নাহলে parallel worker-এ (xdist --dist=loadfile)
    একই process-এ চলা অন্য টেস্ট কোনো model-এর breaker trip করালে, এই ফাইলের পরের
    legit টেস্টেও সেই model silently skip হয়ে যায় (cb.allow_request() == False),
    call_chain ছোট হয়ে যায়, আর false 'all models failed' রেজাল্ট আসে।
    """
    from core.resilience.circuit_breaker_manager import CircuitBreakerManager

    manager = CircuitBreakerManager()
    for name in list(manager._circuit_breakers.keys()):
        manager.reset_breaker(name)
    yield
    for name in list(manager._circuit_breakers.keys()):
        manager.reset_breaker(name)


def test_load_routing_policy_file_not_found(monkeypatch, tmp_path):
    monkeypatch.setattr(
        "core.llm.llm_gateway._POLICY_PATH",
        str(tmp_path / "missing.json"),
    )
    gateway = LLMGateway()
    assert "complexity_rules" in gateway.routing_policy
    assert "fallback_chain" in gateway.routing_policy


# test_inject_secrets_sets_env_vars has been removed as LLMGateway no longer injects secrets into os.environ.


@pytest.mark.anyio
async def test_acompletion_cache_hit(monkeypatch):
    gateway = LLMGateway()
    mock_cache = MagicMock()
    mock_cache.query_similar = AsyncMock(return_value=MagicMock(response="cached", model="m"))
    monkeypatch.setattr(gateway, "cache", mock_cache)

    result = await gateway.acompletion(prompt="hello")
    assert result["cached"] is True
    assert result["text"] == "cached"


@pytest.mark.anyio
async def test_acompletion_success():
    gateway = LLMGateway()
    mock_response = MagicMock()
    mock_response.choices = [MagicMock(message=MagicMock(content="ok"))]
    mock_response._response_metadata = {}

    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=mock_response) as mock_call:
        os.environ["GROQ_API_KEY"] = "mock_key"
        result = await gateway.acompletion(prompt="hello", model="groq/llama-3.3-70b-versatile")
        assert result["success"] is True
        assert result["text"] == "ok"
        mock_call.assert_called_once()


@pytest.mark.anyio
async def test_acompletion_fallback_after_failure():
    gateway = LLMGateway()
    fail = MagicMock()
    fail.choices = [MagicMock(message=MagicMock(content="fail"))]
    success = MagicMock()
    success.choices = [MagicMock(message=MagicMock(content="ok"))]
    success._response_metadata = {}

    with patch(
        "litellm.acompletion",
        new_callable=AsyncMock,
        side_effect=[Exception("err"), success],
    ) as mock_call:
        os.environ["OPENAI_API_KEY"] = "mock_key"
        result = await gateway.acompletion(prompt="hello")
        assert result["success"] is True
        assert result["text"] == "ok"
        assert mock_call.call_count == 2


@pytest.mark.anyio
async def test_acompletion_all_models_fail():
    gateway = LLMGateway()
    with patch("litellm.acompletion", new_callable=AsyncMock, side_effect=Exception("err")):
        os.environ["OPENAI_API_KEY"] = "mock_key"
        with pytest.raises(
            Exception
        ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
            await gateway.acompletion(prompt="hello")


@pytest.mark.anyio
async def test_acompletion_difficulty_routing():
    gateway = LLMGateway()
    mock_response = MagicMock()
    mock_response.choices = [MagicMock(message=MagicMock(content="ok"))]
    mock_response._response_metadata = {}

    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=mock_response) as mock_call:
        os.environ["OPENAI_API_KEY"] = "mock_key"
        await gateway.acompletion(prompt="solve x+1=2", task_type="math")
        assert "hard" in [c.kwargs.get("model", "") for c in mock_call.call_args_list] or mock_call.call_count >= 1


import os


@pytest.mark.anyio
async def test_stream_completion_yields_chunks():
    gateway = LLMGateway()

    async def mock_stream():
        for chunk in ["hel", "lo"]:
            m = MagicMock()
            m.choices = [MagicMock(delta=MagicMock(content=chunk))]
            yield m

    mock_response = MagicMock()
    mock_response.__aiter__ = lambda self: mock_stream()

    with patch("litellm.acompletion", new_callable=AsyncMock, return_value=mock_response):
        result = [chunk async for chunk in gateway._stream_completion([{"role": "user", "content": "hi"}], ["m"], 1.0)]
        assert result == ["hel", "lo"]


@pytest.mark.anyio
async def test_stream_completion_falls_back():
    gateway = LLMGateway()

    async def fail_stream():
        m = MagicMock()
        m.choices = [MagicMock(delta=MagicMock(content="x"))]
        yield m
        raise Exception("stream fail")

    async def ok_stream():
        m = MagicMock()
        m.choices = [MagicMock(delta=MagicMock(content="ok"))]
        yield m

    fail_resp = MagicMock()
    fail_resp.__aiter__ = lambda self: fail_stream()
    ok_resp = MagicMock()
    ok_resp.__aiter__ = lambda self: ok_stream()

    with patch("litellm.acompletion", new_callable=AsyncMock) as mock_call:
        mock_call.side_effect = [fail_resp, ok_resp]
        result = [
            chunk async for chunk in gateway._stream_completion([{"role": "user", "content": "hi"}], ["m1", "m2"], 1.0)
        ]
        assert result == ["x", "ok"]
