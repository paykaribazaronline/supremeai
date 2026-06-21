"""
Cloud-native vector store using Pinecone or Qdrant.
Replaces local ChromaDB for production.
"""
import os
from typing import Dict, Any, List, Optional
from loguru import logger

class CloudVectorStore:
    """
    Production vector database using Pinecone or Qdrant Cloud.
    """

    def __init__(self, provider: str = "pinecone"):
        self.provider = provider
        self.client = None
        self.index = None
        self._init_client()

    def _init_client(self):
        if self.provider == "pinecone":
            from pinecone import Pinecone
            api_key = os.getenv("PINECONE_API_KEY")
            if api_key:
                self.client = Pinecone(api_key=api_key)
                index_name = os.getenv("PINECONE_INDEX", "supremeai-knowledge")
                self.index = self.client.Index(index_name)
                logger.info(f"Pinecone index '{index_name}' connected")
        elif self.provider == "qdrant":
            from qdrant_client import QdrantClient
            url = os.getenv("QDRANT_URL")
            api_key = os.getenv("QDRANT_API_KEY")
            if url:
                self.client = QdrantClient(url=url, api_key=api_key)
                logger.info("Qdrant client connected")

    def upsert(self, vectors: List[Dict[str, Any]], namespace: str = "default"):
        """Upsert vectors to cloud store."""
        if not self.index:
            logger.warning("Vector store not initialized")
            return False

        try:
            if self.provider == "pinecone":
                self.index.upsert(vectors=vectors, namespace=namespace)
            return True
        except Exception as e:
            logger.error(f"Vector upsert failed: {e}")
            return False

    def delete(self, ids: List[str], namespace: str = "default") -> bool:
        """Deletes vectors from cloud store by their unique IDs to prevent RAG desync and phantom memory."""
        if not self.client and not self.index:
            logger.warning("Vector store not initialized")
            return False

        try:
            if self.provider == "pinecone" and self.index:
                self.index.delete(ids=ids, namespace=namespace)
                logger.info(f"Deleted vectors {ids} from Pinecone namespace {namespace}")
                return True
            elif self.provider == "qdrant" and self.client:
                from qdrant_client.http.models import PointIdsList
                collection_name = os.getenv("QDRANT_COLLECTION", "supremeai-knowledge")
                self.client.delete(
                    collection_name=collection_name,
                    points_selector=PointIdsList(points=ids)
                )
                logger.info(f"Deleted points {ids} from Qdrant collection {collection_name}")
                return True
            return False
        except Exception as e:
            logger.error(f"Vector delete failed: {e}")
            return False

    def query(self, vector: List[float], top_k: int = 5, namespace: str = "default") -> List[Dict]:
        """Query similar vectors."""
        if not self.index:
            return []

        try:
            if self.provider == "pinecone":
                result = self.index.query(
                    vector=vector,
                    top_k=top_k,
                    namespace=namespace,
                    include_metadata=True
                )
                return result.matches
        except Exception as e:
            logger.error(f"Vector query failed: {e}")
            return []

        return []

# Keep ChromaDB fallback for local dev
class ChromaDBStore:
    """Local ChromaDB for development only."""
    def __init__(self, persist_dir: str = "data/frontier/chroma"):
        import chromadb
        self.client = chromadb.PersistentClient(path=persist_dir)
        # ... existing ChromaDB implementation ...
