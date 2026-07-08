# 📄 ফাইল: backend/models/handoff_event.py

**প্রকার:** .py  
**সাইজ:** 1,037 বাইট  
**আপডেট:** 2026-07-08T02:25:07.954743

---

## কোড

```py
import uuid
from datetime import UTC
from datetime import datetime

from sqlalchemy import DateTime
from sqlalchemy import ForeignKey
from sqlalchemy import Integer
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped
from sqlalchemy.orm import mapped_column

from models.base import Base


class HandoffEvent(Base):
    __tablename__ = "handoff_events"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("agent_sessions.id", ondelete="CASCADE"), index=True, nullable=False)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), index=True, nullable=False)

    start_ts: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(UTC), nullable=False)
    end_ts: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    actions_taken_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)


```