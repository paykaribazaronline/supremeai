from __future__ import annotations

import os
from unittest.mock import patch

import pytest

from core.config import Settings


@patch.dict(os.environ, {"cors_origins": "https://a.example.com, https://b.example.com"}, clear=False)
def test_parse_cors_origins_comma_separated():
    settings = Settings()
    assert settings.cors_origins == ["https://a.example.com", "https://b.example.com"]


@patch.dict(
    os.environ,
    {
        "env": "production",
        "openrouter_api_key": "sk-open",
        "gemini_api_key": "sk-gemini",
        "SUPREMEAI_JWT_SECRET": "",
    },
    clear=False,
)
def test_settings_raises_when_production_secret_missing():
    with pytest.raises(ValueError):
        Settings()
