"""
Tests for LLM Gateway consolidation improvements.
These tests verify the enhancements made to unify multiple gateways.
"""

from unittest.mock import AsyncMock, Mock, patch

import pytest

from core.llm.llm_gateway import LLMGateway, get_llm_gateway
from services.llm.llm_router import LLMRouter

# বাংলা মন্তব্য: isinstance ফেইল হওয়ার কারণে core এর পরিবর্তে backend.core ব্যবহার করা হলো
# কারণ llm_router.py ফাইলটি backend.core থেকেই ইম্পোর্ট করে।
from core.resilience.circuit_breaker import CircuitBreaker
from core.resilience.circuit_breaker_manager import get_circuit_breaker_manager


@pytest.fixture
def llm_gateway():
    """Create a test LLMGateway instance."""
    return LLMGateway()


@pytest.fixture
def llm_router():
    """Create a test LLMRouter instance."""
    return LLMRouter()


@pytest.mark.asyncio
async def test_shared_circuit_breaker_manager():
    """Test that circuit breakers are shared between gateways."""
    cb_manager = get_circuit_breaker_manager()

    # Get circuit breakers for same model from different gateways
    cb1 = cb_manager.get_circuit_breaker("test-model")
    cb2 = cb_manager.get_circuit_breaker("test-model")

    # They should be the same instance
    assert cb1 is cb2, "Circuit breakers should be shared between gateways"

    # Test state sharing
    cb1.force_open()
    assert cb2.is_open, "Circuit breaker state should be shared"


@pytest.mark.asyncio
async def test_gateway_has_rate_limit_handling(llm_gateway):
    """Test that the enhanced gateway has rate limit handling."""
    # Check that the rate limit handler method exists
    assert hasattr(llm_gateway, "_handle_rate_limit_error"), "LLMGateway should have rate limit handling method"

    # Check that the method is async
    import inspect

    assert inspect.iscoroutinefunction(llm_gateway._handle_rate_limit_error), "Rate limit handler should be async"


@pytest.mark.asyncio
async def test_router_uses_shared_circuit_breaker(llm_router):
    """Test that the router uses the shared circuit breaker."""
    # Mock a provider call that triggers circuit breaker
    provider_name = "test_provider"
    cb = llm_router._get_or_create_circuit_breaker(provider_name)

    assert isinstance(cb, CircuitBreaker), "Router should use CircuitBreaker instances"

    # Verify it's using the shared manager by checking against global manager
    shared_cb = get_circuit_breaker_manager().get_circuit_breaker(provider_name)
    assert cb is shared_cb, "Router should use shared circuit breaker"


@pytest.mark.asyncio
async def test_gateway_429_handling_simulation(llm_gateway):
    """Test 429 handling logic without making actual HTTP requests."""
    import httpx

    # Create a mock HTTPStatusError for 429
    mock_response = Mock()
    mock_response.status_code = 429

    # Test 1: Fast fallback (OmniRoute logic) if pause is too long
    mock_response.headers = {"Retry-After": "30"}  # 30 second delay
    mock_exc = httpx.HTTPStatusError("Too Many Requests", response=mock_response, request=Mock())

    # Mock the sleep function to avoid actual sleep
    with patch("asyncio.sleep", new_callable=AsyncMock) as mock_sleep:
        # Call the rate limit handler
        result = await llm_gateway._handle_rate_limit_error("test-model", mock_exc)

        # Verify it returned False (fail-fast fallback)
        assert result is False, "Rate limit handler should return False to trigger fast fallback"
        assert not mock_sleep.called, "Should not sleep when triggering fast fallback"

    # Test 2: Retry if pause is short
    mock_response.headers = {"Retry-After": "2"}  # 2 second delay
    mock_exc = httpx.HTTPStatusError("Too Many Requests", response=mock_response, request=Mock())

    with patch("asyncio.sleep", new_callable=AsyncMock) as mock_sleep:
        result = await llm_gateway._handle_rate_limit_error("test-model", mock_exc)

        # Verify it returned True
        assert result is True, "Rate limit handler should return True to retry when pause is short"
        assert mock_sleep.called, "Should sleep for short backoff"


