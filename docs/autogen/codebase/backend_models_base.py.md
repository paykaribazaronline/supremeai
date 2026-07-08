# 📄 ফাইল: backend/models/base.py

**প্রকার:** .py  
**সাইজ:** 167 বাইট  
**আপডেট:** 2026-07-08T19:45:59.810157

---

## কোড

```py
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """
    Shared DeclarativeBase for all SQLAlchemy models in SupremeAI.
    """

    pass

```