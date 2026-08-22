"""
SupremeAI — Autonomous Test Gap Hunter & Generator
====================================================
এই স্ক্রিপ্ট নিজে নিজে:
  1. coverage.xml পার্স করে সবচেয়ে কম কভারেজের মডিউল খোঁজে
  2. AST দিয়ে প্রতিটি মডিউলের functions/classes/methods এক্সট্র্যাক্ট করে
  3. OpenRouter (Gemini flash) দিয়ে real, meaningful pytest tests জেনারেট করে
  4. সঠিক tests/ ডিরেক্টরিতে ফাইল লিখে দেয়
  5. নিজে pytest রান করে verify করে

Usage:
    python backend/scripts/auto_test_gen.py
    python backend/scripts/auto_test_gen.py --limit 10 --min-coverage 0.3
    python backend/scripts/auto_test_gen.py --module brain/economic_optimizer.py --dry-run
"""

from __future__ import annotations

import argparse
import ast
import os
import re
import subprocess
import sys
import textwrap
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import requests
from loguru import logger

BACKEND_ROOT = Path(__file__).parent.parent.resolve()
COVERAGE_XML = BACKEND_ROOT / "coverage.xml"
TESTS_ROOT = BACKEND_ROOT / "tests"

OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY", "")
OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
MODEL = "openai/gpt-4o-mini"
MAX_PROMPT_CHARS = 12_000
MAX_SOURCE_LINES = 200


@dataclass
class ModuleInfo:
    source_path: Path
    line_rate: float
    missing_lines: list[int]
    functions: list[str] = field(default_factory=list)
    classes: list[str] = field(default_factory=list)
    async_functions: list[str] = field(default_factory=list)
    source_snippet: str = ""


def parse_coverage_gaps(min_coverage: float = 0.5, limit: int = 20) -> list[ModuleInfo]:
    if not COVERAGE_XML.exists():
        logger.error(f"coverage.xml not found at {COVERAGE_XML}. Run pytest first.")
        sys.exit(1)

    tree = ET.parse(COVERAGE_XML)
    root = tree.getroot()
    sources = [Path(s.text) for s in root.findall(".//source") if s.text]
    source_root = sources[0] if sources else BACKEND_ROOT

    gaps: list[ModuleInfo] = []
    for cls in root.findall(".//class"):
        filename_attr = cls.get("filename", "")
        line_rate = float(cls.get("line-rate", "1.0"))

        if line_rate >= min_coverage:
            continue

        source_path = source_root / filename_attr
        if not source_path.exists():
            source_path = BACKEND_ROOT / filename_attr
            if not source_path.exists():
                continue

        if any(part in source_path.parts for part in ("tests", "alembic", "migrations", "__pycache__")):
            continue
        if source_path.name.startswith("test_") or source_path.name == "__init__.py":
            continue

        missing = [int(l.get("number")) for l in cls.findall(".//line") if l.get("hits") == "0"]
        gaps.append(ModuleInfo(
            source_path=source_path,
            line_rate=line_rate,
            missing_lines=missing[:50],
        ))

    gaps.sort(key=lambda m: m.line_rate)
    logger.info(f"Found {len(gaps)} modules below {min_coverage*100:.0f}% coverage threshold.")
    return gaps[:limit]


def extract_module_signature(module: ModuleInfo) -> ModuleInfo:
    try:
        source = module.source_path.read_text(encoding="utf-8", errors="replace")
        lines = source.splitlines()
        module.source_snippet = "\n".join(lines[:MAX_SOURCE_LINES])
        if len(lines) > MAX_SOURCE_LINES:
            module.source_snippet += f"\n# ... (truncated, {len(lines) - MAX_SOURCE_LINES} more lines)"

        tree = ast.parse(source)
        for node in ast.walk(tree):
            if isinstance(node, ast.AsyncFunctionDef):
                if not node.name.startswith("_"):
                    module.async_functions.append(node.name)
            elif isinstance(node, ast.FunctionDef):
                if not node.name.startswith("_"):
                    module.functions.append(node.name)
            elif isinstance(node, ast.ClassDef):
                if not node.name.startswith("_"):
                    module.classes.append(node.name)
    except SyntaxError as e:
        logger.warning(f"Syntax error in {module.source_path}: {e}")
    return module


def resolve_test_path(module: ModuleInfo) -> Path:
    try:
        rel = module.source_path.relative_to(BACKEND_ROOT)
    except ValueError:
        rel = Path(module.source_path.name)

    parts = list(rel.parts)
    test_rel = Path(*parts[:-1]) / f"test_{parts[-1]}"
    return TESTS_ROOT / test_rel


