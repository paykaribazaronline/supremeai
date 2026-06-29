# বাংলা মন্তব্য: অব্যবহৃত time ইম্পোর্টটি মুছে ফেলা হলো।
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from tools.tenant_rate_limiter import TenantRateLimiter


@pytest.fixture
def limiter():
    mock_redis = MagicMock()
    mock_redis.configured = True
    return TenantRateLimiter(redis_client=mock_redis)


def test_billing_tiers():
    limiter = TenantRateLimiter(redis_client=None)
    assert "free" in limiter.billing_tiers
    assert "pro" in limiter.billing_tiers
    assert "enterprise" in limiter.billing_tiers


def test_redis_key():
    limiter = TenantRateLimiter(redis_client=None)
    key = limiter._redis_key("tenant-1", "rpm")
    assert key == "rate:tenant-1:rpm"


@pytest.mark.asyncio
async def test_get_tier_without_redis():
    limiter = TenantRateLimiter(redis_client=None)
    tier = await limiter.get_tier("tenant-1")
    assert tier == "free"


@pytest.mark.asyncio
async def test_get_tier_with_configured_redis(limiter):
    limiter.queue.get.return_value = b"pro"
    tier = await limiter.get_tier("tenant-1")
    assert tier == "pro"


@pytest.mark.asyncio
async def test_get_tier_with_string_redis(limiter):
    limiter.queue.get.return_value = "enterprise"
    tier = await limiter.get_tier("tenant-1")
    assert tier == "enterprise"


@pytest.mark.asyncio
async def test_get_tier_exception(limiter):
    limiter.queue.get.side_effect = Exception("redis error")
    tier = await limiter.get_tier("tenant-1")
    assert tier == "free"


@pytest.mark.asyncio
async def test_set_tier_without_redis():
    limiter = TenantRateLimiter(redis_client=None)
    await limiter.set_tier("tenant-1", "pro")


@pytest.mark.asyncio
async def test_set_tier_valid(limiter):
    await limiter.set_tier("tenant-1", "pro")
    limiter.queue.set.assert_called_once_with("billing:tier:tenant-1", "pro", ex=3600)


@pytest.mark.asyncio
async def test_set_tier_invalid(limiter):
    with pytest.raises(ValueError):
        await limiter.set_tier("tenant-1", "unknown")


@pytest.mark.asyncio
async def test_set_tier_exception(limiter):
    limiter.queue.set.side_effect = Exception("redis error")
    await limiter.set_tier("tenant-1", "pro")


@pytest.mark.asyncio
async def test_check_quota_admin_override(limiter):
    res = await limiter.check_quota("tenant-1", cost=10.0, admin_override=True)
    assert res["allowed"] is True
    assert res["reason"] == "admin_override"


@pytest.mark.asyncio
async def test_check_quota_no_redis():
    limiter = TenantRateLimiter(redis_client=None)
    res = await limiter.check_quota("tenant-1", cost=0.0)
    assert res["allowed"] is True
    assert res["reason"] == "no_redis"


@pytest.mark.asyncio
async def test_check_quota_rpm_exceeded(limiter):
    limiter.queue.get.side_effect = lambda key: b"100" if key.endswith(":rpm") else b"0"
    res = await limiter.check_quota("tenant-1", cost=0.0)
    assert res["allowed"] is False
    assert res["reason"] == "rpm_exceeded"


@pytest.mark.asyncio
async def test_check_quota_rpd_exceeded(limiter):
    limiter.queue.get.side_effect = lambda key: b"0" if key.endswith(":rpm") else b"100000"
    res = await limiter.check_quota("tenant-1", cost=0.0)
    assert res["allowed"] is False
    assert res["reason"] == "rpd_exceeded"


@pytest.mark.asyncio
async def test_check_quota_redis_exception(limiter):
    limiter.queue.get.side_effect = Exception("redis down")
    res = await limiter.check_quota("tenant-1", cost=0.0)
    assert res["allowed"] is True
    assert res["reason"] == "redis_error"


@pytest.mark.asyncio
async def test_record_usage_without_redis():
    limiter = TenantRateLimiter(redis_client=None)
    res = await limiter.record_usage("tenant-1", cost=0.5, tokens=10)
    assert res["status"] == "success"
    assert res["billed"] == 0.0


@pytest.mark.asyncio
async def test_record_usage_with_pipeline(limiter):
    mock_pipe = MagicMock()
    limiter.queue.pipeline.return_value = mock_pipe
    with patch("tools.tenant_rate_limiter.settings") as mock_settings:
        mock_settings.stripe_api_key = ""
        res = await limiter.record_usage("tenant-1", cost=0.5, tokens=10)
    assert res["status"] == "success"
    mock_pipe.execute.assert_called_once()


@pytest.mark.asyncio
async def test_record_usage_without_pipeline(limiter):
    mock_queue = MagicMock()
    mock_queue.configured = True
    mock_queue.get.return_value = b"0"
    mock_queue.incr.return_value = 1
    mapper = {}
    call_count = [0]

    def side_effect(key):
        call_count[0] += 1
        return mapper.get(key, b"0")

    mock_queue.get.side_effect = side_effect
    limiter.queue = mock_queue
    with patch("tools.tenant_rate_limiter.settings") as mock_settings:
        mock_settings.stripe_api_key = ""
        res = await limiter.record_usage("tenant-1", cost=0.5, tokens=10)
    assert res["status"] == "success"


@pytest.mark.asyncio
async def test_record_usage_redis_exception(limiter):
    limiter.queue.pipeline.side_effect = Exception("redis error")
    with patch("tools.tenant_rate_limiter.settings") as mock_settings:
        mock_settings.stripe_api_key = ""
        res = await limiter.record_usage("tenant-1", cost=0.5, tokens=10)
    assert res["status"] == "success"


@pytest.mark.asyncio
async def test_record_usage_stripe_charge(limiter):
    mock_pipe = MagicMock()
    limiter.queue.pipeline.return_value = mock_pipe
    mock_queue = MagicMock()
    mock_queue.configured = True
    mock_queue.get.return_value = b"10"
    mock_queue.incr.return_value = 1
    mapper = {"rate:tenant-1:cost": b"10"}
    mock_queue.get.side_effect = lambda key: mapper.get(key, b"0")
    limiter.queue = mock_queue
    mock_stripe = MagicMock()
    with patch("tools.tenant_rate_limiter.settings") as mock_settings:
        mock_settings.stripe_api_key = "sk-test"
        with patch.dict("sys.modules", {"stripe": mock_stripe}):
            res = await limiter.record_usage("tenant-1", cost=1.5, tokens=10)
    assert res["total_cost"] == 10.0
    mock_stripe.InvoiceItem.create.assert_called_once()


@pytest.mark.asyncio
async def test_record_usage_stripe_failure(limiter):
    mock_pipe = MagicMock()
    limiter.queue.pipeline.return_value = mock_pipe
    mock_stripe = MagicMock()
    mock_stripe.InvoiceItem.create.side_effect = Exception("stripe error")
    with patch("tools.tenant_rate_limiter.settings") as mock_settings:
        mock_settings.stripe_api_key = "sk-test"
        with patch.dict("sys.modules", {"stripe": mock_stripe}):
            res = await limiter.record_usage("tenant-1", cost=1.5, tokens=10)
    assert res["status"] == "success"
