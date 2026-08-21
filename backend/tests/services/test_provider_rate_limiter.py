# backend/tests/services/test_provider_rate_limiter.py
"""Tests for IntelligentRateLimiter and CloudDBTester."""

import pytest
from core.provider_rate_limiter import IntelligentRateLimiter, get_provider_rate_limiter
from tests.cloud_db_load_test import CloudDBTester


@pytest.mark.asyncio
async def test_intelligent_rate_limiter_request():
    limiter = get_provider_rate_limiter()
    res = await limiter.make_request("Explain quicksort in Python")
    assert res["success"] is True
    assert res["provider_used"] in ["Gemini", "Groq", "OpenRouter", "Ollama Local", "gemini", "groq", "openrouter", "ollama_local"]


@pytest.mark.asyncio
async def test_cloud_db_tester_quick_test():
    tester = CloudDBTester()
    init_ok = await tester.initialize()
    assert init_ok is True

    health = await tester.health_check()
    assert health.connection_ok is True

    res = await tester.run_quick_test()
    assert res.successful_ops > 0
    assert res.error_rate == 0.0
