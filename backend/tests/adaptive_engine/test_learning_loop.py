from datetime import UTC, datetime, timedelta
from unittest.mock import MagicMock

import pytest
from adaptive_engine.learning_loop import (ExperienceClusterer,
                                           LearningCycleResult, LearningLoop,
                                           PerformanceDriftDetector)


# Test cases for ExperienceClusterer
def test_experience_clusterer():
    clusterer = ExperienceClusterer()
    experiences = [
        {"result": "success", "action_taken": "query_db"},
        {
            "result": "failure",
            "error_message": "connection timeout",
            "action_taken": "query_db",
        },
        {
            "result": "failure",
            "error_message": "redis connection error",
            "action_taken": "cache_fetch",
        },
        {
            "result": "failure",
            "error_message": "timeout on query",
            "action_taken": "query_db",
        },
    ]
    clusters = clusterer.cluster_failures(experiences)
    assert len(clusters) > 0
    # Both database timeouts should be grouped or handled accordingly
    assert "generic_failure" in clusters or len(clusters) >= 1


# Test cases for PerformanceDriftDetector
def test_performance_drift_detector():
    detector = PerformanceDriftDetector(window_size=10, z_threshold=1.5)

    # Record baseline metric
    for _ in range(5):
        detector.record_metric("gemini", 100.0, True)

    # Record degraded metric (high latency, high error)
    for _ in range(5):
        detector.record_metric("gemini", 500.0, False)

    drift = detector.detect_drift("gemini")
    assert drift is not None
    assert drift["drift_detected"] is True
    assert drift["recommendation"] == "consider_fallback"


@pytest.mark.asyncio
async def test_learning_loop_cycle_success():
    # Setup mock experience db
    mock_db = MagicMock()
    mock_db.get_all_experiences = MagicMock(
        return_value=[
            {
                "timestamp": (datetime.now(UTC) - timedelta(hours=1)).isoformat(),
                "result": "failure",
                "error_message": "auth token expired",
                "action_taken": "api_call",
                "user_feedback": "negative",
            }
        ]
    )

    # Reset singleton instance
    LearningLoop._instance = None
    loop = LearningLoop(experience_db=mock_db)

    result = await loop.run_cycle()
    assert isinstance(result, LearningCycleResult)
    assert result.status == "completed"
    assert result.total_experiences == 1
    assert len(result.insights_generated) > 0

    stats = loop.get_stats()
    assert stats["total_cycles"] == 1
    assert stats["total_insights"] > 0
