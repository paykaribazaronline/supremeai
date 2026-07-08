# 📄 ফাইল: backend/core/config_cache.py

**প্রকার:** .py  
**সাইজ:** 10,312 বাইট  
**আপডেট:** 2026-07-08T19:05:04.270234

---

## কোড

```py
"""
config_cache.py — Lightweight In-Memory Config Cache
======================================================
SupremeAI 2.0-এর জন্য TTL-based config cache layer.

কেন এটি দরকার:
    "Database-Driven" মানে প্রতি request-এ DB কল না — এতে latency ও cost দুটোই বাড়বে।
    এই ক্যাশ লেয়ার app startup-এ config load করে, TTL-এর মধ্যে in-memory serve করে,
    এবং Supabase Realtime / change-event এলে cache invalidate করে।

ব্যবহার:
    from core.config_cache import config_cache
    
    # Get a config value (cached with TTL)
    threshold = config_cache.get("cache_threshold_code", default=0.95)
    
    # Force refresh
    config_cache.refresh()
    
    # Set a config value (also persists to DB)
    await config_cache.set("cache_threshold_code", 0.90)
"""  # noqa: W293

import threading
import time
from typing import Any

from loguru import logger


# ডিফল্ট কনফিগ — DB না থাকলেও অ্যাপ চালু থাকবে
DEFAULT_CONFIGS: dict[str, Any] = {
    # Semantic Cache Thresholds
    "cache_threshold_code": 0.95,
    "cache_threshold_generation": 0.95,
    "cache_threshold_general": 0.85,
    "cache_threshold_qa": 0.85,
    "cache_threshold_reasoning": 0.80,
    # Feature Flags
    "feature_semantic_cache": True,
    "feature_auto_pr": True,
    "feature_self_healing": True,
    "feature_budget_guardian": True,
    # Rate Limits (override per environment)
    "rate_limit_gemini_rpm": 9,
    "rate_limit_groq_rpm": 28,
    "rate_limit_openrouter_rpm": 19,
    # Provider Metadata
    "provider_base_url_groq": "https://api.groq.com/openai/v1",
    "provider_base_url_deepseek": "https://api.deepseek.com",
    "provider_base_url_openai": "https://api.openai.com/v1",
    "provider_models_groq": ["llama3-70b-8192", "mixtral-8x7b-32768"],
    "provider_models_deepseek": ["deepseek-coder", "deepseek-chat"],
    "provider_models_openai": ["gpt-4", "gpt-4o-mini", "gpt-3.5-turbo"],
    # Self-Healing
    "self_healing_max_retries": 3,
    "self_healing_cooldown_seconds": 300,
}


class ConfigCache:
    """
    TTL-based in-memory config cache.
    
    - App startup-এ DB থেকে config load করে
    - TTL (ডিফল্ট: ৬০ সেকেন্ড) পর্যন্ত in-memory serve করে
    - TTL expire হলে পরবর্তি request-এ DB reload করে
    - force_refresh() দিয়ে ম্যানুয়ালি invalidate করা যায়
    """  # noqa: W293

    def __init__(self, ttl_seconds: int = 60):
        self._cache: dict[str, Any] = {}
        self._ttl = ttl_seconds
        self._last_refresh: float = 0.0
        self._lock = threading.Lock()
        self._loaded = False

    def _should_refresh(self) -> bool:
        """TTL expire হয়েছে কিনা চেক করে।"""
        return (time.time() - self._last_refresh) > self._ttl

    def _load_from_db(self) -> dict[str, Any]:
        """
        DB থেকে active SystemConfig রেকর্ড লোড করে।
        যদি DB না থাকে বা কোন error হয়, DEFAULT_CONFIGS ব্যবহার করে।
        """
        configs = dict(DEFAULT_CONFIGS)  # Start with defaults
        try:
            # Try to load from SystemConfig table
            # Synchronous load for cache initialization
            import asyncio

            from sqlalchemy import select

            from database.session import AsyncSessionLocal
            from models.system_config import SystemConfig

            async def _async_load():
                async with AsyncSessionLocal() as session:
                    stmt = select(SystemConfig).where(SystemConfig.is_active)
                    result = await session.execute(stmt)
                    rows = result.scalars().all()
                    for row in rows:
                        configs[row.key] = row.value
                    return configs

            try:
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
                configs = loop.run_until_complete(_async_load())
                loop.close()
                logger.info(f"ConfigCache: Loaded {len(configs)} configs from DB")
            except RuntimeError as e:
                logger.exception(f"❌ Critical task failure in config_cache.py: {e}")
                from core.event_bus import ErrorEvent
                from core.event_bus import error_event_bus
                error_event_bus.emit(
                    ErrorEvent(
                        module="backend.core.config_cache",
                        error_type=type(e).__name__,
                        message=str(e),
                        severity="WARNING",
                        context={"action": "async_load_fallback"}
                    )
                )

        except Exception as exc:  # noqa: BLE001
            logger.debug(f"ConfigCache: DB load failed, using defaults: {exc}")

        return configs

    def refresh(self):
        """ফোর্স রিফ্রেশ — ক্যাশ DB থেকে রিলোড করে (সিঙ্ক্রোনাস)।"""
        with self._lock:
            try:
                self._cache = self._load_from_db()
            except Exception as exc:  # noqa: BLE001
                # বাংলা মন্তব্য: Sync load failing এ fallback defaults লোড করা হচ্ছে
                logger.debug(f"ConfigCache: Sync load failed, using defaults: {exc}")
                self._cache = dict(DEFAULT_CONFIGS)
            self._last_refresh = time.time()
            self._loaded = True
            logger.debug(f"ConfigCache: Refreshed {len(self._cache)} configs")

    async def refresh_async(self):
        """Asynchronous refresh, mainly for startup."""
        from sqlalchemy import select

        from database.session import AsyncSessionLocal
        from models.system_config import SystemConfig

        configs = dict(DEFAULT_CONFIGS)
        try:
            async with AsyncSessionLocal() as session:
                stmt = select(SystemConfig).where(SystemConfig.is_active)
                result = await session.execute(stmt)
                rows = result.scalars().all()
                for row in rows:
                    configs[row.key] = row.value

            with self._lock:
                self._cache = configs
                self._last_refresh = time.time()
                self._loaded = True
            logger.info(f"ConfigCache: Async loaded {len(configs)} configs from DB")
        except Exception as exc:  # noqa: BLE001
            logger.debug(f"ConfigCache: DB load failed during startup, using defaults: {exc}")
            with self._lock:
                self._cache = configs
                self._last_refresh = time.time()
                self._loaded = True

    def get(self, key: str, default: Any = None) -> Any:
        """
        কনফিগ ভ্যালু রিটার্ন করে।
        - TTL expire হলে auto-refresh করে
        - DB না থাকলে DEFAULT_CONFIGS থেকে নেয়
        """
        if not self._loaded or self._should_refresh():
            self.refresh()

        with self._lock:
            return self._cache.get(key, default)

    def get_all(self, category: str | None = None) -> dict[str, Any]:
        """সব কনফিগ (অথবা নির্দিষ্ট category) রিটার্ন করে।"""
        if not self._loaded or self._should_refresh():
            self.refresh()

        with self._lock:
            if category:
                # Filter by key prefix pattern (e.g., "cache_threshold_", "provider_")
                return {
                    k: v for k, v in self._cache.items()
                    if k.startswith(category)
                }
            return dict(self._cache)

    async def set(self, key: str, value: Any, description: str = "") -> bool:
        """
        কনফিগ ভ্যালু সেট করে — DB-তেও persist করে + cache update করে।
        """
        from sqlalchemy import select

        from database.session import AsyncSessionLocal
        from models.system_config import SystemConfig

        try:
            async with AsyncSessionLocal() as session:
                stmt = select(SystemConfig).where(SystemConfig.key == key)
                result = await session.execute(stmt)
                existing = result.scalar_one_or_none()

                if existing:
                    existing.value = value
                    existing.version += 1
                    if description:
                        existing.description = description
                else:
                    new_config = SystemConfig(
                        key=key,
                        value=value,
                        description=description or f"Auto-created config for '{key}'",
                    )
                    session.add(new_config)

                await session.commit()

                # Update in-memory cache
                with self._lock:
                    self._cache[key] = value

                logger.info(f"ConfigCache: Set '{key}' = {value}")
                return True
        except Exception as exc:  # noqa: BLE001
            logger.error(f"ConfigCache: Failed to set '{key}': {exc}")
            return False

    def invalidate(self, key: str | None = None):
        """
        নির্দিষ্ট key (বা সব) ক্যাশ invalidate করে।
        পরবর্তি get() কল auto-refresh ট্রিগার করবে।
        """
        with self._lock:
            if key:
                self._cache.pop(key, None)
                logger.debug(f"ConfigCache: Invalidated key '{key}'")
            else:
                self._cache.clear()
                self._loaded = False
                logger.debug("ConfigCache: Fully invalidated")


# Global singleton
config_cache = ConfigCache(ttl_seconds=60)

```