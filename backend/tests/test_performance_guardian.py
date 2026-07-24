import sys
from unittest.mock import AsyncMock, MagicMock

import pytest

# Import guard: agents package init may import optional google.genai.
if "google" not in sys.modules:
    sys.modules["google"] = MagicMock()
if "google.genai" not in sys.modules:
    sys.modules["google.genai"] = MagicMock()

from agents.performance_guardian import AnomalyDetector, PerformanceGuardian


def test_anomaly_detector_requires_minimum_points():
    det = AnomalyDetector()
    ok, z = det.detect("cpu", [1, 2, 3, 4], threshold=2.0)
    assert ok is False
    assert z == 0.0


def test_anomaly_detector_detects_outlier():
    det = AnomalyDetector()
    values = [10.0, 10.1, 9.9, 10.0, 100.0]
    ok, z = det.detect("cpu", values, threshold=2.0)
    assert ok is True
    assert z > 0


@pytest.mark.anyio
async def test_check_health_builds_alerts(monkeypatch):
    pg = PerformanceGuardian()

    pg.collector.collect_system_metrics = MagicMock(
        return_value={
            "cpu_percent": 90.0,
            "memory_percent": 90.0,
            "disk_percent": 0.0,
            "network_io": 0.0,
            "timestamp": 0.0,
        }
    )

    res = await pg.check_health()
    assert res["status"] in {"degraded", "healthy"}
    assert len(res["alerts"]) >= 2


@pytest.mark.anyio
async def test_analyze_bottleneck_uses_llm_and_cache(monkeypatch):
    pg = PerformanceGuardian()

    pg.collector.collect_system_metrics = MagicMock(
        return_value={"cpu_percent": 1.0, "memory_percent": 2.0}
    )

    pg.cache.get = AsyncMock(return_value=None)
    pg.cache.set = AsyncMock(return_value=True)

    pg.llm.route = AsyncMock(return_value={"content": "analysis"})

    res = await pg.analyze_bottleneck("op", duration_ms=123.0)
    assert res["analysis"] == "analysis"
