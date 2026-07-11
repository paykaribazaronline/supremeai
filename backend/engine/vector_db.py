# FILE_PATH: engine/vector_db.py
import asyncio
import logging
import os
import uuid
from typing import Any


logger = logging.getLogger(__name__)

# Flag to indicate if the pinecone library is successfully imported
PINECONE_AVAILABLE = False
try:
    from pinecone import Pinecone
    from pinecone import ServerlessSpec
    PINECONE_AVAILABLE = True
except ImportError:
    logger.warning("Pinecone library not found. Vector database functionality will be disabled.")

    # Define mock classes to prevent NameError if VectorDatabaseClient is instantiated
    # but the pinecone library is not available.
    class Pinecone:
        def __init__(self, *args, **kwargs):
            logger.debug("Mock Pinecone client instantiated.")
        def list_indexes(self):
            logger.debug("Mock Pinecone list_indexes called, returning empty.")
            return []
        def create_index(self, *args, **kwargs):
            logger.debug(f"Mock Pinecone create_index called for {kwargs.get('name')}, doing nothing.")
        def Index(self, index_name: str):
            logger.debug(f"Mock Pinecone Index '{index_name}' accessed, returning mock index.")
            return MockPineconeIndex(index_name)

    class ServerlessSpec:
        def __init__(self, *args, **kwargs):
            logger.debug("Mock ServerlessSpec instantiated.")

    class MockPineconeIndex:
        def __init__(self, index_name: str):
            self.index_name = index_name
            logger.debug(f"Mock Pinecone Index '{index_name}' created.")
        def upsert(self, vectors: list):
            logger.debug(f"Mock Pinecone Index '{self.index_name}' upserted {len(vectors)} vectors, doing nothing.")
        def query(self, vector: list[float], top_k: int, include_metadata: bool):
            logger.debug(f"Mock Pinecone Index '{self.index_name}' queried with top_k={top_k}, returning empty.")
            return {"matches": []}


class VectorDatabaseClient:
    """
    Manages long-term Neural Memory using Pinecone Vector DB.
    Allows agents to recall past solutions using RAG architecture.
    """

    def __init__(self, index_name: str = "supreme-memory"):
        api_key = os.getenv("PINECONE_API_KEY", "dummy_key_for_dev")
        self.pc = None
        self.index = None
        self.index_name = index_name

        if PINECONE_AVAILABLE:
            self.pc = Pinecone(api_key=api_key)
            # In a real environment, this blocks. Best to call async initialization
            # or handle exceptions gracefully if the key is dummy.
            try:
                self._ensure_index()
                self.index = self.pc.Index(self.index_name)
            except Exception as e:  # noqa: BLE001
                logger.warning(f"Pinecone init skipped (Missing API Key or Connection Error): {str(e)}")
        else:
            logger.warning("Pinecone library not available. VectorDatabaseClient will operate in a disabled state.")

    def _ensure_index(self):
        # Only attempt to ensure index if Pinecone client was successfully initialized
        if self.pc:
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

        if not self.index:
            logger.debug(f"Pinecone index not available, skipped saving experience: {doc_id}")
            return

        def _upsert():
            self.index.upsert(vectors=[(doc_id, vector, metadata)])

        # Using asyncio.to_thread to prevent blocking the async event loop
        await asyncio.to_thread(_upsert)
        logger.debug(f"🧠 Saved neural memory experience: {doc_id}")

    async def find_similar_experiences(self, vector: list[float], top_k: int = 3):
        """Retrieves relevant past experiences for RAG."""

        if not self.index:
            logger.debug("Pinecone index not available, returning empty matches for similar experiences.")
            return {"matches": []}

        def _query():
            return self.index.query(vector=vector, top_k=top_k, include_metadata=True)

        results = await asyncio.to_thread(_query)
        return results.get("matches", [])


# Global instance
vector_db = VectorDatabaseClient()
