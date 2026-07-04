from __future__ import annotations

from typing import Any
from loguru import logger

try:
    from database.supabase_client import db
    from brain.model_router import ModelRouter
    _DEPENDENCIES_AVAILABLE = True
except ImportError:
    _DEPENDENCIES_AVAILABLE = False


class MemoryManager:
    """
    Manages the agent's long-term memory using a vector database.
    """

    def __init__(self):
        if not _DEPENDENCIES_AVAILABLE:
            raise ImportError("MemoryManager requires Supabase client and ModelRouter.")
        self.model_router = ModelRouter()
        self.db_client = db.client
        logger.info("Initialized MemoryManager.")

    async def add_memory(self, learning: str, url: str, metadata: dict[str, Any] | None = None):
        """
        Adds a new learning to the long-term memory.
        """
        logger.info(f"Adding new memory: '{learning}' from {url}")
        # 1. Generate a real embedding for the learning text
        embedding_response = await self.model_router.get_embedding(learning)
        if not embedding_response.get("success"):
            logger.error("Failed to generate embedding for memory.")
            return
        embedding = embedding_response["embedding"]

        # 2. Store in Supabase 'agent_memories' table
        await self.db_client.table("agent_memories").insert({
            "content": learning,
            "embedding": embedding,
            "source_url": url,
            "metadata": metadata or {}
        }).execute()

    async def retrieve_relevant_memories(self, query: str, top_k: int = 3) -> list[str]:
        """
        Retrieves the most relevant memories for a given query.
        """
        logger.info(f"Retrieving memories relevant to: '{query}'")
        # 1. Generate a real embedding for the query
        embedding_response = await self.model_router.get_embedding(query)
        if not embedding_response.get("success"):
            logger.error("Failed to generate embedding for memory retrieval.")
            return []
        query_embedding = embedding_response["embedding"]

        # 2. Call a Supabase RPC function to perform vector similarity search
        result = await self.db_client.rpc('match_memories', {
            'query_embedding': query_embedding,
            'match_threshold': 0.75,
            'match_count': top_k
        }).execute()

        memories = [item['content'] for item in result.data] if result.data else []
        logger.info(f"Retrieved {len(memories)} relevant memories.")
        return memories