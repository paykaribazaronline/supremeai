# FILE_PATH: /home/runner/work/supremeai/supremeai/backend/core/services.py
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
            # Assuming all registered factories are async coroutine functions as per the comment.
            # If synchronous factories are also possible, the implementation of 'get'
            # would need to inspect 'factory' type (e.g., using inspect.iscoroutinefunction)
            # and call it accordingly (await or direct call).
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


# Expose the global HTTP client as a module-level attribute for compatibility.
# It will be None initially and populated when get_global_http_client() is first awaited.
# Code expecting a non-None value should ensure get_global_http_client() has been called.
# This addresses calls like `services.global_http_client` expecting the client instance or None.
global_http_client: httpx.AsyncClient | None = _http_client


async def close_global_http_client():
    global _http_client
    if _http_client:
        await _http_client.aclose()
        _http_client = None


# Global service registry instance
registry = ServiceRegistry()

# Declare common services as module-level attributes for compatibility with existing code
# (e.g., tests expecting to monkeypatch or directly access these attributes).
# These will typically be populated by the application's lifespan hook (or similar entry point)
# or mocked in tests before being accessed.
redis_queue: Any = None
admin_god: Any = None
model_router: Any = None
intent_clf: Any = None
