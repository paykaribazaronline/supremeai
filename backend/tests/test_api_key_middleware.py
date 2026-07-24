import time
from unittest.mock import AsyncMock, patch

from core.security import hash_api_key, mask_api_key
from core.security.api_key_middleware import APIKeyAuthMiddleware
from fastapi import FastAPI, Request
from fastapi.responses import PlainTextResponse
from starlette.testclient import TestClient


async def acquire_true(*args, **kwargs):
    return True


async def acquire_false(*args, **kwargs):
    return False


class TestAPIKeyAuthMiddleware:
    def test_validates_valid_api_key(self):
        app = FastAPI()

        @app.get("/api/test")
        def test_endpoint(request: Request):
            return PlainTextResponse(
                f"user: {getattr(request.state, 'api_key', {}).get('id', 'none')}"
            )

        mock_row = {
            "id": "key-123",
            "key_hash": "hashed_key",
            "revoked": False,
            "rate_limit_rps": 10,
            "expires_at": None,
        }

        with (
            patch(
                "core.security.api_key_middleware.is_test_environment",
                return_value=False,
            ),
            patch("core.security.api_key_middleware.get_db_pool") as mock_pool,
            patch(
                "core.security.api_key_middleware.hash_api_key",
                return_value="hashed_key",
            ),
            patch("core.rate_limiter.AsyncRateLimiter.acquire", acquire_true),
        ):
            mock_pool.return_value.fetchrow = AsyncMock(return_value=mock_row)

            app.add_middleware(APIKeyAuthMiddleware)
            client = TestClient(app)

            resp = client.get(
                "/api/test",
                headers={"x-api-key": "sk-supreme-1101010101abcdef"},
            )

        assert resp.status_code == 200

    def test_rejects_invalid_api_key(self):
        app = FastAPI()

        @app.get("/api/test")
        def test_endpoint():
            return PlainTextResponse("ok")

        with (
            patch(
                "core.security.api_key_middleware.is_test_environment",
                return_value=False,
            ),
            patch("core.security.api_key_middleware.get_db_pool") as mock_pool,
            patch(
                "core.security.api_key_middleware.hash_api_key",
                return_value="hashed_key",
            ),
            patch("core.rate_limiter.AsyncRateLimiter.acquire", acquire_true),
        ):
            mock_pool.return_value.fetchrow = AsyncMock(return_value=None)

            app.add_middleware(APIKeyAuthMiddleware)
            client = TestClient(app)

            resp = client.get(
                "/api/test",
                headers={"x-api-key": "sk-supreme-2202020202abcdef"},
            )

        assert resp.status_code == 401

    def test_rejects_revoked_api_key(self):
        app = FastAPI()

        @app.get("/api/test")
        def test_endpoint():
            return PlainTextResponse("ok")

        mock_row = {
            "id": "key-123",
            "key_hash": "hashed_key",
            "revoked": True,
            "rate_limit_rps": 10,
            "expires_at": None,
        }

        with (
            patch(
                "core.security.api_key_middleware.is_test_environment",
                return_value=False,
            ),
            patch("core.security.api_key_middleware.get_db_pool") as mock_pool,
            patch(
                "core.security.api_key_middleware.hash_api_key",
                return_value="hashed_key",
            ),
            patch("core.rate_limiter.AsyncRateLimiter.acquire", acquire_true),
        ):
            mock_pool.return_value.fetchrow = AsyncMock(return_value=mock_row)

            app.add_middleware(APIKeyAuthMiddleware)
            client = TestClient(app)

            resp = client.get(
                "/api/test",
                headers={"x-api-key": "sk-supreme-3303030303abcdef"},
            )

        assert resp.status_code == 403

    def test_rejects_expired_api_key(self):
        app = FastAPI()

        @app.get("/api/test")
        def test_endpoint():
            return PlainTextResponse("ok")

        mock_row = {
            "id": "key-123",
            "key_hash": "hashed_key",
            "revoked": False,
            "rate_limit_rps": 10,
            "expires_at": int(time.time()) - 3600,
        }

        with (
            patch(
                "core.security.api_key_middleware.is_test_environment",
                return_value=False,
            ),
            patch("core.security.api_key_middleware.get_db_pool") as mock_pool,
            patch(
                "core.security.api_key_middleware.hash_api_key",
                return_value="hashed_key",
            ),
            patch("core.rate_limiter.AsyncRateLimiter.acquire", acquire_true),
        ):
            mock_pool.return_value.fetchrow = AsyncMock(return_value=mock_row)

            app.add_middleware(APIKeyAuthMiddleware)
            client = TestClient(app)

            resp = client.get(
                "/api/test",
                headers={"x-api-key": "sk-supreme-4404040404abcdef"},
            )

        assert resp.status_code == 403

    def test_rate_limit_exceeded(self):
        app = FastAPI()

        @app.get("/api/test")
        def test_endpoint():
            return PlainTextResponse("ok")

        mock_row = {
            "id": "key-123",
            "key_hash": "hashed_key",
            "revoked": False,
            "rate_limit_rps": 1,
            "expires_at": None,
        }

        with (
            patch(
                "core.security.api_key_middleware.is_test_environment",
                return_value=False,
            ),
            patch("core.security.api_key_middleware.get_db_pool") as mock_pool,
            patch(
                "core.security.api_key_middleware.hash_api_key",
                return_value="hashed_key",
            ),
            patch("core.rate_limiter.AsyncRateLimiter.acquire", acquire_false),
        ):
            mock_pool.return_value.fetchrow = AsyncMock(return_value=mock_row)

            app.add_middleware(APIKeyAuthMiddleware)
            client = TestClient(app)

            resp = client.get(
                "/api/test",
                headers={"x-api-key": "sk-supreme-5505050505abcdef"},
            )

        assert resp.status_code == 429


def test_mask_api_key():
    assert mask_api_key("sk-supreme-1234567890abcdef") == "sk-supreme-1234****cdef"


def test_hash_api_key():
    key = "sk-supreme-1234567890abcdef"
    hash1 = hash_api_key(key)
    hash2 = hash_api_key(key)
    assert hash1 == hash2
    assert key not in hash1
