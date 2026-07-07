# 📄 ফাইল: backend/utils/api_tracker.py

**প্রকার:** .py  
**সাইজ:** 686 বাইট  
**আপডেট:** 2026-07-07T18:04:16.093158

---

## কোড

```py
from dataclasses import dataclass


@dataclass
class APICallRecord:
    provider: str
    model: str
    tokens: int
    cost: float
    latency_ms: float
    timestamp: float
    success: bool
    error: str | None = None


class APITracker:
    def __init__(self) -> None:
        self.records: list[APICallRecord] = []

    def record(self, record: APICallRecord) -> None:
        self.records.append(record)

    def get_recent(self, limit: int = 100) -> list[APICallRecord]:
        return self.records[-limit:]


_tracker: APITracker | None = None


def get_tracker() -> APITracker:
    global _tracker
    if _tracker is None:
        _tracker = APITracker()
    return _tracker

```