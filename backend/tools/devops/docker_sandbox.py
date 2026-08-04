import shlex
import subprocess
from typing import Any

from core.config import settings
from loguru import logger


class DockerSandbox:
    def __init__(self, image: str = "python:3.10-slim"):
        self.image = image
        self.docker_available = self._check_docker()

    def _check_docker(self) -> bool:
        try:
            # Check if docker daemon is running
            subprocess.run(
                ["docker", "info"],
                capture_output=True,
                timeout=3,
                check=True,
            )
            return True
        except (
            FileNotFoundError,
            subprocess.TimeoutExpired,
            OSError,
            subprocess.CalledProcessError,
        ) as e:
            logger.warning(
                f"Docker check failed: {e}. Docker-based execution will be unavailable."
            )
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
        import re

        forbidden_patterns = [
            r"\benviron\b",
            r"\bgetenv\b",
            r"\bgetenvb\b",
            r"os\.environ",
            r"\bcurl\b",
            r"\bwget\b",
            r"\bsocket\b",
            r"\brequests\b",
            r"\burllib\b",
            r"\bhttpx\b",
            r"http\.client",
            r"\bnc\s",
            r"\bnetcat\b",
            r"bash\s+-i",
            r"/dev/tcp",
            r"/dev/udp",
            r"\beval\s*\(",
            r"\bexec\s*\(",
            r"\bsubprocess\b",
            r"\bsystem\s*\(",
        ]

        cmd_lower = cmd.lower()
        if any(kw in cmd_lower for kw in harmful_keywords) or any(
            re.search(pat, cmd_lower) for pat in forbidden_patterns
        ):
            logger.warning(
                "Security Firewall: Command blocked due to high-risk pattern."
            )
            return {
                "success": False,
                "error": "Security Firewall block: command contains forbidden patterns.",
            }

        if not self.docker_available:
            env_name = (
                getattr(settings, "env", None) or getattr(settings, "env", "").lower()
            )
            allow_fallback_str = getattr(settings, "allow_local_sandbox_fallback", None)
            if allow_fallback_str is not None:
                allow_fallback = allow_fallback_str.lower() == "true"
            else:
                allow_fallback = getattr(
                    settings, "allow_local_sandbox_fallback", None
                ) == "true" or getattr(settings, "allow_sandbox_fallback", False)

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
                # বাংলা মন্তব্য: Windows এ echo এর মত built-in command fallback run করার জন্য shell config setup
                import sys

                use_shell = sys.platform == "win32"
                # shell=True ব্যবহার করার সময় shlex.split ব্যবহার করা উচিত নয়
                command_to_run = cmd if use_shell else shlex.split(cmd)
                result = subprocess.run(
                    command_to_run,
                    shell=use_shell,
                    capture_output=True,
                    text=True,
                    timeout=5,
                    check=True,
                )
                return {
                    "success": True,
                    "stdout": result.stdout,
                    "stderr": result.stderr,
                    "exit_code": result.returncode,
                    "simulated": True,
                }
            except (
                FileNotFoundError,
                subprocess.TimeoutExpired,
                OSError,
                subprocess.CalledProcessError,
            ) as e:
                if isinstance(e, subprocess.CalledProcessError):
                    return {
                        "success": False,
                        "error": e.stderr or str(e),
                        "stdout": e.stdout,
                        "simulated": True,
                    }
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
            result = subprocess.run(
                docker_cmd, capture_output=True, text=True, timeout=10, check=True
            )
            return {
                "success": True,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "exit_code": result.returncode,
                "simulated": False,
            }
        except (
            FileNotFoundError,
            subprocess.TimeoutExpired,
            OSError,
            subprocess.CalledProcessError,
        ) as e:
            if isinstance(e, subprocess.CalledProcessError):
                return {
                    "success": False,
                    "error": e.stderr or str(e),
                    "stdout": e.stdout,
                    "simulated": False,
                }
            return {"success": False, "error": str(e), "simulated": False}
