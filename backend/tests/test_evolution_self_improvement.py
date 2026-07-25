"""Integration tests for evolution and self-improvement.

বাংলা: SelfEvolutionAgent — ফিটনেস ইভ্যালুয়েশন, প্রুনের, এবং অটো-স্কিল রিফ্যাক্টরিং।
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock

import pytest
from core.evolution.self_evolution_agent import SelfEvolutionAgent


@pytest.fixture
def mock_fitness_engine():
    engine = MagicMock()
    engine.metrics = {
        "Skill_A": {
            "success_count": 10,
            "failure_count": 5,
            "total_latency": 15.0,
            "token_cost": 0.0,
            "reuse_count": 15,
        }
    }
    engine.calculate_fitness.return_value = 0.25
    return engine


@pytest.fixture
def mock_auto_skill_creator():
    creator = MagicMock()
    creator.generate_and_deploy_skill = AsyncMock(
        return_value={"success": True, "skill_name": "Skill_A_v2"}
    )
    return creator


@pytest.fixture
def agent(mock_fitness_engine, mock_auto_skill_creator):
    return SelfEvolutionAgent(
        fitness_engine=mock_fitness_engine,
        auto_skill_creator=mock_auto_skill_creator,
        interval_seconds=60,
        min_runs_before_action=5,
        max_consecutive_penalties=3,
        refactor_penalty_threshold=0.3,
        fitness_threshold=0.5,
    )


class TestEvolutionSelfImprovement:
    """Tests for evolution self-improvement."""

    @pytest.mark.asyncio
    async def test_evaluate_skill_prunes_below_threshold(
        self, agent, mock_fitness_engine
    ):
        await agent._evaluate_skill("Skill_A")
        mock_fitness_engine.evaluate_and_prune.assert_called_once_with(
            "Skill_A", 0.5, 5
        )

    @pytest.mark.asyncio
    async def test_evaluate_skill_triggers_refactor_on_consecutive_penalties(
        self, agent, mock_fitness_engine, mock_auto_skill_creator
    ):
        agent._consecutive_penalties["Skill_A"] = 3
        await agent._evaluate_skill("Skill_A")
        mock_auto_skill_creator.generate_and_deploy_skill.assert_called_once()
        assert agent._consecutive_penalties["Skill_A"] == 0

    @pytest.mark.asyncio
    async def test_evaluate_skill_skips_below_min_runs(
        self, agent, mock_fitness_engine
    ):
        mock_fitness_engine.metrics = {
            "Skill_S": {
                "success_count": 2,
                "failure_count": 0,
                "total_latency": 2.0,
                "token_cost": 0.0,
                "reuse_count": 2,
            }
        }
        mock_fitness_engine.calculate_fitness.return_value = 0.3
        await agent._evaluate_skill("Skill_S")
        mock_fitness_engine.evaluate_and_prune.assert_not_called()

    @pytest.mark.asyncio
    async def test_register_missing_path_triggers_generation(
        self, agent, mock_auto_skill_creator
    ):
        agent._has_high_fitness_path = MagicMock(return_value=False)
        await agent._register_missing_path("some demand", "NewSkill")
        mock_auto_skill_creator.generate_and_deploy_skill.assert_called_once()

    def test_agent_initialization(self, agent):
        assert agent.fitness_engine is not None
        assert agent.auto_skill_creator is not None
        assert agent.fitness_threshold == 0.5

    @pytest.mark.asyncio
    async def test_consecutive_penalty_reset_after_refactor(
        self, agent, mock_auto_skill_creator
    ):
        agent._consecutive_penalties["Skill_X"] = 2
        agent._consecutive_penalties["Skill_Y"] = 3
        await agent._evaluate_skill("Skill_Y")
        assert agent._consecutive_penalties["Skill_Y"] == 0
        assert agent._consecutive_penalties["Skill_X"] == 2
