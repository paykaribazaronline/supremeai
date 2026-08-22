from unittest.mock import MagicMock

from core.resilience.rollback_monitor import RollbackMonitor


class FakeRedis:
    def __init__(self):
        self.store = {}
        self.configured = True

    def incr(self, key):
        val = int(self.store.get(key, 0)) + 1
        self.store[key] = str(val)
        return val

    def set(self, key, value, ex=None):
        self.store[key] = value
        return True

    def get(self, key):
        return self.store.get(key)

    def expire(self, key, time):
        return True


def test_service_name_validation_error():
    m = RollbackMonitor()
    res = m.record_metrics_and_check("bad name with spaces", latency_ms=10, is_error=False)
    assert res["status"] == "error"


def test_redis_not_configured_skip(monkeypatch):
    m = RollbackMonitor()

    # Patch core.services.redis_queue to look unconfigured
    from core import services

    monkeypatch.setattr(services, "redis_queue", MagicMock(configured=False), raising=False)
    res = m.record_metrics_and_check("svc", latency_ms=10, is_error=False)
    assert res["status"] == "ok"
    assert "skipping" in res["message"].lower()


def test_thresholds_not_breached(monkeypatch):
    m = RollbackMonitor(latency_threshold_ms=2000, error_rate_threshold=5.0)

    redis = FakeRedis()
    redis.configured = True
    from core import services

    monkeypatch.setattr(services, "redis_queue", redis, raising=False)
    for _ in range(10):
        res = m.record_metrics_and_check("svc", latency_ms=100, is_error=False)

    assert res["status"] == "ok"
    assert res["error_rate"] == 0.0


def test_thresholds_breached_triggers_rollback(monkeypatch):
    m = RollbackMonitor(latency_threshold_ms=2000, error_rate_threshold=5.0)

    redis = FakeRedis()
    redis.configured = True
    from core import services

    monkeypatch.setattr(services, "redis_queue", redis, raising=False)

    # বাংলা মন্তব্য: gcloud না থাকলে success:False এখন expected outcome (Patch 20 fix)
    # trigger_rollback ব্যবহার করে mock করা হচ্ছে যাতে external gcloud call না হয়
    m.trigger_rollback = MagicMock(return_value={"success": False, "action": "rollback_failed"})

    for i in range(10):
        is_error = i < 6  # 6 errors => 60% error rate
        res = m.record_metrics_and_check("svc", latency_ms=100, is_error=is_error)

    assert res["status"] == "rolled_back"
    assert "rollback_response" in res
    # rollback_response এখন success:False contain করবে gcloud-unavailable হলে
    assert res["rollback_response"]["action"] == "rollback_failed"
    m.trigger_rollback.assert_called_once_with("svc")
