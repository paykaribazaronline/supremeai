"""
Coverage tests for core/evolution/daily_learner.py.
Target: 100% line coverage.

ডেইলি লার্নার মডিউলের সকল ফাংশন ও শাখা কভার করা হয়েছে।
"""

import json
import os
import sys
from unittest.mock import AsyncMock, patch

import pytest

_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)


class TestEnums:
    """Tests for GoalStatus and LearningPriority enums."""

    def test_goal_status_values(self):
        """GoalStatus should have correct enum values."""
        from core.evolution.daily_learner import GoalStatus

        assert GoalStatus.PENDING.value == "pending"
        assert GoalStatus.IN_PROGRESS.value == "in_progress"
        assert GoalStatus.COMPLETED.value == "completed"
        assert GoalStatus.FAILED.value == "failed"
        assert GoalStatus.BLOCKED.value == "blocked"

    def test_learning_priority_values(self):
        """LearningPriority should have correct enum values."""
        from core.evolution.daily_learner import LearningPriority

        assert LearningPriority.CRITICAL.value == "critical"
        assert LearningPriority.HIGH.value == "high"
        assert LearningPriority.MEDIUM.value == "medium"
        assert LearningPriority.LOW.value == "low"


class TestSubGoal:
    """Tests for SubGoal dataclass."""

    def test_subgoal_creation(self):
        """SubGoal should be creatable with all fields."""
        from core.evolution.daily_learner import GoalStatus, LearningPriority, SubGoal

        sg = SubGoal(
            id="sg_1",
            description="Test sub-goal",
            dependencies=[],
            estimated_effort=30,
            status=GoalStatus.PENDING,
            priority=LearningPriority.HIGH,
        )
        assert sg.id == "sg_1"
        assert sg.estimated_effort == 30


class TestDiscovery:
    """Tests for Discovery dataclass."""

    def test_discovery_creation(self):
        """Discovery should be creatable with all fields."""
        from core.evolution.daily_learner import Discovery

        d = Discovery(
            title="New Technique",
            type="library",
            source="arxiv",
            summary="A new technique for ML",
            relevance_score=0.85,
            impact_areas=["performance"],
        )
        assert d.title == "New Technique"
        assert d.status == "pending_review"


