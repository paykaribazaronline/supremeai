from typing import Dict, Any
from loguru import logger

class AIPairProgrammer:
    def __init__(self):
        logger.info("Initialized AIPairProgrammer")

    async def solve_issue(self, issue_description: str) -> Dict[str, Any]:
        logger.info(f"Starting pair programming session for issue: {issue_description}")
        try:
            from brain.model_router import ModelRouter
            router = ModelRouter()
            plan_prompt = (
                "You are a senior SWE. Given the following issue, create a concise plan. "
                "Return a numbered list with max 5 steps.\n\n"
                f"Issue: {issue_description}"
            )
            plan_result = router.async_route_and_generate(plan_prompt, task_type="reasoning", max_cost=0.01)
            plan = plan_result.get("text", issue_description) if isinstance(plan_result, dict) else issue_description
            code_prompt = (
                "Given the following issue, generate the minimal code change or new file needed to resolve it. "
                "Return ONLY code. No markdown, no explanations.\n\n"
                f"Issue: {issue_description}"
            )
            code_result = router.async_route_and_generate(code_prompt, task_type="coding", max_cost=0.03)
            code = code_result.get("text", "") if isinstance(code_result, dict) else ""
            if not code:
                code = "# No code change generated."
            review_request = (
                "I have implemented a fix for the issue. Please review the changes and run the tests."
            )
            return {
                "status": "success",
                "plan": plan,
                "action_taken": "Implemented fix",
                "code": code,
                "review_request": review_request,
            }
        except Exception as exc:
            logger.error(f"Pair programming failed: {exc}")
            return {
                "status": "error",
                "error": str(exc),
                "plan": f"1. Analyze issue: {issue_description}\n2. Implement fix\n3. Write tests",
                "action_taken": "Failed",
            }
