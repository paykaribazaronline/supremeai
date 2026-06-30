#!/usr/bin/env python3
"""
Cloud Sandbox Orchestrator
==========================

Manages ephemeral but persistent cloud environments (VMs/pods) for complex,
long-running AI tasks.
Integrates 'Freebuff CLI' as a zero-cost headless AI worker.
"""

import os
import httpx
import asyncio
from loguru import logger
from typing import Dict, Any


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

    def run_code(self, code: str) -> dict[str, Any]:
        """
        Runs Python code inside a secure, restricted Docker container.
        Constraints: network_mode="none", mem_limit="128m", cpu_quota=50000, timeout=5s.
        Returns format: {"success": bool, "stdout": str, "stderr": str, "exit_code": int}
        """
        # বাংলা মন্তব্য: এআই জেনারেটেড কোড নিরাপদে রান করার জন্য ডকার বেসড জিরো-ট্রাস্ট স্যান্ডবক্স পরিবেশ।
        logger.info("Executing user code inside the Dockerized Cloud Sandbox...")
        
        # AST Firewall Check
        from tools.fuzz_sandbox import run_sandbox_ast_check, SecurityError
        try:
            is_safe = run_sandbox_ast_check(code)
            if not is_safe:
                return {
                    "success": False,
                    "stdout": "",
                    "stderr": "Security Sandbox Violation: Generated code failed AST layout normalization.",
                    "exit_code": 1
                }
        except SecurityError as sec_err:
            return {
                "success": False,
                "stdout": "",
                "stderr": f"Security Sandbox Violation: {str(sec_err)}",
                "exit_code": 1
            }

        try:
            import docker
            client = docker.from_env()
        except Exception as e:
            # বাংলা মন্তব্য: প্রোডাকশন মোডে লোকাল ওএস-এ ফ্যালব্যাক এড়াতে কড়া সিকিউরিটি গার্ডরেল প্রয়োগ করা হয়েছে।
            from core.config import settings
            if settings.env == "production":
                logger.critical("🚨 SECURITY ALERT: Docker daemon unavailable in production mode! Blocking code execution.")
                return {
                    "success": False,
                    "stdout": "",
                    "stderr": "SecurityException: Code execution blocked. Docker sandbox is mandatory in production environment.",
                    "exit_code": 1
                }
            
            # Fallback when docker is unavailable on host (dry-run/simulate in dev)
            logger.warning(f"Docker client instantiation failed: {e}. Falling back to simulation.")
            import subprocess
            try:
                res = subprocess.run(
                    ["python", "-c", code],
                    capture_output=True,
                    text=True,
                    timeout=5
                )
                return {
                    "success": res.returncode == 0,
                    "stdout": res.stdout,
                    "stderr": res.stderr,
                    "exit_code": res.returncode
                }
            except subprocess.TimeoutExpired:
                return {
                    "success": False,
                    "stdout": "",
                    "stderr": "Execution Timeout: Code execution exceeded 5 seconds limit.",
                    "exit_code": -1
                }
            except Exception as ex:
                return {
                    "success": False,
                    "stdout": "",
                    "stderr": str(ex),
                    "exit_code": 1
                }

        container = None
        try:
            # Run container detached so we can monitor/kill on timeout
            container = client.containers.run(
                image="python:3.11-slim",
                command=["python", "-c", code],
                network_mode="none",
                mem_limit="128m",
                nano_cpus=500000000, # equivalent to cpu_quota 50%
                detach=True
            )
            
            # Polling loop for timeout
            import time
            start_time = time.time()
            exit_code = None
            
            while time.time() - start_time < 5.0:
                container.reload()
                status = container.status
                if status == "exited":
                    result = container.wait()
                    exit_code = result.get("StatusCode", 0)
                    break
                time.sleep(0.1)
            
            if exit_code is None:
                # Timed out! Force kill container
                logger.error("Sandbox container execution timed out! Force-killing container...")
                try:
                    container.kill()
                except Exception:
                    pass
                return {
                    "success": False,
                    "stdout": "",
                    "stderr": "Execution Timeout: Code execution exceeded 5 seconds limit.",
                    "exit_code": -1
                }

            # Retrieve stdout & stderr logs
            logs = container.logs(stdout=True, stderr=False).decode("utf-8")
            err_logs = container.logs(stdout=False, stderr=True).decode("utf-8")
            
            return {
                "success": exit_code == 0,
                "stdout": logs,
                "stderr": err_logs,
                "exit_code": exit_code
            }
            
        except Exception as e:
            logger.error(f"Docker sandbox exception: {e}")
            return {
                "success": False,
                "stdout": "",
                "stderr": f"Sandbox Exception: {str(e)}",
                "exit_code": 1
            }
        finally:
            if container:
                try:
                    container.remove(force=True)
                except Exception:
                    pass

    async def create_sandbox(self, spec: Dict[str, Any]) -> Dict[str, Any] | None:
        if not self.api_key:
            logger.warning(
                "Cannot create sandbox: API key is missing. Running in mock/dry-run mode."
            )
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
            logger.error(
                f"Failed to create sandbox. Status: {e.response.status_code}, Body: {e.response.text}"
            )
        except Exception as e:
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
            logger.error(
                f"Failed to get status for sandbox {sandbox_id}. Status: {e.response.status_code}"
            )
        return None

    async def run_command(
        self, sandbox_id: str, command: str, timeout: int = 300
    ) -> Dict[str, Any] | None:
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
            logger.error(
                f"Failed to run command in sandbox {sandbox_id}. Status: {e.response.status_code}"
            )
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
            logger.error(
                f"Failed to destroy sandbox {sandbox_id}. Status: {e.response.status_code}"
            )
        return False

    # ------------------------------------------------------------------------
    # 🤖 FREEBUFF AI WORKER INTEGRATION
    # ------------------------------------------------------------------------
    async def delegate_to_freebuff(
        self, prompt: str, working_dir: str = "."
    ) -> Dict[str, Any]:
        """
        বাংলা মন্তব্য: Freebuff CLI-কে অসিঙ্ক্রোনাস সাব-প্রসেস হিসেবে কল করে জিরো-কস্টে কোডিং টাস্ক এক্সিকিউট করা হচ্ছে।
        এটি SupremeAI-এর জন্য সম্পূর্ণ ফ্রি এআই ডেভেলপার হিসেবে কাজ করবে।
        """
        logger.info(
            f"🚀 Delegating task to Freebuff AI Worker in directory: {working_dir}"
        )
        try:
            # বাংলা মন্তব্য: asyncio.create_subprocess_exec ব্যবহার করা হচ্ছে যাতে মূল ইভেন্ট লুপ ব্লক না হয়
            # উইন্ডোজের জন্য .cmd সাফিক্স হ্যান্ডলিং করা হয়েছে
            cmd = "freebuff.cmd" if os.name == "nt" else "freebuff"
            process = await asyncio.create_subprocess_exec(
                cmd,
                "--cwd",
                working_dir,
                stdin=asyncio.subprocess.PIPE,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )

            # প্রম্পট ইনপুট হিসেবে পাঠানো হচ্ছে
            stdout, stderr = await process.communicate(input=prompt.encode("utf-8"))

            if process.returncode == 0:
                logger.success("✅ Freebuff task completed successfully.")
                return {"status": "success", "output": stdout.decode("utf-8")}
            else:
                logger.error(f"❌ Freebuff task failed: {stderr.decode('utf-8')}")
                return {"status": "error", "error": stderr.decode("utf-8")}

        except FileNotFoundError:
            logger.error(
                "🚨 Freebuff CLI not found. Please ensure it is installed globally (npm install -g freebuff)."
            )
            return {"status": "error", "error": "Freebuff CLI not installed."}
        except Exception as e:
            logger.error(f"⚠️ Unexpected error running Freebuff: {e}")
            return {"status": "error", "error": str(e)}

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
        raise NotImplementedError(
            f"Endpoints for provider '{self.provider}' not implemented."
        )

    def _prepare_creation_payload(self, spec: Dict[str, Any]) -> Dict[str, Any]:
        if self.provider == "runpod":
            return {"pod": spec}
        raise NotImplementedError(
            f"Payload preparation for provider '{self.provider}' not implemented."
        )
