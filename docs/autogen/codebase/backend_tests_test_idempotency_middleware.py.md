# 📄 ফাইল: backend/tests/test_idempotency_middleware.py

**প্রকার:** .py  
**সাইজ:** 3,753 বাইট  
**আপডেট:** 2026-07-04T12:59:56.857125

---

## কোড

```py
"""Idempotency middleware tests for SupremeAI 2.0."""
import os
import sys
from unittest.mock import AsyncMock
from unittest.mock import MagicMock

import pytest

from core.idempotency_middleware import IdempotencyMiddleware


class TestIdempotencyMiddleware:
    """Tests for IdempotencyMiddleware class."""

    @pytest.mark.anyio
    async def test_middleware_non_http_scope(self):
        """HTTP নয় স্কোপে মিডলওয়্যার বংয়েজ।"""
        mock_app = AsyncMock()
        middleware = IdempotencyMiddleware(mock_app)

        scope = {"type": "websocket"}
        await middleware(scope, MagicMock(), MagicMock())
        mock_app.assert_called_once()

    @pytest.mark.anyio
    async def test_middleware_pytest_environment(self):
        """পাইটেস্ট এনভায়রনমেন্টে মিডলওয়্যার বংয়েজ।"""
        mock_app = AsyncMock()
        middleware = IdempotencyMiddleware(mock_app)

        scope = {
            "type": "http",
            "method": "POST",
            "path": "/api/orchestrate/generate",
            "headers": [],
        }

        import sys

        sys.modules["pytest"] = MagicMock()

        try:
            await middleware(scope, MagicMock(), MagicMock())
            mock_app.assert_called_once()
        finally:
            del sys.modules["pytest"]

    @pytest.mark.anyio
    async def test_middleware_get_request(self):
        """GET রিকোয়েস্টে মিডলওয়্যার বংয়েজ।"""
        mock_app = AsyncMock()
        middleware = IdempotencyMiddleware(mock_app)

        scope = {
            "type": "http",
            "method": "GET",
            "path": "/api/test",
            "headers": [],
        }
        await middleware(scope, MagicMock(), MagicMock())
        mock_app.assert_called_once()

    @pytest.mark.anyio
    async def test_middleware_post_without_idempotency_key(self):
        """Idempotency কি ছাড়াই POST রিকোয়েস্ট অ্যাপ্রভ করা হচ্ছে।"""
        mock_app = AsyncMock()
        middleware = IdempotencyMiddleware(mock_app)

        scope = {
            "type": "http",
            "method": "POST",
            "path": "/api/test",
            "headers": [(b"idempotency-key", b"test-key")],
        }

        import sys

        sys.modules["pytest"] = None
        os_env_backup = {}
        for key in ["ENV"]:
            os_env_backup[key] = os.environ.get(key)

        try:
            import core.services as app_mod
            from unittest.mock import patch

            with patch.dict(os.environ, {"ENV": "production"}, clear=True):
                with patch.object(app_mod, "redis_queue", None):
                    await middleware(scope, MagicMock(), MagicMock())
                    mock_app.assert_called_once()
        finally:
            for key, value in os_env_backup.items():
                if value is not None:
                    os.environ[key] = value
                else:
                    os.environ.pop(key, None)
            if "pytest" in sys.modules:
                del sys.modules["pytest"]

    @pytest.mark.anyio
    async def test_middleware_put_request(self):
        """PUT রিকোয়েস্টে মিডলওয়্যার বংয়েজ।"""
        mock_app = AsyncMock()
        middleware = IdempotencyMiddleware(mock_app)

        scope = {
            "type": "http",
            "method": "PUT",
            "path": "/api/test",
            "headers": [],
        }
        await middleware(scope, MagicMock(), MagicMock())
        mock_app.assert_called_once()

```