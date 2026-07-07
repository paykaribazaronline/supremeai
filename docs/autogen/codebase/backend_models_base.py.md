# 📄 ফাইল: backend/models/base.py

**প্রকার:** .py  
**সাইজ:** 168 বাইট  
**আপডেট:** 2026-07-07T11:31:19.181507

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