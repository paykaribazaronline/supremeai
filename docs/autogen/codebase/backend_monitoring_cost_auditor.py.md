# 📄 ফাইল: backend/monitoring/cost_auditor.py

**প্রকার:** .py  
**সাইজ:** 806 বাইট  
**আপডেট:** 2026-07-10T19:10:52.068919

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
            except Exception as exc:  # noqa: BLE001
                logger.debug(f"Prometheus metric record failed: {exc}")

    def generate_report(self) -> dict[str, Any]:
        return {"status": "ok", "report": ""}

```