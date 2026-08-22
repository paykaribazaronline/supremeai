import asyncio
import json
import sys
from pathlib import Path

# Ensure backend path is on sys.path
backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from services.memory_service import CascadeMemoryService


async def store_fixes():
    mem = CascadeMemoryService()
    fixes = [
        {
            "title": "Telemetry Serialization & Fail-Open Logging Fix",
            "file": "core/llm/telemetry.py",
            "pattern": "LLMCallRecord.to_log_line fails on non-JSON native objects or exception during logging hides valid LLM response",
            "fix": "Use json.dumps(asdict(self), default=str) and best-effort safe logging in finally block without re-raising.",
        },
        {
            "title": "SelfSovereignRouter Backward Compatibility",
            "file": "brain/smart_router.py",
            "pattern": "Missing complexity key in SelfSovereignRouter.route() returned dict breaks legacy consumers expecting complexity instead of tier",
            "fix": "Include both complexity and tier keys returning decision.complexity_tier.",
        },
        {
            "title": "Admin Dashboard Export Codebase Import Fix",
            "file": "api/routes/admin_dashboard.py",
            "pattern": "NameError on export_codebase_to_markdown in admin dashboard codebase export route",
            "fix": "Import export_codebase_to_markdown from tools.knowledge.codebase_exporter.",
        },
        {
            "title": "Traffic Monitor Logger Import in Graceful Degradation",
            "file": "api/routes/traffic_monitor.py",
            "pattern": "NameError on logger.warning when redis_manager.client is None",
            "fix": "Import logger from loguru at top of traffic_monitor.py.",
        },
        {
            "title": "Chaos Auditor Sandbox Check Fail-Closed Security Policy",
            "file": "workers/chaos_worker.py",
            "pattern": "fuzz_sandbox unavailable silently skipped sandbox check and marked deployment gate UNLOCKED (fail-open)",
            "fix": "Add else branch to fail-closed: record failure when fuzz_sandbox tooling is unavailable.",
        },
        {
            "title": "Circuit Breaker Test Isolation in Parallel CI Execution",
            "file": "tests/test_llm_gateway.py",
            "pattern": "CircuitBreakerManager is a process-wide singleton; tests intentionally tripping breakers in parallel worker leave state OPEN, skipping models in subsequent tests and causing false 'all models failed'",
            "fix": "Add an autouse pytest fixture reset_shared_circuit_breakers to reset CircuitBreakerManager state before and after each test in the file.",
        },
    ]

    for f in fixes:
        summary_text = (
            f"[BUG FIX PATTERN] {f['title']}\n"
            f"File: {f['file']}\n"
            f"Pattern: {f['pattern']}\n"
            f"Resolution: {f['fix']}\n"
            f"Category: CI / Runtime Self-Healing"
        )
        mem.store_memory(
            file_path=f["file"],
            content=f"Error Pattern: {f['pattern']}\nFix Applied: {f['fix']}",
            summary=summary_text,
            structure=json.dumps({"type": "bug_fix_pattern", "title": f["title"]}),
            session_id="ci-self-healing-matrix",
            agent_type="self_healer",
            task_type="bug_fix_pattern",
            metadata={"category": "ci_failure_recovery", "auto_fixable": True, "date": "2026-08-22"},
        )
        print(f"[OK] Stored fix pattern in ai_memory: {f['title']}")


if __name__ == "__main__":
    asyncio.run(store_fixes())
