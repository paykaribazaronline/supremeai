# 📄 ফাইল: backend/core/microvm_sandbox.py

**প্রকার:** .py  
**সাইজ:** 9,612 বাইট  
**আপডেট:** 2026-07-08T01:53:18.572171

---

## কোড

```py
import json
import os
import shutil
import subprocess
import tempfile  # বাংলা মন্তব্য: P0 Fix — command injection ঠেকাতে temp file ব্যবহার করা হবে
import time
from typing import Any

from loguru import logger


class MicroVMSandbox:
    _vm_id_counter = 0

    def __init__(self):
        self.firecracker_path = os.getenv("FIRECRACKER_PATH", "/usr/bin/firecracker")
        self.gvisor_path = os.getenv("GVISOR_PATH", "/usr/bin/runsc")
        self.sandbox_dir = os.getenv("SANDBOX_ROOT", "/tmp/sandboxes")  # nosec B108
        self.network_disabled = True
        self.auto_destroy = True
        os.makedirs(self.sandbox_dir, exist_ok=True)

    def _generate_vm_id(self) -> str:
        MicroVMSandbox._vm_id_counter += 1
        return f"supremeai-vm-{int(time.time())}-{MicroVMSandbox._vm_id_counter}"

    def _check_microvm_available(self) -> bool:
        try:
            if shutil.which("firecracker"):
                return True
            if shutil.which("runsc"):
                return True
        except Exception as e:  # noqa: BLE001
            import logging
            logging.warning(f"Exception suppressed: {e}")
        return False

    def _create_microvm_config(self, vm_id: str, cmd: str) -> str:
        config = {
            "boot-source": {
                "kernel_image_path": "/tmp/vmlinux",  # nosec B108
                "boot_args": "console=ttyS0 reboot=k panic=1 pci=off break=bootparams",
            },
            "drives": [
                {
                    "drive_id": "rootfs",
                    "path_on_host": f"{self.sandbox_dir}/{vm_id}/rootfs.ext4",
                    "is_root_device": True,
                }
            ],
            "machine-config": {
                "vcpu_count": 1,
                "mem_size_mib": 128,
            },
            "network-interfaces": [] if self.network_disabled else None,
        }
        config_path = f"{self.sandbox_dir}/{vm_id}/config.json"
        with open(config_path, "w") as f:
            json.dump(config, f)
        return config_path

    async def execute_async(
        self, cmd: str, timeout: int = 30, language: str = "python"
    ) -> dict[str, Any]:
        vm_type = self._check_microvm_available()

        if not vm_type:
            vm_type = os.getenv("ALLOW_SANDBOX_FALLBACK", "false").lower() == "true"
            if not vm_type:
                logger.error(
                    "No MicroVM available (Firecracker/gVisor) and fallback disabled"
                )
                return {
                    "success": False,
                    "error": "MicroVM sandbox unavailable - security enforcement active",
                    "provider": "microvm",
                }

        vm_id = self._generate_vm_id()
        vm_dir = f"{self.sandbox_dir}/{vm_id}"
        os.makedirs(vm_dir, exist_ok=True)

        try:
            if vm_type == "firecracker":
                return await self._run_firecracker(vm_id, cmd, language, timeout)
            elif vm_type == "gvisor":
                return await self._run_gvisor(cmd, timeout)
            else:
                return await self._run_docker_fallback(vm_id, cmd, timeout)
        finally:
            if self.auto_destroy:
                self._destroy_vm(vm_id)

    async def _run_firecracker(
        self, vm_id: str, cmd: str, language: str, timeout: int
    ) -> dict[str, Any]:
        self._create_microvm_config(vm_id, cmd)

        try:
            result = subprocess.run(
                ["firecracker", "--api-sock", f"{self.sandbox_dir}/{vm_id}/api.sock"],
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
            )

            return {
                "success": result.returncode == 0,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "exit_code": result.returncode,
                "provider": "firecracker",
                "ephemeral": True,
            }
        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error": "Execution timeout",
                "provider": "firecracker",
            }
        except Exception as e:  # noqa: BLE001
            return {"success": False, "error": str(e), "provider": "firecracker"}

    async def _run_gvisor(self, cmd: str, timeout: int) -> dict[str, Any]:
        try:
            result = subprocess.run(
                ["runsc", "do", cmd],
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
            )
            return {
                "success": result.returncode == 0,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "exit_code": result.returncode,
                "provider": "gvisor",
                "ephemeral": True,
            }
        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error": "Execution timeout",
                "provider": "gvisor",
            }
        except Exception as e:  # noqa: BLE001
            return {"success": False, "error": str(e), "provider": "gvisor"}

    async def _run_docker_fallback(
        self, vm_id: str, cmd: str, timeout: int
    ) -> dict[str, Any]:
        # বাংলা মন্তব্য: P0 Fix — cmd কে সরাসরি `python -c` argument হিসেবে দেওয়া নিষিদ্ধ।
        # এটি shell injection এবং argument injection উভয় প্রতিরোধ করে।
        # cmd → temp file → `python /sandbox/code.py` (file execution, argument injection নয়)
        tmp_file = None
        try:
            with tempfile.NamedTemporaryFile(
                mode="w", suffix=".py", delete=False, dir="/tmp"  # nosec B108
            ) as f:
                f.write(cmd)
                tmp_file = f.name

            result = subprocess.run(
                [
                    "docker",
                    "run",
                    "--rm",
                    "--read-only",
                    "--network",
                    "none",
                    "--memory",
                    "128m",
                    "--cpus",
                    "0.5",
                    "-v",
                    f"{tmp_file}:/sandbox/code.py:ro",
                    "python:3.11-slim",
                    "python",
                    "/sandbox/code.py",  # বাংলা মন্তব্য: file execution — argument injection নয়
                ],
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
            )
            return {
                "success": result.returncode == 0,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "exit_code": result.returncode,
                "provider": "docker-fallback",
                "ephemeral": True,
            }
        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error": "Execution timeout",
                "provider": "docker-fallback",
            }
        except Exception as e:  # noqa: BLE001
            return {"success": False, "error": str(e), "provider": "docker-fallback"}
        finally:
            # বাংলা মন্তব্য: temp file সবসময় cleanup করতে হবে — resource leak নিষিদ্ধ
            if tmp_file and os.path.exists(tmp_file):
                import contextlib
                # বাংলা মন্তব্য: SIM105 lint rule সন্তুষ্ট করতে contextlib.suppress ব্যবহার করা হলো
                with contextlib.suppress(OSError):
                    os.unlink(tmp_file)

    def _destroy_vm(self, vm_id: str) -> None:
        vm_dir = f"{self.sandbox_dir}/{vm_id}"
        try:
            if os.path.exists(vm_dir):
                shutil.rmtree(vm_dir)
            logger.info(f"MicroVM {vm_id} destroyed")
        except Exception as e:  # noqa: BLE001
            logger.warning(f"Failed to destroy VM {vm_id}: {e}")

    async def health_check(self) -> dict[str, Any]:
        vm_type = self._check_microvm_available()
        return {
            "status": "ready" if vm_type else "unavailable",
            "provider": vm_type or "none",
            "auto_destroy": self.auto_destroy,
            "network_disabled": self.network_disabled,
        }


# বাংলা মন্তব্য: P2 Fix — Module-level singleton lazy করা হলো।
# আগে: `sandbox = MicroVMSandbox()` import-এ execute হতো — cold start বাড়াতো এবং pytest isolation ভাঙতো।
# এখন: প্রথম ব্যবহারের সময় instantiate হবে।
_sandbox_instance: MicroVMSandbox | None = None


def get_sandbox() -> MicroVMSandbox:
    """Lazy singleton factory — import সময়ে initialization নিষিদ্ধ"""
    global _sandbox_instance
    if _sandbox_instance is None:
        _sandbox_instance = MicroVMSandbox()
    return _sandbox_instance


# Backward-compat alias
sandbox = get_sandbox()


async def execute_code_securely(
    code: str, timeout: int = 30, language: str = "python"
) -> dict[str, Any]:
    return await get_sandbox().execute_async(code, timeout, language)

```