# backend/tests/services/test_living_engine.py
import pytest
from unittest.mock import MagicMock

from services.living_engine import LivingEngineOrchestrator


@pytest.fixture
def mock_memory():
    mock = MagicMock()
    mock.store_memory.return_value = None
    mock.search_memory.return_value = []
    mock.retrieve_recent.return_value = []
    return mock


@pytest.mark.asyncio
async def test_living_engine_solves_bengali_bugfix_demand(mock_memory):
    orchestrator = LivingEngineOrchestrator(memory_service=mock_memory)
    prompt = "ডাটাবেস কানেকশন ক্র্যাশ করছে, এটা ঠিক করো"

    solution = await orchestrator.solve_unpredictable_demand(prompt, session_id="test_sess_1")

    assert solution.success is True
    assert solution.domain in ["coder", "bengali"]
    assert len(solution.execution_order) >= 4
    assert solution.fitness_score > 0.5
    assert solution.error is None


@pytest.mark.asyncio
async def test_living_engine_solves_performance_optimization_demand(mock_memory):
    orchestrator = LivingEngineOrchestrator(memory_service=mock_memory)
    prompt = "API response latency is slow, optimize query caching"

    solution = await orchestrator.solve_unpredictable_demand(prompt, session_id="test_sess_2")

    assert solution.success is True
    assert len(solution.execution_order) >= 4
    assert solution.fitness_score > 0.5


@pytest.mark.asyncio
async def test_living_engine_solves_rbac_security_demand(mock_memory):
    orchestrator = LivingEngineOrchestrator(memory_service=mock_memory)
    prompt = "Add RBAC role access guards to all unprotected routes"

    solution = await orchestrator.solve_unpredictable_demand(prompt, session_id="test_sess_3")

    assert solution.success is True
    assert len(solution.execution_order) >= 4


@pytest.mark.asyncio
async def test_living_engine_solves_dynamic_synthesis_demand(mock_memory):
    orchestrator = LivingEngineOrchestrator(memory_service=mock_memory)
    prompt = "Create a custom calculation for user discounts"

    solution = await orchestrator.solve_unpredictable_demand(prompt, session_id="test_sess_4")

    assert solution.success is True
    d = solution.to_dict()
    assert "execution_order" in d
    assert "fitness_score" in d
    assert "execution_time_ms" in d
