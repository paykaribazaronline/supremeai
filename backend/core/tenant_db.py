# backend/core/tenant_db.py
from google.cloud import firestore
from fastapi import HTTPException, status
from loguru import logger

class TenantAwareFirestore:
    """
    Hard-Isolated Multi-tenant Database Client.
    Forces all queries and writes into a specific user's subcollection.
    """
    def __init__(self, tenant_id: str):
        if not tenant_id:
            logger.critical("🚨 SECURITY BREACH: Attempted to initialize DB without a tenant_id!")
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, 
                detail="Database access denied: Missing tenant isolation context."
            )
        
        self.tenant_id = tenant_id
        # Use existing configured firestore client if available, fallback to default
        try:
            from core.gcp_firestore import get_firestore_client
            self._db = get_firestore_client()
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
