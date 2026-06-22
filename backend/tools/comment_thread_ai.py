import asyncio
from typing import Dict, Any, List
from loguru import logger

class CommentThreadAI:
    def __init__(self):
        logger.info("Initialized CommentThreadAI")

    async def handle_pr_comment(self, repo_full_name: str, pr_number: int, comment_body: str, file_path: str, line_number: int) -> Dict[str, Any]:
        logger.info(f"Handling PR comment on {repo_full_name} PR #{pr_number} at {file_path}:{line_number}")
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            prompt = (
                "You are a senior developer. A reviewer left the following comment on a PR. "
                "Propose the minimal code change that resolves the issue. "
                "Return ONLY the replacement lines for the mentioned file/line. No markdown.\n\n"
                f"File: {file_path}\nLine: {line_number}\nComment: {comment_body}\n"
            )
            result = router.async_route_and_generate(prompt, task_type="coding", max_cost=0.02)
            text = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "action": "commit_generated",
                "proposed_fix": text,
                "message": "Proposed fix for PR comment.",
            }
        except Exception as exc:
            logger.error(f"Comment resolution generation failed: {exc}")
            return {
                "status": "error",
                "action": "failed",
                "error": str(exc),
            }
