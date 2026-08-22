import os

from fastapi import FastAPI
from fastapi.testclient import TestClient
from starlette.responses import PlainTextResponse

from core.security.auth_middleware import AuthMiddleware

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


from unittest.mock import patch


def test_auth_middleware_blocks_protected_route_without_token():
    app = FastAPI()

    @app.get("/api/task/execute")
    def task():
        return PlainTextResponse("ok")

    app.add_middleware(AuthMiddleware)
    client = TestClient(app)
    with (
        patch("core.security.auth_middleware.is_test_environment", return_value=False),
        patch("core.security.auth_middleware.settings") as mock_settings,
    ):
        mock_settings.supremeai_api_token = "secure-test-token-value"
        mock_settings.supremeai_public_paths = []
        resp = client.get("/api/task/execute")
    assert resp.status_code == 401

    os.environ["SUPREMEAI_API_KEY"] = "secure-test-token-value"
    app = FastAPI()

    @app.get("/api/task/execute")
    def task_context():
        return PlainTextResponse("ok")

    app.add_middleware(AuthMiddleware)
    client = TestClient(app)
    try:
        with patch("core.security.auth_middleware.settings") as mock_settings:
            mock_settings.supremeai_api_token = "secure-test-token-value"
            mock_settings.supremeai_public_paths = []
            resp = client.get(
                "/api/task/execute",
                headers={"Authorization": "Bearer secure-test-token-value"},
            )
        assert resp.status_code == 200
    finally:
        os.environ.pop("SUPREMEAI_API_KEY", None)
