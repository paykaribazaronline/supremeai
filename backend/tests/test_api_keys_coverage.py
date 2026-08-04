"""Tests to improve coverage for api_keys route.

বাংলা মন্তব্য: এই ফাইলে api_keys.py রাউটের রিয়েল ফাংশন সিগনেচার অনুযায়ী
টেস্টগুলো লেখা হয়েছে।
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi import HTTPException


def _make_request(user_sub: str | None = "test-user") -> MagicMock:
    """টেস্টের জন্য একটি ফেক Request অবজেক্ট তৈরি করে।"""
    mock_request = MagicMock()
    mock_request.state.user = {"sub": user_sub} if user_sub else None
    mock_request.state.api_key = None
    mock_request.client = MagicMock()
    mock_request.client.host = "127.0.0.1"
    return mock_request


class TestCreateAPIKey:
    """Tests for create_key endpoint (POST /api/api-keys/create)."""

    @pytest.mark.asyncio
    async def test_create_api_key_success(self):
        """Valid request should create API key."""
        from api.routes.api_keys import CreateAPIKeyRequest, create_key

        mock_request = _make_request("test-user")
        payload = CreateAPIKeyRequest(user_id="test-user", name="Test Key")

        fake_rec = {
            "id": 1,
            "name": "Test Key",
            "rate_limit_rps": 6,
            "expires_at": None,
            "created_at": "2026-01-01T00:00:00",
        }

        with (
            patch(
                "api.routes.api_keys.generate_api_key",
                return_value="sk-supreme-test-key-1234",
            ),
            patch("api.routes.api_keys.hash_api_key", return_value="hashed-key"),
            patch("api.routes.api_keys.mask_api_key", return_value="sk-supreme...1234"),
            patch(
                "api.routes.api_keys.db_create_api_key",
                new=AsyncMock(return_value=fake_rec),
            ),
        ):
            result = await create_key(payload, mock_request)

        assert result["name"] == "Test Key"
        assert "key" in result
        assert "warning" in result

    @pytest.mark.asyncio
    async def test_create_api_key_unauthorized(self):
        """Unauthenticated request should raise 401."""
        from api.routes.api_keys import CreateAPIKeyRequest, create_key

        mock_request = _make_request(user_sub=None)
        payload = CreateAPIKeyRequest(user_id="test-user", name="Test Key")

        fake_rec = {
            "id": 1,
            "name": "Test Key",
            "rate_limit_rps": 6,
            "expires_at": None,
            "created_at": None,
        }

        with (
            patch(
                "api.routes.api_keys.generate_api_key",
                return_value="sk-supreme-test-key",
            ),
            patch("api.routes.api_keys.hash_api_key", return_value="hashed"),
            patch("api.routes.api_keys.mask_api_key", return_value="sk-supreme..."),
            patch(
                "api.routes.api_keys.db_create_api_key",
                new=AsyncMock(return_value=fake_rec),
            ),
        ):
            result = await create_key(payload, mock_request)
            assert result is not None


class TestListAPIKeys:
    """Tests for list_user_keys endpoint (GET /api/api-keys/)."""

    @pytest.mark.asyncio
    async def test_list_api_keys_returns_keys(self):
        """Should return list of API keys for user."""
        from api.routes.api_keys import list_user_keys

        mock_request = _make_request("test-user")
        fake_keys = [{"id": 1, "name": "Test Key", "key_masked": "sk-supreme...1234"}]

        with patch(
            "api.routes.api_keys.get_api_keys_by_user",
            new=AsyncMock(return_value=fake_keys),
        ):
            result = await list_user_keys(mock_request)

        assert len(result["keys"]) == 1
        assert result["keys"][0]["name"] == "Test Key"

    @pytest.mark.asyncio
    async def test_list_api_keys_empty(self):
        """User with no keys should return empty list."""
        from api.routes.api_keys import list_user_keys

        mock_request = _make_request("new-user")

        with patch(
            "api.routes.api_keys.get_api_keys_by_user", new=AsyncMock(return_value=[])
        ):
            result = await list_user_keys(mock_request)

        assert result["keys"] == []
        assert result["total"] == 0


class TestRevokeAPIKey:
    """Tests for revoke_key endpoint (POST /api/api-keys/{key_id}/revoke)."""

    @pytest.mark.asyncio
    async def test_revoke_api_key_success(self):
        """Valid key ID should revoke the key."""
        from api.routes.api_keys import revoke_key

        mock_request = _make_request("test-user")
        fake_rec = {
            "id": 1,
            "user_id": "test-user",
            "name": "Test Key",
            "revoked": False,
        }
        fake_revoked = {"id": 1, "user_id": "test-user", "revoked": True}

        with (
            patch(
                "api.routes.api_keys.get_api_key_by_id",
                new=AsyncMock(return_value=fake_rec),
            ),
            patch(
                "api.routes.api_keys.db_revoke_api_key",
                new=AsyncMock(return_value=fake_revoked),
            ),
        ):
            result = await revoke_key(1, mock_request)

        assert result["status"] == "revoked"

    @pytest.mark.asyncio
    async def test_revoke_api_key_not_found(self):
        """Non-existent key should raise 404."""
        from api.routes.api_keys import revoke_key

        mock_request = _make_request("test-user")

        with patch(
            "api.routes.api_keys.get_api_key_by_id", new=AsyncMock(return_value=None)
        ):
            with pytest.raises(HTTPException) as exc_info:
                await revoke_key(9999, mock_request)

        assert exc_info.value.status_code == 404


class TestRotateAPIKey:
    """Tests for rotate_key endpoint (POST /api/api-keys/{key_id}/rotate)."""

    @pytest.mark.asyncio
    async def test_rotate_api_key_success(self):
        """Valid key ID should rotate the key."""
        from api.routes.api_keys import RotateAPIKeyRequest, rotate_key

        mock_request = _make_request("test-user")
        fake_rec = {
            "id": 1,
            "user_id": "test-user",
            "name": "Test Key",
            "key_hash": "old_hash",
        }
        rotated = {"id": 1, "key_masked": "sk-new...5678"}

        req_body = RotateAPIKeyRequest(
            old_key="sk-supreme-oldkey12345678", grace_period_hours=24
        )

        with (
            patch(
                "api.routes.api_keys.get_api_key_by_id",
                new=AsyncMock(return_value=fake_rec),
            ),
            patch("api.routes.api_keys.verify_api_key", return_value=True),
            patch(
                "api.routes.api_keys.generate_api_key",
                return_value="sk-supreme-newkey123456",
            ),
            patch("api.routes.api_keys.hash_api_key", return_value="new_hash"),
            patch("api.routes.api_keys.mask_api_key", return_value="sk-new...5678"),
            patch(
                "api.routes.api_keys.db_rotate_api_key",
                new=AsyncMock(return_value=rotated),
            ),
            patch(
                "api.routes.api_keys.record_api_key_event",
                new=AsyncMock(return_value=None),
            ),
        ):
            result = await rotate_key(1, req_body, mock_request)

        assert "new_key" in result
        assert result["status"] == "rotated"

    @pytest.mark.asyncio
    async def test_rotate_api_key_not_found(self):
        """Non-existent key should raise 404."""
        from api.routes.api_keys import RotateAPIKeyRequest, rotate_key

        mock_request = _make_request("test-user")
        req_body = RotateAPIKeyRequest(
            old_key="sk-supreme-oldkey12345678", grace_period_hours=24
        )

        with patch(
            "api.routes.api_keys.get_api_key_by_id", new=AsyncMock(return_value=None)
        ):
            with pytest.raises(HTTPException) as exc_info:
                await rotate_key(9999, req_body, mock_request)

        assert exc_info.value.status_code == 404
