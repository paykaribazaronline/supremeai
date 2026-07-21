"""Integration tests for rate limit enforcement.

বাংলা: টেন্যান্ট রেট লিমিট — RPM, RPD, কোস্ট কোটা এবং admin override।
"""

from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from tools.tenant_rate_limiter import TenantRateLimiter


@pytest.fixture
def limiter():
    mock_redis = MagicMock()
    mock_redis.configured = True
    return TenantRateLimiter(redis_client=mock_redis)


class TestRateLimitEnforcement:
    """Tests for rate limit enforcement."""

    def test_billing_tiers(self):
        limiter = TenantRateLimiter(redis_client=MagicMock())
        assert "free" in limiter.billing_tiers
        assert "pro" in limiter.billing_tiers
        assert "enterprise" in limiter.billing_tiers

    def test_redis_key(self):
        limiter = TenantRateLimiter(redis_client=MagicMock())
        key = limiter._redis_key("tenant-1", "rpm")
        assert key == "rate:tenant-1:rpm"

    @pytest.mark.asyncio
    async def test_get_tier_without_redis(self):
        with patch(
            "tools.tenant_rate_limiter.TenantRateLimiter._resolve_redis_queue",
            return_value=None,
        ):
            limiter = TenantRateLimiter(redis_client=None)
            tier = await limiter.get_tier("tenant-1")
            assert tier == "free"

    @pytest.mark.asyncio
    async def test_get_tier_with_configured_redis(self, limiter):
        limiter.queue.get.return_value = b"pro"
        tier = await limiter.get_tier("tenant-1")
        assert tier == "pro"

    @pytest.mark.asyncio
    async def test_get_tier_exception(self, limiter):
        limiter.queue.get.side_effect = Exception("redis error")
        tier = await limiter.get_tier("tenant-1")
        assert tier == "free"

    @pytest.mark.asyncio
    async def test_check_quota_admin_override(self, limiter):
        res = await limiter.check_quota("tenant-1", cost=10.0, admin_override=True)
        assert res["allowed"] is True
        assert res["reason"] == "admin_override"

    @pytest.mark.asyncio
    async def test_check_quota_no_redis(self):
        with patch(
            "tools.tenant_rate_limiter.TenantRateLimiter._resolve_redis_queue",
            return_value=None,
        ):
            limiter = TenantRateLimiter(redis_client=None)
            limiter.queue = None
            res = await limiter.check_quota("tenant-1", cost=0.0)
            assert res["allowed"] is True
            assert res["reason"] == "no_redis"

    @pytest.mark.asyncio
    async def test_check_quota_rpm_exceeded(self, limiter):
        limiter.queue.get.side_effect = lambda key: (
            b"100" if key.endswith(":rpm") else b"0"
        )
        res = await limiter.check_quota("tenant-1", cost=0.0)
        assert res["allowed"] is False
        assert res["reason"] == "rpm_exceeded"

    @pytest.mark.asyncio
    async def test_check_quota_rpd_exceeded(self, limiter):
        limiter.queue.get.side_effect = lambda key: (
            b"0" if key.endswith(":rpm") else b"100000"
        )
        res = await limiter.check_quota("tenant-1", cost=0.0)
        assert res["allowed"] is False
        assert res["reason"] == "rpd_exceeded"

    @pytest.mark.asyncio
    async def test_check_quota_redis_exception(self, limiter):
        limiter.queue.get.side_effect = Exception("redis down")
        res = await limiter.check_quota("tenant-1", cost=0.0)
        assert res["allowed"] is True
        assert res["reason"] == "redis_error"

    @pytest.mark.asyncio
    async def test_record_usage_without_redis(self):
        with patch(
            "tools.tenant_rate_limiter.TenantRateLimiter._resolve_redis_queue",
            return_value=None,
        ):
            limiter = TenantRateLimiter(redis_client=None)
            limiter.queue = None
            res = await limiter.record_usage("tenant-1", cost=0.5, tokens=10)
            assert res["status"] == "success"
            assert res["billed"] == 0.0

    @pytest.mark.asyncio
    async def test_record_usage_with_pipeline(self, limiter):
        mock_pipe = MagicMock()
        limiter.queue.pipeline.return_value = mock_pipe
        with patch("tools.tenant_rate_limiter.settings") as mock_settings:
            mock_settings.stripe_api_key = ""
            res = await limiter.record_usage("tenant-1", cost=0.5, tokens=10)
        assert res["status"] == "success"
        mock_pipe.execute.assert_called_once()

    @pytest.mark.asyncio
    async def test_record_usage_redis_exception(self, limiter):
        limiter.queue.pipeline.side_effect = Exception("redis error")
        with patch("tools.tenant_rate_limiter.settings") as mock_settings:
            mock_settings.stripe_api_key = ""
            res = await limiter.record_usage("tenant-1", cost=0.5, tokens=10)
        assert res["status"] == "success"
