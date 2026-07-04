# 📄 ফাইল: backend/tests/monitoring/test_cost_auditor.py

**প্রকার:** .py  
**সাইজ:** 436 বাইট  
**আপডেট:** 2026-07-04T08:12:03.177532

---

## কোড

```py
import sys
from unittest.mock import patch


sys.path.append("../..")
from monitoring.cost_auditor import CostAuditor


class TestCostAuditor:
    def test_init(self):
        auditor = CostAuditor()
        assert auditor is not None

    def test_record_call(self):
        with patch("monitoring.cost_auditor.PROMETHEUS_AVAILABLE", False):
            auditor = CostAuditor()
            auditor.record_call("openai", "gpt-4", 0.05)

```