"""
Coverage tests for api/routes/tenant_admin.py.
Target: 100% line coverage.

টেন্যান্ট অ্যাডমিন রাউটের সকল ফাংশন ও শাখা কভার করা হয়েছে।
"""

import os
import sys
from unittest.mock import MagicMock, patch

import pytest

_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)


class TestTenantLimitModels:
    """Tests for TenantLimitCreate and TenantLimitUpdate models."""

    def test_tenant_limit_create_defaults(self):
        """TenantLimitCreate should have correct defaults."""
        from api.routes.tenant_admin import TenantLimitCreate

        model = TenantLimitCreate(tenant_id="test-tenant")
        assert model.tenant_id == "test-tenant"
        assert model.billing_tier == "free"
        assert model.requests_per_minute is None

    def test_tenant_limit_update_allows_partial(self):
        """TenantLimitUpdate should allow partial updates."""
        from api.routes.tenant_admin import TenantLimitUpdate

        model = TenantLimitUpdate(org_name="New Org")
        assert model.org_name == "New Org"
        assert model.billing_tier is None


class TestTierDefaults:
    """Tests for TIER_DEFAULTS."""

    def test_tier_defaults_structure(self):
        """TIER_DEFAULTS should have correct structure."""
        from api.routes.tenant_admin import TIER_DEFAULTS

        assert "free" in TIER_DEFAULTS
        assert "starter" in TIER_DEFAULTS
        assert "pro" in TIER_DEFAULTS
        assert "enterprise" in TIER_DEFAULTS
        assert TIER_DEFAULTS["free"]["requests_per_minute"] == 20
        assert TIER_DEFAULTS["enterprise"]["max_concurrent_sessions"] == 100


class TestGetDB:
    """Tests for _get_db."""

    def test_get_db_success(self):
        """_get_db should return db client when available."""
        from api.routes.tenant_admin import _get_db

        with patch("database.supabase_client.db") as mock_db:
            mock_db.client = MagicMock()
            result = _get_db()
            assert result is not None

    def test_get_db_no_client(self):
        """_get_db should return None when db has no client."""
        from api.routes.tenant_admin import _get_db

        with patch("database.supabase_client.db") as mock_db:
            mock_db.client = None
            result = _get_db()
            assert result is None

    def test_get_db_exception(self):
        """_get_db should return None when db module import fails."""
        from api.routes.tenant_admin import _get_db

        with patch.dict("sys.modules", {"database.supabase_client": None}):
            result = _get_db()
            assert result is None


class TestDBListTenants:
    """Tests for _db_list_tenants."""

    @pytest.mark.asyncio
    async def test_db_list_tenants_success(self):
        """_db_list_tenants should return data from Supabase."""
        from api.routes.tenant_admin import _db_list_tenants

        mock_client = MagicMock()
        mock_res = MagicMock()
        mock_res.data = [{"tenant_id": "t1"}, {"tenant_id": "t2"}]
        mock_client.table.return_value.select.return_value.order.return_value.execute.return_value = mock_res

        with patch("api.routes.tenant_admin._get_db", return_value=mock_client):
            result = await _db_list_tenants()
            assert len(result) == 2

    @pytest.mark.asyncio
    async def test_db_list_tenants_fallback(self):
        """_db_list_tenants should fallback to local store on failure."""
        from api.routes.tenant_admin import _db_list_tenants, _local_store

        _local_store["tenants"] = [{"tenant_id": "local-t1"}]
        with patch("api.routes.tenant_admin._get_db", return_value=None):
            result = await _db_list_tenants()
            assert len(result) == 1
            assert result[0]["tenant_id"] == "local-t1"
        _local_store.clear()


