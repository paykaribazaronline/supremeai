# 📄 ফাইল: backend/core/config_proxy.py

**প্রকার:** .py  
**সাইজ:** 1,892 বাইট  
**আপডেট:** 2026-07-11T16:26:09.333762

---

## কোড

```py
import asyncio
from datetime import datetime
from datetime import timedelta
from typing import Any

from loguru import logger


class DynamicConfigProxy:
    def __init__(self, tenant_id: str, db: Any):
        self._tenant_id = tenant_id
        self._db = db
        self._cache = {}
        self._expiry = datetime.min

    async def get(self, key: str, default: Any = None) -> Any:
        # TTL চেক (১ মিনিট)
        if datetime.now() > self._expiry:
            await self._refresh_cache()

        return self._cache.get(key, default)

    async def _refresh_cache(self):
        try:
            doc_ref = self._db.collection(f"tenants/{self._tenant_id}/config/runtime").document("settings")

            # handle both sync and async get() based on the db client
            if asyncio.iscoroutinefunction(doc_ref.get):
                snapshot = await doc_ref.get()
            else:
                snapshot = doc_ref.get()

            if snapshot.exists:
                self._cache = snapshot.to_dict()
                self._expiry = datetime.now() + timedelta(minutes=1)
            else:
                # ডামি ডকুমেন্ট তৈরি করা হচ্ছে (fallback/test)
                self._cache = {
                    "DEFAULT_CODE_SMELL_THRESHOLDS": {
                        "complexity": 10,
                        "lines": 75,
                        "args": 5,
                        "class_methods": 15,
                    },
                    "COMMON_STRINGS_TO_IGNORE": ["", "utf-8", "rb", "wb", "r", "w", "a", "x", "b", "t", "+"],
                }
                self._expiry = datetime.now() + timedelta(minutes=1)
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to refresh config from DB: {e}")
            raise RuntimeError(f"Failed to refresh config from DB: {e}") from e

```