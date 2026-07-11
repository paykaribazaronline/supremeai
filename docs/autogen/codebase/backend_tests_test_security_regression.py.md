# 📄 ফাইল: backend/tests/test_security_regression.py

**প্রকার:** .py  
**সাইজ:** 1,689 বাইট  
**আপডেট:** 2026-07-11T16:17:51.593096

---

## কোড

```py
from __future__ import annotations

import os
from unittest.mock import patch

import pytest
from pydantic import ValidationError

from core.auth_middleware import AuthMiddleware
from core.config import Settings


@pytest.mark.anyio
async def test_production_jwt_secret_required():
    """Verify that in production environment, a missing jwt_secret raises a validation error."""
    with patch.dict(os.environ, {"ENV": "production"}):
        with pytest.raises(ValidationError) as excinfo:
            Settings(
                jwt_secret=None,
                supremeai_admin_password_hash="dummy-hash",
                openrouter_api_key="valid",
                gemini_api_key="valid",
            )
    assert "SUPREMEAI_JWT_SECRET must be explicitly set in all environments" in str(excinfo.value)


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
    with patch.dict(os.environ, {"SUPREMEAI_API_TOKEN": "super-secure-production-api-token"}):
        resp = client.get("/api/task/execute", headers={"Authorization": "Bearer test-token"})
        assert resp.status_code == 401
        assert resp.json()["detail"] == "Invalid or missing API token."

```