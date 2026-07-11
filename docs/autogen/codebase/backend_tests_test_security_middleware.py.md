# 📄 ফাইল: backend/tests/test_security_middleware.py

**প্রকার:** .py  
**সাইজ:** 1,573 বাইট  
**আপডেট:** 2026-07-11T10:59:17.870973

---

## কোড

```py
import os

from fastapi import FastAPI
from fastapi.testclient import TestClient
from starlette.responses import PlainTextResponse

from core.auth_middleware import AuthMiddleware
# Rate limiter tests have been migrated to APIKeyRateLimiter and TenantRateLimiter


def test_auth_middleware_allows_health_without_token():
    app = FastAPI()

    @app.get("/health")
    def health():
        return PlainTextResponse("ok")

    app.add_middleware(AuthMiddleware)
    client = TestClient(app)
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.text == "ok"


def test_auth_middleware_blocks_protected_route_without_token():
    # Use a secure, randomly generated token for testing
    os.environ["SUPREMEAI_API_TOKEN"] = "secure-test-token-value"
    app = FastAPI()

    @app.get("/api/task/execute")
    def task():
        return PlainTextResponse("ok")

    app.add_middleware(AuthMiddleware)
    client = TestClient(app)
    resp = client.get("/api/task/execute")
    assert resp.status_code == 401
    del os.environ["SUPREMEAI_API_TOKEN"]


def test_auth_middleware_allows_with_valid_token():
    import os

    os.environ["SUPREMEAI_API_TOKEN"] = "secure-test-token-value"
    app = FastAPI()

    @app.get("/api/task/execute")
    def task():
        return PlainTextResponse("ok")

    app.add_middleware(AuthMiddleware)
    client = TestClient(app)
    resp = client.get("/api/task/execute", headers={"Authorization": "Bearer secure-test-token-value"})
    assert resp.status_code == 200
    del os.environ["SUPREMEAI_API_TOKEN"]

```