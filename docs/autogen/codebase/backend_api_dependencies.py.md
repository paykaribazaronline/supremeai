# 📄 ফাইল: backend/api/dependencies.py

**প্রকার:** .py  
**সাইজ:** 1,621 বাইট  
**আপডেট:** 2026-07-07T13:28:54.142440

---

## কোড

```py
# backend/api/dependencies.py
from fastapi import Depends
from fastapi import HTTPException
from fastapi import Request
from loguru import logger

from core.security import verify_token
from core.tenant_db import TenantAwareFirestore

# শেয়ার্ড ইউটিলিটি — টেস্ট এনভায়রনমেন্ট চেক কেন্দ্রীভূত
from utils.environment import is_test_environment


def get_current_user_token(request: Request) -> dict:
    # রিফ্যাক্টর: লোকাল is_test চেকের বদলে শেয়ার্ড ইউটিলিটি ব্যবহার
    if is_test_environment():
        return {"sub": "admin@supremeai.com", "role": "admin"}

    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    token = auth_header.split(" ")[1]
    return verify_token(token)


def get_tenant_db(
    payload: dict = Depends(get_current_user_token),
) -> TenantAwareFirestore:
    """
    Dependency Injection: Extracts tenant_id (user email/uid) from JWT
    and returns a hard-isolated Firestore client.
    """
    tenant_id = payload.get("sub")
    if not tenant_id:
        logger.error("Token payload missing 'sub' (tenant_id) claim.")
        raise HTTPException(status_code=401, detail="Invalid token structure.")

    # রিটার্ন করছে আইসোলেটেড ডিবি ক্লায়েন্ট
    return TenantAwareFirestore(tenant_id=tenant_id)

```