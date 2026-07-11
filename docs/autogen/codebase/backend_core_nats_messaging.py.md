# 📄 ফাইল: backend/core/nats_messaging.py

**প্রকার:** .py  
**সাইজ:** 4,101 বাইট  
**আপডেট:** 2026-07-11T11:05:10.188926

---

## কোড

```py
import json
import logging
from collections.abc import Callable
from typing import Any

import nats
from nats.errors import NoServersError
from nats.js.errors import KeyValueError
from pydantic import BaseModel


logger = logging.getLogger(__name__)


class NATSClient:
    """
    Centralized NATS Client for Distributed Swarm Edge Execution.
    Supports JetStream for Key-Value store (Worker Registry) and persistent queues.
    """

    def __init__(self, url: str = "nats://localhost:4222", token: str | None = "super_secret_token"):
        self.url = url
        self.token = token
        self.nc = None
        self.js = None
        self.kv_store = None

    async def connect(self):
        """Establishes connection to NATS with Token Auth and enables JetStream."""
        try:
            connect_kwargs = {"servers": [self.url]}
            if self.token:
                connect_kwargs["token"] = self.token

            self.nc = await nats.connect(**connect_kwargs)
            self.js = self.nc.jetstream()

            logger.info("✅ Connected to NATS Broker successfully.")

            # Initialize or bind to the Key-Value store for Worker Registry
            try:
                self.kv_store = await self.js.key_value("WORKER_REGISTRY")
            except Exception:  # noqa: BLE001
                # Create the bucket if it doesn't exist
                self.kv_store = await self.js.create_key_value(bucket="WORKER_REGISTRY")
                logger.info("🛠️ Created JetStream Key-Value bucket: WORKER_REGISTRY")

        except NoServersError:
            logger.error("❌ Failed to connect to NATS Broker. Is it running?")
        except Exception as e:  # noqa: BLE001
            logger.error(f"❌ NATS Connection Error: {str(e)}")

    async def publish_event(self, subject: str, data: BaseModel | dict[str, Any]):
        """Publishes an event to a specific NATS subject."""
        if not self.nc:
            logger.warning("NATS client is not connected.")
            return

        payload = data.model_dump() if isinstance(data, BaseModel) else data
        await self.nc.publish(subject, json.dumps(payload).encode())

    async def subscribe(self, subject: str, callback: Callable):
        """Subscribes to a subject and triggers the callback asynchronously."""
        if not self.nc:
            logger.warning("NATS client is not connected.")
            return

        async def message_handler(msg):
            try:
                decoded_data = json.loads(msg.data.decode())
                await callback(decoded_data)
            except Exception as e:  # noqa: BLE001
                logger.error(f"Error handling NATS message on {subject}: {str(e)}")

        await self.nc.subscribe(subject, cb=message_handler)

    # ---------------------------------------------------------
    # Worker Registry Methods (JetStream Key-Value Store)
    # ---------------------------------------------------------

    async def register_worker(self, worker_id: str, payload: dict):
        """Registers or updates a worker's heartbeat in the KV store."""
        if self.kv_store:
            await self.kv_store.put(worker_id, json.dumps(payload).encode())

    async def get_worker(self, worker_id: str) -> dict | None:
        """Retrieves worker info from the KV store."""
        if self.kv_store:
            try:
                entry = await self.kv_store.get(worker_id)
                return json.loads(entry.value.decode())
            except KeyValueError:
                return None
        return None

    async def get_all_workers(self) -> dict[str, dict]:
        """Retrieves all registered workers from the KV store."""
        workers = {}
        if self.kv_store:
            try:
                keys = await self.kv_store.keys()
                for key in keys:
                    entry = await self.kv_store.get(key)
                    workers[key] = json.loads(entry.value.decode())
            except Exception:  # noqa: BLE001
                pass
        return workers


# Global instance
nats_client = NATSClient()

```