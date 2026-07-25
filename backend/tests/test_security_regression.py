from __future__ import annotations

import os
from unittest.mock import patch

import pytest
from core.config import Settings
from core.security.auth_middleware import AuthMiddleware


@pytest.mark.anyio
async def test_production_jwt_secret_required():
    """Verify that in production environment, a missing jwt_secret raises a validation error."""

    with patch.dict(os.environ, {"ENV": "production", "SUPREMEAI_JWT_SECRET": ""}):
        with pytest.raises(ValueError) as excinfo:
            Settings().jwt_secret
    assert "SUPREMEAI_JWT_SECRET must be explicitly set in production" in str(
        excinfo.value
    )


@pytest.mark.skip(reason="Needs update")
def test_auth_middleware_rejects_invalid_api_token():
    """Verify that AuthMiddleware rejects invalid API tokens and 'test-token' if the expected token is different."""
    from fastapi import FastAPI
    from fastapi.testclient import TestClient
    from starlette.responses import PlainTextResponse

    app = FastAPI()

    @app.get("/api/task/execute")
    def task():
        return PlainTextResponse("ok")

    app.add_middleware(AuthMiddleware)
    client = TestClient(app)

    # Setup expected API token env var and test that an invalid token (like 'test-token') gets 401
    with patch.dict(
        os.environ, {"SUPREMEAI_API_TOKEN": "super-secure-production-api-token"}
    ):
        resp = client.get(
            "/api/task/execute", headers={"Authorization": "Bearer test-token"}
        )
        assert resp.status_code == 401
        assert resp.json()["detail"] == "Invalid or missing API token."
