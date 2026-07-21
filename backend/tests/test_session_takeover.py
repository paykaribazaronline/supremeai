"""গ্যাপ ফিক্স রিগ্রেশন টেস্ট: production-এ fake screencast frame আর পাঠানো হবে না,
এবং একই takeover token দ্বিতীয়বার ব্যবহার করা যাবে না (replay protection)।"""

import os
from unittest.mock import AsyncMock, patch

import pytest
from api.routes import session_takeover


@pytest.mark.asyncio
async def test_verify_takeover_token_rejects_unknown_token():
    assert await session_takeover.verify_takeover_token("tok_not_in_allowlist") is False


@pytest.mark.asyncio
async def test_verify_takeover_token_rejects_replay_when_redis_available(monkeypatch):
    monkeypatch.setenv("ALLOWED_TAKEOVER_TOKENS", "tok_abc123")
    fake_redis = AsyncMock()
    # প্রথমবার consume সফল (True), দ্বিতীয়বার token আগেই ব্যবহৃত (None/False)
    fake_redis.set.side_effect = [True, None]

    with patch.object(
        session_takeover, "_redis_client", AsyncMock(return_value=fake_redis)
    ):
        first = await session_takeover.verify_takeover_token("tok_abc123")
        second = await session_takeover.verify_takeover_token("tok_abc123")

    assert first is True
    assert second is False


def test_is_production_flag(monkeypatch):
    monkeypatch.setenv("SUPREMEAI_ENV", "production")
    assert session_takeover._is_production() is True
    monkeypatch.setenv("SUPREMEAI_ENV", "development")
    assert session_takeover._is_production() is False
    os.environ.pop("SUPREMEAI_ENV", None)
    assert session_takeover._is_production() is False
