import os
from typing import Dict, Any, List
from loguru import logger

try:
    from github import Github
    _GITHUB_AVAILABLE = True
except ImportError:
    _GITHUB_AVAILABLE = False
    Github = None

class PRReviewer:
    """
    Automated Pull Request Reviewer.
    (Closes Gap #21)
    """
    def __init__(self, github_token: str = None):
        self.github_token = github_token or os.getenv("GITHUB_TOKEN")
        if not self.github_token:
            logger.warning("GITHUB_TOKEN not found. PR reviewer will run in dry-run mode.")
            self.gh = None
        else:
            if not _GITHUB_AVAILABLE:
                raise ImportError("PyGithub is not installed. Please run 'pip install PyGithub'")
            self.gh = Github(self.github_token)

    async def analyze_diff(self, diff_content: str) -> List[Dict[str, Any]]:
        """
        Analyzes a diff content and returns a list of issues including security issues.
        This is a placeholder for a more sophisticated analysis, potentially using an LLM.
        """
        issues = []
        lines = diff_content.split('\n')
        
        # Security patterns to detect
        security_patterns = {
            r"AKIA[0-9A-Z]{16}": {"type": "AWS API Key", "severity": "critical"},
            r"sk_(?:test|live)_[a-zA-Z0-9]{20,}": {"type": "Stripe Secret Key", "severity": "critical"},
            r"ghp_[0-9a-zA-Z]{36}": {"type": "GitHub Personal Access Token", "severity": "critical"},
            r"(?i)password\s*=\s*['\"][^'\"]+['\"]": {"type": "Hardcoded Password", "severity": "high"},
            r"(?i)secret\s*=\s*['\"][^'\"]+['\"]": {"type": "Hardcoded Secret", "severity": "high"},
            r"(?i)api.?key\s*=\s*['\"][^'\"]+['\"]": {"type": "API Key", "severity": "high"},
        }
        
        import re
        for i, line in enumerate(lines):
            if line.startswith('+'):
                # Check for security patterns
                found_security_issue = False
                for pattern, info in security_patterns.items():
                    if re.search(pattern, line):
                        issues.append({
                            "path": "unknown",
                            "line": i + 1,
                            "severity": info["severity"],
                            "body": f"Security Issue: {info['type']} detected in diff"
                        })
                        found_security_issue = True
                        break
                # Check for TODO comments if no security issue found
                if not found_security_issue and 'TODO' in line:
                    issues.append({
                        "path": "unknown",
                        "line": i + 1,
                        "severity": "low",
                        "body": "Found a 'TODO' comment. Please add a ticket reference."
                    })
        return issues

    async def review_pr(self, repo_full_name: str, pr_number: int) -> Dict[str, Any]:
        """
        Reviews a pull request by fetching the diff and analyzing it for issues.
        """
        if not self.gh:
            logger.warning(f"Dry-run: Would review {repo_full_name}#{pr_number}")
            return {"status": "success", "action_taken": "COMMENT", "comments": []}
        
        try:
            repo = self.gh.get_repo(repo_full_name)
            pr = repo.get_pull(pr_number)
            diff = pr.get_files()
            
            comments = []
            has_critical = False
            
            for file_diff in diff:
                diff_content = file_diff.patch or ""
                file_comments = await self.analyze_diff(diff_content)
                comments.extend(file_comments)
                if any(c["severity"] == "critical" for c in file_comments):
                    has_critical = True
            
            action = "REQUEST_CHANGES" if has_critical else "COMMENT"
            
            return {
                "status": "success",
                "action_taken": action,
                "comments": comments
            }
        except Exception as e:
            logger.error(f"Error reviewing PR: {e}")
            return {"status": "error", "error": str(e), "comments": []}

    async def _post_pr_comment(self, repo_full_name: str, pr_number: int, comment_body: str) -> Dict[str, Any]:
        """Posts a comment on a pull request."""
        if not self.gh:
            logger.warning(f"Dry-run: Would post to {repo_full_name}#{pr_number}: {comment_body}")
            return {"status": "success", "comment_url": "dry-run-url"}
        
        # This part would contain the actual GitHub API call to post a comment.
        # For now, it's a placeholder.
        return {"status": "success", "comment_url": "https://github.com/..."}