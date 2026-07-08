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

# Move asyncio and SQLAlchemy-related imports to the top for proper patching and scope
import asyncio
import threading
import time
from typing import Any

from loguru import logger
from sqlalchemy import select

from core.event_bus import ErrorEvent  # Ensure ErrorEvent is imported for consistent use
from core.event_bus import error_event_bus  # Ensure error_event_bus is imported for consistent use

# These imports were local to methods, causing AttributeError when patched at module level
from database.session import AsyncSessionLocal
from models.system_config import SystemConfig


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

    async def _load_configs_async_helper(self) -> dict[str, Any]:
        """
        Asynchronous helper to load active SystemConfig records from DB.
        If DB fails, returns DEFAULT_CONFIGS.
        """
        configs = dict(DEFAULT_CONFIGS)  # Start with defaults
        try:
            async with AsyncSessionLocal() as session:
                stmt = select(SystemConfig).where(SystemConfig.is_active)
                result = await session.execute(stmt)
                rows = result.scalars().all()
                for row in rows:
                    configs[row.key] = row.value
                logger.info(f"ConfigCache: Async loaded {len(configs)} configs from DB helper")
        except Exception as e:  # noqa: BLE001
            logger.exception(f"❌ Error during async DB load in config_cache helper: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="backend.core.config_cache",
                    error_type=type(e).__name__,
                    message=str(e),
                    severity="WARNING",
                    context={"action": "async_load_helper_failure"},
                )
            )
            # If an error occurs, 'configs' remains DEFAULT_CONFIGS as initialized
        return configs

    def _load_from_db(self) -> dict[str, Any]:
        """
        DB থেকে active SystemConfig রেকর্ড লোড করে।
        যদি DB না থাকে বা কোন error হয়, DEFAULT_CONFIGS ব্যবহার করে।
        Uses asyncio.run() to execute async DB operations from a sync context.
        """
        configs = dict(DEFAULT_CONFIGS) # Initialize with defaults, in case asyncio.run fails immediately
        try:
            # asyncio.run handles event loop creation/destruction safely for sync calls
            configs = asyncio.run(self._load_configs_async_helper())
        except Exception as exc:  # noqa: BLE001
            logger.debug(f"ConfigCache: DB load failed during sync refresh via asyncio.run(), using defaults: {exc}")
            error_event_bus.emit(
                ErrorEvent(
                    module="backend.core.config_cache",
                    error_type=type(exc).__name__,
                    message=str(exc),
                    severity="WARNING",
                    context={"action": "sync_load_fallback_from_asyncio_run"},
                )
            )
            # configs remain DEFAULT_CONFIGS from initialization
        return configs

    def refresh(self):
        """ফোর্স রিফ্রেশ — ক্যাশ DB থেকে রিলোড করে (সিঙ্ক্রোনাস)।"""
        with self._lock:
            try:
                self._cache = self._load_from_db()
            except Exception as exc: # Catch any error from _load_from_db (e.g., a direct test mock raising)
                logger.error(f"ConfigCache: Failed to refresh from DB, falling back to defaults: {exc}")
                self._cache = dict(DEFAULT_CONFIGS) # Ensure cache is at least defaults on refresh failure
            self._last_refresh = time.time()
            self._loaded = True
            logger.debug(f"ConfigCache: Refreshed {len(self._cache)} configs")

    async def refresh_async(self):
        """Asynchronous refresh, mainly for startup."""
        # Use the shared async helper to load configurations
        configs = await self._load_configs_async_helper()

        with self._lock:
            self._cache = configs
            self._last_refresh = time.time()
            self._loaded = True
        logger.info(f"ConfigCache: Async loaded {len(configs)} configs from DB")
        # _load_configs_async_helper already logs exceptions and emits ErrorEvent.

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