class TestDBGetTenant:
    """Tests for _db_get_tenant."""

    @pytest.mark.asyncio
    async def test_db_get_tenant_found(self):
        """_db_get_tenant should return tenant when found."""
        from api.routes.tenant_admin import _db_get_tenant

        mock_client = MagicMock()
        mock_res = MagicMock()
        mock_res.data = [{"tenant_id": "t1", "org_name": "Test"}]
        mock_client.table.return_value.select.return_value.eq.return_value.execute.return_value = mock_res

        with patch("api.routes.tenant_admin._get_db", return_value=mock_client):
            result = await _db_get_tenant("t1")
            assert result is not None
            assert result["tenant_id"] == "t1"

    @pytest.mark.asyncio
    async def test_db_get_tenant_not_found(self):
        """_db_get_tenant should return None when not found."""
        from api.routes.tenant_admin import _db_get_tenant

        mock_client = MagicMock()
        mock_res = MagicMock()
        mock_res.data = []
        mock_client.table.return_value.select.return_value.eq.return_value.execute.return_value = mock_res

        with patch("api.routes.tenant_admin._get_db", return_value=mock_client):
            result = await _db_get_tenant("nonexistent")
            assert result is None


class TestDBUpsertTenant:
    """Tests for _db_upsert_tenant."""

    @pytest.mark.asyncio
    async def test_db_upsert_tenant_success(self):
        """_db_upsert_tenant should upsert via Supabase."""
        from api.routes.tenant_admin import _db_upsert_tenant

        mock_client = MagicMock()
        with patch("api.routes.tenant_admin._get_db", return_value=mock_client):
            result = await _db_upsert_tenant({"tenant_id": "t1", "org_name": "Test"})
            assert result is True

    @pytest.mark.asyncio
    async def test_db_upsert_tenant_local_fallback(self):
        """_db_upsert_tenant should fallback to local store."""
        from api.routes.tenant_admin import _db_upsert_tenant, _local_store

        _local_store.clear()
        with patch("api.routes.tenant_admin._get_db", return_value=None):
            result = await _db_upsert_tenant({"tenant_id": "t1", "org_name": "Test"})
            assert result is True
            assert len(_local_store["tenants"]) == 1
        _local_store.clear()


class TestDBDeleteTenant:
    """Tests for _db_delete_tenant."""

    @pytest.mark.asyncio
    async def test_db_delete_tenant_success(self):
        """_db_delete_tenant should delete via Supabase."""
        from api.routes.tenant_admin import _db_delete_tenant

        mock_client = MagicMock()
        with patch("api.routes.tenant_admin._get_db", return_value=mock_client):
            result = await _db_delete_tenant("t1")
            assert result is True

    @pytest.mark.asyncio
    async def test_db_delete_tenant_local_fallback(self):
        """_db_delete_tenant should fallback to local store."""
        from api.routes.tenant_admin import _db_delete_tenant, _local_store

        _local_store["tenants"] = [{"tenant_id": "t1"}, {"tenant_id": "t2"}]
        with patch("api.routes.tenant_admin._get_db", return_value=None):
            result = await _db_delete_tenant("t1")
            assert result is True
            assert len(_local_store["tenants"]) == 1
        _local_store.clear()


class TestGetTenantUsage:
    """Tests for _get_tenant_usage."""

    @pytest.mark.asyncio
    async def test_get_tenant_usage_redis(self):
        """_get_tenant_usage should read from Redis when available."""
        from api.routes.tenant_admin import _get_tenant_usage

        with (
            patch("core.services") as mock_app,
            patch("api.routes.tenant_admin._get_db") as mock_get_db,
        ):
            mock_queue = MagicMock()
            mock_queue.configured = True
            mock_queue.get.side_effect = ["100", "5000", "0.05"]
            mock_app.redis_queue = mock_queue
            mock_get_db.return_value = None

            result = await _get_tenant_usage("t1")
            assert result["requests_today"] == 100
            assert result["tokens_today"] == 5000

    @pytest.mark.asyncio
    async def test_get_tenant_usage_empty(self):
        """_get_tenant_usage should return zeros when no data."""
        from api.routes.tenant_admin import _get_tenant_usage

        with (
            patch("core.services") as mock_app,
            patch("api.routes.tenant_admin._get_db", return_value=None),
        ):
            mock_app.redis_queue = None
            result = await _get_tenant_usage("t1")
            assert result["requests_today"] == 0
            assert result["tokens_today"] == 0
