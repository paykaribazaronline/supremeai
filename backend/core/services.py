from collections.abc import Callable
from typing import Any

import httpx


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
from core.upstash_redis_queue import UpstashRedisQueue
from admin.god import AdminGodLayer
from brain.model_router import ModelRouter
from core.intent import IntentClassifier
from adaptive_engine.intent_parser import IntentParser
from adaptive_engine.experience_db import ExperienceDatabase

redis_queue = UpstashRedisQueue()
admin_god = AdminGodLayer()
model_router = ModelRouter()
intent_clf = IntentClassifier()
# The 'if model_router' check is kept as a safeguard, though with fail-fast,
# model_router should always be available if IntentParser is.
intent_parser = IntentParser(model_router=model_router) if model_router else None
experience_db = ExperienceDatabase()
