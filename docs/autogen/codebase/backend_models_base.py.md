# 📄 ফাইল: backend/models/base.py

**প্রকার:** .py  
**সাইজ:** 168 বাইট  
**আপডেট:** 2026-07-05T15:31:52.344332

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