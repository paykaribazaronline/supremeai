"""Tests for SecretHunter - Secret scanning and detection.

This module tests:
- SecretFinding: data structure for findings
- SecretReport: structured reporting
- GitleaksRunner: pattern-based secret detection
- AISecretAnalyzer: AI-powered secret analysis
- SecretHunter: main agent
"""

import tempfile
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from core.security.secret_hunter import (
    AISecretAnalyzer,
    GitleaksRunner,
    SecretFinding,
    SecretHunter,
    SecretReport,
)

# --- SecretFinding Tests ---


def test_secret_finding_creation():
    """Test creating a SecretFinding."""
    finding = SecretFinding(
        rule_id="aws-access-key",
        file_path="/app/config.py",
        line_number=10,
        column_start=5,
        column_end=25,
        matched_text="AKIAIOSFODNN7EXAMPLE",
        secret_type="AWS Access Key ID",
        severity="critical",
        remediation="Remove and rotate key",
        ai_confidence=0.95,
    )

    assert finding.rule_id == "aws-access-key"
    assert finding.severity == "critical"
    assert finding.ai_confidence == 0.95


def test_secret_report_to_dict():
    """Test SecretReport serialization."""
    finding = SecretFinding(
        rule_id="github-token",
        file_path="/app/config.py",
        line_number=10,
        column_start=5,
        column_end=50,
        matched_text="ghp_1234567890abcdef",
        secret_type="GitHub Token",
        severity="critical",
    )

    report = SecretReport(
        scan_id="test-scan-001",
        scanned_at="2024-01-01T00:00:00Z",
        total_files=100,
        findings=[finding],
        summary={
            "critical_count": 1,
            "high_count": 0,
            "ai_validated": True,
        },
    )

    result = report.to_dict()

    assert result["scan_id"] == "test-scan-001"
    assert result["total_files"] == 100
    assert result["findings_count"] == 1
    assert result["summary"]["critical_count"] == 1


# --- GitleaksRunner Tests ---


class TestGitleaksRunner:
    """Tests for GitleaksRunner class."""

    def test_compile_patterns(self):
        """Test that patterns are compiled correctly."""
        runner = GitleaksRunner()

        assert len(runner.compiled_patterns) > 0
        assert "aws-access-key" in runner.compiled_patterns
        assert "github-token" in runner.compiled_patterns

    def test_scan_file_with_secret(self):
        """Test scanning a file containing a secret."""
        runner = GitleaksRunner()

        with tempfile.NamedTemporaryFile(mode="w", suffix=".py", delete=False, encoding="utf-8") as f:
            f.write('api_key = "AKIAIOSFODNN7EXAMPLE"\n')
            f.flush()
            file_path = Path(f.name)

        try:
            findings = runner.scan_file(file_path)

            assert len(findings) >= 1
            aws_findings = [f for f in findings if f.rule_id == "aws-access-key"]
            assert len(aws_findings) >= 1
        finally:
            file_path.unlink()

    def test_scan_file_without_secrets(self):
        """Test scanning a file without secrets."""
        runner = GitleaksRunner()

        with tempfile.NamedTemporaryFile(mode="w", suffix=".py", delete=False, encoding="utf-8") as f:
            f.write('def hello():\n    print("hello world")\n')
            f.flush()
            file_path = Path(f.name)

        try:
            findings = runner.scan_file(file_path)

            assert len(findings) == 0
        finally:
            file_path.unlink()

    def test_scan_directory(self):
        """Test scanning a directory for secrets."""
        runner = GitleaksRunner()

        with tempfile.TemporaryDirectory() as tmpdir:
            # Create a file with a secret
            secret_file = Path(tmpdir) / "config.py"
            secret_file.write_text('password = "secret123"\n', encoding="utf-8")

            # Create a safe file
            safe_file = Path(tmpdir) / "main.py"
            safe_file.write_text('print("hello")\n', encoding="utf-8")

            findings = runner.scan_directory(Path(tmpdir))

            assert len(findings) >= 1

    def test_scan_directory_respects_extensions(self):
        """Test that directory scan respects file extensions."""
        runner = GitleaksRunner()

        with tempfile.TemporaryDirectory() as tmpdir:
            # Create a file with non-watched extension
            other_file = Path(tmpdir) / "data.bin"
            other_file.write_text("binary content with AKIAIOSFODNN7EXAMPLE", encoding="utf-8")

            findings = runner.scan_directory(Path(tmpdir), extensions={".py", ".js"})

            assert len(findings) == 0

    def test_scan_directory_skips_hidden_dirs(self):
        """Test that directory scan skips hidden directories."""
        runner = GitleaksRunner()

        with tempfile.TemporaryDirectory() as tmpdir:
            # Create a hidden directory
            hidden_dir = Path(tmpdir) / ".hidden"
            hidden_dir.mkdir()
            hidden_file = hidden_dir / "config.py"
            hidden_file.write_text('api_key = "AKIAIOSFODNN7EXAMPLE"\n', encoding="utf-8")

            findings = runner.scan_directory(Path(tmpdir))

            # Should not scan hidden directory
            assert len(findings) == 0


# --- AISecretAnalyzer Tests ---


