import sys
from unittest.mock import patch


sys.path.append("../..")
from tools.billing.cost_auditor import CostAuditor


class TestCostAuditor:
    def test_init(self):
        auditor = CostAuditor()
        assert auditor is not None

    def test_record_call_disabled(self):
        with patch("monitoring.cost_auditor.PROMETHEUS_AVAILABLE", False):
            auditor = CostAuditor()
            auditor.record_call("openai", "gpt-4", 0.05)
