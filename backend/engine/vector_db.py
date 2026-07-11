# FILE_PATH: engine/vector_db.py
import asyncio
import logging
import os
import uuid
from typing import Any


logger = logging.getLogger(__name__)

_pinecone_available = False
try:
    from pinecone import Pinecone
    from pinecone import ServerlessSpec
    _pinecone_available = True
except ImportError:
    logger.warning("Pinecone library not found. VectorDB functionality will be disabled.")

    class MockPineconeIndex:
        def __init__(self, name: str):
            self.name = name

        def upsert(self, vectors: list[tuple[str, list[float], dict]]):
            logger.warning(f"Attempted to upsert to mock Pinecone index '{self.name}'. No operation performed.")

        def query(self, vector: list[float], top_k: int, include_metadata: bool = False):
            logger.warning(f"Attempted to query mock Pinecone index '{self.name}'. Returning empty matches.")
            return {"matches": []}

    class MockPinecone:
        def __init__(self, api_key: str):
            pass

        def list_indexes(self):
            logger.warning("Attempted to list indexes from mock Pinecone client. Returning empty list.")
            return []

        def create_index(self, name: str, dimension: int, metric: str, spec: Any):
            logger.warning(f"Attempted to create index '{name}' using mock Pinecone client. No operation performed.")

        def Index(self, name: str):
            return MockPineconeIndex(name)

    class MockServerlessSpec:
        def __init__(self, cloud: str, region: str):
            pass

    Pinecone = MockPinecone
    ServerlessSpec = MockServerlessSpec


class VectorDatabaseClient:
    """
    Manages long-term Neural Memory using Pinecone Vector DB.
    Allows agents to recall past solutions using RAG architecture.
    """

    def __init__(self, index_name: str = "supreme-memory"):
        self.index_name = index_name
        self.pc: Any | None = None
        self.index: Any | None = None

        api_key = os.getenv("PINECONE_API_KEY", "dummy_key_for_dev")
        try:
            self.pc = Pinecone(api_key=api_key)
            
            if _pinecone_available:
                self._ensure_index()
            
            self.index = self.pc.Index(self.index_name)

        except Exception as e:  # noqa: BLE001
            logger.warning(f"Pinecone client initialization skipped or failed: {str(e)}. VectorDB functionality disabled.")
            self.pc = None
            self.index = None

    def _ensure_index(self):
        if not _pinecone_available or self.pc is None:
            return
        
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
