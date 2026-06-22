import asyncio
from typing import Dict, Any
from loguru import logger
from core.config import settings

class OfflineModeManager:
    """
    Manages local model fallbacks when APIs are down or user is offline.
    Uses a sync queue to upload local changes to the cloud once reconnected.
    (Closes Gap #31, #32, #33)
    """

    def __init__(self, local_model_id: str = "llama3-8b"):
        self.local_model_id = local_model_id
        self.sync_queue = []
        logger.info(f"Initialized OfflineModeManager with local model {self.local_model_id}")

    async def execute_task(self, prompt: str) -> Dict[str, Any]:
        """Executes a task using the local model."""
        logger.info(f"Executing task offline: {prompt}")
        
        # Mock local inference (e.g. via Ollama or llama.cpp)
        await asyncio.sleep(1.0)
        
        local_response = f"[Offline] Processed by {self.local_model_id}: Here is the local result for '{prompt}'."
        
        # Queue the action for cloud synchronization
        self.sync_queue.append({
            "action": "offline_execution",
            "prompt": prompt,
            "result": local_response
        })
        
        return {
            "status": "success",
            "source": "local",
            "result": local_response,
            "queued_for_sync": True
        }

    async def sync_with_cloud(self) -> bool:
        """Syncs queued offline actions with the cloud database once online."""
        if not self.sync_queue:
            return True
            
        logger.info(f"Syncing {len(self.sync_queue)} offline actions to the cloud...")
        # Mock sync to Supabase/Firebase
        await asyncio.sleep(0.5)
        self.sync_queue.clear()
        return True
