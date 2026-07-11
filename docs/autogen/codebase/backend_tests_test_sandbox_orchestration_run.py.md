# 📄 ফাইল: backend/tests/test_sandbox_orchestration_run.py

**প্রকার:** .py  
**সাইজ:** 1,063 বাইট  
**আপডেট:** 2026-07-11T13:36:50.149020

---

## কোড

```py
import pytest
from tools.cloud_sandbox_orchestrator import CloudSandboxOrchestrator


@pytest.mark.asyncio
async def test_sandbox_run_code_success():
    orchestrator = CloudSandboxOrchestrator()
    code = 'python -c \'x = 5; y = 10; print(f"RESULT:{{\\"val\\": {x + y}}}")\''
    res = await orchestrator.run_command("sandbox-123", code)

    assert res["status"] == "COMPLETED"
    assert res["exitCode"] == 0


@pytest.mark.asyncio
async def test_sandbox_run_code_syntax_error():
    orchestrator = CloudSandboxOrchestrator()
    code = "python -c 'class MismatchedSyntax:\n    def execute(self, kwargs):\n        return }'"
    res = await orchestrator.run_command("sandbox-123", code)

    assert res["status"] == "COMPLETED"
    assert res["exitCode"] == 0


@pytest.mark.asyncio
async def test_sandbox_run_code_timeout():
    orchestrator = CloudSandboxOrchestrator()
    code = "python -c 'import time; time.sleep(6)'"
    res = await orchestrator.run_command("sandbox-123", code)

    assert res["status"] == "COMPLETED"
    assert res["exitCode"] == 0

```