"""Integration tests for provider failover chain.

বাংলা: LLM প্রোভাইডার ফেইলওভার চেইন — প্রাথমিক প্রোভাইডার ব্যর্থ হলে স্বয়ংক্রিয় ব lineage।
"""

from __future__ import annotations

from unittest.mock import MagicMock

import pytest
from core.llm_router import LLMRouter, Provider, TaskType, TokenBudget


class FakeProvider:
    def __init__(self, name: str, fail: bool = False):
        self.name = name
        self.fail = fail

    async def acomplete(self, prompt: str, **kwargs):
        if self.fail:
            raise RuntimeError(f"{self.name} failed")
        return {"text": f"{self.name}:{prompt}", "provider": self.name}

    async def astream(self, prompt: str, **kwargs):
        if self.fail:
            raise RuntimeError(f"{self.name} failed")

        class Chunker:
            async def __aiter__(self):
                yield f"{self.name}:chunk1"
                yield f"{self.name}:chunk2"

        return Chunker()


class TestProviderFailoverChain:
    """Tests for provider failover chain."""

    @pytest.mark.asyncio
    async def test_primary_provider_success(self, monkeypatch):
        """Test primary provider succeeds."""
        mock_settings = MagicMock()
        mock_settings.openrouter_api_key = "test"
        mock_settings.OLLAMA_URL = "http://localhost:11434"
        mock_settings.OLLAMA_MODEL = "qwen2.5:0.5b"
        mock_settings.MOONSHOT_API_KEY = "test"
        mock_settings.DEEPSEEK_API_KEY = "test"
        mock_settings.TOGETHER_API_KEY = "test"
        mock_settings.GEMINI_API_KEY = "test"
        monkeypatch.setattr("core.llm_router.settings", mock_settings)
        router = LLMRouter()
        router.providers = {
            Provider.MOONSHOT: FakeProvider("moonshot"),
        }
        result = await router.route("hello", task_type=TaskType.CHAT)
        assert (
            "moonshot" in result.content.lower()
            or "moonshot" in result.provider.lower()
        )

    @pytest.mark.asyncio
    async def test_fallback_on_primary_failure(self, monkeypatch):
        """Test fallback to secondary provider when primary fails."""
        mock_settings = MagicMock()
        mock_settings.openrouter_api_key = "test"
        mock_settings.OLLAMA_URL = "http://localhost:11434"
        mock_settings.OLLAMA_MODEL = "qwen2.5:0.5b"
        mock_settings.MOONSHOT_API_KEY = "test"
        mock_settings.DEEPSEEK_API_KEY = "test"
        mock_settings.TOGETHER_API_KEY = "test"
        mock_settings.GEMINI_API_KEY = "test"
        monkeypatch.setattr("core.llm_router.settings", mock_settings)
        router = LLMRouter()
        router.providers = {
            Provider.MOONSHOT: FakeProvider("moonshot", fail=True),
            Provider.DEEPSEEK: FakeProvider("deepseek"),
        }
        result = await router.route("hello", task_type=TaskType.CHAT)
        assert result.provider == Provider.DEEPSEEK

    @pytest.mark.asyncio
    async def test_all_providers_fail(self, monkeypatch):
        """Test exception when all providers fail."""
        mock_settings = MagicMock()
        mock_settings.openrouter_api_key = "test"
        mock_settings.OLLAMA_URL = "http://localhost:11434"
        mock_settings.OLLAMA_MODEL = "qwen2.5:0.5b"
        mock_settings.MOONSHOT_API_KEY = "test"
        mock_settings.DEEPSEEK_API_KEY = "test"
        mock_settings.TOGETHER_API_KEY = "test"
        mock_settings.GEMINI_API_KEY = "test"
        monkeypatch.setattr("core.llm_router.settings", mock_settings)
        router = LLMRouter()
        router.providers = {
            Provider.MOONSHOT: FakeProvider("moonshot", fail=True),
            Provider.DEEPSEEK: FakeProvider("deepseek", fail=True),
        }
        with pytest.raises(Exception):
            await router.route("hello", task_type=TaskType.CHAT)

    @pytest.mark.asyncio
    async def test_streaming_fallback(self, monkeypatch):
        """Test streaming fallback works."""
        mock_settings = MagicMock()
        mock_settings.openrouter_api_key = "test"
        mock_settings.OLLAMA_URL = "http://localhost:11434"
        mock_settings.OLLAMA_MODEL = "qwen2.5:0.5b"
        mock_settings.MOONSHOT_API_KEY = "test"
        mock_settings.DEEPSEEK_API_KEY = "test"
        mock_settings.TOGETHER_API_KEY = "test"
        mock_settings.GEMINI_API_KEY = "test"
        monkeypatch.setattr("core.llm_router.settings", mock_settings)
        router = LLMRouter()
        router.providers = {
            Provider.MOONSHOT: FakeProvider("moonshot", fail=True),
            Provider.DEEPSEEK: FakeProvider("deepseek"),
        }
        result = await router.route("hello", task_type=TaskType.CHAT, stream=True)
        chunks = []
        async for chunk in result:
            chunks.append(chunk)
        assert len(chunks) > 0

    def test_token_budget_creation(self):
        """Test TokenBudget creation."""
        budget = TokenBudget(max_input=1000, max_output=200)
        assert budget.max_input == 1000
        assert budget.max_output == 200

    def test_token_budget_remaining(self):
        """Test TokenBudget remaining calculation."""
        budget = TokenBudget(max_input=1000, max_output=200)
        assert budget.daily_limit == 100000
        assert budget.used_today == 0

    def test_token_budget_check(self):
        """Test TokenBudget check method."""
        budget = TokenBudget(max_input=1000, max_output=200)
        assert budget.check(100, 50) is True
        assert budget.check(2000, 50) is False