class TestAISecretAnalyzer:
    """Tests for AISecretAnalyzer class."""

    @pytest.mark.asyncio
    async def test_analyze_finding_success(self):
        """Test successful AI analysis of a finding."""
        analyzer = AISecretAnalyzer()
        finding = SecretFinding(
            rule_id="test-pattern",
            file_path="/app/test.py",
            line_number=10,
            column_start=0,
            column_end=20,
            matched_text="test_secret_value",
            secret_type="Generic Secret",
            severity="high",
        )

        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[
            0
        ].message.content = '{"is_true_positive": true, "secret_type": "API Key", "severity": "critical", "confidence": 0.95, "remediation": "Remove the key"}'

        async def mock_acomplete(*args, **kwargs):
            return mock_response

        with patch(
            "core.security.secret_hunter.llm_gateway.acompletion",
            side_effect=mock_acomplete,
        ):
            result = await analyzer.analyze_finding(finding, "code context")

        assert result.severity == "critical"
        assert result.ai_confidence == 0.95

    @pytest.mark.asyncio
    async def test_analyze_finding_false_positive(self):
        """Test AI analysis marks false positive."""
        analyzer = AISecretAnalyzer()
        finding = SecretFinding(
            rule_id="test-pattern",
            file_path="/app/test.py",
            line_number=10,
            column_start=0,
            column_end=20,
            matched_text="fake_key_placeholder",
            secret_type="Generic Secret",
            severity="high",
        )

        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[
            0
        ].message.content = (
            '{"is_true_positive": false, "secret_type": "placeholder", "severity": "low", "confidence": 0.1}'
        )

        with patch(
            "core.security.secret_hunter.llm_gateway.acompletion",
            return_value=mock_response,
        ):
            result = await analyzer.analyze_finding(finding, "code context")

        assert result.severity == "info"


# --- SecretHunter Tests ---


class TestSecretHunter:
    """Tests for SecretHunter main class."""

    def test_init(self):
        """Test SecretHunter initialization."""
        hunter = SecretHunter()

        assert hunter.gitleaks is not None
        assert hunter.ai_analyzer is not None

    @pytest.mark.asyncio
    async def test_scan_codebase_nonexistent_directory(self):
        """Test scanning nonexistent directory raises error."""
        import tempfile

        hunter = SecretHunter()
        nonexistent_dir = Path(tempfile.gettempdir()) / "supremeai_nonexistent_test_dir_12345"
        # বাংলা মন্তব্য: নিশ্চিত করো ডিরেক্টরিটা আসলে নেই
        if nonexistent_dir.exists():
            nonexistent_dir.rmdir()

        with pytest.raises(FileNotFoundError):
            await hunter.scan_codebase(str(nonexistent_dir))

    @pytest.mark.asyncio
    async def test_scan_codebase_success(self):
        """Test successful codebase scan."""
        hunter = SecretHunter()

        with tempfile.TemporaryDirectory() as tmpdir:
            # Create a file with a secret
            secret_file = Path(tmpdir) / "config.py"
            secret_file.write_text('api_key = "ghp_test1234567890abcdef"\n', encoding="utf-8")

            with patch.object(hunter.ai_analyzer, "analyze_finding", new_callable=AsyncMock) as mock_analyze:
                # Make analyze_finding return the finding with info severity for testing
                mock_analyze.return_value = SecretFinding(
                    rule_id="github-token",
                    file_path=str(secret_file),
                    line_number=1,
                    column_start=0,
                    column_end=50,
                    matched_text="ghp_test1234567890abcdef",
                    secret_type="GitHub Token",
                    severity="high",
                )

                report = await hunter.scan_codebase(tmpdir, use_ai=True, min_severity="low")

        assert isinstance(report, SecretReport)
        assert report.total_files >= 1

    @pytest.mark.asyncio
    async def test_scan_codebase_without_ai(self):
        """Test codebase scan without AI validation."""
        hunter = SecretHunter()

        with tempfile.TemporaryDirectory() as tmpdir:
            secret_file = Path(tmpdir) / "config.py"
            secret_file.write_text('api_key = "ghp_test1234567890abcdef"\n', encoding="utf-8")

            report = await hunter.scan_codebase(tmpdir, use_ai=False, min_severity="low")

        assert isinstance(report, SecretReport)

    def test_generate_pre_commit_hook(self):
        """Test pre-commit hook generation."""
        hunter = SecretHunter()

        hook = hunter.generate_pre_commit_hook()

        assert "# SecretHunter Pre-Commit Hook" in hook
        assert "gitleaks" in hook or "secret" in hook.lower()
        assert "exit 1" in hook  # Should fail on secret detection

    def test_secret_finding_truncation(self):
        """Test that long matched text is truncated in report."""
        long_text = "a" * 100
        finding = SecretFinding(
            rule_id="test",
            file_path="/app/test.py",
            line_number=1,
            column_start=0,
            column_end=100,
            matched_text=long_text,
            secret_type="Test Type",
            severity="high",
        )

        report = SecretReport(
            scan_id="test",
            scanned_at="2024-01-01T00:00:00Z",
            findings=[finding],
        )

        result = report.to_dict()

        # Should be truncated to 50 chars + "..."
        assert len(result["findings"][0]["matched_text"]) == 53
        assert result["findings"][0]["matched_text"].endswith("...")
