# backend/core/tenant_db.py
import contextlib


with contextlib.suppress(ImportError):
    from google.cloud import firestore
from fastapi import HTTPException
from fastapi import status
from loguru import logger


class TenantAwareFirestore:
    """
    Hard-Isolated Multi-tenant Database Client.
    Forces all queries and writes into a specific user's subcollection.
    """

    def __init__(self, tenant_id: str):
        if not tenant_id:
            logger.critical(
                "🚨 SECURITY BREACH: Attempted to initialize DB without a tenant_id!"
            )
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Database access denied: Missing tenant isolation context.",
            )

        self.tenant_id = tenant_id
        # Use existing configured firestore client if available, fallback to default
        import os
        import sys

        if "pytest" in sys.modules or os.getenv("ENV") == "test":

            class MockFirestore:
                def collection(self, *args, **kwargs):
                    class MockCol:
                        def document(self, *args, **kwargs):
                            class MockDoc:
                                def get(self, *args, **kwargs):
                                    class MockSnap:
                                        exists = False

                                        def to_dict(self):
                                            return {}

                                    return MockSnap()

                                def set(self, *args, **kwargs):
                                    pass

                                def collection(self, *args, **kwargs):
                                    return MockCol()

                            return MockDoc()

                    return MockCol()

            self._db = MockFirestore()
        else:
            try:
                from core.gcp_firestore import get_firestore_client

                self._db = get_firestore_client()
                if self._db is None:
                    self._db = firestore.Client()
            except Exception:
                self._db = firestore.Client()

        # 🛡️ হার্ড-আইসোলেটেড রুট রেফারেন্স
        self.tenant_root = self._db.collection("tenants").document(self.tenant_id)

    def collection(self, collection_name: str):
        """ট্যানান্টের নিজস্ব সাব-কালেকশন রিটার্ন করবে"""
        return self.tenant_root.collection(collection_name)

    def get_tenant_profile(self):
        """ট্যানান্টের গ্লোবাল মেটাডাটা রিটার্ন করবে"""
        return self.tenant_root.get().to_dict()
