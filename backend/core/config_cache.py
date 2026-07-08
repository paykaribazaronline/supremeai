# FILE_PATH: core/config_cache.py
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

# === Module-level imports for DB interaction and Event Bus ===
# These imports are moved to the module level to allow consistent patching in tests
# and to avoid repeated imports within functions.
import asyncio
import threading
import time
from typing import Any

from loguru import logger
from sqlalchemy import select

from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus
from database.session import AsyncSessionLocal
from models.system_config import SystemConfig


# =============================================================

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

    async def _async_load_configs_internal(self) -> dict[str, Any]:
        """
        Internal async method to load configurations from the DB.
        This is separated to be callable by both sync and async refresh logic.
        """
        configs = dict(DEFAULT_CONFIGS)  # Start with defaults
        try:
            async with AsyncSessionLocal() as session:
                stmt = select(SystemConfig).where(SystemConfig.is_active)
                result = await session.execute(stmt)
                rows = result.scalars().all()
                for row in rows:
                    configs[row.key] = row.value
            logger.info(f"ConfigCache: Async loaded {len(configs)} configs from DB")
        except Exception as e:
            logger.exception(f"❌ Critical task failure during async DB load in _async_load_configs_internal: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="backend.core.config_cache",
                    error_type=type(e).__name__,
                    message=str(e),
                    severity="WARNING",
                    context={"action": "_async_load_configs_internal_failure"},
                )
            )
            # If DB loading fails, we return defaults to ensure the system can continue operating.
            # The calling function will receive these defaults.
        return configs

    def _load_from_db(self) -> dict[str, Any]:
        """
        DB থেকে active SystemConfig রেকর্ড লোড করে।
        যদি DB না থাকে বা কোন error হয়, DEFAULT_CONFIGS ব্যবহার করে।
        This method is synchronous and provides a bridge to run the async DB fetch.
        It uses asyncio.run() which creates and manages its own event loop.
        """
        configs = dict(DEFAULT_CONFIGS)  # Always start with defaults

        try:
            # asyncio.run() creates and manages an event loop. It will raise RuntimeError
            # if an event loop is already running in the current OS thread.
            configs = asyncio.run(self._async_load_configs_internal())
        except RuntimeError as e:
            # This specific RuntimeError typically occurs if asyncio.run() is called
            # from within an already running async context (e.g., a pytest-asyncio test).
            # In such cases, we fall back to defaults, logging the conflict.
            logger.warning(
                f"ConfigCache: Synchronous DB load failed because an event loop is already running "
                f"in this thread. Falling back to defaults. Error: {e}"
            )
        except Exception as exc:  # noqa: BLE001
            # Catch other potential exceptions during the synchronous bridge execution,
            # (e.g., if _async_load_configs_internal itself raised an unhandled error,
            # or an error occurred before _async_load_configs_internal could execute).
            logger.warning(f"ConfigCache: DB load failed during synchronous refresh, using defaults: {exc}")

        return configs

    def refresh(self):
        """ফোর্স রিফ্রেশ — ক্যাশ DB থেকে রিলোড করে (সিঙ্ক্রোনাস)।"""
        with self._lock:
            try:
                # _load_from_db is now designed to always return a dict (either loaded or defaults).
                # This outer try-except ensures robustness against any unexpected issues
                # that might arise even from a properly structured _load_from_db,
                # like a test monkeypatch directly raising.
                self._cache = self._load_from_db()
                logger.debug(f"ConfigCache: Refreshed {len(self._cache)} configs")
            except Exception as exc:
                logger.warning(f"ConfigCache: Failed to refresh from DB or encountered error: {exc}. Falling back to defaults.")
                self._cache = dict(DEFAULT_CONFIGS) # Ensure defaults are loaded
            self._last_refresh = time.time()
            self._loaded = True

    async def refresh_async(self):
        """Asynchronous refresh, mainly for startup."""
        # This can directly await the internal async loader.
        configs = await self._async_load_configs_internal()

        with self._lock:
            self._cache = configs
            self._last_refresh = time.time()
            self._loaded = True
        logger.info(f"ConfigCache: Async loaded {len(configs)} configs from DB")

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
                return {k: v for k, v in self._cache.items() if k.startswith(category)}
            return dict(self._cache)

    async def set(self, key: str, value: Any, description: str = "") -> bool:
        """
        কনফিগ ভ্যালু সেট করে — DB-তেও persist করে + cache update করে।
        """
        # All required imports are now at the module level.
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
