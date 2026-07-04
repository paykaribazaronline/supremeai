# 📄 ফাইল: tests/test_tenant_di.py

**প্রকার:** .py  
**সাইজ:** 564 বাইট  
**আপডেট:** 2026-07-04T04:31:35.614684

---

## কোড

```py
import asyncio
from backend.core.tenant_db import TenantAwareFirestore
from backend.api.dependencies import get_tenant_db
from fastapi import HTTPException

def test_di():
    # Mocking JWT payload
    payload = {"sub": "test_tenant@supremeai.com", "role": "admin"}
    try:
        db = get_tenant_db(payload)
        print("✅ Success: TenantAwareFirestore initialized for:", db.tenant_id)
        assert db.tenant_id == "test_tenant@supremeai.com"
    except HTTPException as e:
        print("❌ Failed:", e.detail)

if __name__ == "__main__":
    test_di()

```