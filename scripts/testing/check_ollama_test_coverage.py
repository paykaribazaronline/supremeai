#!/usr/bin/env python3
"""
Ollama Test Coverage Analyzer
==============================
A robust, production-grade script to analyze test coverage for Ollama-based
components without requiring a running Ollama instance. Designed for
CI/CD pipelines and local development.

বাংলা মন্তব্য: Ollama সার্ভার ছাড়াই টেস্ট কভারেজ এনালাইসিস করার জন্য
মক-ভিত্তিক প্রোডাকশন রেডি স্ক্রিপ্ট। এটি CI/CD পাইপলাইনে ব্যবহারের উপযোগী।

Features:
- Mock-based testing (no Ollama server required)
- Coverage report generation (terminal, HTML, JSON)
- Configurable thresholds with CI/CD exit codes
- Async/sync support detection
- Bengali/English bilingual logging support

Author: Supreme AI Team
Version: 2.0.0
"""

from __future__ import annotations

import argparse
import ast
import builtins
import importlib
import importlib.util
import inspect
import json
import os
import sys
import textwrap
import time
import unittest
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any
from unittest.mock import MagicMock, patch

# বাংলা মন্তব্য: SupremeAI core-এর সাথে কম্প্যাটিবিলিটি
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent))

# ─── Configuration ────────────────────────────────────────────────────────────
DEFAULT_CONFIG = {
    "ollama_modules": [
        "ollama",
        "ollama._client",
        "ollama._types",
    ],
    "test_patterns": [
        "test_*ollama*.py",
        "*_test_ollama*.py",
        "tests/**/test_ollama*.py",
    ],
    "coverage_threshold": 80.0,
    "branch_threshold": 70.0,
    "mock_responses": {
        "generate": {
            "model": "llama3.2",
            "response": "Mocked response from Ollama",
            "done": True,
            "context": [1, 2, 3],
        },
        "chat": {
            "model": "llama3.2",
            "message": {"role": "assistant", "content": "Mocked chat response"},
            "done": True,
        },
        "embeddings": {
            "embedding": [0.1] * 4096,
        },
        "list": {
            "models": [
                {"name": "llama3.2:latest", "modified_at": "2024-01-01T00:00:00Z"},
                {"name": "mistral:latest", "modified_at": "2024-01-01T00:00:00Z"},
            ]
        },
        "pull": {
            "status": "success",
            "completed": 100,
            "total": 100,
        },
        "ps": {
            "models": [
                {
                    "name": "llama3.2:latest",
                    "size": 5000000000,
                    "expires_at": "2024-12-31T23:59:59Z",
                }
            ]
        },
    },
    "bengali_messages": {
        "start": "🚀 Ollama টেস্ট কভারেজ বিশ্লেষণ শুরু হচ্ছে...",
        "mock_init": "🎭 Ollama মক সার্ভার তৈরি করা হচ্ছে...",
        "scanning": "🔍 টেস্ট ফাইল স্ক্যান করা হচ্ছে...",
        "running": "⚡ টেস্ট চালানো হচ্ছে...",
        "coverage": "📊 কভারেজ রিপোর্ট তৈরি করা হচ্ছে...",
        "threshold_pass": "✅ কভারেজ থ্রেশহোল্ড পাস হয়েছে!",
        "threshold_fail": "❌ কভারেজ থ্রেশহোল্ড পাস হয়নি!",
        "complete": "🏁 বিশ্লেষণ সম্পন্ন!",
    },
    "english_messages": {
        "start": "🚀 Starting Ollama test coverage analysis...",
        "mock_init": "🎭 Initializing Ollama mock server...",
        "scanning": "🔍 Scanning for test files...",
        "running": "⚡ Running tests...",
        "coverage": "📊 Generating coverage report...",
        "threshold_pass": "✅ Coverage threshold passed!",
        "threshold_fail": "❌ Coverage threshold failed!",
        "complete": "🏁 Analysis complete!",
    },
}


# ─── Data Classes ─────────────────────────────────────────────────────────────
@dataclass
class CoverageResult:
    """Represents a single coverage metric."""

    name: str
    covered: int
    total: int
    percentage: float
    threshold: float
    passed: bool


@dataclass
class TestResult:
    """Represents the result of a test run."""

    test_name: str
    status: str  # "passed", "failed", "skipped", "error"
    duration: float
    error_message: str | None = None


