import asyncio
from typing import Any

from loguru import logger


class EnsembleRouter:
    async def route_and_vote(self, prompt: str, models: list[str] | None = None) -> dict[str, Any]:
        if models is None:
            models = ["openrouter", "gemini", "groq", "deepseek"]
        logger.info(f"Running ensemble on models: {models}")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            tasks = [router.async_route_and_generate(prompt, task_type="general", max_cost=0.05) for _ in models]
            responses = await asyncio.gather(*tasks, return_exceptions=True)
            valid = {}
            for model, resp in zip(models, responses, strict=False):
                if isinstance(resp, Exception):
                    logger.warning(f"Ensemble model {model} failed: {resp}")
                    continue
                text = resp.get("text", "") if isinstance(resp, dict) else ""
                valid[model] = text
            best_model, best_response = max(valid.items(), key=lambda item: len(item[1])) if valid else (models[0], "")
            return {
                "status": "success",
                "best_model": best_model,
                "best_response": best_response,
                "all_responses": valid,
            }
        except Exception as exc:
            logger.error(f"Ensemble routing failed: {exc}")
            return {
                "status": "error",
                "error": str(exc),
                "best_model": models[0] if models else "unknown",
                "best_response": "",
            }
