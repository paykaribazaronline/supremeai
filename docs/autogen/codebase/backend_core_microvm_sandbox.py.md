# 📄 ফাইল: backend/core/microvm_sandbox.py

**প্রকার:** .py  
**সাইজ:** 18,309 বাইট  
**আপডেট:** 2026-07-11T16:17:51.567004

---

## কোড

```py
# backend/core/microvm_sandbox.py
# বাংলা মন্তব্য: সম্পূর্ণ রি-ফ্যাক্টর — Path Traversal Whitelist + Strict Validation।
# sandbox_root এখন Settings থেকে আসে এবং startup-এ whitelist validate হয়।
# string interpolation দিয়ে path build করা নিষিদ্ধ — pathlib.Path ব্যবহার।
# Docker image whitelist enforced — arbitrary image run নিষিদ্ধ।
# os.environ-এ secrets inject করা বন্ধ।
# CancelledError সবসময় re-raise।

import asyncio
import contextlib
import json
import re
import shutil
import subprocess
import tempfile
import time
from pathlib import Path
from typing import Any

from loguru import logger

from core.config import settings
from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus


# ── Security Constants ─────────────────────────────────────────────────────────

# বাংলা মন্তব্য: Sandbox root whitelist — অনুমোদিত directories শুধু এখানে থাকতে পারে।
# কেউ SANDBOX_ROOT=/etc/cron.d দিলে startup-এই crash হবে।
_SANDBOX_ROOT_WHITELIST: frozenset[str] = frozenset(
    {
        "/tmp/sandboxes",  # nosec B108 — whitelisted
        "/var/tmp/sandboxes",
        "/run/sandboxes",
    }
)

# বাংলা মন্তব্য: vm_id এ শুধু alphanumeric, hyphen, underscore allowed — path injection prevent
_VM_ID_PATTERN: re.Pattern = re.compile(r"^[a-zA-Z0-9_-]{1,64}$")

# বাংলা মন্তব্য: Docker image whitelist — arbitrary image run নিষিদ্ধ
_ALLOWED_DOCKER_IMAGES: frozenset[str] = frozenset(
    {
        "python:3.11-slim",
        "python:3.12-slim",
        "node:20-slim",
    }
)

# বাংলা মন্তব্য: Default docker image — whitelist-এর প্রথমটি
_DEFAULT_DOCKER_IMAGE: str = "python:3.11-slim"


def _validate_sandbox_root(path_str: str) -> Path:
    """
    বাংলা মন্তব্য: Sandbox root path startup validation।
    Whitelist-এ না থাকলে ValueError — startup crash।
    Symlink traversal check সহ resolved path check করা হয়।
    """
    path = Path(path_str).resolve()
    # বাংলা মন্তব্য: resolved path whitelist-এ check করতে হবে — symlink bypass prevent
    if str(path) not in _SANDBOX_ROOT_WHITELIST:
        raise ValueError(
            f"SANDBOX_ROOT '{path_str}' (resolved: '{path}') is not in the allowed whitelist "
            f"{sorted(_SANDBOX_ROOT_WHITELIST)}. "
            f"Set SANDBOX_ROOT env var to an approved path."
        )
    return path


def _validate_vm_id(vm_id: str) -> str:
    """বাংলা মন্তব্য: vm_id pattern validation — path injection prevent।"""
    if not _VM_ID_PATTERN.match(vm_id):
        raise ValueError(f"Invalid vm_id '{vm_id}'. Only alphanumeric, hyphen, underscore allowed (max 64 chars).")
    return vm_id


def _safe_vm_path(sandbox_root: Path, vm_id: str) -> Path:
    """
    বাংলা মন্তব্য: vm_id থেকে safe path তৈরি।
    Path traversal check: final path অবশ্যই sandbox_root-এর under থাকবে।
    vm_id validated হওয়া সত্ত্বেও double-check করা হয় — defense in depth।
    """
    vm_path = (sandbox_root / vm_id).resolve()
    sandbox_root_str = str(sandbox_root)
    if not str(vm_path).startswith(sandbox_root_str):
        raise ValueError(f"Path traversal detected! vm_id '{vm_id}' resolves to '{vm_path}' which is outside sandbox root '{sandbox_root_str}'.")
    return vm_path


class MicroVMSandbox:
    """
    বাংলা মন্তব্য: Path-Hardened MicroVM Sandbox।
    - সব paths pathlib.Path দিয়ে তৈরি (string interpolation নয়)
    - sandbox_root startup-এ whitelist validated
    - vm_id regex validated
    - Docker image whitelist enforced
    - CancelledError সবসময় re-raise
    """

    _vm_id_counter: int = 0

    def __init__(self) -> None:
        # বাংলা মন্তব্য: paths Settings থেকে আসছে — hardcode নেই
        self.firecracker_path = Path(settings.firecracker_path)
        self.gvisor_path = Path(settings.gvisor_path)

        # বাংলা মন্তব্য: sandbox_root startup-এ validate হবে — invalid = ValueError
        self.sandbox_root = _validate_sandbox_root(settings.sandbox_root)
        self.sandbox_root.mkdir(parents=True, exist_ok=True)

        self.network_disabled = True
        self.auto_destroy = True
        self.allow_fallback = settings.allow_sandbox_fallback

        logger.info(f"[MicroVMSandbox] Initialized. sandbox_root={self.sandbox_root} | allow_fallback={self.allow_fallback}")

    @classmethod
    def _generate_vm_id(cls) -> str:
        """বাংলা মন্তব্য: Validated vm_id generate করা।"""
        cls._vm_id_counter += 1
        vm_id = f"supremeai-vm-{int(time.time())}-{cls._vm_id_counter}"
        return _validate_vm_id(vm_id)

    def _check_microvm_available(self) -> str | None:
        """বাংলা মন্তব্য: Available VM runtime check। String type return করে।"""
        if shutil.which("firecracker"):
            return "firecracker"
        if shutil.which("runsc"):
            return "gvisor"
        return None

    def _create_microvm_config(self, vm_dir: Path, vm_id: str) -> Path:
        """
        বাংলা মন্তব্য: Firecracker config তৈরি — pathlib.Path ব্যবহার।
        string interpolation দিয়ে path build করা সম্পূর্ণ নিষিদ্ধ।
        """
        config = {
            "boot-source": {
                "kernel_image_path": "/tmp/vmlinux",  # nosec B108 — known fixed path
                "boot_args": "console=ttyS0 reboot=k panic=1 pci=off",
            },
            "drives": [
                {
                    "drive_id": "rootfs",
                    # বাংলা মন্তব্য: pathlib.Path — string concatenation নয়
                    "path_on_host": str(vm_dir / "rootfs.ext4"),
                    "is_root_device": True,
                }
            ],
            "machine-config": {"vcpu_count": 1, "mem_size_mib": 128},
            "network-interfaces": [] if self.network_disabled else [],
        }
        config_path = vm_dir / "config.json"
        config_path.write_text(json.dumps(config), encoding="utf-8")
        return config_path

    async def execute_async(self, cmd: str, timeout: int = 30, language: str = "python") -> dict[str, Any]:
        """বাংলা মন্তব্য: Secure code execution। Path validation mandatory।"""
        vm_runtime = self._check_microvm_available()

        if not vm_runtime:
            if not self.allow_fallback:
                logger.error("[MicroVMSandbox] No MicroVM runtime available and fallback disabled.")
                error_event_bus.emit(
                    ErrorEvent(
                        module="microvm_sandbox",
                        error_type="SANDBOX_UNAVAILABLE",
                        message="No MicroVM runtime (Firecracker/gVisor) available.",
                        severity="ERROR",
                        context={"allow_fallback": False, "language": language},
                    )
                )
                return {
                    "success": False,
                    "error": "MicroVM sandbox unavailable — security enforcement active.",
                    "provider": "none",
                }
            vm_runtime = "docker"

        try:
            vm_id = self._generate_vm_id()
            vm_dir = _safe_vm_path(self.sandbox_root, vm_id)
            vm_dir.mkdir(parents=True, exist_ok=True)
        except ValueError as exc:
            logger.exception(f"[MicroVMSandbox] Path validation failed: {exc}")
            return {"success": False, "error": str(exc), "provider": "none"}

        try:
            if vm_runtime == "firecracker":
                return await self._run_firecracker(vm_dir, vm_id, timeout)
            elif vm_runtime == "gvisor":
                return await self._run_gvisor(cmd, timeout)
            else:
                return await self._run_docker_fallback(cmd, timeout)
        except asyncio.CancelledError:
            # বাংলা মন্তব্য: CancelledError re-raise — কখনো suppress করা যাবে না
            logger.warning(f"[MicroVMSandbox] Execution cancelled for vm_id={vm_id}")
            raise
        except Exception as exc:  # noqa: BLE001
            logger.exception(f"[MicroVMSandbox] Unexpected error for vm_id={vm_id}: {exc}")
            error_event_bus.emit(
                ErrorEvent(
                    module="microvm_sandbox",
                    error_type="EXECUTION_FAILED",
                    message=str(exc)[:500],
                    severity="ERROR",
                    context={"vm_id": vm_id, "vm_runtime": vm_runtime},
                )
            )
            return {"success": False, "error": str(exc), "provider": vm_runtime}
        finally:
            if self.auto_destroy:
                self._destroy_vm_dir(vm_dir)

    async def _run_firecracker(self, vm_dir: Path, vm_id: str, timeout: int) -> dict[str, Any]:
        """বাংলা মন্তব্য: Firecracker run — pathlib.Path দিয়ে args build।"""
        self._create_microvm_config(vm_dir, vm_id)
        api_sock = vm_dir / "api.sock"

        try:
            result = subprocess.run(
                # বাংলা মন্তব্য: list-based args — shell=True নিষিদ্ধ
                ["firecracker", "--api-sock", str(api_sock)],
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
                shell=False,  # explicit — no shell injection
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
        except Exception as exc:  # noqa: BLE001
            logger.exception(f"[MicroVMSandbox] Firecracker error: {exc}")
            return {"success": False, "error": str(exc), "provider": "firecracker"}

    async def _run_gvisor(self, cmd: str, timeout: int) -> dict[str, Any]:
        """
        বাংলা মন্তব্য: gVisor run — cmd tempfile-এ write করে execute।
        string interpolation দিয়ে cmd argument inject করা নিষিদ্ধ।
        tempfile sandbox_root-এ তৈরি হয় — /tmp bypass নয়।
        """
        tmp_path: Path | None = None
        try:
            # বাংলা মন্তব্য: tempfile sandbox_root-এ — arbitrary dir নয়
            with tempfile.NamedTemporaryFile(
                mode="w",
                suffix=".py",
                delete=False,
                dir=str(self.sandbox_root),
            ) as tf:
                tf.write(cmd)
                tmp_path = Path(tf.name)

            result = subprocess.run(
                # বাংলা মন্তব্য: "--" separator — flags injection prevent
                ["runsc", "do", "--", "python3", str(tmp_path)],
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
                shell=False,
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
        except Exception as exc:  # noqa: BLE001
            logger.exception(f"[MicroVMSandbox] gVisor error: {exc}")
            return {"success": False, "error": str(exc), "provider": "gvisor"}
        finally:
            # বাংলা মন্তব্য: temp file সবসময় cleanup — resource leak নিষিদ্ধ
            if tmp_path and tmp_path.exists():
                with contextlib.suppress(OSError):
                    tmp_path.unlink()

    async def _run_docker_fallback(self, cmd: str, timeout: int) -> dict[str, Any]:
        """
        বাংলা মন্তব্য: Docker fallback — whitelist image only।
        cmd tempfile-এ write করা হয় — argument injection নয়।
        """
        # বাংলা মন্তব্য: _ALLOWED_DOCKER_IMAGES whitelist enforce
        docker_image = _DEFAULT_DOCKER_IMAGE
        assert docker_image in _ALLOWED_DOCKER_IMAGES  # nosec B101

        tmp_path: Path | None = None
        try:
            with tempfile.NamedTemporaryFile(
                mode="w",
                suffix=".py",
                delete=False,
                dir=str(self.sandbox_root),  # sandbox root-এ — arbitrary dir নয়
            ) as tf:
                tf.write(cmd)
                tmp_path = Path(tf.name)

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
                    # বাংলা মন্তব্য: validated temp file path — string interpolation নয়
                    "-v",
                    f"{tmp_path}:/sandbox/code.py:ro",
                    docker_image,  # whitelisted image শুধু
                    "python",
                    "/sandbox/code.py",
                ],
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
                shell=False,
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
        except Exception as exc:  # noqa: BLE001
            logger.exception(f"[MicroVMSandbox] Docker fallback error: {exc}")
            return {"success": False, "error": str(exc), "provider": "docker-fallback"}
        finally:
            # বাংলা মন্তব্য: temp file সবসময় cleanup — resource leak নিষিদ্ধ
            if tmp_path and tmp_path.exists():
                with contextlib.suppress(OSError):
                    tmp_path.unlink()

    def _destroy_vm_dir(self, vm_dir: Path) -> None:
        """বাংলা মন্তব্য: VM directory cleanup — pathlib.Path দিয়ে।"""
        try:
            if vm_dir.exists():
                shutil.rmtree(vm_dir)
            logger.debug(f"[MicroVMSandbox] VM dir destroyed: {vm_dir}")
        except Exception as exc:  # noqa: BLE001
            logger.warning(f"[MicroVMSandbox] Failed to destroy VM dir {vm_dir}: {exc}")

    async def health_check(self) -> dict[str, Any]:
        """বাংলা মন্তব্য: Health check — admin dashboard-এ expose করা যাবে।"""
        vm_runtime = self._check_microvm_available()
        return {
            "status": "ready" if vm_runtime else "unavailable",
            "provider": vm_runtime or "none",
            "auto_destroy": self.auto_destroy,
            "network_disabled": self.network_disabled,
            "sandbox_root": str(self.sandbox_root),
            "allow_fallback": self.allow_fallback,
        }


# ── Lazy Singleton ─────────────────────────────────────────────────────────────
# বাংলা মন্তব্য: Lazy singleton — import time-এ initialization নিষিদ্ধ।
# আগে: `sandbox = MicroVMSandbox()` import-এ execute হতো — cold start বাড়াতো।
# এখন: প্রথম ব্যবহারের সময় instantiate হবে।
_sandbox_instance: MicroVMSandbox | None = None


def get_sandbox() -> MicroVMSandbox:
    """বাংলা মন্তব্য: Lazy singleton factory — import সময়ে initialization নিষিদ্ধ।"""
    global _sandbox_instance
    if _sandbox_instance is None:
        _sandbox_instance = MicroVMSandbox()
    return _sandbox_instance


async def execute_code_securely(code: str, timeout: int = 30, language: str = "python") -> dict[str, Any]:
    """বাংলা মন্তব্য: Public API — sandbox validate করে code execute করে।"""
    return await get_sandbox().execute_async(code, timeout, language)


# বাংলা মন্তব্য: import asyncio উপরে সরানো হয়েছে।

```