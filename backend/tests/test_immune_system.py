from unittest.mock import MagicMock

import pytest

from core.auto_remediation import AutoRemediation
from core.rollback_monitor import RollbackMonitor
from core.rules_mutator import RulesMutator


@pytest.fixture
def mock_redis(monkeypatch):
    queue = MagicMock()
    queue.configured = True
    # Default return for GET is None
    queue.get.return_value = None
    queue.incr.return_value = 1

    import core.app as core_app

    monkeypatch.setattr(core_app, "redis_queue", queue, raising=True)
    return queue


def test_auto_remediation_success(tmp_path):
    # Create a temporary file to test patch application
    test_file = tmp_path / "test_vuln.py"
    test_file.write_text("password = 'hardcoded_secrets'\n", encoding="utf-8")

    remediator = AutoRemediation(gemini_api_key="")

    res = remediator.process_security_alert(
        file_path=str(test_file),
        line_number=1,
        issue="Hardcoded secret detected",
        severity="high",
    )

    assert res["success"] is True
    assert res["patch_applied"] is True
    assert "supremeai-improvements" in res["branch"]

    # Verify file content was patched (mock prefix added since api key is empty)
    patched_content = test_file.read_text(encoding="utf-8")
    assert "Secure Patch Applied" in patched_content


def test_rules_mutator_blocks_ip(mock_redis):
    mutator = RulesMutator()
    ip = "192.168.1.50"

    # Mock redis check returns something when blocked
    mock_redis.get.return_value = "blocked:suspicious_activity"
    assert mutator.is_ip_blocked(ip) is True

    # Try blocking
    mock_redis.get.return_value = None
    res = mutator.block_ip(ip, reason="ddos_attempt")
    assert res is True
    mock_redis.set.assert_called_with(f"blocklist:ip:{ip}", "blocked:ddos_attempt", ex=1800)


def test_rollback_monitor_triggers_rollback(mock_redis):
    monitor = RollbackMonitor(latency_threshold_ms=1000.0, error_rate_threshold=10.0)
    service = "supremeai-api-service"

    # Mock redis increment sequence to simulate 10 requests with 3 errors
    # (3/10 = 30% error rate, which breaches the 10% threshold)
    mock_redis.incr.return_value = 10  # Requests count
    mock_redis.get.side_effect = lambda k: "3.0" if "errors" in k else ("10" if "total" in k else "15000.0")

    res = monitor.record_metrics_and_check(service, latency_ms=1500.0, is_error=True)

    assert res["status"] == "rolled_back"
    assert res["error_rate"] == 30.0
    assert res["rollback_response"]["success"] is True
