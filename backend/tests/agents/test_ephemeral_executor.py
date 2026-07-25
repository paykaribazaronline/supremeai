from unittest.mock import MagicMock, patch

from agents.ephemeral_executor import (EphemeralExecutor, ExecutionStatus,
                                       SecurityScanner)


def test_security_scanner_safe_code():
    scanner = SecurityScanner()
    code = "def main(payload):\n    return payload.get('value', 0) * 2"
    is_safe, violations = scanner.scan(code, "test_skill")
    assert is_safe is True
    assert len(violations) == 0


def test_security_scanner_forbidden_import():
    scanner = SecurityScanner()
    code = "import os\ndef main(payload):\n    os.system('rm -rf /')"
    is_safe, violations = scanner.scan(code, "test_skill")
    assert is_safe is False
    assert any("Forbidden import" in v for v in violations)


def test_security_scanner_forbidden_pattern():
    scanner = SecurityScanner()
    code = "def main(payload):\n    eval('1 + 1')"
    is_safe, violations = scanner.scan(code, "test_skill")
    assert is_safe is False
    assert any("Dangerous pattern" in v or "Dangerous call" in v for v in violations)


def test_validate_skill_id():
    executor = EphemeralExecutor(enable_security_scan=False)
    assert executor.validate_skill_id("valid_name_1")[0] is True
    assert executor.validate_skill_id("1_invalid_start")[0] is False
    assert executor.validate_skill_id("invalid/path/traversal")[0] is False
    assert executor.validate_skill_id("invalid..traversal")[0] is False


@patch("core.microvm_sandbox.MicroVMSandbox")
def test_execute_blocked_by_security(mock_sandbox_class, tmp_path):
    mock_sandbox = MagicMock()
    mock_sandbox_class.return_value = mock_sandbox
    executor = EphemeralExecutor(
        base_skills_dir=str(tmp_path), enable_security_scan=True
    )
    # Code containing forbidden import
    code = "import os\n"
    res = executor.execute_use_and_throw("test_skill", code, "{}")
    assert res.status == ExecutionStatus.BLOCKED