SYSTEM_PROMPT = textwrap.dedent("""
You are an expert Python test engineer for SupremeAI, a self-evolving AI platform.
Your job is to write production-quality pytest tests for the given Python module.

RULES:
1. Use `pytest` and `pytest-asyncio` (asyncio_mode = "auto" is set globally).
2. Use `unittest.mock.patch`, `MagicMock`, `AsyncMock` to mock heavy dependencies (DB, Redis, HTTP).
3. Write REAL tests — not placeholders. Each test must actually assert something meaningful.
4. For async functions, use `async def test_...` (no @pytest.mark.asyncio needed, it is global).
5. Do NOT import the module at top-level if it has heavy side-effects — use local imports inside test functions.
6. Group tests in a TestClassName class if the source has classes.
7. Add at least 1 happy-path test, 1 edge-case test, and 1 error-path test per public function.
8. Output ONLY the Python test file content — no markdown fences, no explanation.
9. Start with the module docstring.
10. Always add conftest-compatible fixtures if needed.
""").strip()


def call_llm(prompt: str) -> str:
    if not OPENROUTER_API_KEY:
        logger.error("OPENROUTER_API_KEY is not set. Cannot call LLM.")
        sys.exit(1)

    headers = {
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://supremeai.app",
        "X-Title": "SupremeAI Test Generator",
    }
    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt[:MAX_PROMPT_CHARS]},
        ],
        "temperature": 0.2,
        "max_tokens": 3000,
    }

    try:
        resp = requests.post(OPENROUTER_URL, headers=headers, json=payload, timeout=60)
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"].strip()
    except requests.RequestException as e:
        logger.error(f"LLM API call failed: {e}")
        return ""


def build_prompt(module: ModuleInfo) -> str:
    rel_path = module.source_path.relative_to(BACKEND_ROOT)
    public_api = []
    if module.classes:
        public_api.append(f"Classes: {', '.join(module.classes)}")
    if module.functions:
        public_api.append(f"Sync functions: {', '.join(module.functions)}")
    if module.async_functions:
        public_api.append(f"Async functions: {', '.join(module.async_functions)}")

    missing_info = ""
    if module.missing_lines:
        missing_info = f"\nLines with ZERO coverage (most important to test): {module.missing_lines}"

    prompt = textwrap.dedent(f"""
    Generate a complete pytest test file for the following SupremeAI module.

    MODULE PATH: {rel_path}
    CURRENT COVERAGE: {module.line_rate * 100:.1f}%
    {missing_info}

    PUBLIC API:
    {chr(10).join(public_api) if public_api else "No public API detected."}

    SOURCE CODE:
    ```python
    {module.source_snippet}
    ```

    The import path for this module from the tests directory would be:
    `from {str(rel_path).replace(os.sep, ".").removesuffix(".py")} import *`
    """).strip()
    return prompt


def clean_llm_output(raw: str) -> str:
    raw = re.sub(r"^```python\s*", "", raw, flags=re.MULTILINE)
    raw = re.sub(r"^```\s*$", "", raw, flags=re.MULTILINE)
    return raw.strip()


def write_test_file(test_path: Path, content: str, overwrite: bool = False) -> bool:
    if test_path.exists() and not overwrite:
        logger.info(f"  Skipping (already exists): {test_path.relative_to(BACKEND_ROOT)}")
        return False

    test_path.parent.mkdir(parents=True, exist_ok=True)
    for parent in test_path.parents:
        if parent == TESTS_ROOT or parent == BACKEND_ROOT:
            break
        init = parent / "__init__.py"
        if not init.exists():
            init.write_text("", encoding="utf-8")

    test_path.write_text(content, encoding="utf-8")
    logger.success(f"  Written: {test_path.relative_to(BACKEND_ROOT)}")
    return True


