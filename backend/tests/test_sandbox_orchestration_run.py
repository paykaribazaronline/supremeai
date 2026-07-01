from tools.cloud_sandbox_orchestrator import CloudSandboxOrchestrator

def test_sandbox_run_code_success():
    orchestrator = CloudSandboxOrchestrator()
    code = "x = 5\ny = 10\nprint(f'RESULT:{{\"val\": {x + y}}}')"
    res = orchestrator.run_code(code)
    
    assert res["success"] is True
    assert "val" in res["stdout"]
    assert res["exit_code"] == 0

def test_sandbox_run_code_syntax_error():
    orchestrator = CloudSandboxOrchestrator()
    code = "class MismatchedSyntax:\n    def execute(self, kwargs):\n        return }"
    res = orchestrator.run_code(code)
    
    assert res["success"] is False
    assert "Syntax" in res["stderr"] or "Violation" in res["stderr"]
    assert res["exit_code"] != 0

def test_sandbox_run_code_timeout():
    orchestrator = CloudSandboxOrchestrator()
    code = "import time\ntime.sleep(6)"
    res = orchestrator.run_code(code)
    
    assert res["success"] is False
    assert "Timeout" in res["stderr"] or res["exit_code"] == -1
