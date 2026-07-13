from collections.abc import Callable
from typing import Any

import httpx


global_http_client: httpx.AsyncClient | None = None


class ServiceRegistry:
    """
    বাংলা মন্তব্য: P2 Fix — Factory pattern with async initialization.
    Instance নয়, factory register করুন। async def create() classmethod দিয়ে async initialization করুন।
    """

    def __init__(self):
        self._services: dict[str, Callable] = {}
        self._instances: dict[str, Any] = {}

    def register(self, name: str, factory: Callable):
        self._services[name] = factory

    async def get(self, name: str) -> Any:
        if name not in self._instances:
            factory = self._services.get(name)
            if not factory:
                raise KeyError(f"Service '{name}' not registered")
            self._instances[name] = await factory()
        return self._instances[name]

    def has(self, name: str) -> bool:
        return name in self._services


# Lazy HTTP client — initialized on first use
_http_client: httpx.AsyncClient | None = None


async def get_global_http_client() -> httpx.AsyncClient:
    global _http_client
    if _http_client is None:
        _http_client = httpx.AsyncClient(
            timeout=httpx.Timeout(30.0, connect=10.0),
            limits=httpx.Limits(max_connections=100, max_keepalive_connections=20),
        )
    return _http_client


async def close_global_http_client():
    global _http_client
    if _http_client:
        await _http_client.aclose()
        _http_client = None


# Global service registry instance
registry = ServiceRegistry()

# Synchronous instances for legacy sync code.
# These imports are mandatory for the application to function correctly.
# Removing the try/except blocks ensures a "fail-fast" startup if a
# critical dependency is missing, preventing NoneType errors at runtime.
from adaptive_engine.experience_db import ExperienceDatabase  # noqa: E402
from adaptive_engine.intent_parser import IntentParser  # noqa: E402
from admin.god import AdminGodLayer  # noqa: E402
from brain.model_router import ModelRouter  # noqa: E402
from core.intent import IntentClassifier  # noqa: E402
from core.messaging.upstash_redis_queue import UpstashRedisQueue  # noqa: E402


redis_queue = UpstashRedisQueue()
admin_god = AdminGodLayer()
model_router = ModelRouter()
intent_clf = IntentClassifier()
# The 'if model_router' check is kept as a safeguard, though with fail-fast,
# model_router should always be available if IntentParser is.
intent_parser = IntentParser(model_router=model_router) if model_router else None
experience_db = ExperienceDatabase()


# Add explicit global_http_client
global_http_client: httpx.AsyncClient | None = None


def __getattr__(name: str):
    """what is code: ডায়নামিক সার্ভিস গেটার — লিগ্যাসি টেস্ট এবং রাউটারগুলোর ব্যাকওয়ার্ড কম্প্যাটিবিলিটি নিশ্চিত করে।"""
    if name == "registry":
        raise AttributeError("Registry not initialized")

    # Attempt to resolve from registry
    from core.services import registry

    if registry:
        if hasattr(registry, "get_service"):
            svc = registry.get_service(name)
            if svc is not None:
                return svc
        if hasattr(registry, "services") and name in registry.services:
            return registry.services[name]

    import os

    if os.getenv("ENV", "local").lower() in ("test", "testing"):
        import logging

        logging.getLogger(__name__).warning(f"⚠️ Service '{name}' is missing and is being mock injected dynamically in test environment!")
        from unittest.mock import MagicMock

        return MagicMock()

    raise AttributeError(f"Module 'core.services' has no attribute '{name}'")
