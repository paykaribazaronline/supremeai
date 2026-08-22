import pytest

from reports.optimization_engine import OptimizationEngine


@pytest.fixture
def engine():
    return OptimizationEngine()


@pytest.mark.asyncio
async def test_weekly_audit(engine):
    result = await engine.weekly_audit()
    assert result == {"period": "weekly", "recommendations": []}


@pytest.mark.asyncio
async def test_suggest_free_alternatives(engine):
    result = await engine.suggest_free_alternatives("openai")
    assert result == []
