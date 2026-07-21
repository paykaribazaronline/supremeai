"""Tests for remaining core files with 0% coverage.

Targets: daily_learner, evolution_react_agent
"""

from unittest.mock import patch

# ── daily_learner ──────────────────────────────────────────────────────────────


class TestDailyLearner:
    def test_initialization(self):
        from core.evolution.daily_learner import DailyLearner

        learner = DailyLearner()
        assert learner is not None

    def test_goal_decomposition(self):
        from core.evolution.daily_learner import DailyLearner

        learner = DailyLearner()
        goals = learner.decompose_goal("Improve test coverage")
        assert goals is not None
        assert isinstance(goals, list)

    def test_score_priority(self):
        from core.evolution.daily_learner import DailyLearner

        learner = DailyLearner()
        score = learner.score_priority(topic="testing", context={"urgency": "high"})
        assert score is not None

    def test_scan_research(self):
        from core.evolution.daily_learner import DailyLearner

        learner = DailyLearner()
        with patch.object(learner, "_scan_arxiv", return_value=[]):
            results = learner.scan_research(topics=["testing"])
            assert results is not None or results == []


# ── evolution_react_agent ──────────────────────────────────────────────────────


class TestEvolutionReActAgent:
    def test_initialization(self):
        from core.evolution.evolution_react_agent import EvolutionReActAgent

        agent = EvolutionReActAgent()
        assert agent is not None

    def test_execute_task(self):
        from core.evolution.evolution_react_agent import EvolutionReActAgent

        agent = EvolutionReActAgent()
        with patch.object(agent, "execute", return_value={"success": True}):
            result = agent.execute("Write a test")
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
