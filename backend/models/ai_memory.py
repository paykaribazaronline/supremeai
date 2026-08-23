"""
SupremeAI AI Memory Model — pgvector Integration
v4.0: Optimized vector storage with HNSW indexing for fast similarity search

Schema:
  - id: UUID primary key
  - user_id: Owner reference
  - content: Original text content
  - embedding: Vector (1536 dims for OpenAI, 768 for local models)
  - metadata: JSONB for flexible attributes
  - created_at, updated_at: Timestamps

Indexes:
  - Primary key (id)
  - user_id B-tree index (filter by user)
  - embedding IVFFlat/HNSW vector index (similarity search)
  - created_at composite index (time-based queries)
"""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import Any

from sqlalchemy import (
    DateTime,
    ForeignKey,
    Index,
    String,
    Text,
    func,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from core.db import Base


class AIMemory(Base):
    """AI Memory entries with vector embeddings for semantic search."""
    
    __tablename__ = "ai_memory"

    # Primary key
    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
    )
    
    # Owner reference
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,  # B-tree index for user filtering
    )

    # Content
    content: Mapped[str] = mapped_column(Text, nullable=False)
    content_type: Mapped[str] = mapped_column(
        String(50),
        default="text",  # text, conversation, preference, fact
    )
    
    # Vector embedding (1536 dimensions for OpenAI ada-002)
    embedding: Mapped[list[float]] = mapped_column(
        # Note: Requires pgvector extension installed
        # In migration: CREATE EXTENSION IF NOT EXISTS vector;
        # ALTER TABLE ai_memory ADD COLUMN embedding vector(1536);
        "vector(1536)",
        nullable=True,
    )

    # Flexible metadata
    metadata_: Mapped[dict[str, Any]] = mapped_column(
        "metadata",
        JSONB,
        default=dict,
        server_default="{}",
    )

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    # Relationships
    user = relationship("User", back_populates="ai_memories")

    # -------------------------------------------------------------------------
    # INDEXES — Optimized for common query patterns
    # -------------------------------------------------------------------------
    __table_args__ = (
        # HNSW index for high-performance vector similarity search
        # Best for datasets > 100K vectors
        Index(
            "ix_ai_memory_embedding_hnsw",
            embedding,
            postgresql_using="hnsw",
            postgresql_with={"m": 16, "ef_construction": 64},
            postgresql_ops={"embedding": "vector_cosine_ops"},
        ),
        # Composite index for time-based user queries
        Index(
            "ix_ai_memory_user_created",
            user_id,
            created_at.desc(),
        ),
        # Partial index for content_type filtering
        Index(
            "ix_ai_memory_content_type",
            content_type,
        ),
    )

    def __repr__(self) -> str:
        return f"<AIMemory(id={self.id!s}, type={self.content_type})>"

    @classmethod
    async def similarity_search(
        cls,
        session,
        query_embedding: list[float],
        user_id: uuid.UUID | None = None,
        limit: int = 10,
        threshold: float = 0.7,
    ) -> list["AIMemory"]:
        """
        Perform vector similarity search.
        
        Args:
            session: Async DB session
            query_embedding: Query vector (must match dimension)
            user_id: Optional user filter
            limit: Max results
            threshold: Minimum cosine similarity (0-1)
        
        Returns:
            List of AIMemory ordered by similarity
        """
        from sqlalchemy import select
        
        # Build query with optional user filter
        query = select(cls).order_by(
            cls.embedding.cosine_distance(query_embedding)
        ).limit(limit)

        if user_id is not None:
            query = query.where(cls.user_id == user_id)

        result = await session.execute(query)
        memories = result.scalars().all()

        # Post-filter by threshold (more accurate than DB-level filter)
        import numpy as np
        filtered = []
        for mem in memories:
            if mem.embedding:
                similarity = 1 - np.cosine(query_embedding, mem.embedding)
                if similarity >= threshold:
                    filtered.append(mem)

        return filtered

    @classmethod
    async def store_memory(
        cls,
        session,
        user_id: uuid.UUID,
        content: str,
        embedding: list[float] | None = None,
        content_type: str = "text",
        metadata: dict[str, Any] | None = None,
    ) -> "AIMemory":
        """Store a new memory entry."""
        memory = cls(
            user_id=user_id,
            content=content,
            embedding=embedding,
            content_type=content_type,
            metadata_=metadata or {},
        )
        session.add(memory)
        await session.flush()
        return memory


# -----------------------------------------------------------------------------
# FILE 7: backend/api/middleware/query_timing.py — Query Performance Middleware
# -----------------------------------------------------------------------------
