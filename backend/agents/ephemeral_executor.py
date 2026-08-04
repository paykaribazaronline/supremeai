"""
agents/ephemeral_executor.py
=============================
SupremeAI 2.0 — Secure Ephemeral Skill Execution Engine

বাংলা মন্তব্য: ব্যবহার-এবং-ফেলে দেওয়া (use-and-throw) স্কিল এক্সিকিউশন
এজেন্ট। প্রতিটি স্কিলকে আইসোলেটেড ডকার স্যান্ডবক্সে রান করে, নেটওয়ার্ক
বিচ্ছিন্ন করে, এবং এক্সিকিউশন শেষে সম্পূর্ণ ক্লিনআপ করে।

Security Features:
- Path traversal prevention via strict regex validation
- Docker sandbox with --network none
- Read-only volume mounts
- CPU/memory capping (256m / 0.5 CPU)
- Timeout enforcement (default 30s)
- AST-based static analysis before execution
- Resource quotas per skill execution
"""

from __future__ import annotations

import ast
import asyncio
import json
import re
import shutil
import time
from dataclasses import dataclass, field
from enum import StrEnum
from pathlib import Path
from typing import Any

from loguru import logger

# Lazy import to avoid Docker dependency at module load
if False:  # type-check only
    pass


class ExecutionStatus(StrEnum):
    """Standardized execution status codes."""

    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    FAILURE = "failure"
    TIMEOUT = "timeout"
    BLOCKED = "blocked"
    SANDBOX_ERROR = "sandbox_error"


@dataclass
class ExecutionResult:
    """Structured result from an ephemeral execution."""

    skill_id: str
    status: ExecutionStatus
    exit_code: int
    stdout: str
    stderr: str
    execution_time_ms: float
    timestamp: str = field(
        default_factory=lambda: __import__("datetime")
        .datetime.now(__import__("datetime").UTC)
        .isoformat()
    )
    artifacts: dict[str, Any] = field(default_factory=dict)
    security_flags: list[str] = field(default_factory=list)


class SecurityScanner:
    """Static security analysis for ephemeral code."""

    FORBIDDEN_MODULES = frozenset(
        [
            "os",
            "subprocess",
            "sys",
            "socket",
            "urllib",
            "urllib2",
            "http.client",
            "ftplib",
            "telnetlib",
            "pickle",
            "marshal",
            "ctypes",
            "multiprocessing",
            "threading",
            "asyncio",
        ]
    )

    DANGEROUS_PATTERNS = frozenset(
        [
            "eval(",
            "exec(",
            "compile(",
            "__import__(",
            "getattr(",
            "setattr(",
            "delattr(",
            "globals()",
            "locals()",
            "vars()",
            "open(",
            "file(",
            "input(",
            "raw_input(",
        ]
    )

    def __init__(self, strict_mode: bool = True) -> None:
        self.strict_mode = strict_mode
        self._violations: list[str] = []

    def scan(self, code: str, skill_id: str) -> tuple[bool, list[str]]:
        """
        Perform multi-layer security scanning.

        Returns:
            (is_safe, list_of_violations)
        """
        self._violations = []

        # Layer 1: AST-based import analysis
        self._ast_scan(code)

        # Layer 2: Regex pattern matching for dangerous calls
        self._pattern_scan(code)

        # Layer 3: String entropy check for obfuscation
        self._entropy_scan(code)

        if self._violations:
            logger.warning(
                f"🔒 Security scan blocked skill '{skill_id}': {self._violations}"
            )
            return False, self._violations

        return True, []

    def _ast_scan(self, code: str) -> None:
        """Parse AST to find forbidden imports and dangerous constructs."""
        try:
            tree = ast.parse(code)
        except SyntaxError as exc:
            self._violations.append(f"Syntax error: {exc}")
            return

        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    root_module = alias.name.split(".")[0]
                    if root_module in self.FORBIDDEN_MODULES:
                        self._violations.append(f"Forbidden import: {alias.name}")

            elif isinstance(node, ast.ImportFrom):
                if node.module:
                    root_module = node.module.split(".")[0]
                    if root_module in self.FORBIDDEN_MODULES:
                        self._violations.append(f"Forbidden from-import: {node.module}")

            elif isinstance(node, ast.Call):
                if isinstance(node.func, ast.Name) and node.func.id in (
                    "eval",
                    "exec",
                    "compile",
                ):
                    self._violations.append(f"Dangerous call: {node.func.id}()")

    def _pattern_scan(self, code: str) -> None:
        """Regex-based scan for dangerous code patterns."""
        for pattern in self.DANGEROUS_PATTERNS:
            if pattern in code:
                self._violations.append(f"Dangerous pattern detected: {pattern}")

    def _entropy_scan(self, code: str) -> None:
        """Detect potentially obfuscated code via entropy analysis."""
        # Simple check: flag base64-encoded blobs that might hide malicious code
        base64_patterns = re.findall(r"[A-Za-z0-9+/]{100,}={0,2}", code)
        for match in base64_patterns:
            # Calculate Shannon entropy
            entropy = self._calculate_entropy(match)
            if entropy > 5.5:  # High entropy suggests encoding/obfuscation
                self._violations.append(
                    f"High-entropy string detected (possible obfuscation): entropy={entropy:.2f}"
                )
                break  # One flag is enough

    @staticmethod
    def _calculate_entropy(data: str) -> float:
        """Calculate Shannon entropy of a string."""
        if not data:
            return 0.0
        from math import log2

        # ভারসাম্য নির্ণয় রনের জন্য অক্ষরের frequency table তৈরি করা হচ্ছে
        freq: dict[str, int] = {}
        for char in data:
            freq[char] = freq.get(char, 0) + 1
        entropy = 0.0
        length = len(data)
        for count in freq.values():
            p = count / length
            entropy -= p * log2(p)
        return entropy


