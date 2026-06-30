# Self-Evolution Engine models tracking autonomous code updates
# বাংলা মন্তব্য: এআই কর্তৃক জেনারেটেড নতুন স্কিল, স্বয়ংক্রিয় প্রপোজাল ট্র্যাকিং এবং ফিটনেস স্কোরিং মডেল।

import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Integer, Float, DateTime, Text, Boolean
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.dialects.postgresql import UUID, JSONB

class Base(DeclarativeBase):
    pass

class SkillFitness(Base):
    __tablename__ = "skill_fitness"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    skill_name: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    success_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    failure_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    fitness_score: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    last_run_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=True)
    
    # Optimistic Concurrency Control (OCC)
    version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc))

    __mapper_args__ = {
        "version_id_col": version  # SQLAlchemy অটোমেটিকভাবে ভার্সন ট্র্যাকিং এবং রেস-কন্ডিশন ব্লক করবে
    }

class CodeProposal(Base):
    __tablename__ = "code_proposals"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    proposal_id: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    skill_name: Mapped[str] = mapped_column(String(255), nullable=False)
    
    # Pro Tip: Text allows arbitrary code length without database truncation.
    generated_code: Mapped[str] = mapped_column(Text, nullable=False)
    ast_validated: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    ci_passed: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    status: Mapped[str] = mapped_column(String(50), default="proposed", nullable=False)  # proposed, approved, rejected, applied
    
    # Pro Tip: JSONB is highly optimized for PostgreSQL query matching.
    metadata_json: Mapped[dict] = mapped_column(JSONB, default=dict, nullable=True)
    version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

    __mapper_args__ = {
        "version_id_col": version
    }
