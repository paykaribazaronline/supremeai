# backend/tests/services/test_factory_wiring.py
"""Tests for SupremeAIFactory and Master Wiring."""

import pytest

from core.factory import get_factory


@pytest.mark.asyncio
async def test_factory_production_instance_wiring():
    factory = get_factory()
    ai = await factory.create_production_instance()

    assert ai is not None
    assert ai.initialized is True
    assert ai.rate_limiter is not None
    assert ai.benchmarker is not None
    assert ai.optimizer is not None

    health = factory.health_check()
    assert health["status"] == "healthy"
    assert health["components"]["integrator"] is True
    assert health["components"]["rate_limiter"] is True


@pytest.mark.asyncio
async def test_factory_safe_process():
    factory = get_factory()
    res = await factory.safe_process("Debug Python code: def test(): pass")

    assert res["success"] is True
    assert "answer" in res
    assert res["rate_limited"] is False
