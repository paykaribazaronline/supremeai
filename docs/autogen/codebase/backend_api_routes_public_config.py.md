# 📄 ফাইল: backend/api/routes/public_config.py

**প্রকার:** .py  
**সাইজ:** 659 বাইট  
**আপডেট:** 2026-07-08T12:17:29.866771

---

## কোড

```py

from fastapi import APIRouter
from pydantic import BaseModel


router = APIRouter(
    prefix="/config/public",
    tags=["public_config"],
)

class PublicConfigResponse(BaseModel):
    adminEmail: str
    maxConcurrency: int
    features: dict[str, bool]

@router.get("", response_model=PublicConfigResponse)
async def get_public_config():
    # In a real database-driven app, fetch these from DB or environment securely.
    # We return safe defaults here.
    return PublicConfigResponse(
        adminEmail="admin@supremeai.dev",
        maxConcurrency=3,
        features={
            "selfHealing": True,
            "costGuard": True
        }
    )

```