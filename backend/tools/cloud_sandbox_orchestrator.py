#!/usr/bin/env python3
"""
Cloud Sandbox Orchestrator
===========================

Manages ephemeral but persistent cloud environments (VMs/pods) for complex,
long-running AI tasks. Integrates 'Freebuff CLI' as a zero-cost headless AI worker.

এই মডিউলটি দুটি শ্রেণি (class) প্রদান করে:
  1. CloudSandboxOrchestrator — RunPod/Modal ক্লাউড API দিয়ে স্যান্ডবক্স পরিচালনা করে।
  2. PersistentSandbox — সেশন-সচেতন (session-aware) দীর্ঘস্থায়ী স্যান্ডবক্স যা ফাইল সিস্টেম
     অপারেশন, ডিপেন্ডেন্সি ইনস্টল এবং লাইভ লগ স্ট্রিমিং সাপোর্ট করে। API key না থাকলে
     লোকাল পারসিস্টেন্ট ভলিউম (local directory) ব্যবহার করে বাস্তব কমান্ড এক্সিকিউশন করে।
"""

from __future__ import annotations

import asyncio
import json
import os
import time
import uuid
from dataclasses import dataclass, field
from typing import Any, AsyncIterator

import httpx
from loguru import logger


# বাংলা মন্তব্য: স্যান্ডবক্স সেশনের অবস্থা সংরক্ষণের জন্য ডেটাক্লাস।
@dataclass
class SandboxSession:
    """দীর্ঘস্থায়ী স্যান্ডবক্স সেশনের মেটাডেটা।"""

    id: str
    status: str = "running"
    provider: str = "local"
    volume_path: str = ""
    created_at: float = field(default_factory=time.time)
    last_active: float = field(default_factory=time.time)
    command_history: list[dict[str, Any]] = field(default_factory=list)
    mock: bool = False


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

        # বাংলা মন্তব্য: httpx ক্লায়েন্টে সবসময় স্পষ্ট টাইমআউট দেওয়া হচ্ছে (CI gate শর্ত)।
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


