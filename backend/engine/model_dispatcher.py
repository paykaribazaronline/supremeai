from typing import Any

from loguru import logger


try:
    import litellm
    HAS_LITELLM = True
except ImportError:
    HAS_LITELLM = False

try:
    from langsmith import traceable
    HAS_LANGSMITH = True
except ImportError:
    HAS_LANGSMITH = False


ROUTING_TABLE = [
    {"complexity_max": 3, "model": "ollama/llama3.2"},
    {"complexity_max": 6, "model": "openrouter/mistral-7b-free"},
    {"complexity_max": 8, "model": "gemini/gemini-flash"},
    {"complexity_max": 10, "model": "openai/gpt-4o-mini"},
]


def select_model(complexity: int, user_mode: str) -> str:
    for rule in ROUTING_TABLE:
        if complexity <= rule["complexity_max"]:
            return rule["model"]
    return ROUTING_TABLE[-1]["model"]


def get_fallback_chain(model: str) -> list[str]:
    chain = [r["model"] for r in ROUTING_TABLE]
    try:
        idx = chain.index(model)
        return chain[idx + 1 :] + chain[:idx]
    except ValueError:
        return chain


if HAS_LANGSMITH:
    @traceable(name="model_dispatch")
    async def dispatch(task: str, complexity: int, user_mode: str) -> dict[str, Any]:
        model = select_model(complexity, user_mode)
        if not HAS_LITELLM:
            return {"model": model, "text": "", "error": "litellm not installed"}
        try:
            response = litellm.acompletion(
                model=model,
                messages=[{"role": "user", "content": task}],
                fallbacks=get_fallback_chain(model),
            )
            return {"model": model, "text": response.choices[0].message.content}
        except Exception as exc:
            logger.error(f"Model dispatch failed: {exc}")
            return {"model": model, "text": "", "error": str(exc)}
else:
    async def dispatch(task: str, complexity: int, user_mode: str) -> dict[str, Any]:
        model = select_model(complexity, user_mode)
        return {"model": model, "text": "", "error": "langsmith not installed"}
