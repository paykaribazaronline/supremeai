# 📄 ফাইল: backend/memory/long_term_memory.py

**প্রকার:** .py  
**সাইজ:** 4,273 বাইট  
**আপডেট:** 2026-07-11T13:38:55.690148

---

## কোড

```py
from __future__ import annotations

from typing import Any

from loguru import logger


try:
    from brain.model_router import ModelRouter
    from database.supabase_client import db

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
        await (
            self.db_client.table("agent_memories")
            .insert({"content": learning, "embedding": embedding, "source_url": url, "metadata": metadata or {}})
            .execute()
        )

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
        result = await self.db_client.rpc(
            "match_memories", {"query_embedding": query_embedding, "match_threshold": 0.75, "match_count": top_k}
        ).execute()

        memories = [item["content"] for item in result.data] if result.data else []
        logger.info(f"Retrieved {len(memories)} relevant memories.")
        return memories


class LongTermMemory:
    def __init__(self, db_path: str = ":memory:", session_id: str = "default"):
        self.memory_manager = MemoryManager()
        self.session_id = session_id
        self._facts: list[dict[str, Any]] = []
        self._summaries: list[dict[str, Any]] = []

    def remember_fact(self, content: str, category: str = "general", importance: float = 0.5, source: str = "unknown") -> dict[str, Any]:
        """Store a simple fact in memory for later recall and context building."""
        fact = {
            "content": content,
            "category": category,
            "importance": importance,
            "source": source,
        }
        self._facts.append(fact)
        return fact

    def recall_facts(self, category: str | None = None) -> list[dict[str, Any]]:
        """Return stored facts, optionally filtered by category."""
        if category is None:
            return list(self._facts)
        return [fact for fact in self._facts if fact.get("category") == category]

    def save_summary(self, content: str, turn_count: int = 1) -> dict[str, Any]:
        """Store a summary entry used by context generation."""
        summary = {"content": content, "turn_count": turn_count}
        self._summaries.append(summary)
        return summary

    def build_context(self) -> str:
        """Build a simple human-readable context string from stored facts and summaries."""
        parts: list[str] = []
        if self._summaries:
            parts.append("Summary: " + "; ".join(item["content"] for item in self._summaries))
        if self._facts:
            parts.append("Facts: " + "; ".join(item["content"] for item in self._facts))
        return "\n".join(parts) if parts else "No memory available."

```