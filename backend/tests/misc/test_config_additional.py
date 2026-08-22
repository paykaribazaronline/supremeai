from __future__ import annotations

import os
from unittest.mock import patch

import pytest

from core.config import Settings


@patch.dict(
    os.environ,
    {"CORS_ORIGINS": "https://a.example.com, https://b.example.com"},
    clear=False,
)
def test_parse_cors_origins_comma_separated():
    settings = Settings()
    assert settings.cors_origins == ["https://a.example.com", "https://b.example.com"]


def test_settings_raises_when_production_secret_missing():
    with patch.dict(
        os.environ,
        {
            "ENV": "production",
            "ALLOW_TEST_AUTH_BYPASS": "false",
            "OPENROUTER_API_KEY": "sk-open",
            "GEMINI_API_KEY": "sk-gemini",
        },
        clear=True,
    ):
        with patch("core.config_secrets.secret_vault.fetch_secret", return_value=None):
            with pytest.raises((ValueError, RuntimeError)):
                Settings()
