# 📄 ফাইল: backend/tests/test_admin_god.py

**প্রকার:** .py  
**সাইজ:** 5,725 বাইট  
**আপডেট:** 2026-07-11T13:53:46.559018

---

## কোড

```py
"""Admin God Layer tests for SupremeAI 2.0."""

import pytest

from core.admin_god import AdminGodLayer
from core.rbac import UserContext


class TestAdminGodLayer:
    """Tests for AdminGodLayer enforcement and constraint injection."""

    from unittest.mock import patch
    import os

    @patch.dict(os.environ, {"SUPREMEAI_ADMIN_PASSWORD_HASH": ""})
    def test_init_default(self):
        """ডিফল্ট ইনিশialization ঠিক আছে।"""
        layer = AdminGodLayer()
        assert layer.rules_engine is not None
        assert layer.rbac is not None
        assert layer.admin_password_hash == ""

    def test_init_with_custom_rules_engine(self):
        """কাস্টম রুলস ইঞ্জিন সহ ইনিশialization করা হচ্ছে।"""
        from core.universal_rules import UniversalRulesEngine

        custom_engine = UniversalRulesEngine()
        layer = AdminGodLayer(rules_engine=custom_engine)
        assert layer.rules_engine is custom_engine

    def test_verify_admin_no_password(self):
        """খালি পাসওয়ার্ড রিজেক্স করা হচ্ছে।"""
        layer = AdminGodLayer()
        assert layer.verify_admin("") is False
        assert layer.verify_admin(None) is False

    def test_verify_admin_no_hash(self):
        """অ্যাডমিন হ্যাশ ছাড়াই ভেরিফিকেশন ব্যর্থ হয়।"""
        layer = AdminGodLayer.__new__(AdminGodLayer)
        layer.admin_password_hash = ""
        layer.rules_engine = None
        layer.rbac = None
        assert layer.verify_admin("password") is False

    def test_enforce_no_user_context(self):
        """UserContext ছাড়াই enforce করলে ডিফল্ট ভিউয়ার রোল ব্যবহার হয়।"""
        layer = AdminGodLayer()
        ctx = UserContext(user_id="test-user", role="admin")
        result = layer.enforce("read", ctx)
        assert result["allowed"] is True
        assert result["role"] == "admin"

    def test_enforce_with_string_context(self):
        """স্ট্রিং রোল সহ UserContext তৈরি করে enforce করা হচ্ছে।"""
        layer = AdminGodLayer()
        result = layer.enforce("read", "admin")
        assert result["allowed"] is True
        assert result["role"] == "admin"

    def test_enforce_with_none_context(self):
        """None কন্টেক্সটে ডিফল্ট ভিউয়ার রোল ব্যবহার হয়।"""
        layer = AdminGodLayer()
        with pytest.raises(PermissionError):
            layer.enforce("admin", None)

    def test_enforce_permission_denied(self):
        """অনুমতি ছাড়াই enforce করলে PermissionError দেওয়া হয়।"""
        layer = AdminGodLayer()
        ctx = UserContext(user_id="test-user", role="viewer")
        with pytest.raises(PermissionError, match="Permission denied"):
            layer.enforce("admin", ctx)

    def test_enforce_rules(self):
        """এনফোর্স রুলস ফাংশন কাজ করছে।"""
        layer = AdminGodLayer()
        context = {"test": "value"}
        result = layer.enforce_rules(context)
        assert isinstance(result, dict)

    def test_inject_prompt_constraints(self):
        """প্রম্পট কনস্ট্রেন্টস ইনজেক্ট করা হচ্ছে।"""
        layer = AdminGodLayer()
        original_prompt = "You are a helpful assistant."
        result = layer.inject_prompt_constraints(original_prompt)
        assert "CONSTITUTIONAL RULES" in result
        assert original_prompt in result

    def test_inject_prompt_constraints_empty_prompt(self):
        """�ালি প্রম্পটের উপর ইনজেকশন করা হচ্ছে।"""
        layer = AdminGodLayer()
        result = layer.inject_prompt_constraints("")
        assert "CONSTITUTIONAL RULES" in result

    def test_inject_prompt_constraints_with_rules(self):
        """রুলস সহ প্রম্পট কনস্ট্রেন্টস ইনজেক্ট করা হচ্ছে।"""
        layer = AdminGodLayer()
        # Add a custom rule to test the injection
        layer.rules_engine.rules["test_rule"] = "test value"
        result = layer.inject_prompt_constraints("Original prompt")
        assert "Test Rule" in result
        assert "test value" in result


class TestRBACIntegration:
    """Tests for RBAC integration with AdminGodLayer."""

    def test_rbac_has_permission_admin(self):
        """অ্যাডমিন রোলের অনুমতি চেক করা হচ্ছে।"""
        layer = AdminGodLayer()
        ctx = UserContext(user_id="admin", role="admin")
        result = layer.enforce("admin", ctx)
        assert result["allowed"] is True

    def test_rbac_has_permission_viewer(self):
        """ভিউয়ার রোলের অনুমতি সীমিত থাকে।"""
        layer = AdminGodLayer()
        ctx = UserContext(user_id="viewer", role="viewer")
        result = layer.enforce("read", ctx)
        assert result["allowed"] is True

    def test_rbac_permission_denied_viewer_admin(self):
        """ভিউয়ার রোলের অ্যাডমিন অ্যাকশন অনুমতি নেই।"""
        layer = AdminGodLayer()
        ctx = UserContext(user_id="viewer", role="viewer")
        with pytest.raises(PermissionError):
            layer.enforce("admin", ctx)

```