# 📄 ফাইল: backend/api/routes/selector_healing.py

**প্রকার:** .py  
**সাইজ:** 1,381 বাইট  
**আপডেট:** 2026-07-07T19:02:10.496881

---

## কোড

```py
import time

from fastapi import APIRouter
from pydantic import BaseModel


router = APIRouter(prefix="/api/admin/selector-healing", tags=["Self-Healing Logs"])

class HealingEventOut(BaseModel):
    id: str
    ts: str
    action_id: int
    original_selector: str
    healed_selector: str
    confidence_score: int
    auto_applied: bool
    screenshot_before_base64: str = ""
    screenshot_after_base64: str = ""

class DecisionIn(BaseModel):
    approve: bool

# In-memory mock for now since the DB schema (selector_healing_event) is handled by SQLAlchemy in phase 1
MOCK_EVENTS = [
    {
        "id": "evt_001",
        "ts": time.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "action_id": 4,
        "original_selector": "#login-form > div.submit-wrapper > button",
        "healed_selector": "button[data-testid='login-submit']",
        "confidence_score": 98,
        "auto_applied": False,
        "screenshot_before_base64": "",
        "screenshot_after_base64": ""
    }
]

@router.get("/")
def get_healing_logs():
    return {"items": MOCK_EVENTS}

@router.post("/{event_id}/decision")
def make_healing_decision(event_id: str, payload: DecisionIn):
    for evt in MOCK_EVENTS:
        if evt["id"] == event_id:
            evt["auto_applied"] = payload.approve
            return {"status": "success", "event": evt}
    return {"status": "error", "message": "not found"}

```