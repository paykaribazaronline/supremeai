# 📄 ফাইল: backend/engine/worker_registry.py

**প্রকার:** .py  
**সাইজ:** 3,102 বাইট  
**আপডেট:** 2026-07-11T15:50:11.309997

---

## কোড

```py
import asyncio
import logging
from datetime import UTC
from datetime import datetime

from core.nats_messaging import nats_client


logger = logging.getLogger(__name__)


class WorkerRegistry:
    """
    Control Plane service that monitors active workers via NATS JetStream KV store.
    Provides Smart Routing capabilities.
    """

    def __init__(self):
        self.active_workers: dict[str, dict] = {}
        self.is_running = True

    async def watch_registry(self):
        """Watches the KV store for new/updated workers (Heartbeat Monitor)."""
        await nats_client.connect()
        if not nats_client.kv_store:
            logger.error("NATS KV store not initialized. Cannot watch registry.")
            return

        logger.info("🔭 WorkerRegistry is now watching for active edge nodes...")

        while self.is_running:
            try:
                workers = await nats_client.get_all_workers()
                current_time = datetime.now(UTC)

                valid_workers = {}
                for worker_id, data in workers.items():
                    # Check if worker is stale (no heartbeat in 15 seconds)
                    try:
                        last_heartbeat = datetime.fromisoformat(data["last_heartbeat"])
                        delta = (current_time - last_heartbeat).total_seconds()

                        if delta < 15:
                            if worker_id not in self.active_workers:
                                logger.info(f"🟢 New Worker Discovered: {worker_id} [{data.get('agent_type')}]")
                            valid_workers[worker_id] = data
                        else:
                            if worker_id in self.active_workers:
                                logger.warning(f"⚠️ Worker {worker_id} is stale and has been removed from active registry.")
                            # Delete stale record from KV
                            await nats_client.kv_store.delete(worker_id)
                    except Exception:  # noqa: BLE001
                        pass

                self.active_workers = valid_workers
            except Exception as e:  # noqa: BLE001
                logger.error(f"Error reading worker registry: {str(e)}")

            await asyncio.sleep(5)

    def get_workers_by_type(self, agent_type: str) -> list[dict]:
        """Returns active workers matching the requested type."""
        return [w for w in self.active_workers.values() if w.get("agent_type") == agent_type]

    def get_smart_route(self, agent_type: str, requires_gpu: bool = False) -> str | None:
        """
        Implements Smart Routing:
        Finds the best available worker for the task.
        """
        candidates = self.get_workers_by_type(agent_type)
        if requires_gpu:
            candidates = [c for c in candidates if c.get("gpu_available")]

        if not candidates:
            return None

        # For simplicity, returning the first available worker. Can be expanded to Round-Robin or Load-based.
        return candidates[0]["worker_id"]


worker_registry = WorkerRegistry()

```