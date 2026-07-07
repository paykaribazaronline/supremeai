# 📄 ফাইল: backend/tools/docker_sandbox.py

**প্রকার:** .py  
**সাইজ:** 4,399 বাইট  
**আপডেট:** 2026-07-07T15:17:41.612504

---

## কোড

```py
import os
import subprocess
import shlex
from typing import Any

from loguru import logger


class DockerSandbox:
    def __init__(self, image: str = "python:3.10-slim"):
        self.image = image
        self.docker_available = self._check_docker()

    def _check_docker(self) -> bool:
        try:
            # Check if docker daemon is running
            res = subprocess.run(
                ["docker", "info"],
                capture_output=True,
                text=True,
                timeout=3,
                check=False,
            )
            return res.returncode == 0
        except Exception as e:
            try:
                import loguru
                loguru.logger.error(f"Tool execution error: {e}")
            except Exception as e:
                import logging
                logging.warning(f"Exception suppressed: {e}")
            return False

    def execute_command(self, cmd: str) -> dict[str, Any]:
        """Runs a command inside a sandboxed Docker container."""
        # RCE/Prompt Injection Pre-flight Firewall Check
        harmful_keywords = [
            "rm -rf",
            "mkfs",
            "dd if=",
            "shutdown",
            "reboot",
            ":(){ :|:& };:",
        ]
        forbidden_patterns = [
            "environ",
            "getenv",
            "getenvb",
            "os.environ",
            "curl",
            "wget",
            "socket",
            "requests",
            "urllib",
            "httpx",
            "http.client",
            "nc ",
            "netcat",
            "bash -i",
            "/dev/tcp",
            "/dev/udp",
            "eval(",
            "exec(",
            "subprocess",
            "system(",
        ]

        cmd_lower = cmd.lower()
        if any(kw in cmd_lower for kw in harmful_keywords) or any(
            pat in cmd_lower for pat in forbidden_patterns
        ):
            logger.warning(
                "Security Firewall: Command blocked due to high-risk pattern."
            )
            return {
                "success": False,
                "error": "Security Firewall block: command contains forbidden patterns.",
            }

        if not self.docker_available:
            env_name = os.getenv("ENV", "").lower()
            allow_fallback = os.getenv("ALLOW_LOCAL_SANDBOX_FALLBACK") == "true"

            if env_name in {"production", "staging"} or not allow_fallback:
                logger.error(
                    "Docker is not available and local execution fallback is disabled."
                )
                return {
                    "success": False,
                    "error": "Sandbox execution failed: Docker is not running and local execution is disabled for safety.",
                }

            logger.warning(
                "Docker is not available. Simulating command execution in local process."
            )
            try:
                res = subprocess.run(
                    shlex.split(cmd),
                    shell=False,
                    capture_output=True,
                    text=True,
                    timeout=5,
                    check=False,
                )
                return {
                    "success": res.returncode == 0,
                    "stdout": res.stdout,
                    "stderr": res.stderr,
                    "exit_code": res.returncode,
                    "simulated": True,
                }
            except Exception as e:
                return {"success": False, "error": str(e), "simulated": True}

        # Run command securely inside docker
        try:
            docker_cmd = [
                "docker",
                "run",
                "--rm",
                "--network",
                "none",  # Sandbox: no internet
                self.image,
                "sh",
                "-c",
                cmd,
            ]
            res = subprocess.run(
                docker_cmd, capture_output=True, text=True, timeout=10, check=False
            )
            return {
                "success": res.returncode == 0,
                "stdout": res.stdout,
                "stderr": res.stderr,
                "exit_code": res.returncode,
                "simulated": False,
            }
        except Exception as e:
            return {"success": False, "error": str(e), "simulated": False}

```