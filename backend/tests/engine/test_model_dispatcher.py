import sys


sys.path.append("../..")
from engine.model_dispatcher import select_model, get_fallback_chain


class TestSelectModel:
    def test_select_low_complexity(self):
        assert select_model(2, "free") == "ollama/llama3.2"

    def test_select_medium_complexity(self):
        assert select_model(5, "free") == "openrouter/mistral-7b-free"

    def test_select_high_complexity(self):
        assert select_model(7, "free") == "gemini/gemini-flash"

    def test_select_very_high_complexity(self):
        assert select_model(9, "free") == "openai/gpt-4o-mini"

    def test_select_max_complexity(self):
        assert select_model(15, "free") == "openai/gpt-4o-mini"

    def test_select_model_paid(self):
        assert select_model(2, "paid") == "ollama/llama3.2"


class TestGetFallbackChain:
    def test_fallback_chain_first(self):
        chain = get_fallback_chain("ollama/llama3.2")
        assert "openrouter/mistral-7b-free" in chain

    def test_fallback_chain_last(self):
        chain = get_fallback_chain("openai/gpt-4o-mini")
        assert "ollama/llama3.2" in chain

    def test_fallback_chain_unknown(self):
        chain = get_fallback_chain("unknown/model")
        assert len(chain) == 4