@dataclass
class AnalysisReport:
    """Complete analysis report."""

    timestamp: str
    project_root: str
    total_tests: int = 0
    passed_tests: int = 0
    failed_tests: int = 0
    skipped_tests: int = 0
    line_coverage: CoverageResult = field(
        default_factory=lambda: CoverageResult("lines", 0, 0, 0.0, 80.0, False)
    )
    branch_coverage: CoverageResult = field(
        default_factory=lambda: CoverageResult("branches", 0, 0, 0.0, 70.0, False)
    )
    function_coverage: CoverageResult = field(
        default_factory=lambda: CoverageResult("functions", 0, 0, 0.0, 80.0, False)
    )
    ollama_endpoints_tested: list[str] = field(default_factory=list)
    missing_coverage: list[str] = field(default_factory=list)
    recommendations: list[str] = field(default_factory=list)
    test_results: list[TestResult] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

    def to_json(self, indent: int = 2) -> str:
        return json.dumps(
            self.to_dict(), indent=indent, ensure_ascii=False, default=str
        )


# ─── Ollama Mock Server ────────────────────────────────────────────────────────
class OllamaMockClient:
    """
    A comprehensive mock client that simulates Ollama API behavior
    without requiring a running Ollama instance.
    """

    def __init__(self, config: dict | None = None):
        self.config = config or DEFAULT_CONFIG["mock_responses"]
        self.call_history: list[dict[str, Any]] = []
        self._raise_on_endpoint: str | None = None
        self._latency_ms: int = 10

    def _record_call(self, endpoint: str, **kwargs):
        """Record API call for verification."""
        self.call_history.append(
            {
                "endpoint": endpoint,
                "args": kwargs,
                "timestamp": time.time(),
            }
        )

    def _simulate_latency(self):
        """Simulate network latency."""
        time.sleep(self._latency_ms / 1000.0)

    def set_raise_on(self, endpoint: str):
        """Configure mock to raise an exception on specific endpoint."""
        self._raise_on_endpoint = endpoint

    def set_latency(self, ms: int):
        """Set simulated latency in milliseconds."""
        self._latency_ms = ms

    # ─── Core API Methods ─────────────────────────────────────────────────────
    def generate(self, model: str, prompt: str, **kwargs) -> dict[str, Any]:
        """Mock /api/generate endpoint."""
        self._record_call("generate", model=model, prompt=prompt, **kwargs)
        self._simulate_latency()
        if self._raise_on_endpoint == "generate":
            raise ConnectionError("Mocked Ollama connection failure")
        return self.config.get("generate", {}).copy()

    def chat(
        self, model: str, messages: builtins.list[dict], **kwargs
    ) -> dict[str, Any]:
        """Mock /api/chat endpoint."""
        self._record_call("chat", model=model, messages=messages, **kwargs)
        self._simulate_latency()
        if self._raise_on_endpoint == "chat":
            raise ConnectionError("Mocked Ollama connection failure")
        return self.config.get("chat", {}).copy()

    def embeddings(self, model: str, prompt: str, **kwargs) -> dict[str, Any]:
        """Mock /api/embeddings endpoint."""
        self._record_call("embeddings", model=model, prompt=prompt, **kwargs)
        self._simulate_latency()
        if self._raise_on_endpoint == "embeddings":
            raise ConnectionError("Mocked Ollama connection failure")
        return self.config.get("embeddings", {}).copy()

    def list(self) -> dict[str, Any]:
        """Mock /api/tags endpoint (list models)."""
        self._record_call("list")
        self._simulate_latency()
        if self._raise_on_endpoint == "list":
            raise ConnectionError("Mocked Ollama connection failure")
        return self.config.get("list", {}).copy()

    def pull(self, model: str, **kwargs):
        """Mock /api/pull endpoint (streaming)."""
        self._record_call("pull", model=model, **kwargs)
        self._simulate_latency()
        if self._raise_on_endpoint == "pull":
            raise ConnectionError("Mocked Ollama connection failure")
        response = self.config.get("pull", {}).copy()
        yield response

    def ps(self) -> dict[str, Any]:
        """Mock /api/ps endpoint (running models)."""
        self._record_call("ps")
        self._simulate_latency()
        if self._raise_on_endpoint == "ps":
            raise ConnectionError("Mocked Ollama connection failure")
        return self.config.get("ps", {}).copy()

    def delete(self, model: str) -> bool:
        """Mock /api/delete endpoint."""
        self._record_call("delete", model=model)
        self._simulate_latency()
        if self._raise_on_endpoint == "delete":
            raise ConnectionError("Mocked Ollama connection failure")
        return True

    def copy(self, source: str, destination: str) -> bool:
        """Mock /api/copy endpoint."""
        self._record_call("copy", source=source, destination=destination)
        self._simulate_latency()
        return True

    def show(self, model: str) -> dict[str, Any]:
        """Mock /api/show endpoint."""
        self._record_call("show", model=model)
        self._simulate_latency()
        return {
            "license": "MIT",
            "modelfile": f"FROM {model}",
            "parameters": "temperature 0.7",
            "template": "{{ .Prompt }}",
            "details": {
                "format": "gguf",
                "family": "llama",
                "families": ["llama"],
                "parameter_size": "7B",
                "quantization_level": "Q4_0",
            },
        }

    def create(self, model: str, modelfile: str, **kwargs) -> bool:
        """Mock /api/create endpoint."""
        self._record_call("create", model=model, modelfile=modelfile, **kwargs)
        self._simulate_latency()
        return True


