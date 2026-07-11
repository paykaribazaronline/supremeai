# 📄 ফাইল: backend/scripts/benchmark/load_test_phase3.py

**প্রকার:** .py  
**সাইজ:** 3,563 বাইট  
**আপডেট:** 2026-07-11T16:17:51.570039

---

## কোড

```py
import asyncio
import sys
import time
from unittest.mock import AsyncMock
from unittest.mock import patch

from loguru import logger

from core.cloud_sandbox_orchestrator import CloudSandboxOrchestrator
from core.llm_gateway import llm_gateway
from utils.firestore_helpers import get_firestore_db


logger.remove()
logger.add(sys.stdout, level="INFO")


async def simulate_request(tenant_id: str, request_id: int):
    try:
        await llm_gateway.acompletion(prompt=f"Test prompt {request_id}", model="openai/gpt-3.5-turbo", tenant_id=tenant_id)
        return "success"
    except Exception as e:  # noqa: BLE001
        if "402 Payment Required" in str(e):
            return "402"
        return "error"


async def main():
    print("Starting Phase 3 Load Test (1,000 Transactions)")  # noqa: T201
    tenant_id = "tenant-load-test"
    db = get_firestore_db()

    # Pre-configure mock DB if needed
    if db:
        budget_ref = db.collection("tenants").document(tenant_id).collection("budget").document("current")
        await budget_ref.set({"monthly_limit": 100.0, "spent_amount": 0.0})

    # Mock LiteLLM so we don't make real API calls
    with patch("litellm.acompletion", new_callable=AsyncMock) as mock_litellm:
        # Simulate 1% failure rate for SelfHealer testing
        def mock_acompletion_side_effect(*args, **kwargs):
            import random

            if random.random() < 0.01:
                raise Exception("Simulated LiteLLM Error for SelfHealer")
            return AsyncMock()

        mock_litellm.side_effect = mock_acompletion_side_effect

        start_time = time.perf_counter()

        tasks = [simulate_request(tenant_id, i) for i in range(1000)]
        results = await asyncio.gather(*tasks)

        elapsed = time.perf_counter() - start_time

        successes = results.count("success")
        payment_required = results.count("402")
        errors = results.count("error")

        print("\n=== Load Test Results ===")  # noqa: T201
        print("Total Requests: 1000")  # noqa: T201
        print(f"Success: {successes}")  # noqa: T201
        print(f"402 Payment Required (False Positives?): {payment_required}")  # noqa: T201
        print(f"Other Errors (Triggered SelfHealer): {errors}")  # noqa: T201
        print(f"Total Time: {elapsed:.2f} seconds")  # noqa: T201
        print(f"Latency: {(elapsed / 1000) * 1000:.2f} ms / request (avg concurrency)")  # noqa: T201
        print(f"RPS: {1000 / elapsed:.2f} req/s")  # noqa: T201

        # Test Sandbox TTL
        print("\n=== Testing Sandbox Auto-Destroy ===")  # noqa: T201
        orchestrator = CloudSandboxOrchestrator(provider="runpod")
        sandbox_id = "load-test-sandbox-1"
        orchestrator._active_sandboxes[sandbox_id] = {
            "created_at": time.time() - 700,  # 11.6 minutes ago (exceeds 10m TTL)
            "status": "running",
        }

        print(f"Injected sandbox {sandbox_id} with age 11.6 minutes.")  # noqa: T201
        print("Starting auto_destroy_worker for 1 iteration (mocked sleep to exit)...")  # noqa: T201

        with patch("asyncio.sleep", AsyncMock(side_effect=Exception("Exit Loop"))):
            try:
                await orchestrator.auto_destroy_worker(tenant_id)
            except Exception as e:  # noqa: BLE001
                if str(e) == "Exit Loop":
                    pass

        remaining = len(orchestrator._active_sandboxes)
        print(f"Remaining sandboxes after cleanup: {remaining} (Expected 0)")  # noqa: T201


if __name__ == "__main__":
    asyncio.run(main())

```