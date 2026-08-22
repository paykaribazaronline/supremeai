import asyncio
import logging

from core.messaging.nats_messaging import nats_client
from core.utils.background_tasks import track_task
from engine.worker_node import SwarmWorkerNode
from engine.worker_registry import worker_registry

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def main():
    logger.info("--- Starting Dynamic Topology Discovery Test ---")

    # 1. Connect NATS globally
    await nats_client.connect()

    # 2. Start the Control Plane Registry Watcher
    logger.info("Starting Control Plane Worker Registry...")
    registry_task = track_task(asyncio.create_task(worker_registry.watch_registry()))
    _ = registry_task  # kept alive via track_task registry; referenced to satisfy linters

    # Give it a second to initialize
    await asyncio.sleep(2)

    # 3. Spin up an Edge Worker Node
    logger.info("Starting Mock Edge Worker [Architect]...")
    worker = SwarmWorkerNode(agent_type="Architect")
    worker_task = track_task(asyncio.create_task(worker.start()))
    _ = worker_task  # kept alive via track_task registry; referenced to satisfy linters

    # 4. Wait for Control Plane to discover it via JetStream KV
    logger.info("Waiting for Discovery...")
    for i in range(5):
        await asyncio.sleep(5)
        architects = worker_registry.get_workers_by_type("Architect")
        if architects:
            logger.info(f"✅ Success! Control Plane discovered workers: {[w['worker_id'] for w in architects]}")
            logger.info(f"Capabilities: {architects[0]}")
            break
        else:
            logger.info(f"...still waiting ({i + 1}/5)...")

    # Cleanup
    worker.is_running = False
    worker_registry.is_running = False
    logger.info("Test completed.")


if __name__ == "__main__":
    asyncio.run(main())
