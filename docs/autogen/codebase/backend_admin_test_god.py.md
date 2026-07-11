# 📄 ফাইল: backend/admin/test_god.py

**প্রকার:** .py  
**সাইজ:** 11,199 বাইট  
**আপডেট:** 2026-07-11T17:11:02.659725

---

## কোড

```py
# বাংলা মন্তব্য: অব্যবহৃত ইম্পোর্ট (os, time, MagicMock, logger) মুছে ফেলা হলো।
import asyncio
import concurrent.futures
import os
import sys
import tempfile
from unittest.mock import patch

import pytest

from backend.admin.god import AdminGodLayer


@pytest.fixture
def admin_god_layer():
    fd, path = tempfile.mkstemp()
    os.close(fd)
    layer = AdminGodLayer(db_path=path)
    yield layer
    os.remove(path)


class TestAdminGodLayer:
    def test_init_db_path(self):
        # Test initializing AdminGodLayer with db_path
        admin_god_layer = AdminGodLayer(db_path="test_db_path")
        assert admin_god_layer.collection_name == "constitutional_rules"
        assert admin_god_layer._db is None

    def test_init_db_no_path(self):
        # Test initializing AdminGodLayer without db_path
        admin_god_layer = AdminGodLayer()
        assert admin_god_layer.collection_name == "constitutional_rules"
        if "pytest" in sys.modules:
            assert admin_god_layer._db is None

    @patch("backend.admin.god.get_firestore_db")
    def test_init_db_with_firestore(self, mock_get_db):
        # Test initializing AdminGodLayer with Firestore
        mock_db = mock_get_db.return_value
        admin_god_layer = AdminGodLayer()
        mock_db.collection.assert_any_call(admin_god_layer.collection_name)

    def test_init_db_no_firestore(self):
        # Test initializing AdminGodLayer without Firestore
        with patch("backend.admin.god.get_firestore_db", return_value=None):
            admin_god_layer = AdminGodLayer()
            assert admin_god_layer._db is None

    @patch("backend.admin.god.get_firestore_db")
    def test_get_rule(self, mock_get_db):
        # Test getting a rule
        mock_db = mock_get_db.return_value
        mock_doc_ref = mock_db.collection.return_value.document.return_value
        mock_doc = mock_doc_ref.get.return_value
        mock_doc.exists = True
        mock_doc.to_dict.return_value = {"value": "test_value"}
        admin_god_layer = AdminGodLayer()
        rule = admin_god_layer.get_rule("test_key")
        assert rule == "test_value"

    @patch("backend.admin.god.get_firestore_db")
    def test_get_rule_not_found(self, mock_get_db):
        # Test getting a rule that is not found
        mock_db = mock_get_db.return_value
        mock_doc_ref = mock_db.collection.return_value.document.return_value
        mock_doc = mock_doc_ref.get.return_value
        mock_doc.exists = False
        admin_god_layer = AdminGodLayer()
        rule = admin_god_layer.get_rule("test_key")
        assert rule is None

    @patch("backend.admin.god.get_firestore_db")
    def test_get_rule_with_default(self, mock_get_db):
        # Test getting a rule with a default value
        mock_db = mock_get_db.return_value
        mock_doc_ref = mock_db.collection.return_value.document.return_value
        mock_doc = mock_doc_ref.get.return_value
        mock_doc.exists = False
        admin_god_layer = AdminGodLayer()
        rule = admin_god_layer.get_rule("test_key", default="default_value")
        assert rule == "default_value"

    @patch("backend.admin.god.get_firestore_db")
    def test_set_rule(self, mock_get_db):
        # Test setting a rule
        mock_db = mock_get_db.return_value
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("test_key", "test_value")
        mock_db.collection.assert_called_with(admin_god_layer.collection_name)

    @patch("backend.admin.god.get_firestore_db", return_value=None)
    def test_set_rule_no_firestore(self, mock_get_db):
        # Test setting a rule without Firestore
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("test_key", "test_value")
        assert admin_god_layer.get_rule("test_key") == "test_value"

    def test_is_admin_action_allowed_whitelist(self):
        # Test is_admin_action_allowed with a whitelisted action
        admin_god_layer = AdminGodLayer()
        allowed = admin_god_layer.is_admin_action_allowed("health")
        assert allowed is True

    def test_is_admin_action_allowed_not_whitelist(self):
        # Test is_admin_action_allowed with a non-whitelisted action
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("admin_authorized", "false")
        allowed = admin_god_layer.is_admin_action_allowed("not_whitelist")
        assert allowed is False

    def test_is_admin_action_allowed_admin_authorized(self):
        # Test is_admin_action_allowed with admin_authorized set to true
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("admin_authorized", "true")
        allowed = admin_god_layer.is_admin_action_allowed("not_whitelist")
        assert allowed is True

    def test_enforce_allowed(self):
        # Test enforce with an allowed action
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("admin_authorized", "true")
        try:
            admin_god_layer.enforce("not_whitelist")
        except PermissionError:
            pytest.fail("PermissionError should not be raised")

    def test_enforce_not_allowed(self):
        # Test enforce with a not allowed action
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("admin_authorized", "false")
        with pytest.raises(PermissionError):
            admin_god_layer.enforce("not_whitelist")

    @patch("backend.admin.god.get_firestore_db")
    def test_init_db_concurrent(self, mock_get_db):
        # Test initializing AdminGodLayer with Firestore concurrently
        mock_db = mock_get_db.return_value
        fd, path = tempfile.mkstemp()
        os.close(fd)
        admin_god_layer1 = AdminGodLayer(db_path=path)
        admin_god_layer2 = AdminGodLayer(db_path=path)
        with concurrent.futures.ThreadPoolExecutor() as executor:
            future1 = executor.submit(admin_god_layer1._init_db)
            future2 = executor.submit(admin_god_layer2._init_db)
            future1.result()
            future2.result()
        os.remove(path)
        mock_db.collection.assert_called_with(admin_god_layer1.collection_name)

    @patch("backend.admin.god.get_firestore_db")
    def test_get_rule_concurrent(self, mock_get_db):
        # Test getting a rule concurrently
        mock_db = mock_get_db.return_value
        fd, path = tempfile.mkstemp()
        os.close(fd)
        admin_god_layer1 = AdminGodLayer(db_path=path)
        admin_god_layer2 = AdminGodLayer(db_path=path)
        with concurrent.futures.ThreadPoolExecutor() as executor:
            future1 = executor.submit(admin_god_layer1.get_rule, "test_key")
            future2 = executor.submit(admin_god_layer2.get_rule, "test_key")
            future1.result()
            future2.result()
        os.remove(path)
        mock_db.collection.return_value.document.assert_called_with("test_key")

    @patch("backend.admin.god.get_firestore_db")
    def test_set_rule_concurrent(self, mock_get_db):
        # Test setting a rule concurrently
        mock_db = mock_get_db.return_value
        fd, path = tempfile.mkstemp()
        os.close(fd)
        admin_god_layer1 = AdminGodLayer(db_path=path)
        admin_god_layer2 = AdminGodLayer(db_path=path)
        with concurrent.futures.ThreadPoolExecutor() as executor:
            future1 = executor.submit(admin_god_layer1.set_rule, "test_key", "test_value")
            future2 = executor.submit(admin_god_layer2.set_rule, "test_key", "test_value")
            future1.result()
            future2.result()
        os.remove(path)
        mock_db.collection.return_value.document.assert_called_with("test_key")

    @pytest.mark.asyncio
    async def test_init_db_concurrent_async(self):
        # Test initializing AdminGodLayer with Firestore concurrently using asyncio
        admin_god_layer1 = AdminGodLayer()
        admin_god_layer2 = AdminGodLayer()
        await asyncio.gather(asyncio.to_thread(admin_god_layer1._init_db), asyncio.to_thread(admin_god_layer2._init_db))

    @pytest.mark.asyncio
    async def test_get_rule_concurrent_async(self):
        # Test getting a rule concurrently using asyncio
        admin_god_layer1 = AdminGodLayer()
        admin_god_layer2 = AdminGodLayer()
        await asyncio.gather(asyncio.to_thread(admin_god_layer1.get_rule, "test_key"), asyncio.to_thread(admin_god_layer2.get_rule, "test_key"))

    @pytest.mark.asyncio
    async def test_set_rule_concurrent_async(self):
        # Test setting a rule concurrently using asyncio
        admin_god_layer1 = AdminGodLayer()
        admin_god_layer2 = AdminGodLayer()
        await asyncio.gather(
            asyncio.to_thread(admin_god_layer1.set_rule, "test_key", "test_value"),
            asyncio.to_thread(admin_god_layer2.set_rule, "test_key", "test_value"),
        )

    def test_init_db_empty_db_path(self):
        # Test initializing AdminGodLayer with an empty db_path
        admin_god_layer = AdminGodLayer(db_path="")
        assert admin_god_layer.collection_name == "constitutional_rules"
        assert admin_god_layer._db is None

    def test_get_rule_empty_key(self):
        # Test getting a rule with an empty key
        fd, path = tempfile.mkstemp()
        os.close(fd)
        admin_god_layer = AdminGodLayer(db_path=path)
        rule = admin_god_layer.get_rule("")
        assert rule is None
        os.remove(path)

    def test_set_rule_empty_key(self):
        # Test setting a rule with an empty key
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("", "test_value")
        assert admin_god_layer.get_rule("") == "test_value"

    def test_set_rule_none_value(self):
        # Test setting a rule with a None value
        import sqlite3

        admin_god_layer = AdminGodLayer()
        with pytest.raises(sqlite3.IntegrityError):
            admin_god_layer.set_rule("test_key", None)

    def test_enforce_none_action(self):
        # Test enforce with a None action
        admin_god_layer = AdminGodLayer()
        with pytest.raises(PermissionError):
            admin_god_layer.enforce(None)

    def test_is_admin_action_allowed_none_action(self):
        # Test is_admin_action_allowed with a None action
        admin_god_layer = AdminGodLayer()
        allowed = admin_god_layer.is_admin_action_allowed(None)
        assert allowed is False

    def test_init_db_large_db_path(self):
        # Test initializing AdminGodLayer with a large db_path
        import sqlite3

        with pytest.raises(sqlite3.OperationalError):
            AdminGodLayer(db_path="a" * 1000)

    def test_get_rule_large_key(self):
        # Test getting a rule with a large key
        fd, path = tempfile.mkstemp()
        os.close(fd)
        admin_god_layer = AdminGodLayer(db_path=path)
        rule = admin_god_layer.get_rule("a" * 1000)
        assert rule is None
        os.remove(path)

    def test_set_rule_large_key(self):
        # Test setting a rule with a large key
        admin_god_layer = AdminGodLayer()
        admin_god_layer.set_rule("a" * 1000, "test_value")
        assert admin_god_layer.get_rule("a" * 1000) == "test_value"

```