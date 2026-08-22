"""TrustedOriginMiddleware এর ইউনিট টেস্ট।

বাংলা: এখানে শুধু portal_role নির্ধারণ ও allowed_origins গণনার লজিক কভার করা হয়েছে
(নেটওয়ার্ক/ডিস্প্যাচ ছাড়া)। settings-এর বিভিন্ন অ্যাট্রিবিউট মক করে আইসোলেশন নিশ্চিত করা হয়েছে।
"""

from __future__ import annotations

from unittest.mock import MagicMock

import pytest

from core.security import origin_validator
from core.security.origin_validator import (
    ADMIN_DEFAULT_TRUSTED_ORIGINS,
    USER_DEFAULT_TRUSTED_ORIGINS,
    TrustedOriginMiddleware,
)


@pytest.fixture
def fake_settings():
    s = MagicMock()
    s.service_role = "user"
    s.admin_cors_origins = []
    s.user_cors_origins = []
    s.cors_origins = []
    s.env = "local"
    s.is_origin_bypass_allowed = False
    s.supremeai_public_paths = ["/api/v1/health"]
    s.allowed_hosts = []
    return s


def test_portal_role_override_admin(fake_settings):
    with pytest.MagicMock() if False else _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock(), portal_role="admin")
        assert mw.portal_role == "admin"


def test_portal_role_override_user(fake_settings):
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock(), portal_role="USER")
        assert mw.portal_role == "user"


def test_portal_role_from_settings_admin(fake_settings):
    fake_settings.service_role = "admin"
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock())
        assert mw.portal_role == "admin"


def test_portal_role_default_user(fake_settings):
    fake_settings.service_role = "unknown"
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock())
        assert mw.portal_role == "user"


def test_allowed_origins_user_defaults(fake_settings):
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock(), portal_role="user")
        origins = mw.allowed_origins
        assert USER_DEFAULT_TRUSTED_ORIGINS.issubset(origins)
        # বাংলা: Unified backend আর্কিটেকচারে ইউজার পোর্টাল অ্যাডমিন অরিজিনও ট্রাস্ট করবে
        assert ADMIN_DEFAULT_TRUSTED_ORIGINS.issubset(origins)


def test_allowed_origins_admin_defaults(fake_settings):
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock(), portal_role="admin")
        origins = mw.allowed_origins
        assert ADMIN_DEFAULT_TRUSTED_ORIGINS.issubset(origins)
        assert USER_DEFAULT_TRUSTED_ORIGINS.issubset(origins)


def test_allowed_origins_strips_wildcard(fake_settings):
    fake_settings.user_cors_origins = ["*", "https://evil.example.com"]
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock(), portal_role="user")
        origins = mw.allowed_origins
        assert "*" not in origins


def test_allowed_origins_localhost_in_dev(fake_settings):
    fake_settings.env = "local"
    fake_settings.cors_origins = ["http://localhost:3000", "http://127.0.0.1:5173"]
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock(), portal_role="user")
        origins = mw.allowed_origins
        assert "http://localhost:3000" in origins
        assert "http://127.0.0.1:5173" in origins


def test_allowed_origins_no_localhost_in_production(fake_settings):
    # বাংলা: প্রোডাকশনে স্পষ্টভাবে কনফিগ না করা localhost অটো-যোগ হবে না
    fake_settings.env = "production"
    fake_settings.cors_origins = ["https://supremeai-lac.vercel.app"]
    with _patch_settings(fake_settings):
        mw = TrustedOriginMiddleware(app=MagicMock(), portal_role="user")
        origins = mw.allowed_origins
        assert "http://localhost:3000" not in origins
        assert "https://supremeai-lac.vercel.app" in origins


def test_default_origin_constants_non_empty():
    assert len(USER_DEFAULT_TRUSTED_ORIGINS) > 0
    assert len(ADMIN_DEFAULT_TRUSTED_ORIGINS) > 0


import contextlib


@contextlib.contextmanager
def _patch_settings(fake_settings):
    """settings মডিউল অবজেক্টকে মক দিয়ে প্যাচ করে।"""
    with pytest.MonkeyPatch().context() as mp:
        mp.setattr(origin_validator, "settings", fake_settings)
        yield
