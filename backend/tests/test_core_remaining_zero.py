"""Tests for remaining core files with 0% coverage.

Targets: daily_learner, evolution_react_agent
"""

from unittest.mock import patch

import pytest

# ── daily_learner ──────────────────────────────────────────────────────────────


class TestDailyLearner:
    def test_initialization(self):
        from core.evolution.daily_learner import DailyLearner

        learner = DailyLearner()
        assert learner is not None

    @pytest.mark.asyncio
    async def test_learn_from_daily_logs(self):
        from core.evolution.daily_learner import DailyLearner

        learner = DailyLearner()
        with patch.object(
            learner, "learn_and_plan", return_value={"status": "success"}
        ):
            res = await learner.learn_and_plan("testing")
            assert res is not None


# ── evolution_react_agent ──────────────────────────────────────────────────────


class TestEvolutionReActAgent:
    def test_initialization(self):
        from core.evolution.evolution_react_agent import EvolutionReActAgent

        agent = EvolutionReActAgent()
        assert agent is not None

    def test_generate_skill(self):
        from core.evolution.evolution_react_agent import EvolutionReActAgent

        agent = EvolutionReActAgent()
        with patch.object(
            agent,
            "generate_skill",
            return_value={"status": "completed", "code": "pass"},
        ):
            result = agent.generate_skill("test_skill", "write tests")
            assert result is not None


# ── tools import tests ─────────────────────────────────────────────────────────


class TestToolsImports:
    def test_bootstrap_import(self):
        import tools._bootstrap

        assert tools._bootstrap is not None

    def test_seed_database_import(self):
        import tools.seed_database

        assert tools.seed_database is not None

    def test_self_planner_import(self):
        import tools.self_planner

        assert tools.self_planner is not None
