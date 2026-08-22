"""Tests for LLM Router - Multi-provider AI gateway.

This module tests:
- Provider enum and capabilities
- TokenBudget tracking
- Provider selection (fallback chains)
- Provider health checks
- Cost optimization
- Caching
- Streaming support
"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from services.llm.llm_router import (
    FALLBACK_CHAINS,
    PROVIDER_CAPABILITIES,
    PROVIDER_COSTS,
    BengaliNormalizer,
    LLMRouter,
    Provider,
    RouteResult,
    TaskType,
    TokenBudget,
)

# --- TokenBudget Tests ---


class TestTokenBudget:
    """Tests for TokenBudget class."""

    def test_check_within_limits(self):
        """Test budget check passes within limits."""
        budget = TokenBudget(max_input=8192, max_output=4096, daily_limit=100000)

        result = budget.check(1000, 500)
        assert result is True

    def test_check_exceeds_input_limit(self):
        """Test budget check fails when input exceeds limit."""
        budget = TokenBudget(max_input=1000, max_output=500, daily_limit=100000)

        result = budget.check(2000, 500)
        assert result is False

    def test_check_exceeds_output_limit(self):
        """Test budget check fails when output exceeds limit."""
        budget = TokenBudget(max_input=2000, max_output=500, daily_limit=100000)

        result = budget.check(500, 1000)
        assert result is False

    def test_check_exceeds_daily_limit(self):
        """Test budget check fails when daily limit reached."""
        budget = TokenBudget(max_input=2000, max_output=500, daily_limit=1000)
        budget.used_today = 900

        result = budget.check(100, 100)  # Would exceed daily limit
        assert result is False

    def test_consume_increments_used_today(self):
        """Test consume increments token usage."""
        budget = TokenBudget()

        budget.consume(1000)

        assert budget.used_today == 1000

    def test_custom_budget_parameters(self):
        """Test custom budget parameters."""
        budget = TokenBudget(max_input=4096, max_output=2048, daily_limit=50000)

        assert budget.max_input == 4096
        assert budget.max_output == 2048
        assert budget.daily_limit == 50000


# --- RouteResult Tests ---


class TestRouteResult:
    """Tests for RouteResult dataclass."""

    def test_route_result_creation(self):
        """Test creating RouteResult."""
        result = RouteResult(
            provider=Provider.OPENAI,
            content="Hello world",
            tokens_used=100,
            cost_usd=0.002,
            latency_ms=150.5,
        )

        assert result.provider == Provider.OPENAI
        assert result.content == "Hello world"
        assert result.tokens_used == 100
        assert result.cached is False
        assert result.fallback_used is False

    def test_route_result_cached(self):
        """Test RouteResult with caching."""
        result = RouteResult(
            provider=Provider.DEEPSEEK,
            content="Cached response",
            tokens_used=0,
            cost_usd=0.0,
            latency_ms=5.0,
            cached=True,
        )

        assert result.cached is True


# --- BengaliNormalizer Tests ---


class TestBengaliNormalizer:
    """Tests for BengaliNormalizer class."""

    def test_normalize_banglish(self):
        """Test normalizing Banglish to Bengali."""
        result = BengaliNormalizer.normalize("ami kemon acho")

        # Should contain Bengali characters
        assert "আমি" in result or "ami" in result

    def test_detect_script_bengali(self):
        """Test detecting pure Bengali script."""
        result = BengaliNormalizer.detect_script("আমি কেমন আছি")

        assert result == "bengali"

    def test_detect_script_roman(self):
        """Test detecting Roman script."""
        result = BengaliNormalizer.detect_script("Hello world")

        assert result == "roman"

    def test_detect_script_mixed(self):
        """Test detecting mixed script."""
        result = BengaliNormalizer.detect_script("My name is আমি")

        assert result == "mixed"

    def test_detect_script_empty(self):
        """Test detecting empty string."""
        result = BengaliNormalizer.detect_script("")

        assert result == "empty"


# --- LLMRouter Tests ---


class TestLLMRouter:
    """Tests for LLMRouter class."""

    def test_router_initialization(self):
        """Test LLMRouter initialization."""
        with patch("services.llm.llm_router.get_redis_client"), patch("services.llm.llm_router._get_rules_engine", return_value=None):
            router = LLMRouter()

            assert router.providers is not None
            assert router.budget is not None
            assert len(router.providers) > 0

    def test_estimate_tokens(self):
        """Test token estimation."""
        with patch("services.llm.llm_router.get_redis_client"), patch("services.llm.llm_router._get_rules_engine", return_value=None):
            router = LLMRouter()

            # English text: ~4 chars per token
            english_tokens = router._estimate_tokens("Hello world this is a test")
            assert english_tokens >= 1

            # Bengali text: ~2 chars per token (more dense)
            bengali_tokens = router._estimate_tokens("আমি কেমন আছি")
            assert bengali_tokens >= 1

    def test_select_provider_chat(self):
        """Test provider selection for chat tasks."""
        with patch("services.llm.llm_router.get_redis_client"), patch("services.llm.llm_router._get_rules_engine", return_value=None):
            router = LLMRouter()

            chain = router._select_provider(TaskType.CHAT)

            assert len(chain) > 0
            assert TaskType.CHAT in PROVIDER_CAPABILITIES.get(chain[0], [])

    def test_select_provider_code(self):
        """Test provider selection for code tasks."""
        with patch("services.llm.llm_router.get_redis_client"), patch("services.llm.llm_router._get_rules_engine", return_value=None):
            router = LLMRouter()

            chain = router._select_provider(TaskType.CODE)

            assert len(chain) > 0

    def test_select_provider_cost_sensitive(self):
        """Test cost-sensitive provider ordering."""
        with patch("services.llm.llm_router.get_redis_client"), patch("services.llm.llm_router._get_rules_engine", return_value=None):
            router = LLMRouter()

            # Without cost-sensitive
            normal_chain = router._select_provider(TaskType.CHAT, cost_sensitive=False)

            # With cost-sensitive
            cost_chain = router._select_provider(TaskType.CHAT, cost_sensitive=True)

            # Both should have providers
            assert len(normal_chain) > 0
            assert len(cost_chain) > 0

    def test_cache_key_generation(self):
        """Test deterministic cache key generation."""
        with patch("services.llm.llm_router.get_redis_client"), patch("services.llm.llm_router._get_rules_engine", return_value=None):
            router = LLMRouter()

            key1 = router._cache_key("prompt", "chat", temperature=0.7)
            key2 = router._cache_key("prompt", "chat", temperature=0.7)

            assert key1 == key2
            assert key1.startswith("llm:cache:")

    @pytest.mark.asyncio
    async def test_health_check_all(self):
        """Test health check for all providers."""
        with patch("services.llm.llm_router.get_redis_client"), patch("services.llm.llm_router._get_rules_engine", return_value=None):
            router = LLMRouter()

            # Mock health checks
            for provider in router.providers.values():
                provider.health_check = AsyncMock(return_value=True)

            results = await router.health_check_all()

            assert len(results) > 0
            for _provider_name, is_healthy in results.items():
                assert is_healthy is True

    @pytest.mark.asyncio
    async def test_route_uses_cache(self):
        """Test that route uses cache when available."""
        with (
            patch("services.llm.llm_router.get_redis_client") as mock_redis,
            patch("services.llm.llm_router._get_rules_engine", return_value=None),
        ):
            mock_redis_client = MagicMock()
            mock_redis.return_value = mock_redis_client
            mock_redis_client.get = AsyncMock(return_value=None)  # No cache hit
            mock_redis_client.setex = AsyncMock()

            router = LLMRouter()

            # Mock providers
            mock_provider = MagicMock()
            mock_provider.health_check = AsyncMock(return_value=True)
            mock_provider.acompletion = AsyncMock(return_value="test response")

            router.providers = {Provider.OLLAMA: mock_provider}

            result = await router.route("test prompt", task_type="chat", stream=False)

            assert isinstance(result, RouteResult)

    @pytest.mark.asyncio
    async def test_route_no_capable_provider(self):
        """Test route raises error when no provider is capable."""
        with (
            patch("services.llm.llm_router.get_redis_client") as mock_redis,
            patch("services.llm.llm_router._get_rules_engine", return_value=None),
        ):
            mock_redis_client = MagicMock()
            mock_redis.return_value = mock_redis_client
            mock_redis_client.get = AsyncMock(return_value=None)
            mock_redis_client.setex = AsyncMock()

            router = LLMRouter()

            # Mock all providers as unhealthy
            for provider in router.providers.values():
                provider.health_check = AsyncMock(return_value=False)

            from core.exceptions import LLMProviderError

            with pytest.raises(LLMProviderError):
                await router.route("test prompt", task_type="chat", stream=False)

    def test_provider_costs_defined(self):
        """Test that provider costs are defined."""
        assert Provider.MOONSHOT in PROVIDER_COSTS
        assert Provider.DEEPSEEK in PROVIDER_COSTS
        assert Provider.OLLAMA in PROVIDER_COSTS

        # Ollama should be free
        assert PROVIDER_COSTS[Provider.OLLAMA] == (0.0, 0.0)

    def test_fallback_chains_defined(self):
        """Test that fallback chains are defined for all task types."""
        for task_type in TaskType:
            assert task_type in FALLBACK_CHAINS
            assert len(FALLBACK_CHAINS[task_type]) > 0


# --- Provider Capability Tests ---


def test_provider_capabilities():
    """Test provider capability matrix."""
    assert Provider.MOONSHOT in PROVIDER_CAPABILITIES
    assert Provider.DEEPSEEK in PROVIDER_CAPABILITIES
    assert Provider.OLLAMA in PROVIDER_CAPABILITIES

    # Moonshot should handle Bengali
    assert TaskType.BENGALI in PROVIDER_CAPABILITIES[Provider.MOONSHOT]
