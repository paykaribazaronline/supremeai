# 📄 ফাইল: backend/monitoring/cost_auditor.py

**প্রকার:** .py  
**সাইজ:** 789 বাইট  
**আপডেট:** 2026-07-03T14:04:42.601128

---

## কোড

```py
from typing import Any

from loguru import logger


try:
    from prometheus_client import Counter
    PROMETHEUS_AVAILABLE = True
except ImportError:
    PROMETHEUS_AVAILABLE = False


class CostAuditor:
    def __init__(self) -> None:
        if PROMETHEUS_AVAILABLE:
            self.cost_counter = Counter("supremeai_cost_total", "Total API cost", ["provider", "model"])

    def record_call(self, provider: str, model: str, cost: float) -> None:
        if PROMETHEUS_AVAILABLE:
            try:
                self.cost_counter.labels(provider=provider, model=model).inc(cost)
            except Exception as exc:
                logger.debug(f"Prometheus metric record failed: {exc}")

    def generate_report(self) -> dict[str, Any]:
        return {"status": "ok", "report": ""}

```