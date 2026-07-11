# 📄 ফাইল: backend/engine/vector_db.py

**প্রকার:** .py  
**সাইজ:** 2,477 বাইট  
**আপডেট:** 2026-07-11T13:51:38.393493

---

## কোড

```py
import asyncio
import logging
import os
import uuid
from typing import Any

from pinecone import Pinecone
from pinecone import ServerlessSpec


logger = logging.getLogger(__name__)


class VectorDatabaseClient:
    """
    Manages long-term Neural Memory using Pinecone Vector DB.
    Allows agents to recall past solutions using RAG architecture.
    """

    def __init__(self, index_name: str = "supreme-memory"):
        api_key = os.getenv("PINECONE_API_KEY", "dummy_key_for_dev")
        self.pc = Pinecone(api_key=api_key)
        self.index_name = index_name

        # In a real environment, this blocks. Best to call async initialization
        # or handle exceptions gracefully if the key is dummy.
        try:
            self._ensure_index()
            self.index = self.pc.Index(self.index_name)
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Pinecone init skipped (Missing API Key or Connection Error): {str(e)}")
            self.index = None

    def _ensure_index(self):
        indexes = [idx.name for idx in self.pc.list_indexes()]
        if self.index_name not in indexes:
            logger.info(f"Creating Pinecone index: {self.index_name} (Dim: 1536)")
            self.pc.create_index(
                name=self.index_name,
                dimension=1536,  # OpenAI text-embedding-3-small dimension
                metric="cosine",
                spec=ServerlessSpec(cloud="aws", region="us-east-1"),
            )

    async def save_experience(self, vector: list[float], metadata: dict[str, Any]):
        """Saves a new code fix or logic insight into Pinecone."""
        doc_id = metadata.get("patch_id", f"exp_{uuid.uuid4().hex[:8]}")

        def _upsert():
            if self.index:
                self.index.upsert(vectors=[(doc_id, vector, metadata)])

        # Using asyncio.to_thread to prevent blocking the async event loop
        await asyncio.to_thread(_upsert)
        logger.debug(f"🧠 Saved neural memory experience: {doc_id}")

    async def find_similar_experiences(self, vector: list[float], top_k: int = 3):
        """Retrieves relevant past experiences for RAG."""

        def _query():
            if self.index:
                return self.index.query(vector=vector, top_k=top_k, include_metadata=True)
            return {"matches": []}

        results = await asyncio.to_thread(_query)
        return results.get("matches", [])


# Global instance
vector_db = VectorDatabaseClient()

```