from __future__ import annotations

import os
import pytest
from unittest.mock import MagicMock, patch
from pydantic import ValidationError

from core.config import Settings
from core.auth_middleware import AuthMiddleware

@pytest.mark.anyio
async def test_production_jwt_secret_required():
    """Verify that in production environment, a missing jwt_secret raises a validation error."""
    with pytest.raises(ValidationError) as excinfo:
        Settings(
            env="production",
            SUPREMEAI_JWT_SECRET=None,
            openrouter_api_key="valid",
            gemini_api_key="valid"
        )
    assert "SUPREMEAI_JWT_SECRET environment variable must be set in production" in str(excinfo.value)

@pytest.mark.anyio
async def test_auth_middleware_rejects_invalid_api_token():
    """Verify that AuthMiddleware rejects invalid API tokens and 'test-token' if the expected token is different."""
    app_mock = MagicMock()
    middleware = AuthMiddleware(app_mock)

    # Setup expected API token env var
    with patch.dict(os.environ, {"SUPREMEAI_API_TOKEN": "super-secure-production-api-token"}):
        scope = {
            "type": "http",
            "path": "/api/task/execute",
            "headers": [
                (b"authorization", b"Bearer test-token")
            ]
        }

        sent_messages = []
        async def mock_send(message):
            sent_messages.append(message)

        # Call middleware
        await middleware(scope, None, mock_send)

        # Verify that the middleware responded with 401 Unauthorized
        assert len(sent_messages) == 2
        response_start = sent_messages[0]
        assert response_start["type"] == "http.response.start"
        assert response_start["status"] == 401
