"""Tests for API files that had 0% coverage.

Targets: errors, deps, execution_policies, selector_healing,
session_stream, simulator_admin, app_admin, app_user.
"""

from unittest.mock import MagicMock

import pytest
from fastapi import HTTPException, Request

# ── api.errors ─────────────────────────────────────────────────────────────────


class TestApiErrors:
    def test_raise_unauthorized(self):
        from api.errors import raise_unauthorized

        with pytest.raises(HTTPException) as exc:
            raise_unauthorized("Not allowed")
        assert exc.value.status_code == 401

    def test_raise_forbidden(self):
        from api.errors import raise_forbidden

        with pytest.raises(HTTPException) as exc:
            raise_forbidden("Access denied")
        assert exc.value.status_code == 403

    def test_raise_not_found(self):
        from api.errors import raise_not_found

        with pytest.raises(HTTPException) as exc:
            raise_not_found("Not found")
        assert exc.value.status_code == 404

    def test_raise_bad_request(self):
        from api.errors import raise_bad_request

        with pytest.raises(HTTPException) as exc:
            raise_bad_request("Bad input")
        assert exc.value.status_code == 400

    def test_raise_conflict(self):
        from api.errors import raise_conflict

        with pytest.raises(HTTPException) as exc:
            raise_conflict("Conflict")
        assert exc.value.status_code == 409


# ── api.deps ───────────────────────────────────────────────────────────────────


class TestApiDeps:
    def test_get_fitness_engine(self):
        from api.deps import get_fitness_engine

        engine = get_fitness_engine()
        assert engine is not None

    @pytest.mark.asyncio
    async def test_get_current_user_token(self):
        from api.deps import get_current_user_token

        req = MagicMock(spec=Request)
        req.state.user = {"sub": "test@user.com", "role": "admin"}
        result = await get_current_user_token(req)
        assert result["sub"] == "test@user.com"


# ── API route import tests ─────────────────────────────────────────────────────


class TestAPIRouteImports:
    def test_execution_policies_router(self):
        from api.routes.execution_policies import router

        assert router is not None

    def test_session_stream_router(self):
        from api.routes.session_stream import router

        assert router is not None

    def test_selector_healing_router(self):
        from api.routes.selector_healing import router

        assert router is not None

    def test_simulator_admin_router(self):
        from api.routes.simulator_admin import router

        assert router is not None



