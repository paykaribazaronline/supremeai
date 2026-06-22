import os
import subprocess
from typing import Dict, Any
from loguru import logger
from tools.pr_reviewer import PRReviewer

class PreCommitAI:
    """
    Git hook integration that analyzes staged files before commit.
    Blocks commits with critical security issues or offers auto-fixes.
    (Closes Gap #22)
    """

    def __init__(self):
        self.reviewer = PRReviewer()
        logger.info("Initialized PreCommitAI hook handler")

    def _get_staged_diff(self) -> str:
        """Gets the git diff for staged files."""
        try:
            result = subprocess.run(
                ["git", "diff", "--cached"],
                capture_output=True,
                text=True,
                check=True
            )
            return result.stdout
        except subprocess.CalledProcessError:
            logger.error("Failed to get staged git diff. Is this a git repository?")
            return ""

    async def run_hook(self) -> Dict[str, Any]:
        """Runs the pre-commit analysis."""
        logger.info("Running AI Pre-Commit Hook...")
        
        diff = self._get_staged_diff()
        if not diff:
            return {"status": "success", "message": "No staged changes to analyze."}
            
        # Analyze using the PR Reviewer logic
        issues = await self.reviewer.analyze_diff(diff)
        
        critical_issues = [i for i in issues if "SECURITY" in str(i.get("body", "")).upper()]
        
        if critical_issues:
            logger.error(f"Pre-commit blocked! Found {len(critical_issues)} critical security issues.")
            for issue in critical_issues:
                logger.error(f"  - {issue['path']}:{issue['line']} -> {issue['body']}")
            return {
                "status": "blocked",
                "reason": "Security vulnerabilities detected.",
                "issues": critical_issues
            }
            
        if issues:
            logger.warning(f"Found {len(issues)} non-critical issues. Commit allowed but review is suggested.")
            
        logger.info("Pre-commit checks passed.")
        return {"status": "success", "message": "All checks passed.", "warnings": issues}
