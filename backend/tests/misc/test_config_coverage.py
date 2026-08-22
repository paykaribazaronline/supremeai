# Tests updated for refactored config
from __future__ import annotations

import os
from unittest.mock import patch

import pytest

from core.config import Settings
from core.security.secret_vault import secret_vault


@pytest.fixture(autouse=True)
def mock_secret_vault():
    with patch.object(secret_vault, "fetch_secret", return_value="test-secret" * 10) as mock:
        yield mock


@patch.dict(
    os.environ,
    {
        "ENV": "production",
        "CORS_ORIGINS": "https://example.com,https://test.com",
        "ADMIN_EMAILS": "a@x.com,b@y.com",
    },
    clear=True,
)
def test_settings_production_env(mock_secret_vault):
    s = Settings()
    assert s.env == "production"
    assert s.jwt_secret == "test-secret" * 10


@patch.dict(os.environ, {"ENV": "local", "CORS_ORIGINS": "http://localhost:3000"}, clear=True)
def test_settings_local_env(mock_secret_vault):
    s = Settings()
    assert s.env == "local"


@patch.dict(os.environ, {"ENV": "invalid"}, clear=True)
def test_settings_invalid_env(mock_secret_vault):
    with pytest.raises(ValueError):
        Settings()
