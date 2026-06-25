from __future__ import annotations

import pytest
from datetime import timedelta
from unittest.mock import MagicMock, patch

from fastapi import FastAPI
from pydantic import ValidationError

from api.routes.auth import (
    router,
    create_access_token,
    optional_current_user,
    LoginRequest,
    TokenResponse,
    MeResponse,
    UserContext,
)


@pytest.fixture
def client():
    app = FastAPI()
    app.include_router(router)
    from fastapi.testclient import TestClient
    return TestClient(app)


class TestCreateAccessToken:
    def test_create_access_token_returns_string(self):
        mock_jwt = MagicMock()
        mock_jwt.encode.return_value = "token_string"
        with patch("api.routes.auth.jwt", mock_jwt):
            with patch("api.routes.auth.SECRET_KEY", "test_secret"):
                token = create_access_token({"sub": "user1", "role": "admin"})
        assert token == "token_string"
        mock_jwt.encode.assert_called_once()
        args = mock_jwt.encode.call_args.args
        assert args[1] == "test_secret"
        assert mock_jwt.encode.call_args.kwargs["algorithm"] == "HS256"

    def test_create_access_token_with_expires_delta(self):
        mock_jwt = MagicMock()
        with patch("api.routes.auth.jwt", mock_jwt):
            with patch("api.routes.auth.SECRET_KEY", "test_secret"):
                create_access_token({"sub": "u"}, expires_delta=timedelta(minutes=30))
        assert mock_jwt.encode.call_args.kwargs["algorithm"] == "HS256"

    def test_create_access_token_missing_jose(self):
        import api.routes.auth as auth_module
        old_jwt = auth_module.jwt
        try:
            auth_module.jwt = None
            with pytest.raises(RuntimeError, match="python-jose"):
                create_access_token({"sub": "u"})
        finally:
            auth_module.jwt = old_jwt


class TestOptionalCurrentUser:
    @pytest.mark.anyio
    async def test_no_token_returns_none(self):
        with patch("api.routes.auth.jwt") as mock_jwt:
            result = await optional_current_user(token=None)
        assert result is None
        mock_jwt.decode.assert_not_called()

    @pytest.mark.anyio
    async def test_invalid_token_returns_none(self):
        with patch("api.routes.auth.jwt") as mock_jwt:
            mock_jwt.decode.side_effect = Exception("bad token")
            result = await optional_current_user(token="bad.token.here")
        assert result is None

    @pytest.mark.anyio
    async def test_valid_token_returns_user_context(self):
        with patch("api.routes.auth.jwt") as mock_jwt:
            mock_jwt.decode.return_value = {"sub": "user1", "role": "admin"}
            result = await optional_current_user(token="valid.token.here")
        assert result is not None
        assert isinstance(result, UserContext)
        assert result.user_id == "user1"
        assert result.role == "admin"

    @pytest.mark.anyio
    async def test_valid_token_defaults_role(self):
        with patch("api.routes.auth.jwt") as mock_jwt:
            mock_jwt.decode.return_value = {"sub": "user1"}
            result = await optional_current_user(token="valid.token.here")
        assert result.role == "viewer"

    @pytest.mark.anyio
    async def test_missing_jose_returns_none(self):
        import api.routes.auth as auth_module
        old_jwt = auth_module.jwt
        try:
            auth_module.jwt = None
            result = await optional_current_user(token="any.token")
        finally:
            auth_module.jwt = old_jwt
        assert result is None


class TestLoginEndpoint:
    def test_login_returns_501(self, client):
        resp = client.post("/auth/login", json={"username": "test", "password": "test"})
        assert resp.status_code == 501
        assert resp.json()["detail"] == "Direct login is not supported. Use the admin TOTP flow or an OAuth provider."

    def test_login_missing_username(self, client):
        resp = client.post("/auth/login", json={"password": "test"})
        assert resp.status_code == 422

    def test_login_missing_password(self, client):
        resp = client.post("/auth/login", json={"username": "test"})
        assert resp.status_code == 422

    def test_login_missing_body(self, client):
        resp = client.post("/auth/login", json={})
        assert resp.status_code == 422


class TestMeEndpoint:
    @pytest.mark.anyio
    async def test_me_without_token_returns_401(self, client):
        resp = client.get("/auth/me")
        assert resp.status_code == 401
        assert resp.json()["detail"] == "Not authenticated"

    @pytest.mark.anyio
    async def test_me_with_valid_token(self, client):
        with patch("api.routes.auth.jwt") as mock_jwt:
            mock_jwt.decode.return_value = {"sub": "user1", "role": "admin"}
            resp = client.get("/auth/me", headers={"Authorization": "Bearer valid.token"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["user_id"] == "user1"
        assert data["role"] == "admin"
        assert data["scopes"] == []

    @pytest.mark.anyio
    async def test_me_with_invalid_token(self, client):
        with patch("api.routes.auth.jwt") as mock_jwt:
            mock_jwt.decode.side_effect = Exception("bad token")
            resp = client.get("/auth/me", headers={"Authorization": "Bearer bad.token"})
        assert resp.status_code == 401


class TestRequestModels:
    def test_login_request_valid(self):
        body = LoginRequest(username="user", password="pass")
        assert body.username == "user"
        assert body.password == "pass"

    def test_login_request_missing_username(self):
        with pytest.raises(ValidationError):
            LoginRequest(password="pass")

    def test_login_request_missing_password(self):
        with pytest.raises(ValidationError):
            LoginRequest(username="user")

    def test_token_response_defaults(self):
        resp = TokenResponse(access_token="tok", user_id="u1", role="admin")
        assert resp.access_token == "tok"
        assert resp.token_type == "bearer"
        assert resp.user_id == "u1"
        assert resp.role == "admin"

    def test_me_response_defaults(self):
        resp = MeResponse(user_id="u1", role="admin")
        assert resp.scopes == ()

    def test_me_response_with_scopes(self):
        resp = MeResponse(user_id="u1", role="admin", scopes=("read", "write"))
        assert resp.scopes == ("read", "write")
