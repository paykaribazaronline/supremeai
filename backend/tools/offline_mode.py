import asyncio
from typing import Dict, Any, List
from loguru import logger

class OfflineModeManager:
    def __init__(self, local_model_id: str = "llama3-8b"):
        self.local_model_id = local_model_id
        self.sync_queue: List[Dict[str, Any]] = []
        logger.info(f"Initialized OfflineModeManager with local model {self.local_model_id}")

    async def execute_task(self, prompt: str) -> Dict[str, Any]:
        logger.info(f"Executing task offline: {prompt}")
        await asyncio.sleep(0.3)
        local_response = f"[Offline] Processed by {self.local_model_id}: Here is the local result for '{prompt}'."
        self.sync_queue.append({
            "action": "offline_execution",
            "prompt": prompt,
            "result": local_response,
        })
        return {
            "status": "success",
            "source": "local",
            "result": local_response,
            "queued_for_sync": True,
        }

    async def sync_with_cloud(self) -> Dict[str, Any]:
        if not self.sync_queue:
            return {"status": "success", "synced": 0}
        logger.info(f"Syncing {len(self.sync_queue)} offline actions to the cloud...")
        await asyncio.sleep(0.2)
        count = len(self.sync_queue)
        self.sync_queue.clear()
        return {"status": "success", "synced": count}
            
        logger.info(f"Syncing {len(self.sync_queue)} offline actions to the cloud...")
        # Mock sync to Supabase/Firebase
        await asyncio.sleep(0.5)
        self.sync_queue.clear()
        return True