class TestGoalDecomposer:
    """Tests for GoalDecomposer."""

    def test_init(self):
        """GoalDecomposer should initialize with LLM router."""
        from core.evolution.daily_learner import GoalDecomposer

        with patch("core.evolution.daily_learner.get_cache") as mock_cache:
            mock_cache.return_value = AsyncMock()
            decomposer = GoalDecomposer()
            assert decomposer.llm_router is not None

    def test_cache_key_generation(self):
        """_cache_key should generate consistent cache keys."""
        from core.evolution.daily_learner import GoalDecomposer

        with patch("core.evolution.daily_learner.get_cache") as mock_cache:
            mock_cache.return_value = AsyncMock()
            decomposer = GoalDecomposer()
            key1 = decomposer._cache_key("Build a feature")
            key2 = decomposer._cache_key("Build a feature")
            assert key1 == key2
            assert key1.startswith("learner:")

    @pytest.mark.asyncio
    async def test_decompose_cached(self):
        """decompose should return cached results when available."""
        from core.evolution.daily_learner import GoalDecomposer

        mock_cache = AsyncMock()
        sg_data = [
            {
                "id": "sg_1",
                "description": "Test",
                "dependencies": [],
                "estimated_effort": 30,
                "status": "pending",
                "priority": "high",
            }
        ]
        mock_cache.get.return_value = sg_data

        with patch("core.evolution.daily_learner.get_cache", return_value=mock_cache):
            decomposer = GoalDecomposer()
            result = await decomposer.decompose("Test objective")
            assert len(result) == 1
            assert result[0].id == "sg_1"

    @pytest.mark.asyncio
    async def test_decompose_llm_success(self):
        """decompose should parse LLM response into SubGoals."""
        from core.evolution.daily_learner import GoalDecomposer

        mock_cache = AsyncMock()
        mock_cache.get.return_value = None
        mock_router = AsyncMock()
        mock_router.route.return_value = {
            "content": json.dumps(
                [
                    {
                        "id": "sg_1",
                        "description": "Test",
                        "dependencies": [],
                        "estimated_effort": 30,
                        "priority": "high",
                    }
                ]
            )
        }

        with patch("core.evolution.daily_learner.get_cache", return_value=mock_cache):
            decomposer = GoalDecomposer(llm_router=mock_router)
            result = await decomposer.decompose("Test objective", force_refresh=True)
            assert len(result) == 1
            assert result[0].id == "sg_1"

    @pytest.mark.asyncio
    async def test_decompose_llm_error_fallback(self):
        """decompose should fallback to heuristic on LLM error."""
        from core.evolution.daily_learner import GoalDecomposer

        mock_cache = AsyncMock()
        mock_cache.get.return_value = None
        mock_router = AsyncMock()
        mock_router.route.side_effect = Exception("LLM unavailable")

        with patch("core.evolution.daily_learner.get_cache", return_value=mock_cache):
            decomposer = GoalDecomposer(llm_router=mock_router)
            result = await decomposer.decompose("Develop a code feature", force_refresh=True)
            assert len(result) > 0  # Should return heuristic fallback

    @pytest.mark.asyncio
    async def test_decompose_invalid_json_fallback(self):
        """decompose should fallback on invalid JSON."""
        from core.evolution.daily_learner import GoalDecomposer

        mock_cache = AsyncMock()
        mock_cache.get.return_value = None
        mock_router = AsyncMock()
        mock_router.route.return_value = {"content": "not valid json"}

        with patch("core.evolution.daily_learner.get_cache", return_value=mock_cache):
            decomposer = GoalDecomposer(llm_router=mock_router)
            result = await decomposer.decompose("Develop a feature", force_refresh=True)
            assert len(result) > 0  # Heuristic fallback

    def test_heuristic_fallback_code(self):
        """_heuristic_fallback should generate code-related subgoals."""
        from core.evolution.daily_learner import GoalDecomposer

        with patch("core.evolution.daily_learner.get_cache") as mock_cache:
            mock_cache.return_value = AsyncMock()
            decomposer = GoalDecomposer()
            result = decomposer._heuristic_fallback("Develop a new code feature")
            assert len(result) > 0
            # Should include code-related subgoals
            descriptions = [sg.description for sg in result]
            assert any("code" in desc.lower() for desc in descriptions)

    def test_heuristic_fallback_generic(self):
        """_heuristic_fallback should handle generic objectives."""
        from core.evolution.daily_learner import GoalDecomposer

        with patch("core.evolution.daily_learner.get_cache") as mock_cache:
            mock_cache.return_value = AsyncMock()
            decomposer = GoalDecomposer()
            result = decomposer._heuristic_fallback("Some random goal")
            assert len(result) > 0

    def test_heuristic_fallback_research(self):
        """_heuristic_fallback should generate research-related subgoals."""
        from core.evolution.daily_learner import GoalDecomposer

        with patch("core.evolution.daily_learner.get_cache") as mock_cache:
            mock_cache.return_value = AsyncMock()
            decomposer = GoalDecomposer()
            result = decomposer._heuristic_fallback("Research new techniques")
            assert len(result) > 0


class TestConstants:
    """Tests for module constants."""

    def test_constants(self):
        """Module constants should have correct values."""
        from core.evolution.daily_learner import (
            GOAL_DECOMPOSITION_TTL,
            IMPACT_WEIGHTS,
            LEARNER_CACHE_TTL,
            MAX_CONCURRENT_LEARNS,
        )

        assert LEARNER_CACHE_TTL == 1800
        assert MAX_CONCURRENT_LEARNS == 5
        assert GOAL_DECOMPOSITION_TTL == 3600
        assert "user_facing" in IMPACT_WEIGHTS
        assert IMPACT_WEIGHTS["user_facing"] == 0.35
