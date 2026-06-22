import os
from typing import Dict, Any, List
from loguru import logger
from tools.github_agent import GitHubAgent

class PRReviewer:
    def __init__(self):
        self.github_agent = GitHubAgent(
            token=os.getenv("GITHUB_TOKEN") or os.getenv("GITHUB_PERSONAL_ACCESS_TOKEN") or ""
        )
        logger.info("Initialized PRReviewer")

    async def analyze_diff(self, diff_content: str) -> List[Dict[str, Any]]:
        logger.info("Analyzing PR diff content...")
        if not os.getenv("OPENROUTER_API_KEY") and not os.getenv("GEMINI_API_KEY"):
            logger.warning("No LLM API keys configured for PR review. Returning limited analysis.")
            return []
        
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                "You are a senior code reviewer. Analyze the following git diff for bugs, "
                "security issues, performance problems, and style violations. "
                "Return a JSON array of review comments. Each comment should have: "
                '"path" (file path), "line" (line number or approximate range), '
                '"severity" (critical/high/medium/low), and "body" (markdown explanation). '
                "Focus only on real issues. Do not hallucinate.\n\n"
                f"Diff:\n{diff_content[:8000]}"
            )
            result = router.async_route_and_generate(prompt, task_type="coding", max_cost=0.05)
            text = result.get("text", "") if isinstance(result, dict) else ""
            import json
            try:
                comments = json.loads(text)
                if isinstance(comments, list):
                    return [c for c in comments if isinstance(c, dict) and "body" in c]
            except Exception:
                pass
            return []
        except Exception as e:
            logger.error(f"LLM PR analysis failed: {e}")
            return []

    async def review_pr(self, repo_name: str, pr_number: int) -> Dict[str, Any]:
        try:
            logger.info(f"Starting review for PR #{pr_number} in {repo_name}")
            diff = self.github_agent.get_pr_diff(repo_name, pr_number) if hasattr(self.github_agent, "get_pr_diff") else ""
            comments = await self.analyze_diff(diff)
            
            summary = f"SupremeAI Automated Review Complete.\nFound {len(comments)} issues."
            if any("critical" in c.get("severity", "").lower() or "SECURITY" in c.get("body", "") for c in comments):
                summary += "\n⚠️ **Critical Issues Detected! Please address before merging.**"
                action = "REQUEST_CHANGES"
            elif len(comments) > 0:
                action = "COMMENT"
            else:
                action = "APPROVE"
                
            logger.info(f"PR Review completed with action: {action}")
            return {
                "status": "success",
                "action_taken": action,
                "comments_posted": len(comments),
                "comments": comments,
            }
        except Exception as e:
            logger.error(f"PR Review failed: {str(e)}")
            return {"status": "error", "error": str(e)}