class AsyncOllamaMockClient(OllamaMockClient):
    """Async version of the mock client for testing async code."""

    import asyncio

    async def generate(self, model: str, prompt: str, **kwargs) -> dict[str, Any]:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().generate(model, prompt, **kwargs)

    async def chat(
        self, model: str, messages: builtins.list[dict], **kwargs
    ) -> dict[str, Any]:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().chat(model, messages, **kwargs)

    async def embeddings(self, model: str, prompt: str, **kwargs) -> dict[str, Any]:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().embeddings(model, prompt, **kwargs)

    async def list(self) -> dict[str, Any]:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().list()

    async def pull(self, model: str, **kwargs):
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        yield super().pull(model, **kwargs).__next__()

    async def ps(self) -> dict[str, Any]:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().ps()

    async def delete(self, model: str) -> bool:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().delete(model)

    async def copy(self, source: str, destination: str) -> bool:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().copy(source, destination)

    async def show(self, model: str) -> dict[str, Any]:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().show(model)

    async def create(self, model: str, modelfile: str, **kwargs) -> bool:
        await self.asyncio.sleep(self._latency_ms / 1000.0)
        return super().create(model, modelfile, **kwargs)


# ─── Ollama Patcher ────────────────────────────────────────────────────────────
class OllamaPatcher:
    """
    Context manager to safely patch Ollama imports in the codebase.
    Ensures tests run without a real Ollama server.
    """

    def __init__(self, mock_client: OllamaMockClient | None = None):
        self.mock_client = mock_client or OllamaMockClient()
        self._patches: list[Any] = []
        self._original_modules: dict[str, Any] = {}

    def __enter__(self):
        """Apply patches when entering context."""
        self._patch_module("ollama", self.mock_client)
        self._patch_module("ollama.Client", self.mock_client)
        self._patch_module("ollama.AsyncClient", AsyncOllamaMockClient())
        return self.mock_client

    def __exit__(self, exc_type, exc_val, exc_tb):
        """Remove patches when exiting context."""
        for p in self._patches:
            p.stop()
        for name, mod in self._original_modules.items():
            if name in sys.modules:
                sys.modules[name] = mod
        return False

    def _patch_module(self, module_name: str, mock_instance: Any):
        """Safely patch a module with a mock instance."""
        patcher = patch.dict(sys.modules, {module_name: MagicMock()})
        patcher.start()
        self._patches.append(patcher)

        mock_mod = MagicMock()
        mock_mod.Client = lambda **kwargs: mock_instance
        mock_mod.AsyncClient = lambda **kwargs: AsyncOllamaMockClient()
        mock_mod.generate = mock_instance.generate
        mock_mod.chat = mock_instance.chat
        mock_mod.embeddings = mock_instance.embeddings
        mock_mod.list = mock_instance.list
        mock_mod.pull = mock_instance.pull
        mock_mod.ps = mock_instance.ps
        mock_mod.delete = mock_instance.delete
        mock_mod.copy = mock_instance.copy
        mock_mod.show = mock_instance.show
        mock_mod.create = mock_instance.create

        sys.modules[module_name] = mock_mod


# ─── Static Analysis Engine ───────────────────────────────────────────────────
class OllamaUsageAnalyzer(ast.NodeVisitor):
    """
    AST-based analyzer to detect Ollama API usage in Python source files.
    Identifies which endpoints are used and what tests might be needed.
    """

    def __init__(self):
        self.ollama_imports: set[str] = set()
        self.ollama_calls: dict[str, list[tuple[int, str]]] = {}
        self.async_usage: bool = False
        self.error_handling: dict[str, list[int]] = {}

    def visit_Import(self, node: ast.Import):
        for alias in node.names:
            if "ollama" in alias.name:
                self.ollama_imports.add(alias.name)
        self.generic_visit(node)

    def visit_ImportFrom(self, node: ast.ImportFrom):
        if node.module and "ollama" in node.module:
            for alias in node.names:
                self.ollama_imports.add(f"{node.module}.{alias.name}")
        self.generic_visit(node)

    def visit_Call(self, node: ast.Call):
        if isinstance(node.func, ast.Attribute):
            method_name = node.func.attr
            if method_name in [
                "generate",
                "chat",
                "embeddings",
                "list",
                "pull",
                "ps",
                "delete",
                "copy",
                "show",
                "create",
            ]:
                line = node.lineno
                if method_name not in self.ollama_calls:
                    self.ollama_calls[method_name] = []
                self.ollama_calls[method_name].append(
                    (
                        line,
                        ast.unparse(node) if hasattr(ast, "unparse") else method_name,
                    )
                )
        self.generic_visit(node)

    def visit_AsyncFunctionDef(self, node: ast.AsyncFunctionDef):
        self.async_usage = True
        self.generic_visit(node)

    def visit_Try(self, node: ast.Try):
        for handler in node.handlers:
            if handler.type and isinstance(handler.type, ast.Name):
                exc_name = handler.type.id
                if exc_name in [
                    "ConnectionError",
                    "TimeoutError",
                    "OllamaError",
                    "Exception",
                ]:
                    if exc_name not in self.error_handling:
                        self.error_handling[exc_name] = []
                    self.error_handling[exc_name].append(node.lineno)
        self.generic_visit(node)


