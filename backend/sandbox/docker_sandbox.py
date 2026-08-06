# backend/sandbox/docker_sandbox.py
import logging
import re
import subprocess
from pathlib import Path
from typing import Any

logger = logging.getLogger("supremeai.sandbox.docker")


class DockerSandbox:
    def __init__(self, image_name: str = "python:3.11-slim"):
        self.image_name = image_name
        self.memory_limit = "256m"
        self.cpu_limit = "0.5"
        self.timeout_seconds = 10

    def _sanitize_module_name(self, entry_file: str) -> str:
        """Sanitize entry file name - only allow alphanumeric and underscore to prevent injection."""
        # Remove .py extension and sanitize
        safe_name = entry_file.replace(".py", "").replace(".PY", "").replace(".Py", "")
        # Only allow alphanumeric and underscore characters
        safe_name = re.sub(r"[^a-zA-Z0-9_]", "", safe_name)
        if not safe_name:
            raise ValueError("Invalid entry file name after sanitization")
        return safe_name

    def run_quarantine_test(self, staging_path: Path, entry_file: str, test_payload: str) -> dict[str, Any]:
        """
        Default-deny network এবং Read-only মাউন্টে একটি পাইথন ফাইল স্যান্ডবক্সে রান করায়।
        """
        if not staging_path.exists():
            return {
                "exit_code": -1,
                "stdout": "",
                "stderr": "Staging path does not exist.",
            }

        # স্যান্ডবক্সের ভেতর এক্সিকিউট করার জন্য একটি সেফ রানিং স্ক্রিপ্ট ইনজেক্ট করা হচ্ছে
        # এটি নিশ্চিত করে যে কোডটি রান করার পর আউটপুটটি জেসন ফরম্যাটে ট্র্যাপড হবে
        target_file_path = staging_path / entry_file
        if not target_file_path.exists():
            return {
                "exit_code": -1,
                "stdout": "",
                "stderr": f"Entry file {entry_file} not found.",
            }

        # নিরাপত্তা: entry_file নাম স্যানিটাইজ করা হচ্ছে (শুধুমাত্র alphanumeric ও underscore)
        safe_module_name = self._sanitize_module_name(entry_file)

        # Subprocess এর মাধ্যমে সরাসরি ডকার সিএলআই এনফোর্সমেন্ট
        # বাংলা মন্তব্য: পাইথন ইনজেকশন এড়াতে payload-টি সরাসরি কমান্ড স্ট্রিং-এ কনক্যাট না করে এনভায়রনমেন্ট ভ্যারিয়েবল হিসেবে পাস করা হচ্ছে।
        cmd = [
            "docker",
            "run",
            "--rm",
            "--network",
            "none",  # 🔒 নো নেটওয়ার্ক (Default-deny)
            "--memory",
            self.memory_limit,  # 📉 মেমরি ক্যাপ
            "--cpus",
            self.cpu_limit,  # 📊 সিপিইউ ক্যাপ
            "-e",
            f"SANDBOX_PAYLOAD={test_payload}",
            "-v",
            f"{staging_path.resolve()}:/workspace:ro",  # 📁 রিড-ওনলি মাউন্ট
            "-w",
            "/workspace",
            self.image_name,
            "python",
            "-c",
            f"import os, sys, json, ast; import {safe_module_name} as tool; "
            f"payload = ast.literal_eval(os.environ.get('SANDBOX_PAYLOAD', '{{}}')); "
            f"print(json.dumps(tool.execute_tool(payload)))",
        ]

        try:
            # বাংলা মন্তব্য: UP022 ফিক্স — capture_output=True ব্যবহার করা হয়েছে
            # stdout=PIPE + stderr=PIPE এর চেয়ে আধুনিক ও Pythonic পদ্ধতি
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=self.timeout_seconds,
                check=False,
            )
            return {
                "exit_code": result.returncode,
                "stdout": result.stdout.strip(),
                "stderr": result.stderr.strip(),
            }
        except subprocess.TimeoutExpired:
            return {
                "exit_code": 124,  # Standard timeout exit code
                "stdout": "",
                "stderr": f"🚨 Security Sandbox Timeout: Execution exceeded {self.timeout_seconds}s limit.",
            }
        except Exception as e:
            return {
                "exit_code": -1,
                "stdout": "",
                "stderr": f"Docker execution engine failure: {e!s}",
            }

    def run_safe_container(self, payload: dict[str, Any]) -> dict[str, Any]:
        """
        হোস্টের আইসোলেটেড ফাইলকে কন্টেইনারের ভেতর Read-Only মাউন্ট করে
        নিরাপদে পাইথন স্ক্রিপ্ট এক্সিকিউট করে এবং আউটপুট রিটার্ন করে।

        Security: Script-কে সরাসরি `-c` argument-এ পাস না করে temp file-এ লিখে
        read-only mount করা হয় — command injection প্রতিরোধ।
        """
        import tempfile

        script = payload.get("script", "")
        bind_source = payload.get("bind_mount_source", "")
        bind_target = payload.get("bind_mount_target", "")

        if not script:
            return {
                "exit_code": 1,
                "stdout": "",
                "stderr": "No script provided for sandbox execution.",
            }

        # 🛡️ ডকার সিকিউরিটি এবং আইসোলেশন ফ্ল্যাগস এনফোর্সমেন্ট
        # অতিরিক্ত সুরক্ষা: bind_source-এর path traversal প্রতিরোধ
        if ".." in bind_source or ".." in bind_target:
            logger.critical(f"Suspicious path detected in bind mount: {bind_source}")
            return {
                "exit_code": 1,
                "stdout": "",
                "stderr": "Invalid bind mount path detected.",
            }

        # 🔒 Security Fix: Script-কে temp file-এ লিখে read-only mount করা হয়
        # যাতে command injection (shell metacharacters) প্রতিরোধ হয়।
        script_path = None
        try:
            with tempfile.NamedTemporaryFile(mode="w", suffix=".py", delete=False, encoding="utf-8") as f:
                f.write(script)
                script_path = f.name

            docker_command = [
                "docker",
                "run",
                "--rm",
                "--network",
                "none",
                "--read-only",
                "-v",
                f"{script_path}:/sandbox_script.py:ro",
                "-v",
                f"{bind_source}:{bind_target}:ro",
                self.image_name,
                "python3",
                "/sandbox_script.py",
            ]

            try:
                logger.info(f"⚡ Spawning Docker Sandbox for source volume: {bind_source}")

                result = subprocess.run(
                    docker_command,
                    capture_output=True,
                    text=True,
                    timeout=self.timeout_seconds,
                    check=False,
                )

                return {
                    "exit_code": result.returncode,
                    "stdout": result.stdout.strip(),
                    "stderr": result.stderr.strip(),
                }

            except subprocess.TimeoutExpired:
                logger.error(f"❌ Sandbox execution timed out after {self.timeout_seconds}s limit.")
                return {
                    "exit_code": 124,
                    "stdout": "",
                    "stderr": f"Execution barrier breached: Timeout of {self.timeout_seconds}s exceeded.",
                }
            except Exception as e:
                logger.error(f"Critical exception inside Docker execution wrapper: {e!s}")
                return {
                    "exit_code": -1,
                    "stdout": "",
                    "stderr": f"Sandbox Runtime Anomaly: {e!s}",
                }
        finally:
            # Cleanup temp file
            if script_path:
                try:
                    import os

                    os.unlink(script_path)
                except OSError:
                    pass
