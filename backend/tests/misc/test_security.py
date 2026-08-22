from __future__ import annotations

import os
from unittest.mock import patch

import pytest

from core.config import settings


@pytest.mark.asyncio
async def test_jwt_secret_persistence():
    """Test JWT secret persistence across restarts."""
    original_env = settings.env
    settings.env = "development"
    try:
        # Clear cached property if exists
        if hasattr(settings, "_jwt_secret"):
            delattr(settings, "_jwt_secret")

        secret1 = settings.jwt_secret

        # Clear cached property to simulate reload/restart
        if hasattr(settings, "_jwt_secret"):
            delattr(settings, "_jwt_secret")

        secret2 = settings.jwt_secret
        assert secret1 == secret2
    finally:
        settings.env = original_env



@pytest.mark.asyncio
async def test_cors_origin_validation():
    """Test CORS origin validation in production/staging.

    বাংলা মন্তব্য: নতুন scheme-only validation — operator-configured যেকোনো https:// domain গ্রহণযোগ্য।
    পুরনো hardcoded supremeai.com allowlist সরানো হয়েছে।
    """
    original_env = settings.env
    try:
        # বাংলা মন্তব্য: production-এ non-https origin reject হবে → RuntimeError
        settings.env = "production"
        os.environ["STRICT_CORS_TEST"] = "1"
        os.environ["CORS_ORIGINS"] = "http://insecure-origin.com"
        with pytest.raises(RuntimeError):
            _ = settings.cors_origins

        # বাংলা মন্তব্য: যেকোনো https:// origin এখন গ্রহণযোগ্য (onrender.com, vercel.app, web.app)
        os.environ["CORS_ORIGINS"] = (
            "https://supremeai-studio-client.onrender.com,"
            "https://supremeai-lac.vercel.app"
        )
        origins = settings.cors_origins
        assert "https://supremeai-studio-client.onrender.com" in origins
        assert "https://supremeai-lac.vercel.app" in origins
    finally:
        settings.env = original_env
        os.environ.pop("CORS_ORIGINS", None)
        os.environ.pop("STRICT_CORS_TEST", None)



@pytest.mark.asyncio
async def test_rate_limiting_failure_mode():
    """Test rate limiter behavior when Redis is unavailable."""
    from core.rate_limiter import AsyncRateLimiter

    limiter = AsyncRateLimiter()

    # Mock redis_manager to return None (simulating down/unavailable)
    with patch("core.rate_limiter.redis_manager.get_client_async", return_value=None):
        original_env = settings.env
        try:
            # In production/staging, it should fail-closed (return False)
            settings.env = "production"
            assert not await limiter.acquire("test_key")

            # In development, it should fail-open (return True)
            settings.env = "development"
            assert await limiter.acquire("test_key")
        finally:
            settings.env = original_env
