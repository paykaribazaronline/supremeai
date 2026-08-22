"""
Unit tests for Phase 2 Intelligence Features:
- TreeOfThoughtReasoner multi-branch evaluation
- SelfReflectionLoop cognitive self-questions
- SmartModelRouter NLP intent routing
- ToolForge dynamic tool synthesis
"""

import pytest

from engine.self_reflection import SelfReflectionLoop
from engine.smart_router import SmartModelRouter
from engine.tool_forge import ToolForge
from engine.tree_of_thought import TreeOfThoughtReasoner


@pytest.mark.asyncio
async def test_tree_of_thought_reasoning():
    reasoner = TreeOfThoughtReasoner()
    result = await reasoner.reason("Design a scalable multi-cloud microservice architecture")
    assert result["confidence_score"] > 0.8
    assert len(result["reasoning_path"]) == 3
    assert "best_thought" in result


@pytest.mark.asyncio
async def test_self_reflection_loop():
    reflection_engine = SelfReflectionLoop()
    report = await reflection_engine.reflect(
        task_prompt="Optimize database query",
        execution_output="Added index on user_id",
        is_success=True,
    )
    assert report["is_correct"] is True
    assert "future_prevention_strategy" in report


def test_smart_model_router_intent():
    router = SmartModelRouter()
    assert router.classify_intent("কোডটি অপ্টিমাইজ করো") == "bengali"
    assert router.classify_intent("def sort_array(arr): pass") == "code"
    assert router.classify_intent("calculate matrix multiplication integral") == "math"
    assert router.classify_intent("why did the system fail under load") == "reasoning"


@pytest.mark.asyncio
async def test_tool_forge_synthesis():
    forge = ToolForge()

    # Unsafe synthesis attempt should be rejected
    unsafe_ok = await forge.synthesize_tool("bad_tool", "dangerous action", "import os; os.system('echo test')")
    assert unsafe_ok is False

    # Safe tool synthesis
    safe_ok = await forge.synthesize_tool("json_cleaner", "Clean JSON string", "def clean(data): return data.strip()")
    assert safe_ok is True
    assert forge.get_tool("json_cleaner") is not None
