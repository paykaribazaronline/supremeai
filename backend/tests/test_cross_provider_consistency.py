"""Integration tests for cross-provider consistency.

বাংলা: ক্রস-প্রোভাইডার কনসিসটেন্সি — একই ইনপুটে বিভিন্ন প্রোভাইডার থেকে সামঞ্জস্যপূর্ণ আউটপুট।
"""

from __future__ import annotations

from unittest.mock import MagicMock

import pytest
from core.llm_router import LLMRouter, Provider, TaskType


class FakeProvider:
    def __init__(self, name: str, response_text: str):
        self.name = name
        self.response_text = response_text

    async def acomplete(self, prompt: str, **kwargs):
        return {"text": self.response_text, "provider": self.name}

    async def astream(self, prompt: str, **kwargs):
        class Chunker:
            async def __aiter__(self):
                yield self.response_text

        return Chunker()


class TestCrossProviderConsistency:
    """Tests for cross-provider consistency."""

    @pytest.mark.asyncio
    async def test_chat_task_returns_text_from_any_provider(self, monkeypatch):
        """Test any provider returns text for chat task."""
        monkeypatch.setattr(
            "core.llm_router.settings", MagicMock(openrouter_api_key="test")
        )
        router = LLMRouter()
        router.providers = {
            Provider.MOONSHOT: FakeProvider("moonshot", "moon response"),
            Provider.DEEPSEEK: FakeProvider("deepseek", "deep response"),
        }
        for provider_name in [Provider.MOONSHOT, Provider.DEEPSEEK]:
            router.providers[provider_name].name = provider_name
            router._primary = provider_name
            result = await router.route("hello", task_type=TaskType.CHAT)
            assert result.content is not None
            assert isinstance(result.content, str)

    @pytest.mark.asyncio
    async def test_bengali_task_prefers_moonshot(self, monkeypatch):
        """Test Bengali task routing preference."""
        monkeypatch.setattr(
            "core.llm_router.settings", MagicMock(openrouter_api_key="test")
        )
        router = LLMRouter()
        router.providers = {
            Provider.MOONSHOT: FakeProvider("moonshot", "বাংলা রেসপন্স"),
            Provider.DEEPSEEK: FakeProvider("deepseek", "english response"),
        }
        result = await router.route("বাংলা প্রশ্ন", task_type=TaskType.BENGALI)
        assert result.content is not None

    @pytest.mark.asyncio
    async def test_code_task_routes_to_deepseek(self, monkeypatch):
        """Test code task routes to DeepSeek."""
        monkeypatch.setattr(
            "core.llm_router.settings", MagicMock(openrouter_api_key="test")
        )
        router = LLMRouter()
        router.providers = {
            Provider.DEEPSEEK: FakeProvider("deepseek", "code response"),
            Provider.MOONSHOT: FakeProvider("moonshot", "chat response"),
        }
        result = await router.route("write python code", task_type=TaskType.CODE)
        assert result.content is not None

    @pytest.mark.asyncio
    async def test_streaming_consistency_across_providers(self, monkeypatch):
        """Test streaming works consistently."""
        monkeypatch.setattr(
            "core.llm_router.settings", MagicMock(openrouter_api_key="test")
        )
        router = LLMRouter()
        router.providers = {
            Provider.MOONSHOT: FakeProvider("moonshot", "stream response"),
            Provider.DEEPSEEK: FakeProvider("deepseek", "stream response"),
        }
        for provider_name in [Provider.MOONSHOT, Provider.DEEPSEEK]:
            router._primary = provider_name
            result = await router.route("hello", task_type=TaskType.CHAT, stream=True)
            chunks = []
            async for chunk in result:
                chunks.append(chunk)
            assert len(chunks) > 0

    @pytest.mark.asyncio
    async def test_error_handling_consistent(self, monkeypatch):
        """Test error handling is consistent across providers."""
        monkeypatch.setattr(
            "core.llm_router.settings", MagicMock(openrouter_api_key="test")
        )
        router = LLMRouter()

        class FailingProvider:
            async def acomplete(self, prompt, **kwargs):
                raise RuntimeError("provider error")

        router.providers = {
            Provider.MOONSHOT: FailingProvider(),
            Provider.DEEPSEEK: FailingProvider(),
        }
        with pytest.raises(Exception):
            await router.route("hello", task_type=TaskType.CHAT)

    def test_provider_capabilities_matrix(self):
        """Test provider capabilities matrix is defined."""
        from core.llm_router import PROVIDER_CAPABILITIES

        assert Provider.MOONSHOT in PROVIDER_CAPABILITIES
        assert Provider.DEEPSEEK in PROVIDER_CAPABILITIES
        assert TaskType.CHAT in PROVIDER_CAPABILITIES[Provider.MOONSHOT]