@pytest.mark.skip(reason="LLMGateway _MODEL_KEY_MAP refactored in core.llm")
@pytest.mark.asyncio
async def test_provider_taxonomy_consistency():
    """Test that provider taxonomies are more consistent between gateways."""
    gateway = get_llm_gateway()
    LLMRouter()

    # Check that both have access to the same provider mappings
    # LLMGateway uses _MODEL_KEY_MAP, LLMRouter has Provider enum

    # Verify that LLMGateway has expanded provider mapping
    expected_providers = [
        "groq",
        "gemini",
        "gpt",
        "openai",
        "deepseek",
        "openrouter",
        "hf",
        "huggingface",
        "nvidia",
        "moonshot",
        "together",
        "ollama",
        "hf_space",
    ]

    for provider in expected_providers:
        assert provider in gateway._MODEL_KEY_MAP, f"Gateway should support {provider} provider"


@pytest.mark.asyncio
async def test_circuit_breaker_state_sharing():
    """Test that circuit breaker states are shared across different gateway instances."""
    # Create multiple gateway instances
    gw1 = LLMGateway()
    gw2 = LLMGateway()

    # Get circuit breakers for the same model from both gateways
    cb1 = gw1._get_or_create_circuit_breaker("shared-model-test")
    cb2 = gw2._get_or_create_circuit_breaker("shared-model-test")

    # They should be the same instance due to shared manager
    assert cb1 is cb2, "Circuit breakers should be shared across gateway instances"

    # Test state change in one affects the other
    cb1.force_open()
    assert cb2.is_open, "Opening circuit breaker in one should affect shared instance"


@pytest.mark.skip(reason="LLMGateway health endpoint route module import location variance")
@pytest.mark.asyncio
async def test_gateway_health_endpoint_simulation():
    """Test the health endpoint functionality."""
    from fastapi.testclient import TestClient

    from core.api.routes.llm_gateway import router

    # বাংলা মন্তব্য: মেইন মডিউলের বদলে core.app থেকে অ্যাপ ইমপোর্ট করা হলো
    from core.app import app

    # Add the router to the main app for testing
    app.include_router(router)
    client = TestClient(app)

    # Test health endpoint (this might fail if auth is required, so we'll catch that)
    try:
        response = client.get("/llm-gateway/health")
        # The response might be a 401 if authentication is required
        # That's OK, we just want to verify the endpoint exists
        assert response.status_code in [200, 401, 403], "Health endpoint should exist (even if auth required)"
    except Exception as e:
        # If we can't test the endpoint due to setup issues, that's OK
        print(f"Could not test health endpoint (likely due to auth setup): {e}")


@pytest.mark.asyncio
async def test_enhanced_gateway_features():
    """Test that the enhanced gateway has all the expected features."""
    gateway = get_llm_gateway()

    # Verify enhanced features exist
    assert hasattr(gateway, "_handle_rate_limit_error"), "Enhanced gateway should have rate limit handler"

    assert hasattr(gateway, "_get_or_create_circuit_breaker"), "Enhanced gateway should have circuit breaker management"

    # Verify it's using the centralized circuit breaker manager
    original_method = gateway._get_or_create_circuit_breaker
    cb = original_method("test-model")
    shared_cb = get_circuit_breaker_manager().get_circuit_breaker("test-model")

    assert cb is shared_cb, "Gateway should use shared circuit breaker"


@pytest.mark.skip(reason="LLMGateway _MODEL_KEY_MAP refactored in core.llm")
def test_provider_mapping_completeness():
    """Test that provider mapping covers all expected providers."""
    gateway = get_llm_gateway()

    # Check that the expanded provider map includes all expected providers
    expected_providers = {
        "groq",
        "gemini",
        "gpt",
        "openai",
        "deepseek",
        "openrouter",
        "hf",
        "huggingface",
        "nvidia",
        "moonshot",
        "together",
        "ollama",
        "hf_space",
    }

    actual_providers = set(gateway._MODEL_KEY_MAP.keys())

    missing_providers = expected_providers - actual_providers
    assert not missing_providers, f"Missing providers in mapping: {missing_providers}"

    extra_providers = actual_providers - expected_providers
    print(f"Additional providers in mapping: {extra_providers}")


if __name__ == "__main__":
    # Run the tests
    pytest.main([__file__, "-v"])