class PersistentSandbox:
    """
    সেশন-সচেতন (session-aware) দীর্ঘস্থায়ী স্যান্ডবক্স।

    Devin-এর মতো স্বায়ত্তশাসিত কোডিং ক্ষমতার জন্য এটি নিচের সুবিধাগুলো দেয়:
      - Persistent volume mount (কাজ মাঝপথে থামলে পুনরায় শুরু করা যায়)
      - Multi-file read/write/execute
      - Dependency installer (pip, npm, apt)
      - WebSocket/stream-based live log streaming
    """

    def __init__(self, provider: str = "local", base_volume_dir: str | None = None):
        self.provider = provider.lower()
        self.api_key = os.getenv(f"{self.provider.upper()}_API_KEY") if provider != "local" else None
        # বাংলা মন্তব্য: ইন-মেমরি সেশন স্টোর; প্রয়োজনে ডিস্কেও পারসিস্ট করা হয়।
        self._sessions: dict[str, SandboxSession] = {}
        self._base_volume_dir = base_volume_dir or os.path.join(
            os.path.dirname(os.path.abspath(__file__)), "..", "data", "sandbox_volumes"
        )
        os.makedirs(self._base_volume_dir, exist_ok=True)
        logger.info(f"Initialized PersistentSandbox (provider={self.provider}, volume_dir={self._base_volume_dir})")

    # ------------------------------------------------------------------ #
    # Session lifecycle
    # ------------------------------------------------------------------ #
    async def create_with_volume(self, spec: dict[str, Any] | None = None) -> SandboxSession:
        """দীর্ঘস্থায়ী ভলিউম সহ নতুন স্যান্ডবক্স সেশন তৈরি করে।"""
        spec = spec or {}
        session_id = spec.get("id") or uuid.uuid4().hex[:12]
        volume_path = os.path.join(self._base_volume_dir, session_id)
        os.makedirs(volume_path, exist_ok=True)
        # বাংলা মন্তব্য: কাজ মাঝপথে থামলে পুনরায় শুরু করার জন্য মেটাডেটা ফাইলে সংরক্ষণ।
        session = SandboxSession(
            id=session_id,
            status="running",
            provider=self.provider,
            volume_path=volume_path,
            mock=self.api_key is None,
        )
        self._sessions[session_id] = session
        self._persist_session(session)
        logger.success(f"Created persistent sandbox session {session_id} at {volume_path}")
        return session

    async def execute_in_session(self, session_id: str, command: str, timeout: int = 300) -> dict[str, Any]:
        """
        সেশনের ভলিউম ডিরেক্টরির ভিতরে কমান্ড এক্সিকিউট করে এবং স্ট্রিমড আউটপুট ফেরত দেয়।
        """
        session = self._sessions.get(session_id)
        if session is None:
            return {"status": "ERROR", "exitCode": 1, "stdout": "", "stderr": f"Session {session_id} not found"}

        session.last_active = time.time()
        if self.api_key and self.provider != "local":
            # বাংলা মন্তব্য: ক্লাউড প্রোভাইডারে রাউট করার সময় CloudSandboxOrchestrator ব্যবহার করা হচ্ছে।
            orch = CloudSandboxOrchestrator(provider=self.provider)
            result = await orch.run_command(session_id, command, timeout=timeout)
            return result or {"status": "ERROR", "exitCode": 1, "stdout": "", "stderr": "Cloud execution failed"}

        # লোকাল এক্সিকিউশন (বাস্তবভাবে কাজ করে)।
        try:
            proc = await asyncio.create_subprocess_shell(
                command,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                cwd=session.volume_path,
            )
            stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=timeout)
            out = stdout.decode("utf-8", errors="replace")
            err = stderr.decode("utf-8", errors="replace")
            record = {
                "command": command,
                "exit_code": proc.returncode,
                "timestamp": time.time(),
            }
            session.command_history.append(record)
            self._persist_session(session)
            return {
                "status": "COMPLETED" if proc.returncode == 0 else "FAILED",
                "exitCode": proc.returncode,
                "stdout": out,
                "stderr": err,
            }
        except TimeoutError:
            return {"status": "TIMEOUT", "exitCode": 124, "stdout": "", "stderr": f"Command timed out after {timeout}s"}
        except Exception as e:  # noqa: BLE001
            logger.error(f"Local execution failed in session {session_id}: {e}")
            return {"status": "ERROR", "exitCode": 1, "stdout": "", "stderr": str(e)}

    async def install_dependency(self, session_id: str, pkg_manager: str, package: str) -> bool:
        """সেশনে ডিপেন্ডেন্সি ইনস্টল করে (pip / npm / apt)।"""
        session = self._sessions.get(session_id)
        if session is None:
            logger.error(f"Cannot install dependency: session {session_id} not found")
            return False

        commands = {
            "pip": f"pip install {package}",
            "npm": f"npm install {package}",
            "apt": f"apt-get install -y {package}",
        }
        cmd = commands.get(pkg_manager.lower())
        if not cmd:
            logger.error(f"Unsupported package manager: {pkg_manager}")
            return False

        result = await self.execute_in_session(session_id, cmd, timeout=600)
        success = result.get("exitCode") == 0
        if success:
            logger.success(f"Installed {package} via {pkg_manager} in session {session_id}")
        else:
            logger.warning(f"Failed to install {package} in session {session_id}: {result.get('stderr')}")
        return success

    async def upload_file(self, session_id: str, path: str, content: str | bytes) -> bool:
        """সেশন ভলিউমে ফাইল আপলোড (লিখে দেওয়া) করে।"""
        session = self._sessions.get(session_id)
        if session is None:
            logger.error(f"Cannot upload file: session {session_id} not found")
            return False

        # বাংলা মন্তব্য: পাথ ট্রাভার্সাল (path traversal) আটকানো হচ্ছে।
        abs_path = os.path.normpath(os.path.join(session.volume_path, path))
        if not abs_path.startswith(os.path.normpath(session.volume_path)):
            logger.error(f"Path traversal blocked for upload: {path}")
            return False

        try:
            os.makedirs(os.path.dirname(abs_path), exist_ok=True)
            mode = "wb" if isinstance(content, bytes) else "w"
            with open(abs_path, mode) as f:
                f.write(content)  # type: ignore[arg-type]
            logger.info(f"Uploaded file to {abs_path} in session {session_id}")
            return True
        except Exception as e:  # noqa: BLE001
            logger.error(f"File upload failed: {e}")
            return False

    async def download_file(self, session_id: str, path: str) -> bytes | None:
        """সেশন ভলিউম থেকে ফাইল ডাউনলোড করে।"""
        session = self._sessions.get(session_id)
        if session is None:
            logger.error(f"Cannot download file: session {session_id} not found")
            return None

        abs_path = os.path.normpath(os.path.join(session.volume_path, path))
        if not abs_path.startswith(os.path.normpath(session.volume_path)):
            logger.error(f"Path traversal blocked for download: {path}")
            return None

        try:
            with open(abs_path, "rb") as f:
                return f.read()
        except FileNotFoundError:
            logger.error(f"File not found for download: {abs_path}")
            return None
        except Exception as e:  # noqa: BLE001
            logger.error(f"File download failed: {e}")
            return None

    async def destroy_sandbox(self, session_id: str) -> bool:
        """স্যান্ডবক্স সেশন ও তার ভলিউম মুছে ফেলে।"""
        session = self._sessions.pop(session_id, None)
        if session is None:
            logger.warning(f"Cannot destroy: session {session_id} not found")
            return False

        if self.api_key and self.provider != "local":
            orch = CloudSandboxOrchestrator(provider=self.provider)
            return await orch.destroy_sandbox(session_id)

        import shutil

        try:
            shutil.rmtree(session.volume_path, ignore_errors=True)
            meta = os.path.join(self._base_volume_dir, f"{session_id}.json")
            if os.path.exists(meta):
                os.remove(meta)
            logger.success(f"Destroyed sandbox session {session_id}")
            return True
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to destroy sandbox {session_id}: {e}")
            return False

    # ------------------------------------------------------------------ #
    # Live log streaming (WebSocket/SSE friendly)
    # ------------------------------------------------------------------ #
    async def stream_logs(self, session_id: str, command: str, timeout: int = 300) -> AsyncIterator[str]:
        """
        কমান্ড চলাকালীন লাইন-বাই-লাইন লাইভ লগ স্ট্রিম করে (async generator)।
        WebSocket বা SSE এন্ডপয়েন্ট থেকে সরাসরি কনজিউম করা যায়।
        """
        session = self._sessions.get(session_id)
        if session is None:
            yield json.dumps({"error": f"Session {session_id} not found"})
            return

        if self.api_key and self.provider != "local":
            # বাংলা মন্তব্য: ক্লাউডে স্ট্রিমিং সাপোর্ট না থাকলে একক রেস্পন্স দেওয়া হচ্ছে।
            result = await self.execute_in_session(session_id, command, timeout=timeout)
            yield json.dumps(result)
            return

        try:
            proc = await asyncio.create_subprocess_shell(
                command,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.STDOUT,
                cwd=session.volume_path,
            )
            assert proc.stdout is not None
            async for line in proc.stdout:
                yield line.decode("utf-8", errors="replace")
            await proc.wait()
            yield json.dumps({"status": "DONE", "exitCode": proc.returncode})
        except Exception as e:  # noqa: BLE001
            yield json.dumps({"error": str(e)})

    # ------------------------------------------------------------------ #
    # Persistence helpers
    # ------------------------------------------------------------------ #
    def _persist_session(self, session: SandboxSession) -> None:
        """সেশন মেটাডেটা ডিস্কে সংরক্ষণ করে (resume-এর জন্য)।"""
        try:
            meta_path = os.path.join(self._base_volume_dir, f"{session.id}.json")
            with open(meta_path, "w", encoding="utf-8") as f:
                json.dump(
                    {
                        "id": session.id,
                        "status": session.status,
                        "provider": session.provider,
                        "volume_path": session.volume_path,
                        "created_at": session.created_at,
                        "last_active": session.last_active,
                        "command_history": session.command_history[-50:],
                        "mock": session.mock,
                    },
                    f,
                    indent=2,
                )
        except Exception as e:  # noqa: BLE001
            logger.debug(f"Session persistence skipped: {e}")

    def list_sessions(self) -> list[dict[str, Any]]:
        """বর্তমানে থাকা সব সেশনের সারসংক্ষেপ ফেরত দেয়।"""
        return [
            {
                "id": s.id,
                "status": s.status,
                "provider": s.provider,
                "last_active": s.last_active,
                "commands_run": len(s.command_history),
            }
            for s in self._sessions.values()
        ]