def run(
    limit: int = 15,
    min_coverage: float = 0.4,
    dry_run: bool = False,
    overwrite: bool = False,
    verify: bool = True,
    specific_module: str | None = None,
    max_retries: int = 2,
) -> dict[str, Any]:
    stats = {"processed": 0, "written": 0, "passed": 0, "failed": 0, "skipped": 0}

    if specific_module:
        path = BACKEND_ROOT / specific_module
        if not path.exists():
            logger.error(f"Module not found: {path}")
            sys.exit(1)
        modules = [ModuleInfo(source_path=path, line_rate=0.0, missing_lines=[])]
    else:
        logger.info("Scanning coverage.xml for test gaps...")
        modules = parse_coverage_gaps(min_coverage=min_coverage, limit=limit)

    if not modules:
        logger.success("No gaps found! Coverage is already above threshold.")
        return stats

    logger.info(f"\nProcessing {len(modules)} modules:\n")
    for i, module in enumerate(modules, 1):
        logger.info(
            f"[{i}/{len(modules)}] {module.source_path.relative_to(BACKEND_ROOT)} "
            f"(coverage: {module.line_rate*100:.1f}%)"
        )
        module = extract_module_signature(module)

        if not module.source_snippet:
            logger.warning("  Could not read source. Skipping.")
            stats["skipped"] += 1
            continue

        if not module.functions and not module.classes and not module.async_functions:
            logger.info("  No public API found. Skipping.")
            stats["skipped"] += 1
            continue

        test_path = resolve_test_path(module)
        stats["processed"] += 1

        if dry_run:
            logger.info(f"  [DRY RUN] Would write: {test_path.relative_to(BACKEND_ROOT)}")
            logger.info(f"  Functions : {module.functions + module.async_functions}")
            logger.info(f"  Classes   : {module.classes}")
            continue

        logger.info("  Calling LLM to generate tests...")
        prompt = build_prompt(module)
        raw_output = call_llm(prompt)

        if not raw_output:
            logger.warning("  LLM returned empty response. Skipping.")
            stats["failed"] += 1
            continue

        test_content = clean_llm_output(raw_output)
        written = write_test_file(test_path, test_content, overwrite=overwrite)
        if not written:
            stats["skipped"] += 1
            continue

        stats["written"] += 1

        if not verify:
            continue

        for attempt in range(1, max_retries + 2):
            result = subprocess.run(
                [
                    sys.executable, "-m", "pytest", str(test_path),
                    "-x", "--tb=short", "-q", "--no-header",
                    "--import-mode=importlib", "--no-cov",
                ],
                capture_output=True, text=True, cwd=BACKEND_ROOT,
            )
            if result.returncode == 0:
                logger.success(f"  PASS (attempt {attempt})")
                stats["passed"] += 1
                break
            else:
                logger.warning(f"  FAIL (attempt {attempt}/{max_retries + 1})")
                if attempt <= max_retries:
                    logger.info("  Self-healing: asking LLM to fix the test...")
                    error_out = result.stdout[-2000:] + result.stderr[-500:]
                    fix_prompt = textwrap.dedent(f"""
                    The test file for `{module.source_path.name}` failed:
                    ```
                    {error_out}
                    ```
                    Original source:
                    ```python
                    {module.source_snippet[:3000]}
                    ```
                    Fix the test file. Output ONLY the corrected Python test file.
                    """).strip()
                    fixed = clean_llm_output(call_llm(fix_prompt))
                    if fixed:
                        test_path.write_text(fixed, encoding="utf-8")
                else:
                    stats["failed"] += 1
                    logger.warning(f"  Kept failing test at {test_path.name} for manual review.")

    logger.info("\n" + "=" * 55)
    logger.info("AUTO-TEST GENERATION COMPLETE")
    logger.info(f"  Processed : {stats['processed']}")
    logger.info(f"  Written   : {stats['written']}")
    logger.info(f"  Passed    : {stats['passed']}")
    logger.info(f"  Failed    : {stats['failed']}")
    logger.info(f"  Skipped   : {stats['skipped']}")
    logger.info("=" * 55)
    return stats


def main():
    parser = argparse.ArgumentParser(
        description="SupremeAI Autonomous Test Gap Hunter",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent("""
        Examples:
          python backend/scripts/auto_test_gen.py
          python backend/scripts/auto_test_gen.py --limit 30 --min-coverage 0.3
          python backend/scripts/auto_test_gen.py --module brain/economic_optimizer.py --dry-run
          python backend/scripts/auto_test_gen.py --overwrite --no-verify --limit 50
        """),
    )
    parser.add_argument("--limit", type=int, default=15)
    parser.add_argument("--min-coverage", type=float, default=0.4)
    parser.add_argument("--module", type=str, default=None)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--no-verify", action="store_true")
    parser.add_argument("--max-retries", type=int, default=2)

    args = parser.parse_args()
    logger.remove()
    logger.add(sys.stderr, level="INFO",
               format="<green>{time:HH:mm:ss}</green> | <level>{message}</level>")

    run(
        limit=args.limit,
        min_coverage=args.min_coverage,
        dry_run=args.dry_run,
        overwrite=args.overwrite,
        verify=not args.no_verify,
        specific_module=args.module,
        max_retries=args.max_retries,
    )


if __name__ == "__main__":
    main()