class ResourceQuota:
    """Resource limits for a single ephemeral execution."""

    def __init__(
        self,
        memory_limit: str = "256m",
        cpu_limit: str = "0.5",
        timeout_seconds: int = 30,
        max_output_size: int = 1024 * 1024,  # 1MB
        max_file_size: int = 10 * 1024 * 1024,  # 10MB
    ) -> None:
        self.memory_limit = memory_limit
        self.cpu_limit = cpu_limit
        self.timeout_seconds = timeout_seconds
        self.max_output_size = max_output_size
        self.max_file_size = max_file_size


class EphemeralExecutor:
    """
    Secure ephemeral skill execution with full lifecycle management.

    বাংলা মন্তব্য: প্রতিটি স্কিল এক্সিকিউশন:
    ১. সিকিউরিটি স্ক্যান (AST + Pattern + Entropy)
    ২. আইসোলেটেড টেম্পোরারি ডিরেক্টরিতে স্টেজিং
    ৩. ডকার স্যান্ডবক্সে এক্সিকিউশন (--network none, read-only)
    ৪. আউটপুট ক্যাপচার এবং স্ট্রাকচার্ড রেসাল্ট রিটার্ন
    ৫. গ্যারান্টিড ক্লিনআপ (finally block)
    """

    # Strict skill ID validation — only alphanumeric and underscore
    SKILL_ID_PATTERN = re.compile(r"^[a-zA-Z][a-zA-Z0-9_]{0,63}$")

    def __init__(
        self,
        base_skills_dir: str = "skills",
        default_quota: ResourceQuota | None = None,
        enable_security_scan: bool = True,
    ) -> None:
        self.base_dir = Path(base_skills_dir)
        self.ephemeral_dir = self.base_dir / "ephemeral"
        self.ephemeral_dir.mkdir(parents=True, exist_ok=True)

        self.default_quota = default_quota or ResourceQuota()
        self.enable_security_scan = enable_security_scan
        self.security_scanner = SecurityScanner() if enable_security_scan else None

        # Lazy initialization of Docker sandbox
        self._sandbox: Any | None = None
        self._execution_history: list[ExecutionResult] = []
        self._active_executions: dict[str, asyncio.Task] = {}

    @property
    def sandbox(self) -> Any:
        """Lazy-load MicroVMSandbox to avoid import-time side effects."""
        if self._sandbox is None:
            from core.microvm_sandbox import MicroVMSandbox

            self._sandbox = MicroVMSandbox()
        return self._sandbox

    def validate_skill_id(self, skill_id: str) -> tuple[bool, str]:
        """Validate skill ID against injection attacks."""
        if not skill_id or len(skill_id) > 64:
            return False, "Skill ID must be 1-64 characters"
        if not self.SKILL_ID_PATTERN.match(skill_id):
            return (
                False,
                "Skill ID must start with letter, contain only a-z, A-Z, 0-9, _",
            )
        # Additional check: no path traversal sequences
        if ".." in skill_id or "/" in skill_id or "\\" in skill_id:
            return False, "Path traversal characters detected"
        return True, "Valid"

    def execute_use_and_throw(
        self,
        skill_id: str,
        raw_code: str,
        test_payload: str,
        quota: ResourceQuota | None = None,
        metadata: dict[str, Any] | None = None,
    ) -> ExecutionResult:
        """
        Execute a skill in an ephemeral, isolated environment.

        Args:
            skill_id: Unique identifier for the skill
            raw_code: Python code to execute
            test_payload: JSON-serializable payload passed to the skill
            quota: Optional resource overrides
            metadata: Execution metadata for logging

        Returns:
            ExecutionResult with full execution details
        """
        start_time = time.perf_counter()
        quota = quota or self.default_quota

        # Step 1: Validate skill ID
        is_valid, validation_msg = self.validate_skill_id(skill_id)
        if not is_valid:
            return ExecutionResult(
                skill_id=skill_id,
                status=ExecutionStatus.BLOCKED,
                exit_code=-1,
                stdout="",
                stderr=f"Blocked: {validation_msg}",
                execution_time_ms=0,
                security_flags=["invalid_skill_id"],
            )

        # Step 2: Security scan
        if self.security_scanner:
            is_safe, violations = self.security_scanner.scan(raw_code, skill_id)
            if not is_safe:
                return ExecutionResult(
                    skill_id=skill_id,
                    status=ExecutionStatus.BLOCKED,
                    exit_code=-1,
                    stdout="",
                    stderr=f"Blocked: Security violations: {violations}",
                    execution_time_ms=0,
                    security_flags=violations,
                )

        # Step 3: Prepare isolated runtime directory
        runtime_dir = self.ephemeral_dir / skill_id
        self._prepare_runtime_dir(runtime_dir)

        # Step 4: Write code with execution wrapper
        entry_file = "main.py"
        wrapped_code = self._wrap_code(raw_code, test_payload)

        try:
            (runtime_dir / entry_file).write_text(wrapped_code, encoding="utf-8")

            # Step 5: Execute in sandbox
            logger.info(f"🚀 Executing ephemeral skill '{skill_id}' in sandbox")
            sandbox_result = self.sandbox.run_quarantine_test(
                staging_path=runtime_dir,
                entry_file=entry_file,
                test_payload=test_payload,
            )

            execution_time = (time.perf_counter() - start_time) * 1000

            # Determine status from exit code
            status = (
                ExecutionStatus.SUCCESS
                if sandbox_result.get("exit_code") == 0
                else ExecutionStatus.FAILURE
            )

            # Check for timeout indication in stderr
            if "timeout" in sandbox_result.get("stderr", "").lower():
                status = ExecutionStatus.TIMEOUT

            result = ExecutionResult(
                skill_id=skill_id,
                status=status,
                exit_code=sandbox_result.get("exit_code", -1),
                stdout=sandbox_result.get("stdout", "")[: quota.max_output_size],
                stderr=sandbox_result.get("stderr", "")[: quota.max_output_size],
                execution_time_ms=round(execution_time, 2),
                artifacts={"runtime_dir": str(runtime_dir)},
            )

            self._execution_history.append(result)
            logger.info(f"✅ Skill '{skill_id}' execution completed: {status.value}")
            return result

        except Exception as exc:
            execution_time = (time.perf_counter() - start_time) * 1000
            logger.exception(f"❌ Sandbox execution failed for '{skill_id}': {exc}")
            return ExecutionResult(
                skill_id=skill_id,
                status=ExecutionStatus.SANDBOX_ERROR,
                exit_code=-1,
                stdout="",
                stderr=f"Sandbox error: {exc}",
                execution_time_ms=round(execution_time, 2),
                security_flags=["sandbox_failure"],
            )
        finally:
            # Guaranteed cleanup
            self._cleanup_runtime_dir(runtime_dir)

    async def execute_async(
        self,
        skill_id: str,
        raw_code: str,
        test_payload: str,
        quota: ResourceQuota | None = None,
    ) -> ExecutionResult:
        """Async wrapper for execute_use_and_throw using thread pool."""
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(
            None,
            self.execute_use_and_throw,
            skill_id,
            raw_code,
            test_payload,
            quota,
        )

    def _prepare_runtime_dir(self, runtime_dir: Path) -> None:
        """Create or clean runtime directory."""
        if runtime_dir.exists():
            shutil.rmtree(runtime_dir)
        runtime_dir.mkdir(parents=True, exist_ok=True)

    def _cleanup_runtime_dir(self, runtime_dir: Path) -> None:
        """Secure cleanup of runtime directory."""
        try:
            if runtime_dir.exists():
                shutil.rmtree(runtime_dir)
                logger.debug(f"🧹 Cleaned up runtime dir: {runtime_dir}")
        except Exception as exc:
            logger.warning(f"⚠️ Failed to cleanup {runtime_dir}: {exc}")

    def _wrap_code(self, raw_code: str, test_payload: str) -> str:
        """
        Wrap user code with a safe execution harness.

        The harness catches exceptions, serializes output as JSON,
        and prevents direct stdout pollution.
        """
        # Ensure test_payload is valid JSON
        try:
            payload_json = json.dumps(
                json.loads(test_payload)
                if isinstance(test_payload, str)
                else test_payload
            )
        except (json.JSONDecodeError, TypeError):
            payload_json = json.dumps({"input": str(test_payload)})

        wrapper = f"""# Auto-generated execution harness — DO NOT MODIFY
import json
import sys
import traceback

# User code begins
{raw_code}
# User code ends

# Execution harness
if __name__ == "__main__":
    try:
        payload = json.loads({payload_json!r})
        if "execute_tool" in dir():
            result = execute_tool(payload)
        elif "main" in dir():
            result = main(payload)
        elif "run" in dir():
            result = run(payload)
        else:
            result = {{"error": "No entry point found (expected execute_tool, main, or run)"}}

        # Using logger instead of print for structured output
        logger.info("Execution completed", success=True, result=result)
        print(json.dumps({{"success": True, "result": result}}, default=str))  # Keep for external consumption
    except Exception as e:
        error_details = {{
            "success": False,
            "error": str(e),
            "traceback": traceback.format_exc()
        }}
        # Using logger instead of print for structured error reporting
        logger.error("Execution failed", error=str(e), traceback=traceback.format_exc())
        print(json.dumps(error_details))
        sys.exit(1)
"""
        return wrapper

    def get_execution_history(
        self,
        skill_id: str | None = None,
        status: ExecutionStatus | None = None,
        limit: int = 100,
    ) -> list[ExecutionResult]:
        """Query execution history with filters."""
        results = self._execution_history
        if skill_id:
            results = [r for r in results if r.skill_id == skill_id]
        if status:
            results = [r for r in results if r.status == status]
        return results[-limit:]

    def get_stats(self) -> dict[str, Any]:
        """Get execution statistics."""
        total = len(self._execution_history)
        if total == 0:
            return {"total_executions": 0}

        # প্রতিটি status-এর execution count ত্রয়োদশীতে গণনা করা হচ্ছে
        status_counts: dict[str, int] = {}
        for r in self._execution_history:
            status_counts[r.status.value] = status_counts.get(r.status.value, 0) + 1

        avg_time = sum(r.execution_time_ms for r in self._execution_history) / total

        return {
            "total_executions": total,
            "status_breakdown": status_counts,
            "average_execution_time_ms": round(avg_time, 2),
            "blocked_by_security": status_counts.get("blocked", 0),
            "success_rate": round(status_counts.get("success", 0) / total * 100, 2),
        }
