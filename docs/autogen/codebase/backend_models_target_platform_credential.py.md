# 📄 ফাইল: backend/models/target_platform_credential.py

**প্রকার:** .py  
**সাইজ:** 1,679 বাইট  
**আপডেট:** 2026-07-11T13:46:44.122559

---

## কোড

```py
import enum
import uuid
from datetime import UTC
from datetime import datetime

from sqlalchemy import DateTime
from sqlalchemy import Enum
from sqlalchemy import LargeBinary
from sqlalchemy import String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped
from sqlalchemy.orm import mapped_column

from models.base import Base


class AuthType(enum.StrEnum):
    oauth2 = "oauth2"
    cookie_session = "cookie_session"
    api_key = "api_key"
    basic_auth = "basic_auth"


class CredentialStatus(enum.StrEnum):
    active = "active"
    expired = "expired"
    revoked = "revoked"
    needs_reauth = "needs_reauth"


class TargetPlatformCredential(Base):
    __tablename__ = "target_platform_credentials"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[str] = mapped_column(String(255), index=True, nullable=False)

    platform_label: Mapped[str] = mapped_column(String(255), nullable=False)

    auth_type: Mapped[AuthType] = mapped_column(Enum(AuthType, name="auth_type_enum", create_type=True), nullable=False)

    encrypted_blob: Mapped[bytes] = mapped_column(LargeBinary, nullable=False)
    kms_key_ref: Mapped[str | None] = mapped_column(String(255), nullable=True)

    status: Mapped[CredentialStatus] = mapped_column(
        Enum(CredentialStatus, name="credential_status_enum", create_type=True), nullable=False, default=CredentialStatus.active
    )

    last_used_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(UTC))

```