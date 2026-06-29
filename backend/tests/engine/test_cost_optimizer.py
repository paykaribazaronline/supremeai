import sys
from unittest.mock import patch

import pytest

sys.path.append("../..")
from engine.cost_optimizer import ComplexityAnalyzer, CostOptimizer


class TestComplexityAnalyzer:
    def test_classify_simple(self):
        assert ComplexityAnalyzer.classify("hello there") == "simple"

    def test_classify_medium(self):
        assert ComplexityAnalyzer.classify("explain this code") == "medium"

    def test_classify_complex(self):
        assert ComplexityAnalyzer.classify("implement a full architecture") == "complex"

    def test_classify_fallback(self):
        assert ComplexityAnalyzer.classify("random text without keywords") == "simple"


class TestCostOptimizer:
    def test_init(self):
        optimizer = CostOptimizer()
        assert optimizer.free_tier_tracker is None
        assert optimizer.litellm_callbacks == []

    def test_register_litellm_callback(self):
        optimizer = CostOptimizer()
        cb = lambda: None
        optimizer.register_litellm_callback(cb)
        assert cb in optimizer.litellm_callbacks

    def test_register_litellm_callback_duplicate(self):
        optimizer = CostOptimizer()
        cb = lambda: None
        optimizer.register_litellm_callback(cb)
        optimizer.register_litellm_callback(cb)
        assert optimizer.litellm_callbacks.count(cb) == 1

    @pytest.mark.asyncio
    async def test_get_optimal_route_simple_paid(self):
        optimizer = CostOptimizer()
        with patch.object(optimizer, "_get_best_free_provider", return_value=None):
            result = await optimizer.get_optimal_route({"prompt": "hello"}, "paid")
            assert result == "ollama/llama3.2"

    @pytest.mark.asyncio
    async def test_get_optimal_route_complex_free(self):
        optimizer = CostOptimizer()
        with patch.object(optimizer, "_get_best_free_provider", return_value="anthropic"):
            result = await optimizer.get_optimal_route({"prompt": "implement architecture"}, "free")
            assert result.startswith("anthropic")
