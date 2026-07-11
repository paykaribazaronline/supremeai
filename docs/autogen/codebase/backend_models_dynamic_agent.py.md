# 📄 ফাইল: backend/models/dynamic_agent.py

**প্রকার:** .py  
**সাইজ:** 1,133 বাইট  
**আপডেট:** 2026-07-11T13:13:34.458490

---

## কোড

```py
from sqlalchemy import JSON
from sqlalchemy import Boolean
from sqlalchemy import Column
from sqlalchemy import DateTime
from sqlalchemy import Integer
from sqlalchemy import String
from sqlalchemy import func

from models.base import Base


class DynamicAgent(Base):
    """
    ডাইনামিক এজেন্ট রেজিস্ট্রি মডেল।
    এআই দ্বারা জেনারেট করা ফ্রি লোকাল এজেন্টগুলোর কনফিগারেশন আজীবনের জন্য এখানে সেভ করা থাকবে।
    """

    __tablename__ = "dynamic_agents"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), unique=True, index=True, nullable=False)
    description = Column(String(500), nullable=True)
    execution_steps = Column(JSON, nullable=False)  # প্লেরাইট স্ক্রিপ্ট বা কনফিগারেশন স্টেপস
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

```