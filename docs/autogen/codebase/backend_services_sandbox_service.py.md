# 📄 ফাইল: backend/services/sandbox_service.py

**প্রকার:** .py  
**সাইজ:** 3,648 বাইট  
**আপডেট:** 2026-07-11T17:11:02.691990

---

## কোড

```py
import asyncio
import logging
from typing import Any

import docker
from docker.errors import ContainerError


logger = logging.getLogger(__name__)


class SandboxService:
    """
    Server-side Docker sandbox for executing heavy data analysis or GitHub tasks.
    """

    def __init__(self, timeout: int = 30):
        self.timeout = timeout
        try:
            self.client = docker.from_env()
            logger.info("Docker client initialized successfully.")
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to initialize Docker client: {e}")
            self.client = None

    async def execute_in_docker(self, code: str, language: str = "python") -> dict[str, Any]:
        """
        Executes code in an isolated Docker container with strict timeouts
        to prevent infinite loops.
        """
        if not self.client:
            return {"status": "FAILED", "stdout": "", "stderr": "Docker is not running or accessible.", "execution_time_ms": 0}

        logger.info(f"Executing {language} code in Docker Sandbox (timeout: {self.timeout}s)...")

        image = "python:3.11-slim"
        if language != "python":
            return {"status": "FAILED", "stderr": f"Unsupported language: {language}", "execution_time_ms": 0}

        start_time = asyncio.get_event_loop().time()

        try:
            # Run blocking docker call in thread pool to not block asyncio
            loop = asyncio.get_event_loop()

            def run_container():
                return self.client.containers.run(
                    image,
                    command=["python", "-c", code],
                    remove=True,
                    stderr=True,
                    stdout=True,
                    mem_limit="128m",
                    network_mode="none",  # Strict isolation
                    # In python docker SDK, timeout is handled via wait() or similar,
                    # but here we rely on asyncio.wait_for
                )

            # Execute with timeout
            output_bytes = await asyncio.wait_for(loop.run_in_executor(None, run_container), timeout=self.timeout)

            execution_time = int((asyncio.get_event_loop().time() - start_time) * 1000)

            return {
                "status": "SUCCESS",
                "stdout": output_bytes.decode("utf-8") if isinstance(output_bytes, bytes) else str(output_bytes),
                "stderr": "",
                "execution_time_ms": execution_time,
            }

        except TimeoutError:
            logger.error(f"Execution exceeded timeout of {self.timeout}s")
            return {
                "status": "TIMEOUT",
                "stdout": "",
                "stderr": f"Execution exceeded {self.timeout}s timeout.",
                "execution_time_ms": int((asyncio.get_event_loop().time() - start_time) * 1000),
            }
        except ContainerError as e:
            logger.error(f"Container execution error: {e}")
            return {
                "status": "FAILED",
                "stdout": "",
                "stderr": str(e.stderr.decode("utf-8")) if getattr(e, "stderr", None) else str(e),
                "execution_time_ms": int((asyncio.get_event_loop().time() - start_time) * 1000),
            }
        except Exception as e:  # noqa: BLE001
            logger.error(f"Sandbox execution failed: {e}")
            return {
                "status": "FAILED",
                "stdout": "",
                "stderr": str(e),
                "execution_time_ms": int((asyncio.get_event_loop().time() - start_time) * 1000),
            }


sandbox_service = SandboxService()

```