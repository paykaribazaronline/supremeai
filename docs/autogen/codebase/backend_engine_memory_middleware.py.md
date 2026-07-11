# 📄 ফাইল: backend/engine/memory_middleware.py

**প্রকার:** .py  
**সাইজ:** 1,598 বাইট  
**আপডেট:** 2026-07-11T14:41:19.332084

---

## কোড

```py
import logging

from engine.embedding import embedding_service
from engine.vector_db import vector_db


logger = logging.getLogger(__name__)


class MemoryMiddleware:
    """
    Injects Neural Memory (RAG) into Swarm Tasks by retrieving past experiences.
    Allows agents to learn from historical data.
    """

    def __init__(self):
        self.vector_db = vector_db
        self.embedder = embedding_service

    async def augment_task(self, task_prompt: str) -> str:
        try:
            logger.info("🧠 MemoryMiddleware: Fetching relevant past experiences...")
            # 1. Convert task to embedding
            vector = await self.embedder.generate_embedding(task_prompt)

            # 2. Query past experiences
            experiences = await self.vector_db.find_similar_experiences(vector, top_k=2)

            if not experiences:
                logger.info("🧠 MemoryMiddleware: No relevant past experiences found.")
                return task_prompt

            # 3. Add context
            logger.info(f"🧠 MemoryMiddleware: Found {len(experiences)} relevant memory chunks. Augmenting prompt.")
            memory_context = "\n".join([f"- Past insight: {exp['metadata'].get('solution', 'Unknown')}" for exp in experiences])
            return f"{task_prompt}\n\n--- RELEVANT PAST EXPERIENCE ---\n{memory_context}\n--------------------------------"
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to augment task with memory: {str(e)}")
            return task_prompt  # Fallback to original prompt


memory_mw = MemoryMiddleware()

```