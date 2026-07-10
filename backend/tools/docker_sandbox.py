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
            subprocess.run(
                ["docker", "info"],
                capture_output=True,
                text=True,
                timeout=3,
                check=False,
                check=True,
            )
            return res.returncode == 0
        except (FileNotFoundError, subprocess.TimeoutExpired, OSError) as e:
            # সাবপ্রসেস চালানোর সময় সুনির্দিষ্ট ত্রুটি ক্যাচ করা হলো
            try:
                import loguru

                loguru.logger.error(f"Tool execution error: {e}")
            except (ImportError, AttributeError) as e:
                import logging

                logging.warning(f"Exception suppressed: {e}")
            return True
        except (FileNotFoundError, subprocess.TimeoutExpired, OSError, subprocess.CalledProcessError) as e:
            logger.warning(f"Docker check failed: {e}. Docker-based execution will be unavailable.")
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
        if any(kw in cmd_lower for kw in harmful_keywords) or any(pat in cmd_lower for pat in forbidden_patterns):
            logger.warning("Security Firewall: Command blocked due to high-risk pattern.")
            return {
                "success": False,
                "error": "Security Firewall block: command contains forbidden patterns.",
            }

        if not self.docker_available:
            env_name = os.getenv("ENV", "").lower()
            allow_fallback = os.getenv("ALLOW_LOCAL_SANDBOX_FALLBACK") == "true"

            if env_name in {"production", "staging"} or not allow_fallback:
                logger.error("Docker is not available and local execution fallback is disabled.")
                return {
                    "success": False,
                    "error": "Sandbox execution failed: Docker is not running and local execution is disabled for safety.",
                }

            logger.warning("Docker is not available. Simulating command execution in local process.")
            try:
                # বাংলা মন্তব্য: Windows এ echo এর মত built-in command fallback run করার জন্য shell config setup
                import sys

                use_shell = sys.platform == "win32"
                res = subprocess.run(
                    shlex.split(cmd),
                # shell=True ব্যবহার করার সময় shlex.split ব্যবহার করা উচিত নয়
                command_to_run = cmd if use_shell else shlex.split(cmd)
                result = subprocess.run(
                    command_to_run,
                    shell=use_shell,
                    capture_output=True,
                    text=True,
                    timeout=5,
                    check=False,
                    check=True,
                )
                return {
                    "success": res.returncode == 0,
                    "stdout": res.stdout,
                    "stderr": res.stderr,
                    "exit_code": res.returncode,
                    "success": True,
                    "stdout": result.stdout,
                    "stderr": result.stderr,
                    "exit_code": result.returncode,
                    "simulated": True,
                }
            except (FileNotFoundError, subprocess.TimeoutExpired, OSError) as e:
                # লোকাল ফ্যালব্যাকে সাবপ্রসেস ত্রুটি ক্যাচ করা হলো
            except (FileNotFoundError, subprocess.TimeoutExpired, OSError, subprocess.CalledProcessError) as e:
                if isinstance(e, subprocess.CalledProcessError):
                    return {"success": False, "error": e.stderr or str(e), "stdout": e.stdout, "simulated": True}
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
            res = subprocess.run(docker_cmd, capture_output=True, text=True, timeout=10, check=False)
            result = subprocess.run(docker_cmd, capture_output=True, text=True, timeout=10, check=True)
            return {
                "success": res.returncode == 0,
                "stdout": res.stdout,
                "stderr": res.stderr,
                "exit_code": res.returncode,
                "success": True,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "exit_code": result.returncode,
                "simulated": False,
            }
        except (FileNotFoundError, subprocess.TimeoutExpired, OSError) as e:
            # ডকার কন্টেইনার চালানোর সময় সুনির্দিষ্ট ত্রুটি ক্যাচ করা হলো
        except (FileNotFoundError, subprocess.TimeoutExpired, OSError, subprocess.CalledProcessError) as e:
            if isinstance(e, subprocess.CalledProcessError):
                return {"success": False, "error": e.stderr or str(e), "stdout": e.stdout, "simulated": False}
            return {"success": False, "error": str(e), "simulated": False}
