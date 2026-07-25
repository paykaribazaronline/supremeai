"""Tests to improve coverage for api_keys route (30.9% -> target 60%)."""

from unittest.mock import MagicMock, patch

import pytest
from fastapi import HTTPException


class TestCreateAPIKey:
    """Tests for create_api_key endpoint."""

    def test_create_api_key_success(self):
        """Valid request should create API key."""
        from api.routes.api_keys import CreateAPIKeyRequest, create_api_key

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        payload = CreateAPIKeyRequest(user_id="test-user", name="Test Key")

        with patch("api.routes.api_keys.generate_api_key", return_value="sk-test-key"):
            with patch("api.routes.api_keys.hash_api_key", return_value="hashed-key"):
                with patch("api.routes.api_keys.create_api_key") as mock_create:
                    mock_create.return_value = {
                        "id": "key-1",
                        "name": "Test Key",
                        "key": "sk-test-key",
                    }
                    result = create_api_key(payload, mock_request)

        assert result["name"] == "Test Key"
        assert "key" in result

    def test_create_api_key_unauthorized(self):
        """Unauthenticated request should raise 401."""
        from api.routes.api_keys import CreateAPIKeyRequest, create_api_key

        mock_request = MagicMock()
        mock_request.state.user = None

        payload = CreateAPIKeyRequest(user_id="test-user", name="Test Key")

        with pytest.raises(HTTPException) as exc_info:
            create_api_key(payload, mock_request)

        assert exc_info.value.status_code == 401


class TestListAPIKeys:
    """Tests for list_api_keys endpoint."""

    def test_list_api_keys_returns_keys(self):
        """Should return list of API keys for user."""
        from api.routes.api_keys import list_api_keys

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        with patch("api.routes.api_keys.get_api_keys_by_user") as mock_get:
            mock_get.return_value = [
                {"id": "key-1", "name": "Test Key", "masked_key": "sk-test...xyz"}
            ]
            result = list_api_keys(mock_request)

        assert len(result) == 1
        assert result[0]["name"] == "Test Key"

    def test_list_api_keys_empty(self):
        """User with no keys should return empty list."""
        from api.routes.api_keys import list_api_keys

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "new-user"}

        with patch("api.routes.api_keys.get_api_keys_by_user") as mock_get:
            mock_get.return_value = []
            result = list_api_keys(mock_request)

        assert result == []


class TestRevokeAPIKey:
    """Tests for revoke_api_key endpoint."""

    def test_revoke_api_key_success(self):
        """Valid key ID should revoke the key."""
        from api.routes.api_keys import revoke_api_key

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        with patch("api.routes.api_keys.revoke_api_key") as mock_revoke:
            mock_revoke.return_value = {"id": "key-1", "status": "revoked"}
            result = revoke_api_key("key-1", mock_request)

        assert result["status"] == "revoked"

    def test_revoke_api_key_not_found(self):
        """Non-existent key should raise 404."""
        from api.routes.api_keys import revoke_api_key

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        with patch("api.routes.api_keys.revoke_api_key", return_value=None):
            with pytest.raises(HTTPException) as exc_info:
                revoke_api_key("nonexistent-key", mock_request)

        assert exc_info.value.status_code == 404


class TestRotateAPIKey:
    """Tests for rotate_api_key endpoint."""

    def test_rotate_api_key_success(self):
        """Valid key ID should rotate the key."""
        from api.routes.api_keys import rotate_api_key

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        with patch("api.routes.api_keys.rotate_api_key") as mock_rotate:
            mock_rotate.return_value = {"id": "key-1", "new_key": "sk-new-key"}
            result = rotate_api_key("key-1", mock_request)

        assert "new_key" in result

    def test_rotate_api_key_not_found(self):
        """Non-existent key should raise 404."""
        from api.routes.api_keys import rotate_api_key

        mock_request = MagicMock()
        mock_request.state.user = {"sub": "test-user"}

        with patch("api.routes.api_keys.rotate_api_key", return_value=None):
            with pytest.raises(HTTPException) as exc_info:
                rotate_api_key("nonexistent-key", mock_request)

        assert exc_info.value.status_code == 404
