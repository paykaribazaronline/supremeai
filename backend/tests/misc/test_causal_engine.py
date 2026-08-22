from datetime import datetime

import pytest
pd = pytest.importorskip("pandas")

from brain.causal.discovery import CausalDiscoveryEngine
from brain.causal.interventions import (
    Intervention,
    InterventionTracker,
    InterventionType,
)
from brain.causal.root_cause import RootCauseAnalyzer


@pytest.mark.asyncio
async def test_intervention_tracker():
    tracker = InterventionTracker()
    intervention = Intervention(
        id="int_001",
        timestamp=datetime.utcnow(),
        type=InterventionType.CONFIG_CHANGE,
        actor="dev_user",
        target_service="payment_service",
        description="Changed pool size from 10 to 50",
        before_state={"latency_ms": 120},
        after_state={"latency_ms": 45},
    )
    await tracker.log_intervention(intervention)

    experiments = await tracker.get_natural_experiments("payment_service", time_window_hours=24)
    assert len(experiments) == 1
    assert experiments[0].id == "int_001"


@pytest.mark.asyncio
async def test_causal_discovery():
    engine = CausalDiscoveryEngine(algorithm="pc")
    data = pd.DataFrame(
        {
            "config_change": [0, 1, 0, 1, 1],
            "db_latency": [10, 80, 12, 95, 88],
            "error_rate": [0.01, 0.08, 0.01, 0.10, 0.09],
        }
    )

    # Since pandas is mocked, we need to mock the dataframe methods used by discover_graph
    data.columns = ["config_change", "db_latency", "error_rate"]

    # Mock corr() to return a mock correlation matrix
    from unittest.mock import MagicMock

    class MockLoc:
        def __getitem__(self, keys):
            return 0.8

    mock_corr = MagicMock()
    mock_corr.loc = MockLoc()
    data.corr.return_value = mock_corr

    dag = await engine.discover_graph(data)
    assert "nodes" in dag
    assert "edges" in dag
    assert len(dag["nodes"]) == 3


@pytest.mark.asyncio
async def test_root_cause_analyzer():
    analyzer = RootCauseAnalyzer()
    data = pd.DataFrame(
        {
            "lb_config_change": [1, 0, 1, 0, 1],
            "latency_spike": [200, 10, 250, 15, 300],
            "500_errors": [15, 0, 20, 0, 25],
        }
    )

    result = await analyzer.analyze_root_cause("500_errors", data)
    assert "root_cause" in result
    assert "confidence" in result
    assert result["confidence"] > 0.5
    assert "causal_chain" in result
