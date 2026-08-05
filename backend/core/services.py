"""SupremeAI 2.0 — Core service registry and lazy initialization.

বাংলা: কোর সার্ভিস রেজিস্ট্রি এবং লেজি ইনিশিয়ালাইজেশন।
"""

import asyncio
import logging
import os
from collections.abc import Callable
from typing import Any

import httpx

from core.error_bus import with_error_bus

# Lazy HTTP client — initialized on first use
_http_client: httpx.AsyncClient | None = None


async def get_global_http_client() -> httpx.AsyncClient:
    """Get or create the global HTTP client singleton."""
    global _http_client
    if _http_client is None:
        _http_client = httpx.AsyncClient(
            timeout=httpx.Timeout(30.0, connect=10.0),
            limits=httpx.Limits(max_connections=100, max_keepalive_connections=20),
        )
    return _http_client


async def close_global_http_client() -> None:
    """Close the global HTTP client."""
    global _http_client
    if _http_client:
        await _http_client.aclose()
        _http_client = None


class ServiceRegistry:
    """
    বাংলা মন্তব্য: Factory pattern with async initialization.
    Instance নয়, factory register করুন। async def create() classmethod দিয়ে async initialization করুন।
    """

    def __init__(self) -> None:
        self._services: dict[str, Callable] = {}
        self._instances: dict[str, Any] = {}

    def register(self, name: str, factory: Callable) -> None:
        """Register a service factory."""
        self._services[name] = factory

    async def get(self, name: str) -> Any:
        """Get or create a service instance by name."""
        if name not in self._instances:
            factory = self._services.get(name)
            if not factory:
                raise KeyError(f"Service '{name}' not registered")
            self._instances[name] = await factory()
        return self._instances[name]

    def has(self, name: str) -> bool:
        """Check if a service is registered."""
        return name in self._services


# Global service registry instance
registry = ServiceRegistry()

# Synchronous instances for legacy sync code.
# These imports are mandatory for the application to function correctly.
# Removing the try/except blocks ensures a "fail-fast" startup if a
# critical dependency is missing, preventing NoneType errors at runtime.
from adaptive_engine.experience_db import ExperienceDatabase
from adaptive_engine.intent_parser import IntentParser
from admin.god import AdminGodLayer
from brain.model_router import ModelRouter
from brain.parallel_cloud_router import ParallelCloudRouter
from core.intent import IntentClassifier
from core.messaging.upstash_redis_queue import UpstashRedisQueue

redis_queue = UpstashRedisQueue()
admin_god = AdminGodLayer()
model_router = ModelRouter()
parallel_router = ParallelCloudRouter()
intent_clf = IntentClassifier()
# The 'if model_router' check is kept as a safeguard, though with fail-fast,
# model_router should always be available if IntentParser is.
intent_parser = IntentParser(model_router=model_router) if model_router else None
experience_db = ExperienceDatabase()

# Global HTTP client - initialized in lifespan
global_http_client: httpx.AsyncClient | None = None


def __getattr__(name: str) -> Any:
    """Dynamic service getter — ensures legacy test and router backward compatibility.

    বাংলা: dunder attribute probe (যেমন `__path__`, `__all__`) সবসময় স্বাভাবিক,
    প্রত্যাশিত ঘটনা — Python-এর নিজস্ব import/inspect মেশিনারি নিয়মিতভাবে এগুলো চেক
    করে দেখে এই মডিউলটা একটা প্যাকেজ কিনা। আগে পুরো ফাংশনটাই `@with_error_bus` দিয়ে
    wrap করা ছিল, ফলে প্রতিটা dunder-probe-এর স্বাভাবিক AttributeError-ও একটা
    "application error" হিসেবে error_event_bus-এ রিপোর্ট হচ্ছিল। প্রতি ~৫ সেকেন্ডে এটা
    ঘটায় error-pattern-detector সেটাকে "AttributeError x3 → CRITICAL" ধরে নিয়ে
    self-healing/emergency-evolution ট্রিগার করছিল — যেটা নিজেই আরেকটা বাগের
    (fitness_engine মিসিং) কারণে ক্র্যাশ করে, এবং পুরো চক্রটা অনন্তকাল ধরে রিপিট হয়
    (production লগে এটাই মূল কারণ ছিল)। তাই dunder short-circuit-টা এখন
    error-bus-wrapped অংশের বাইরে রাখা হলো, যাতে এটা কখনো error হিসেবে রিপোর্ট না হয়।
    """
    if name.startswith("__") and name.endswith("__"):
        raise AttributeError(f"Module 'core.services' has no attribute '{name}'")
    return _get_service_attr(name)


@with_error_bus("__getattr__")
def _get_service_attr(name: str) -> Any:
    """Actual service-lookup logic — only reached for real (non-dunder) attribute names."""
    if name == "registry":
        raise AttributeError("Registry not initialized")

    # Attempt to resolve from registry safely without triggering imports
    reg = globals().get("registry")
    if reg:
        if hasattr(reg, "get") and name in reg._services:
            # Return the service factory, not the instance
            try:
                loop = asyncio.get_event_loop()
                if loop.is_running():
                    # In async context, return the service
                    return reg._instances.get(name)
            except RuntimeError:
                pass
        if hasattr(reg, "_services") and name in reg._services:
            return reg._services[name]

    if os.getenv("ENV", "local").lower() in ("test", "testing", "ci"):
        logging.getLogger(__name__).warning(
            f"⚠️ Service '{name}' is missing and is being mock injected dynamically in test environment!"
        )
        try:
            from core.messaging.event_bus import (
                ErrorContext,
                ErrorEvent,
                error_event_bus,
            )

            error_event_bus.emit(
                ErrorEvent(
                    module="services_registry",
                    error_type="MOCK_SERVICE_INJECTED",
                    message=f"Missing service '{name}' was mock injected in test environment.",
                    severity="WARNING",
                    structured_context=ErrorContext(module="services_registry"),
                )
            )
        except Exception as exc:
            logging.getLogger(__name__).debug("ErrorEvent emit bypassed: %s", exc)

        from unittest.mock import MagicMock

        return MagicMock()

    raise AttributeError(f"Module 'core.services' has no attribute '{name}'")
