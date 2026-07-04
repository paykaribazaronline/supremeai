# 📄 ফাইল: backend/models/base.py

**প্রকার:** .py  
**সাইজ:** 365 বাইট  
**আপডেট:** 2026-07-04T23:47:17.578222

---

## কোড

```py
import uuid
from datetime import UTC
from datetime import datetime
from typing import Any

from sqlalchemy import DateTime
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy.orm import Mapped
from sqlalchemy.orm import mapped_column


class Base(DeclarativeBase):
    """
    Shared DeclarativeBase for all SQLAlchemy models in SupremeAI.
    """
    pass


```