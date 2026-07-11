# 📄 ফাইল: backend/api/routes/traffic_monitor.py

**প্রকার:** .py  
**সাইজ:** 2,665 বাইট  
**আপডেট:** 2026-07-11T18:21:34.918098

---

## কোড

```py
import json
import time
from typing import Any

from fastapi import APIRouter
from fastapi import Depends
from fastapi import HTTPException

from api.routes.admin import get_current_admin
from core.redis_manager import redis_manager


router = APIRouter(prefix="/api/admin/traffic", tags=["traffic"])


@router.get("/live")
async def get_live_traffic(admin: dict = Depends(get_current_admin)) -> dict[str, Any]:
    """বাংলা মন্তব্য: রিয়েল-টাইম ট্রাফিক, p95 ল্যাটেন্সি এবং অ্যারর রেট।
    এটি স্টুডিও ক্লায়েন্ট বা ফ্লাটার ড্যাশবোর্ডে লাইভ স্ট্রিমিং এর জন্য ব্যবহার হবে।"""

    if not redis_manager.client:
        raise HTTPException(status_code=503, detail="Redis is not connected.")

    now = int(time.time())
    current_minute = now // 60
    # Fetch data for the current and previous minute to have a smooth rolling window
    keys_to_fetch = [f"traffic:live:{current_minute}", f"traffic:live:{current_minute - 1}"]

    total_requests = 0
    errors = 0
    durations = []

    try:
        for key in keys_to_fetch:
            # Fetch last 1000 items from each key
            raw_data = await redis_manager.client.lrange(key, 0, 999)
            for item_str in raw_data:
                try:
                    item = json.loads(item_str)
                    total_requests += 1
                    if item.get("status", 200) >= 400 or item.get("error"):
                        errors += 1
                    durations.append(item.get("duration", 0.0))
                except json.JSONDecodeError:
                    continue

        # Calculate metrics
        error_rate = (errors / total_requests) * 100 if total_requests > 0 else 0.0

        p95_latency = 0.0
        if durations:
            durations.sort()
            p95_idx = int(len(durations) * 0.95)
            p95_latency = durations[p95_idx] * 1000  # convert to ms

        requests_per_second = total_requests / 120.0  # roughly over a 2 minute window

        return {
            "status": "success",
            "data": {
                "requests_per_second": round(requests_per_second, 2),
                "total_requests_window": total_requests,
                "error_rate_percent": round(error_rate, 2),
                "p95_latency_ms": round(p95_latency, 2),
                "timestamp": now,
            },
        }
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(e)) from e

```