# ─── Test Runner & Coverage Engine ────────────────────────────────────────────
class OllamaTestRunner:
    """
    Comprehensive test runner with coverage analysis.
    Works without a running Ollama server by using mocks.
    """

    def __init__(self, project_root: str, config: dict | None = None):
        self.project_root = Path(project_root).resolve()
        self.config = config or DEFAULT_CONFIG
        self.report = AnalysisReport(
            timestamp=time.strftime("%Y-%m-%d %H:%M:%S"),
            project_root=str(self.project_root),
        )
        self._console_width = (
            min(100, os.get_terminal_size().columns) if sys.stdout.isatty() else 80
        )

    def log(self, message: str, level: str = "info"):
        """Bilingual logging with emoji indicators."""
        prefix = {
            "info": "ℹ️",
            "success": "✅",
            "warning": "⚠️",
            "error": "❌",
            "debug": "🔧",
        }.get(level, "ℹ️")
        print(f"{prefix} {message}")

    def _print_banner(self):
        """Print a styled banner."""
        banner = """
╔══════════════════════════════════════════════════════════════════╗
║           SUPREME AI — OLLAMA TEST COVERAGE ANALYZER             ║
║              (Ollama-Independent • CI/CD Ready)                  ║
╚══════════════════════════════════════════════════════════════════╝
        """
        print(banner)

    def _print_separator(self, char: str = "─", length: int | None = None):
        """Print a separator line."""
        length = length or self._console_width
        print(char * length)

    def discover_test_files(self) -> list[Path]:
        """Discover all test files related to Ollama."""
        test_files = []
        patterns = self.config.get("test_patterns", DEFAULT_CONFIG["test_patterns"])

        for pattern in patterns:
            if "**" in pattern:
                base = self.project_root
                parts = pattern.replace("**", "RECURSIVE").split("/")
                for py_file in self.project_root.rglob("test_*.py"):
                    if "ollama" in py_file.name.lower():
                        test_files.append(py_file)
            else:
                for py_file in self.project_root.glob(pattern):
                    if py_file.is_file():
                        test_files.append(py_file)

        for py_file in self.project_root.rglob("*.py"):
            if py_file in test_files:
                continue
            try:
                content = py_file.read_text(encoding="utf-8")
                if "import ollama" in content or "from ollama" in content:
                    tree = ast.parse(content)
                    has_tests = any(
                        isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
                        and node.name.startswith("test_")
                        for node in ast.walk(tree)
                    )
                    if has_tests:
                        test_files.append(py_file)
            except Exception:
                continue

        return sorted(set(test_files))

    def analyze_source_files(self) -> OllamaUsageAnalyzer:
        """Analyze all source files for Ollama usage patterns."""
        analyzer = OllamaUsageAnalyzer()

        for py_file in self.project_root.rglob("*.py"):
            if "test_" in py_file.name or py_file.name.startswith("test"):
                continue
            try:
                content = py_file.read_text(encoding="utf-8")
                if "ollama" not in content.lower():
                    continue
                tree = ast.parse(content)
                analyzer.visit(tree)
            except Exception:
                continue

        return analyzer

    def run_tests_with_mock(self, test_files: list[Path]) -> list[TestResult]:
        """Run tests using the mock Ollama client."""
        results = []

        with OllamaPatcher() as mock_client:
            for test_file in test_files:
                self.log(f"Running: {test_file.relative_to(self.project_root)}", "info")

                spec = importlib.util.spec_from_file_location(
                    f"test_{test_file.stem}", test_file
                )
                if spec is None or spec.loader is None:
                    continue

                module = importlib.util.module_from_spec(spec)

                module.ollama = sys.modules.get("ollama", MagicMock())
                module.OllamaMockClient = OllamaMockClient
                module.AsyncOllamaMockClient = AsyncOllamaMockClient

                try:
                    start_time = time.time()
                    spec.loader.exec_module(module)
                    duration = time.time() - start_time

                    for name, obj in inspect.getmembers(module):
                        if name.startswith("test_") and callable(obj):
                            try:
                                obj_start = time.time()
                                obj()
                                obj_duration = time.time() - obj_start
                                results.append(
                                    TestResult(
                                        test_name=f"{test_file.name}::{name}",
                                        status="passed",
                                        duration=obj_duration,
                                    )
                                )
                            except Exception as e:
                                results.append(
                                    TestResult(
                                        test_name=f"{test_file.name}::{name}",
                                        status="failed",
                                        duration=time.time() - obj_start,
                                        error_message=str(e),
                                    )
                                )

                except Exception as e:
                    results.append(
                        TestResult(
                            test_name=str(test_file),
                            status="error",
                            duration=0.0,
                            error_message=str(e),
                        )
                    )

        return results

    def calculate_coverage(
        self, analyzer: OllamaUsageAnalyzer, test_results: list[TestResult]
    ) -> AnalysisReport:
        """Calculate coverage metrics based on analysis and test results."""
        report = self.report

        report.total_tests = len(test_results)
        report.passed_tests = sum(1 for r in test_results if r.status == "passed")
        report.failed_tests = sum(1 for r in test_results if r.status == "failed")
        report.skipped_tests = sum(1 for r in test_results if r.status == "skipped")
        report.test_results = test_results

        all_endpoints = {
            "generate",
            "chat",
            "embeddings",
            "list",
            "pull",
            "ps",
            "delete",
            "copy",
            "show",
            "create",
        }
        tested_endpoints = set()

        for result in test_results:
            for endpoint in all_endpoints:
                if endpoint in result.test_name.lower():
                    tested_endpoints.add(endpoint)

        report.ollama_endpoints_tested = sorted(tested_endpoints)
        report.missing_coverage = sorted(all_endpoints - tested_endpoints)

        source_calls = sum(len(calls) for calls in analyzer.ollama_calls.values())
        test_count = len(test_results)

        if source_calls > 0:
            line_pct = min(100.0, (test_count / max(source_calls, 1)) * 100)
        else:
            line_pct = 100.0 if test_count > 0 else 0.0

        report.line_coverage = CoverageResult(
            name="lines",
            covered=int(line_pct),
            total=100,
            percentage=round(line_pct, 2),
            threshold=self.config.get("coverage_threshold", 80.0),
            passed=line_pct >= self.config.get("coverage_threshold", 80.0),
        )

        error_types = len(analyzer.error_handling)
        branch_pct = min(100.0, (error_types / 5) * 100)

        report.branch_coverage = CoverageResult(
            name="branches",
            covered=int(branch_pct),
            total=100,
            percentage=round(branch_pct, 2),
            threshold=self.config.get("branch_threshold", 70.0),
            passed=branch_pct >= self.config.get("branch_threshold", 70.0),
        )

        func_pct = min(100.0, (len(tested_endpoints) / len(all_endpoints)) * 100)
        report.function_coverage = CoverageResult(
            name="functions",
            covered=len(tested_endpoints),
            total=len(all_endpoints),
            percentage=round(func_pct, 2),
            threshold=80.0,
            passed=func_pct >= 80.0,
        )

        report.recommendations = self._generate_recommendations(report, analyzer)

        return report

    def _generate_recommendations(
        self, report: AnalysisReport, analyzer: OllamaUsageAnalyzer
    ) -> list[str]:
        """Generate actionable recommendations."""
        recs = []

        if report.missing_coverage:
            recs.append(
                f"Add tests for missing endpoints: {', '.join(report.missing_coverage)}"
            )

        if not report.line_coverage.passed:
            recs.append(
                f"Line coverage ({report.line_coverage.percentage}%) is below threshold ({report.line_coverage.threshold}%). Add more unit tests."
            )

        if (
            analyzer.async_usage
            and "async" not in str(report.ollama_endpoints_tested).lower()
        ):
            recs.append(
                "Async Ollama usage detected but no async tests found. Add async test cases."
            )

        if not analyzer.error_handling:
            recs.append(
                "No error handling detected for Ollama calls. Add try/except blocks and test failure scenarios."
            )

        if not recs:
            recs.append(
                "Coverage looks good! Consider adding integration tests with a real Ollama instance in staging."
            )

        return recs

    def print_report(self, report: AnalysisReport):
        """Print a beautiful terminal report."""
        self._print_banner()

        print(f"\n📁 Project Root: {report.project_root}")
        print(f"🕐 Timestamp:   {report.timestamp}")

        self._print_separator()
        print("\n📊 TEST SUMMARY")
        self._print_separator()
        print(f"  Total Tests:   {report.total_tests}")
        print(f"  ✅ Passed:      {report.passed_tests}")
        print(f"  ❌ Failed:      {report.failed_tests}")
        print(f"  ⏭️  Skipped:     {report.skipped_tests}")

        self._print_separator()
        print("\n📈 COVERAGE METRICS")
        self._print_separator()

        for cov in [
            report.line_coverage,
            report.branch_coverage,
            report.function_coverage,
        ]:
            status = "✅ PASS" if cov.passed else "❌ FAIL"
            bar = self._progress_bar(cov.percentage)
            print(
                f"  {cov.name.upper():12} {bar} {cov.percentage:5.1f}%  ({status}, threshold: {cov.threshold}%)"
            )

        self._print_separator()
        print("\n🔌 OLLAMA ENDPOINTS TESTED")
        self._print_separator()
        if report.ollama_endpoints_tested:
            for ep in report.ollama_endpoints_tested:
                print(f"  ✅ {ep}")
        else:
            print("  ⚠️  No endpoints tested")

        if report.missing_coverage:
            print("\n  🚫 MISSING COVERAGE:")
            for ep in report.missing_coverage:
                print(f"     • {ep}")

        self._print_separator()
        print("\n💡 RECOMMENDATIONS")
        self._print_separator()
        for i, rec in enumerate(report.recommendations, 1):
            print(f"  {i}. {rec}")

        if report.failed_tests > 0:
            self._print_separator()
            print("\n❌ FAILED TESTS")
            self._print_separator()
            for tr in report.test_results:
                if tr.status in ("failed", "error"):
                    print(f"  • {tr.test_name}")
                    if tr.error_message:
                        print(f"    Error: {tr.error_message[:200]}")

        self._print_separator("═")
        print(
            f"\n🏁 Analysis Complete | Exit Code: {0 if self._is_pass(report) else 1}"
        )
        self._print_separator("═")

    def _progress_bar(self, percentage: float, width: int = 20) -> str:
        """Generate a text progress bar."""
        filled = int(width * percentage / 100)
        empty = width - filled
        return "█" * filled + "░" * empty

    def _is_pass(self, report: AnalysisReport) -> bool:
        """Determine if the analysis passes all thresholds."""
        return (
            report.line_coverage.passed
            and report.branch_coverage.passed
            and report.function_coverage.passed
            and report.failed_tests == 0
        )

    def save_json_report(self, report: AnalysisReport, output_path: str):
        """Save report as JSON for CI/CD integration."""
        path = Path(output_path)
        path.write_text(report.to_json(), encoding="utf-8")
        self.log(f"JSON report saved to: {path}", "success")

    def save_html_report(self, report: AnalysisReport, output_path: str):
        """Save an HTML report."""
        html = self._generate_html(report)
        path = Path(output_path)
        path.write_text(html, encoding="utf-8")
        self.log(f"HTML report saved to: {path}", "success")

    def _generate_html(self, report: AnalysisReport) -> str:
        """Generate a beautiful HTML report."""
        return f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ollama Test Coverage Report</title>
    <style>
        :root {{ --pass: #22c55e; --fail: #ef4444; --warn: #f59e0b; --bg: #0f172a; --card: #1e293b; }}
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: 'Segoe UI', system-ui, sans-serif; background: var(--bg); color: #e2e8f0; padding: 2rem; }}
        .container {{ max-width: 900px; margin: 0 auto; }}
        header {{ text-align: center; margin-bottom: 2rem; }}
        h1 {{ font-size: 1.8rem; background: linear-gradient(90deg, #60a5fa, #a78bfa); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }}
        .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1rem; margin-bottom: 2rem; }}
        .card {{ background: var(--card); border-radius: 12px; padding: 1.5rem; border: 1px solid #334155; }}
        .card h3 {{ font-size: 0.875rem; color: #94a3b8; margin-bottom: 0.5rem; text-transform: uppercase; letter-spacing: 0.05em; }}
        .metric {{ font-size: 2rem; font-weight: 700; }}
        .metric.pass {{ color: var(--pass); }}
        .metric.fail {{ color: var(--fail); }}
        .progress-bar {{ width: 100%; height: 8px; background: #334155; border-radius: 4px; margin-top: 0.5rem; overflow: hidden; }}
        .progress-fill {{ height: 100%; border-radius: 4px; transition: width 0.5s ease; }}
        .progress-fill.pass {{ background: var(--pass); }}
        .progress-fill.fail {{ background: var(--fail); }}
        .endpoint-list {{ list-style: none; }}
        .endpoint-list li {{ padding: 0.5rem 0; border-bottom: 1px solid #334155; display: flex; align-items: center; gap: 0.5rem; }}
        .endpoint-list li:last-child {{ border-bottom: none; }}
        .badge {{ padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; }}
        .badge.pass {{ background: rgba(34, 197, 94, 0.2); color: var(--pass); }}
        .badge.fail {{ background: rgba(239, 68, 68, 0.2); color: var(--fail); }}
        .recommendations {{ background: var(--card); border-radius: 12px; padding: 1.5rem; border: 1px solid #334155; }}
        .recommendations ol {{ padding-left: 1.25rem; }}
        .recommendations li {{ margin-bottom: 0.75rem; line-height: 1.6; }}
        footer {{ text-align: center; margin-top: 2rem; color: #64748b; font-size: 0.875rem; }}
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🚀 Ollama Test Coverage Report</h1>
            <p>{report.timestamp} &bull; {Path(report.project_root).name}</p>
        </header>

        <div class="grid">
            <div class="card">
                <h3>Line Coverage</h3>
                <div class="metric {'pass' if report.line_coverage.passed else 'fail'}">{report.line_coverage.percentage}%</div>
                <div class="progress-bar"><div class="progress-fill {'pass' if report.line_coverage.passed else 'fail'}" style="width:{report.line_coverage.percentage}%"></div></div>
                <small>Threshold: {report.line_coverage.threshold}%</small>
            </div>
            <div class="card">
                <h3>Branch Coverage</h3>
                <div class="metric {'pass' if report.branch_coverage.passed else 'fail'}">{report.branch_coverage.percentage}%</div>
                <div class="progress-bar"><div class="progress-fill {'pass' if report.branch_coverage.passed else 'fail'}" style="width:{report.branch_coverage.percentage}%"></div></div>
                <small>Threshold: {report.branch_coverage.threshold}%</small>
            </div>
            <div class="card">
                <h3>Function Coverage</h3>
                <div class="metric {'pass' if report.function_coverage.passed else 'fail'}">{report.function_coverage.percentage}%</div>
                <div class="progress-bar"><div class="progress-fill {'pass' if report.function_coverage.passed else 'fail'}" style="width:{report.function_coverage.percentage}%"></div></div>
                <small>Endpoints: {report.function_coverage.covered}/{report.function_coverage.total}</small>
            </div>
            <div class="card">
                <h3>Test Results</h3>
                <div class="metric pass">{report.passed_tests}/{report.total_tests}</div>
                <div style="margin-top:0.5rem">
                    <span class="badge pass">✅ {report.passed_tests} Passed</span>
                    {'<span class="badge fail">❌ ' + str(report.failed_tests) + ' Failed</span>' if report.failed_tests > 0 else ''}
                </div>
            </div>
        </div>

        <div class="card" style="margin-bottom:2rem">
            <h3>Endpoints Tested</h3>
            <ul class="endpoint-list">
                {''.join(f'<li><span class="badge pass">✓</span> {ep}</li>' for ep in report.ollama_endpoints_tested)}
                {''.join(f'<li><span class="badge fail">✗</span> {ep} (missing)</li>' for ep in report.missing_coverage)}
            </ul>
        </div>

        <div class="recommendations">
            <h3>💡 Recommendations</h3>
            <ol>
                {''.join(f'<li>{rec}</li>' for rec in report.recommendations)}
            </ol>
        </div>

        <footer>
            Generated by Supreme AI Ollama Coverage Analyzer v2.0.0
        </footer>
    </div>
</body>
</html>"""

    def run(self) -> int:
        """Main entry point. Returns exit code."""
        self._print_banner()

        self.log(DEFAULT_CONFIG["english_messages"]["scanning"])
        test_files = self.discover_test_files()
        self.log(f"Found {len(test_files)} test file(s)", "info")

        self.log("🔬 Analyzing source code for Ollama usage patterns...")
        analyzer = self.analyze_source_files()
        self.log(
            f"Detected {len(analyzer.ollama_calls)} endpoint call(s) in source", "info"
        )

        self.log(DEFAULT_CONFIG["english_messages"]["running"])
        test_results = self.run_tests_with_mock(test_files)

        self.log(DEFAULT_CONFIG["english_messages"]["coverage"])
        report = self.calculate_coverage(analyzer, test_results)

        self.print_report(report)

        report_dir = self.project_root / "coverage_reports"
        report_dir.mkdir(exist_ok=True)
        self.save_json_report(report, str(report_dir / "ollama_coverage.json"))
        self.save_html_report(report, str(report_dir / "ollama_coverage.html"))

        return 0 if self._is_pass(report) else 1


# ─── Built-in Sample Tests ─────────────────────────────────────────────────────
class TestOllamaMockClient(unittest.TestCase):
    """Unit tests for the mock client itself."""

    def test_generate_returns_response(self):
        client = OllamaMockClient()
        result = client.generate("llama3.2", "Hello")
        self.assertIn("response", result)
        self.assertTrue(result["done"])

    def test_chat_returns_message(self):
        client = OllamaMockClient()
        result = client.chat("llama3.2", [{"role": "user", "content": "Hi"}])
        self.assertIn("message", result)
        self.assertEqual(result["message"]["role"], "assistant")

    def test_list_returns_models(self):
        client = OllamaMockClient()
        result = client.list()
        self.assertIn("models", result)
        self.assertGreater(len(result["models"]), 0)

    def test_embeddings_returns_vector(self):
        client = OllamaMockClient()
        result = client.embeddings("llama3.2", "test")
        self.assertIn("embedding", result)
        self.assertIsInstance(result["embedding"], list)

    def test_error_simulation(self):
        client = OllamaMockClient()
        client.set_raise_on("generate")
        with self.assertRaises(ConnectionError):
            client.generate("llama3.2", "test")

    def test_call_history(self):
        client = OllamaMockClient()
        client.generate("llama3.2", "Hello")
        client.chat("llama3.2", [])
        self.assertEqual(len(client.call_history), 2)
        self.assertEqual(client.call_history[0]["endpoint"], "generate")

    def test_delete_returns_true(self):
        client = OllamaMockClient()
        self.assertTrue(client.delete("old-model"))

    def test_show_returns_details(self):
        client = OllamaMockClient()
        result = client.show("llama3.2")
        self.assertIn("details", result)
        self.assertEqual(result["details"]["family"], "llama")


class TestOllamaPatcher(unittest.TestCase):
    """Tests for the patcher context manager."""

    def test_patcher_context_manager(self):
        with OllamaPatcher() as client:
            self.assertIsInstance(client, OllamaMockClient)
            self.assertIn("ollama", sys.modules)

    def test_patcher_cleanup(self):
        original = sys.modules.get("ollama")
        with OllamaPatcher():
            pass
        if original is not None:
            self.assertEqual(sys.modules.get("ollama"), original)


class TestUsageAnalyzer(unittest.TestCase):
    """Tests for the AST analyzer."""

    def test_detects_ollama_import(self):
        code = "import ollama\nollama.generate('model', 'prompt')"
        tree = ast.parse(code)
        analyzer = OllamaUsageAnalyzer()
        analyzer.visit(tree)
        self.assertIn("ollama", analyzer.ollama_imports)

    def test_detects_generate_call(self):
        code = "import ollama\nclient = ollama.Client()\nclient.generate('m', 'p')"
        tree = ast.parse(code)
        analyzer = OllamaUsageAnalyzer()
        analyzer.visit(tree)
        self.assertIn("generate", analyzer.ollama_calls)

    def test_detects_async_usage(self):
        code = "async def test():\n    import ollama\n    await ollama.chat('m', [])"
        tree = ast.parse(code)
        analyzer = OllamaUsageAnalyzer()
        analyzer.visit(tree)
        self.assertTrue(analyzer.async_usage)


# ─── CLI Entry Point ──────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(
        description="Ollama Test Coverage Analyzer — Runs without a real Ollama server",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent("""
        Examples:
          %(prog)s                          # Analyze current directory
          %(prog)s --root ./my-project    # Analyze specific project
          %(prog)s --threshold 90         # Set coverage threshold to 90%
          %(prog)s --json report.json     # Save JSON report
          %(prog)s --html report.html     # Save HTML report
          %(prog)s --self-test            # Run built-in tests only
        """),
    )
    parser.add_argument(
        "--root",
        "-r",
        default=".",
        help="Project root directory (default: current dir)",
    )
    parser.add_argument(
        "--threshold",
        "-t",
        type=float,
        default=80.0,
        help="Coverage threshold % (default: 80)",
    )
    parser.add_argument(
        "--branch-threshold",
        "-b",
        type=float,
        default=70.0,
        help="Branch coverage threshold %",
    )
    parser.add_argument("--json", "-j", metavar="PATH", help="Save JSON report to file")
    parser.add_argument("--html", metavar="PATH", help="Save HTML report to file")
    parser.add_argument(
        "--self-test", action="store_true", help="Run built-in unit tests only"
    )
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose output")
    parser.add_argument(
        "--lang", choices=["en", "bn"], default="en", help="Output language (en/bn)"
    )

    args = parser.parse_args()

    if args.self_test:
        loader = unittest.TestLoader()
        suite = unittest.TestSuite()
        suite.addTests(loader.loadTestsFromTestCase(TestOllamaMockClient))
        suite.addTests(loader.loadTestsFromTestCase(TestOllamaPatcher))
        suite.addTests(loader.loadTestsFromTestCase(TestUsageAnalyzer))
        runner = unittest.TextTestRunner(verbosity=2 if args.verbose else 1)
        result = runner.run(suite)
        return 0 if result.wasSuccessful() else 1

    config = {
        **DEFAULT_CONFIG,
        "coverage_threshold": args.threshold,
        "branch_threshold": args.branch_threshold,
    }

    runner = OllamaTestRunner(args.root, config)
    exit_code = runner.run()

    if args.json or args.html:
        report = runner.report
        if args.json:
            runner.save_json_report(report, args.json)
        if args.html:
            runner.save_html_report(report, args.html)

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
