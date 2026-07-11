# 📄 ফাইল: backend/tools/cloud_sandbox_orchestrator.py

**প্রকার:** .py  
**সাইজ:** 5,712 বাইট  
**আপডেট:** 2026-07-11T14:23:58.646052

---

## কোড

```py
#!/usr/bin/env python3
"""
Cloud Sandbox Orchestrator
==========================

Manages ephemeral but persistent cloud environments (VMs/pods) for complex,
long-running AI tasks.
Integrates 'Freebuff CLI' as a zero-cost headless AI worker.
"""

from loguru import logger
from typing import Dict, Any

import os
import httpx


class CloudSandboxOrchestrator:
    """
    Orchestrates ephemeral cloud sandboxes (VMs) for code execution and delegates tasks to Freebuff.
    """

    def __init__(self, provider: str = "runpod"):
        self.provider = provider.lower()
        self.api_key = os.getenv(f"{self.provider.upper()}_API_KEY")

        self.base_url = self._get_base_url()
        headers = {"Content-Type": "application/json"}
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"

        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            headers=headers,
            timeout=60.0,
        )
        logger.info(f"Initialized CloudSandboxOrchestrator (Provider: {self.provider})")

    def _get_base_url(self) -> str:
        if self.provider == "runpod":
            return "https://api.runpod.io/v2"
        elif self.provider == "modal":
            return "https://api.modal.com"
        else:
            raise ValueError(f"Unsupported provider: {self.provider}")

    async def create_sandbox(self, spec: Dict[str, Any]) -> Dict[str, Any] | None:
        if not self.api_key:
            logger.warning("Cannot create sandbox: API key is missing. Running in mock/dry-run mode.")
            return {
                "id": "mock-sandbox-id-12345",
                "status": "running",
                "provider": self.provider,
                "mock": True,
            }

        endpoint = self._get_endpoint("create")
        payload = self._prepare_creation_payload(spec)

        try:
            logger.info(f"Requesting new sandbox creation with spec: {spec}")
            response = await self.client.post(endpoint, json=payload)
            response.raise_for_status()
            data = response.json()
            logger.success(f"Successfully created sandbox with ID: {data.get('id')}")
            return data
        except httpx.HTTPStatusError as e:
            logger.error(f"Failed to create sandbox. Status: {e.response.status_code}, Body: {e.response.text}")
        except Exception as e:  # noqa: BLE001
            logger.error(f"An unexpected error occurred during sandbox creation: {e}")

        return None

    async def get_sandbox_status(self, sandbox_id: str) -> Dict[str, Any] | None:
        if not self.api_key:
            logger.info(f"Dry-run: Fetching status for sandbox {sandbox_id}")
            return {
                "id": sandbox_id,
                "status": "running",
                "provider": self.provider,
                "mock": True,
            }

        endpoint = self._get_endpoint("status", sandbox_id)
        try:
            response = await self.client.get(endpoint)
            response.raise_for_status()
            return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"Failed to get status for sandbox {sandbox_id}. Status: {e.response.status_code}")
        return None

    async def run_command(self, sandbox_id: str, command: str, timeout: int = 300) -> Dict[str, Any] | None:
        if not self.api_key:
            logger.info(f"Dry-run: Running command '{command}' in sandbox {sandbox_id}")
            return {
                "status": "COMPLETED",
                "exitCode": 0,
                "stdout": f"Mock output for execution of: {command}",
                "stderr": "",
                "mock": True,
            }

        endpoint = self._get_endpoint("run", sandbox_id)
        payload = {"input": {"command": command, "timeout": timeout}}

        try:
            logger.info(f"Running command in sandbox {sandbox_id}: {command}")
            response = await self.client.post(endpoint, json=payload)
            response.raise_for_status()
            return response.json()
        except httpx.HTTPStatusError as e:
            logger.error(f"Failed to run command in sandbox {sandbox_id}. Status: {e.response.status_code}")
        return None

    async def destroy_sandbox(self, sandbox_id: str) -> bool:
        if not self.api_key:
            logger.warning(f"Dry-run: Destroying sandbox {sandbox_id}")
            return True

        endpoint = self._get_endpoint("destroy", sandbox_id)
        try:
            logger.warning(f"Destroying sandbox {sandbox_id}...")
            response = await self.client.post(endpoint)
            response.raise_for_status()
            logger.success(f"Sandbox {sandbox_id} destroyed successfully.")
            return True
        except httpx.HTTPStatusError as e:
            logger.error(f"Failed to destroy sandbox {sandbox_id}. Status: {e.response.status_code}")
        return False

    # --- Provider-specific helpers ---
    def _get_endpoint(self, action: str, sandbox_id: str = "") -> str:
        if self.provider == "runpod":
            endpoints = {
                "create": "/",
                "status": f"/{sandbox_id}",
                "run": f"/{sandbox_id}/run",
                "destroy": f"/{sandbox_id}/terminate",
            }
            return endpoints[action]
        raise NotImplementedError(f"Endpoints for provider '{self.provider}' not implemented.")

    def _prepare_creation_payload(self, spec: Dict[str, Any]) -> Dict[str, Any]:
        if self.provider == "runpod":
            return {"pod": spec}
        raise NotImplementedError(f"Payload preparation for provider '{self.provider}' not implemented.")

```