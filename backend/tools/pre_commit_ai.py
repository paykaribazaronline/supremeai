import subprocess
import os
from typing import Dict, Any, List
from loguru import logger

try:
    from tools.pr_reviewer import PRReviewer
    _PR_REVIEWER_AVAILABLE = True
except Exception:
    _PR_REVIEWER_AVAILABLE = False
    PRReviewer = None  # type: ignore[misc,assignment]


class PreCommitAI:
    """
    Git hook integration that analyzes staged files before commit.
    Blocks commits with critical security issues or offers auto-fixes.
    Supports pre-commit framework via .pre-commit-config.yaml.
    (Closes Gap #22)
    """

    AUTO_FIX_RULES = {
        "trailing_whitespace": True,
        "end_of_file_newline": True,
        "import_sort": True,
        "line_length": 120,
    }

    def __init__(self):
        self.reviewer = PRReviewer() if _PR_REVIEWER_AVAILABLE and PRReviewer is not None else None
        self._check_tools()
        logger.info("Initialized PreCommitAI hook handler")

    def _check_tools(self) -> None:
        self.isort_available = False
        self.black_available = False
        try:
            import isort  # noqa: F401
            self.isort_available = True
        except ImportError:
            pass
        try:
            import black  # noqa: F401
            self.black_available = True
        except ImportError:
            pass
        logger.debug(f"PreCommit tools: isort={self.isort_available}, black={self.black_available}")

    def _get_staged_diff(self) -> str:
        """Gets the git diff for staged files."""
        try:
            result = subprocess.run(
                ["git", "diff", "--cached"],
                capture_output=True,
                text=True,
                check=True,
            )
            return result.stdout
        except subprocess.CalledProcessError:
            logger.error("Failed to get staged git diff. Is this a git repository?")
            return ""
        except FileNotFoundError:
            logger.error("git not found in PATH.")
            return ""

    def _get_staged_files(self) -> List[str]:
        """Returns list of staged file paths."""
        try:
            result = subprocess.run(
                ["git", "diff", "--cached", "--name-only", "--diff-filter=ACM"],
                capture_output=True,
                text=True,
                check=True,
            )
            return [f for f in result.stdout.splitlines() if f.strip()]
        except Exception as exc:
            logger.error(f"Failed to list staged files: {exc}")
            return []

    def _auto_fix(self, files: List[str]) -> Dict[str, Any]:
        """Apply simple auto-fixes: trailing whitespace, import sort."""
        fixes_applied: List[Dict[str, str]] = []
        for filepath in files:
            if not os.path.exists(filepath):
                continue
            ext = os.path.splitext(filepath)[1].lower()
            original_content = ""
            with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                original_content = f.read()
            
            new_content = original_content

            try:
                if ext == ".py":
                    if self.isort_available:
                        import isort
                        new_content = isort.code(new_content)
                        if new_content != original_content:
                            fixes_applied.append({"file": filepath, "action": "isort"})
                    if self.black_available:
                        import black
                        # To apply black formatting, we need to write to a temporary file
                        # or handle it in memory if black supports it.
                        # For simplicity, we'll re-format the file in place.
                        black.format_file_in_place(
                            black.FilePath(filepath),
                            mode=black.Mode(),
                            fast=False,
                            write_back=black.WriteBack.YES,
                        )
                        fixes_applied.append({"file": filepath, "action": "black"})
                        # Reread content after black formatting
                        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                            new_content = f.read()

                if self.AUTO_FIX_RULES.get("trailing_whitespace"):
                    cleaned_content = "\n".join(line.rstrip() for line in new_content.splitlines())
                    if cleaned_content != new_content:
                        new_content = cleaned_content
                        fixes_applied.append({"file": filepath, "action": "trim_trailing_whitespace"})
                
                if new_content != original_content:
                    with open(filepath, "w", encoding="utf-8") as f:
                        f.write(new_content)
            except Exception as exc:
                logger.debug(f"Auto-fix failed for {filepath}: {exc}")
        return {"fixes": fixes_applied, "count": len(fixes_applied)}

    async def run_hook(self, auto_fix: bool = True) -> Dict[str, Any]:
        """Runs the pre-commit analysis."""
        logger.info("Running AI Pre-Commit Hook...")

        diff = self._get_staged_diff()
        if not diff:
            return {"status": "success", "message": "No staged changes to analyze."}

        issues: List[Dict[str, Any]] = []
        if self.reviewer is not None:
            try:
                issues = await self.reviewer.analyze_diff(diff)
            except Exception as exc:
                logger.error(f"AI analysis failed: {exc}")
                return {"status": "error", "message": f"AI analysis failed: {exc}"}
        else:
            issues = self._static_security_scan(diff)

        critical_issues = [
            i for i in issues
            if "critical" in str(i.get("severity", "")).lower()
            or "SECURITY" in str(i.get("body", "")).upper()
        ]

        if critical_issues:
            logger.error(f"Pre-commit blocked! Found {len(critical_issues)} critical security issues.")
            for issue in critical_issues:
                logger.error(
                    f"  - {issue.get('path', '?')}:{issue.get('line', '?')} -> {issue.get('body', issue.get('type', ''))}"
                )
            return {
                "status": "blocked",
                "reason": "Critical security vulnerabilities detected.",
                "issues": critical_issues,
            }

        files = self._get_staged_files()

        if auto_fix and files:
            fix_report = self._auto_fix(files)
            if fix_report["count"] > 0:
                for fp in files:
                    try:
                        subprocess.run(["git", "add", fp], check=True, capture_output=True)
                    except Exception:
                        pass
                logger.info(f"Auto-fixed {fix_report['count']} issue(s).")
                # The commit was not blocked, files were fixed and re-staged.
                # Allow the commit to proceed.
                logger.info("Auto-fixed files were re-staged. Allowing commit to proceed.")
                # The hook should return success now. The calling pre-commit framework will handle the rest.


        if issues:
            logger.warning(f"Found {len(issues)} non-critical issues. Commit allowed but review is suggested.")

        logger.info("Pre-commit checks passed.")
        return {"status": "success", "message": "All checks passed.", "warnings": issues}

    def _static_security_scan(self, diff_content: str) -> List[Dict[str, Any]]:
        """Regex-based fallback when PRReviewer/LLM is unavailable."""
        import re

        issues: List[Dict[str, Any]] = []
        lines = diff_content.split("\n")
        current_file = "unknown"
        line_num = 0
        secret_patterns = {
            "AWS API Key": r"AKIA[0-9A-Z]{16}",
            "Stripe Secret Key": r"sk_live_[0-9a-zA-Z]{24}",
            "Generic Secret/Password": r"(password|passwd|secret|api_key|token)\s*=\s*['\"][^'\"]{8,}['\"]",
        }
        for line in lines:
            if line.startswith("+++ b/"):
                current_file = line[6:]
                line_num = 0
                continue
            if line.startswith("@@"):
                match = re.search(r"\+(\d+)", line)
                line_num = int(match.group(1)) if match else 0
                continue
            if line.startswith("+") and not line.startswith("+++"):
                for name, pat in secret_patterns.items():
                    if re.search(pat, line, re.IGNORECASE):
                        issues.append({
                            "path": current_file,
                            "line": line_num,
                            "severity": "critical",
                            "body": f"SECURITY: Potential {name} exposure detected.",
                        })
                line_num += 1
            elif not line.startswith("-"):
                line_num += 1
        return issues


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="SupremeAI Pre-Commit AI Gate")
    parser.add_argument("--no-fix", action="store_true", help="Run analysis without auto-fixing")
    parser.add_argument("files", nargs="*", help="Optional file list (ignored; uses staged diff)")
    args = parser.parse_args()

    hook = PreCommitAI()
    import asyncio
    result = asyncio.run(hook.run_hook(auto_fix=not args.no_fix))
    status = result.get("status", "error")
    if status == "blocked":
        print("❌ Commit blocked:", result.get("reason"))
        for issue in result.get("issues", []):
            print(f"  - {issue.get('path', '?')}:{issue.get('line', '?')} -> {issue.get('body', '')}")
        raise SystemExit(1)
    elif status == "fixed":
        print("⚠️  Auto-fixed issues. Review and re-commit.")
        raise SystemExit(1)
    elif status == "error":
        print("❌ Error:", result.get("message"))
        raise SystemExit(1)
    else:
        print("✅ Pre-commit checks passed.")
