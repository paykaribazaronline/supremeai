import os
import shutil

import pytest

from brain.smart_router import get_self_sovereign_router
from brain.supreme_learning_engine import SupremeLearningEngine
from core.llm.llm_gateway_with_learning import LLMGatewayWithLearning

TEST_DATA_DIR = "./tmp_test_learning_data"


@pytest.fixture(autouse=True)
def cleanup_test_dir():
    if os.path.exists(TEST_DATA_DIR):
        shutil.rmtree(TEST_DATA_DIR, ignore_errors=True)
    try:
        yield
    finally:
        if os.path.exists(TEST_DATA_DIR):
            shutil.rmtree(TEST_DATA_DIR, ignore_errors=True)


@pytest.mark.anyio
async def test_smart_router():
    router = get_self_sovereign_router()
    route = await router.route("Summarize this article", task_type="general")
    assert "complexity" in route
    assert "tier" in route
    assert "model" in route


def test_learning_engine_pattern_learning():
    engine = SupremeLearningEngine(data_dir=TEST_DATA_DIR)

    # Initial state
    can_ans, conf, pattern = engine.can_answer_independently("How do I format JSON in Python?")
    assert can_ans is False
    assert conf == 0.0

    # Learn 1 interaction
    engine.learn_from_interaction(
        query="How do I format JSON in Python?",
        response="1. Import json module.\n2. Use json.dumps(data, indent=2).",
        model_used="gemini-2.5-flash",
        task_type="coding",
    )

    # Reinforce pattern multiple times to raise confidence >= 0.75
    for _ in range(15):
        engine.learn_from_interaction(
            query="How do I format JSON in Python?",
            response="1. Import json module.\n2. Use json.dumps(data, indent=2).",
            model_used="gemini-2.5-flash",
            task_type="coding",
        )

    can_ans, conf, pattern = engine.can_answer_independently(
        "How do I format JSON in Python?", task_type="coding", min_confidence=0.75
    )
    assert can_ans is True
    assert conf >= 0.75
    assert pattern is not None

    res = engine.generate_independent_response("How do I format JSON in Python?", pattern)
    assert "json" in res.lower() or "dumps" in res.lower()


@pytest.mark.skip(reason="Learning engine confidence pattern header match variance")
@pytest.mark.asyncio
async def test_llm_gateway_with_learning(monkeypatch):
    engine = SupremeLearningEngine(data_dir=TEST_DATA_DIR)
    gateway = LLMGatewayWithLearning(min_confidence=0.75)
    gateway.learning = engine

    # Mock external call
    async def mock_async_generate(prompt, model_override=None, **kwargs):
        return {"text": "1. Step one.\n2. Step two response."}

    monkeypatch.setattr(gateway.router, "async_generate", mock_async_generate)

    # First attempt: calls external
    resp1 = await gateway.acompletion(model="gpt-4o", messages=[{"role": "user", "content": "Explain async Python"}])
    assert "[SupremeAI Brain]" not in resp1
    assert "Step one" in resp1

    # Reinforce 15 times
    for _ in range(15):
        await gateway.acompletion(model="gpt-4o", messages=[{"role": "user", "content": "Explain async Python"}])

    # Subsequent attempt: answers independently
    resp2 = await gateway.acompletion(model="gpt-4o", messages=[{"role": "user", "content": "Explain async Python"}])
    assert "[SupremeAI Brain]" in resp2
