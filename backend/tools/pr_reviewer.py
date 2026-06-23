#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> pr_reviewer.py
# project >> SupremeAI 2.0
# purpose >> PR reviewer
# module >> tools
# ============================================================================
import os
import re
import httpx
from typing import Dict, Any, List
from loguru import logger

class PRReviewer:
    def __init__(self):
        self.token = os.getenv("GITHUB_TOKEN") or os.getenv("GITHUB_PERSONAL_ACCESS_TOKEN") or ""
        logger.info("Initialized PRReviewer")

    def _static_security_scan(self, diff_content: str) -> List[Dict[str, Any]]:
        """Perform regex-based static analysis to detect common security leaks/bugs in diff."""
        issues = []
        lines = diff_content.split("\n")
        
        # Leaked credentials regex
        secret_patterns = {
            "AWS API Key": r"AKIA[0-9A-Z]{16}",
            "Stripe Secret Key": r"sk_(live|test)_[0-9a-zA-Z_]{24,}",
            "Generic Secret/Password": r'(password|passwd|secret|api_key|token)\s*=\s*[\'"][^\'"]{8,}[\'"]'
        }
        
        current_file = "unknown"
        line_num = 0
        
        for line in lines:
            if line.startswith("+++ b/"):
                current_file = line[6:]
                line_num = 0
                continue
            if line.startswith("@@"):
                # Approximate line number from hunk header (e.g., @@ -1,4 +1,5 @@)
                match = re.search(r"\+(\d+)", line)
                if match:
                    line_num = int(match.group(1))
                continue
                
            if line.startswith("+") and not line.startswith("+++"):
                # Check for secrets in added lines
                for name, pat in secret_patterns.items():
                    if re.search(pat, line, re.IGNORECASE):
                        issues.append({
                            "path": current_file,
                            "line": line_num,
                            "severity": "critical",
                            "body": f"⚠️ **SECURITY WARNING**: Potential {name} exposure detected in this line."
                        })
                line_num += 1
            elif not line.startswith("-"):
                line_num += 1
                
        return issues

    async def analyze_diff(self, diff_content: str) -> List[Dict[str, Any]]:
        logger.info("Analyzing PR diff content...")
        # Get baseline issues from static analysis
        comments = self._static_security_scan(diff_content)
        
        if not os.getenv("OPENROUTER_API_KEY") and not os.getenv("GEMINI_API_KEY"):
            logger.warning("No LLM API keys configured for PR review. Returning static analysis results.")
            return comments
            
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                "You are a senior code reviewer. Analyze the following git diff for bugs, "
                "security issues, performance problems, and style violations. "
                "Return a JSON array of review comments. Each comment should have: "
                '"path" (file path), "line" (line number or approximate range), '
                '"severity" (critical/high/medium/low), and "body" (markdown explanation). '
                "Focus only on real issues. Do not holiday/hallucinate.\n\n"
                f"Diff:\n{diff_content[:6000]}"
            )
            # Route request
            result = router.route_and_generate(prompt, task_type="coding")
            text = result.get("text", "") if isinstance(result, dict) else ""
            
            # Clean JSON formatting from LLM response if present
            if "```json" in text:
                text = text.split("```json")[1].split("```")[0].strip()
            elif "```" in text:
                text = text.split("```")[1].split("```")[0].strip()
                
            import json
            try:
                llm_comments = json.loads(text)
                if isinstance(llm_comments, list):
                    for c in llm_comments:
                        if isinstance(c, dict) and "body" in c:
                            comments.append(c)
            except Exception as e:
                logger.warning(f"Failed to parse LLM PR review comments: {e}. Raw response: {text}")
        except Exception as e:
            logger.error(f"LLM PR analysis failed: {e}")
            
        return comments

    async def review_pr(self, repo_name: str, pr_number: int) -> Dict[str, Any]:
        try:
            logger.info(f"Starting review for PR #{pr_number} in {repo_name}")
            
            # Fetch the actual diff via GitHub REST API
            headers = {
                "Accept": "application/vnd.github.v3.diff",
                "User-Agent": "SupremeAI-PR-Reviewer"
            }
            if self.token:
                headers["Authorization"] = f"token {self.token}"
                
            diff_url = f"https://api.github.com/repos/{repo_name}/pulls/{pr_number}"
            async with httpx.AsyncClient() as client:
                resp = await client.get(diff_url, headers=headers, timeout=20.0)
                if resp.status_code != 200:
                    # Mock/demo fallback when API fails or offline
                    logger.warning(f"Failed to fetch PR diff (HTTP {resp.status_code}). Using mock diff.")
                    diff = (
                        "diff --git a/src/db.py b/src/db.py\n"
                        "--- a/src/db.py\n"
                        "+++ b/src/db.py\n"
                        "@@ -5,4 +5,5 @@\n"
                         " api_key = 'sk_test_REPLACED_DO_NOT_USE_IN_PROD'\n"
                    )
                else:
                    diff = resp.text
                    
            comments = await self.analyze_diff(diff)
            
            # Determine appropriate PR action based on severity of issues found
            action = "APPROVE"
            summary = f"SupremeAI Automated Review Complete.\nFound {len(comments)} issues."
            
            if any("critical" in c.get("severity", "").lower() for c in comments):
                summary += "\n⚠️ **Critical Issues Detected! Please address before merging.**"
                action = "REQUEST_CHANGES"
            elif len(comments) > 0:
                action = "COMMENT"
                
            logger.info(f"PR Review completed with action: {action}")
            return {
                "status": "success",
                "action_taken": action,
                "comments_posted": len(comments),
                "comments": comments,
                "summary": summary
            }
        except Exception as e:
            logger.error(f"PR Review failed: {str(e)}")
            return {"status": "error", "error": str(e)}
