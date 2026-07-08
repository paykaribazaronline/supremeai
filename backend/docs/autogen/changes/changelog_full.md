# 📜 SupremeAI 2.0 Centralized Changelog

# 📋 Auto-Fix Commit 26c27f2d

## Commit Stats
```
commit 26c27f2d5cc1672b3622f4ff1baca96521e731bc
Author: SupremeAI Bot <bot@supremeai.dev>
Date:   2026-07-08 01:48:57 UTC

    fix: auto-fix applied for CI failure

    File: config_cache.py
```

## 🤖 AI Auto-Fix Context

| Field | Value |
|-------|-------|
| **Fixed File** | `core/config_cache.py` |
| **AI Model Used** | `gemini/gemini-2.5-flash` |
| **Timestamp** | 2026-07-08 01:48:57 UTC |
| **Branch** | `main` |
| **Commit** | [`26c27f2d`](https://github.com/paykaribazaronline/supremeai/commit/26c27f2d5cc1672b3622f4ff1baca96521e731bc) |

## Error Log (Truncated)
```
FFFFFFFFFF.FFFFFFFFFFF                                                   [100%]
=================================== FAILURES ===================================
_ TestConfigCacheMissingBranches.test_r
```

## Diff Detail
```diff
diff --git a/backend/core/config_cache.py b/backend/core/config_cache.py
index d75f526..8515a2e 100644
--- a/backend/core/config_cache.py
+++ b/backend/core/config_cache.py
@@ -1,3 +1,4 @@
+# FILE_PATH: core/config_cache.py
 """
 config_cache.py — Lightweight In-Memory Config Cache
 ======================================================
@@ -10,23 +11,35 @@ SupremeAI 2.0-এর জন্য TTL-based config cache layer.
 
 ব্যবহার:
     from core.config_cache import config_cache
-    
+
     # Get a config value (cached with TTL)
     threshold = config_cache.get("cache_threshold_code", default=0.95)
-    
+
     # Force refresh
     config_cache.refresh()
-    
+
     # Set a config value (also persists to DB)
     await config_cache.set("cache_threshold_code", 0.90)
 """  # noqa: W293
 
+# === Module-level imports for DB interaction and Event Bus ===
+# These imports are moved to the module level to allow consistent patching in tests
+# and to avoid repeated imports within functions.
+import asyncio
 import threading
 import time
 from typing import Any
 
 from loguru import logger
+from sqlalchemy import select
 
+from core.event_bus import ErrorEvent
+from core.event_bus import error_event_bus
+from database.session import AsyncSessionLocal
+from models.system_config import SystemConfig
+
+
+# =============================================================
 
 # ডিফল্ট কনফিগ — DB না থাকলেও অ্যাপ চালু থাকবে
 DEFAULT_CONFIGS: dict[str, Any] = {
@@ -61,7 +74,7 @@ DEFAULT_CONFIGS: dict[str, Any] = {
 class ConfigCache:
     """
     TTL-based in-memory config cache.
-    
+
     - App startup-এ DB থেকে config load করে
     - TTL (ডিফল্ট: ৬০ সেকেন্ড) পর্যন্ত in-memory serve করে
     - TTL expire হলে পরবর্তি request-এ DB reload করে
@@ -79,91 +92,90 @@ class ConfigCache:
         """TTL expire হয়েছে কিনা চেক করে।"""
         return (time.time() - self._last_refresh) > self._ttl
 
-    def _load_from_db(self) -> dict[str, Any]:
+    async def _async_load_configs_internal(self) -> dict[str, Any]:
         """
-        DB থেকে active SystemConfig রেকর্ড লোড করে।
-        যদি DB না থাকে বা কোন error হয়, DEFAULT_CONFIGS ব্যবহার করে।
+        Internal async method to load configurations from the DB.
+        This is separated to be callable by both sync and async refresh logic.
         """
         configs = dict(DEFAULT_CONFIGS)  # Start with defaults
         try:
-            # Try to load from SystemConfig table
-            # Synchronous load for cache initialization
-            import asyncio
-
-            from sqlalchemy import select
-
-            from database.session import AsyncSessionLocal
-            from models.system_config import SystemConfig
-
-            async def _async_load():
-                async with AsyncSessionLocal() as session:
-                    stmt = select(SystemConfig).where(SystemConfig.is_active)
-                    result = await session.execute(stmt)
-                    rows = result.scalars().all()
-                    for row in rows:
-                        configs[row.key] = row.value
-                    return configs
-
-            try:
-                loop = asyncio.new_event_loop()
-                asyncio.set_event_loop(loop)
-                configs = loop.run_until_complete(_async_load())
-                loop.close()
-                logger.info(f"ConfigCache: Loaded {len(configs)} configs from DB")
-            except RuntimeError as e:
-                logger.exception(f"❌ Critical task failure in config_cache.py: {e}")
-                from core.event_bus import ErrorEvent
-                from core.event_bus import error_event_bus
-                error_event_bus.emit(
-                    ErrorEvent(
-                        module="backend.core.config_cache",
-                        error_type=type(e).__name__,
-                        message=str(e),
-                        severity="WARNING",
-                        context={"action": "async_load_fallback"}
-                    )
+            async with AsyncSessionLocal() as session:
+                stmt = select(SystemConfig).where(SystemConfig.is_active)
+                result = await session.execute(stmt)
+                rows = result.scalars().all()
+                for row in rows:
+                    configs[row.key] = row.value
+            logger.info(f"ConfigCache: Async loaded {len(configs)} configs from DB")
+        except Exception as e:
+            logger.exception(f"❌ Critical task failure during async DB load in _async_load_configs_internal: {e}")
+            error_event_bus.emit(
+                ErrorEvent(
+                    module="backend.core.config_cache",
+                    error_type=type(e).__name__,
+                    message=str(e),
+                    severity="WARNING",
+                    context={"action": "_async_load_configs_internal_failure"},
                 )
+            )
+            # If DB loading fails, we return defaults to ensure the system can con
```

---
_Auto-generated by SupremeAI CI Auto-Fix Engine v3_


