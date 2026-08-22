"""Unit tests for Layer 6 Self-Evolution (Meta-AI) database models and logic."""

# বাংলা মন্তব্য: মেটা-এআই ব্রিডার ও পারফরম্যান্স ওরাল মডিউলের জন্য ইউনিট টেস্টসমূহ।

from __future__ import annotations

import uuid
from unittest.mock import AsyncMock, patch

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from core.evolution.agent_breeder import AgentBreeder, BreederConfig
from core.evolution.performance_oracle import OracleConfig, PerformanceOracle
from models.meta_ai import AgentGenome, AgentStatus, MetricType


@pytest.mark.asyncio
async def test_meta_ai_models_and_logic():
    """Verify Meta-AI models can be populated and database operations work."""
    # বাংলা মন্তব্য: ডাটাবেজ মডেল ও জেনেটিক সিলেকশন লজিক মক করে টেস্ট করা
    db_mock = AsyncMock(spec=AsyncSession)

    config = BreederConfig(
        mutation_rate=0.1,
        crossover_rate=0.9,
        elite_ratio=0.1,
        tournament_size=2,
        max_generations=10,
        llm_temperature=0.3,
        llm_model_name="mock-gemini",
    )

    breeder = AgentBreeder(db_mock, config=config)

    p1 = AgentGenome(
        id=uuid.uuid4(),
        agent_name="parent_a",
        chromosome={"prompt_template": "Prompt A", "temperature": 0.7},
        fitness_score=0.8,
        generation=1,
        status=AgentStatus.ACTIVE,
        lineage=[],
    )

    p2 = AgentGenome(
        id=uuid.uuid4(),
        agent_name="parent_b",
        chromosome={"prompt_template": "Prompt B", "temperature": 0.5},
        fitness_score=0.75,
        generation=1,
        status=AgentStatus.ACTIVE,
        lineage=[],
    )

    # Test breeding method directly
    with patch("litellm.acompletion", new_callable=AsyncMock) as mock_complete:
        mock_complete.return_value.choices[0].message.content = "Improved text prompt"
        offspring = await breeder.breed(p1, p2, offspring_name="child_agent")

        assert offspring.offspring_name == "child_agent"
        assert offspring.parent_a_id == p1.id
        assert offspring.parent_b_id == p2.id
        assert offspring.chromosome is not None

    # Test evaluation
    fitness = await breeder.evaluate_offspring(offspring)
    assert fitness > 0.0

    # Test promotion
    promoted = await breeder.promote_if_elite(offspring, p1, p2)
    assert promoted is None  # since offspring fitness is 0.5 by default which is <= parent fitness 0.8

    # Verify PerformanceOracle
    oracle = PerformanceOracle(db_mock, config=OracleConfig.from_settings())
    metric = await oracle.record_metric("test_agent", MetricType.RESPONSE_TIME_MS, 150.0, "ms")
    assert metric.agent_name == "test_agent"
    assert metric.value == 150.0
