import pytest

from brain.expert_router import ExpertType, SupremeMoERouter
from services.llm.llm_router import get_llm_gateway


def test_moe_prompt_classification():
    # Bengali prompts (Unicode and transliterated)
    assert SupremeMoERouter.classify_prompt("আমার নাম কি?") == ExpertType.BENGALI
    assert SupremeMoERouter.classify_prompt("kemon acho brother?") == ExpertType.BENGALI

    # Coder prompts
    assert SupremeMoERouter.classify_prompt("def async_generate(self, prompt: str):") == ExpertType.CODER
    assert SupremeMoERouter.classify_prompt("How to fix Docker API connection error?") == ExpertType.CODER

    # Reasoner prompts
    assert SupremeMoERouter.classify_prompt("Calculate the theorem proof for calculus equation") == ExpertType.REASONER

    # General prompts
    assert SupremeMoERouter.classify_prompt("What is the capital of France?") == ExpertType.GENERAL


def test_moe_model_chain_generation():
    bengali_chain = SupremeMoERouter.get_model_chain("বাংলাদেশে সবচেয়ে বড় নদী কোনটি?")
    assert len(bengali_chain) > 0
    assert "hf_space/supreme-hybrid-8b" in bengali_chain or "groq/llama-3.3-70b-versatile" in bengali_chain

    coder_chain = SupremeMoERouter.get_model_chain("import numpy as np; print(np.array([1,2]))")
    assert "deepseek/deepseek-coder" in coder_chain


@pytest.mark.asyncio
async def test_llm_gateway_moe_integration(monkeypatch):
    gateway = get_llm_gateway()

    # Mock route response
    class DummyResult:
        content = "Mocked Response"
        provider = type("DummyProvider", (), {"value": "moonshot"})()
        tokens_used = 50
        cost_usd = 0.0
        latency_ms = 10.0
        cached = False

    async def mock_route(prompt, **kwargs):
        return DummyResult()

    monkeypatch.setattr(gateway._router, "route", mock_route)

    res = await gateway.async_generate("Explain python list comprehension", use_moe=True)
    assert res["text"] == "Mocked Response"
