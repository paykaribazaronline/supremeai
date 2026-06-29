# Model Router for SupremeAI 2.0 (Refactored Thin Wrapper)
# বাংলা মন্তব্য: এটি পুরানো রাউটিং লজিকগুলোর বদলে সরাসরি নতুন llm_gateway.py এর মাধ্যমে রিকোয়েস্ট ফরোয়ার্ড করে।

import asyncio
from typing import Any, Dict
from loguru import logger
from core.llm_gateway import llm_gateway

def run_async_as_sync(coro):
    from concurrent.futures import ThreadPoolExecutor
    try:
        loop = asyncio.get_running_loop()
    except RuntimeError:
        loop = None

    if loop and loop.is_running():
        with ThreadPoolExecutor(max_workers=1) as executor:
            future = executor.submit(asyncio.run, coro)
            return future.result()
    else:
        return asyncio.run(coro)

class ModelRouter:
    """
    Thin wrapper over LLMGateway for backward compatibility.
    """
    def __init__(self):
        logger.info("Initializing refactored ModelRouter (LiteLLM Wrapper)")

    def route_and_generate(
        self, prompt: str, task_type: str = "general", max_cost: float = 0.01
    ) -> Dict[str, Any]:
        return run_async_as_sync(
            self.async_route_and_generate(prompt, task_type, max_cost)
        )

    async def async_route_and_generate(
        self, prompt: str, task_type: str = "general", max_cost: float = 0.01
    ) -> Dict[str, Any]:
        logger.info(f"[ModelRouter] Forwarding task_type='{task_type}' to LLMGateway")
        try:
            # Delegate directly to our new LiteLLM universal gateway
            response = await llm_gateway.acompletion(
                prompt=prompt,
                task_type=task_type,
                stream=False
            )
            return response
        except Exception as e:
            logger.error(f"[ModelRouter] Gateway completion failed: {e}")
            return {
                "success": False,
                "error": str(e)
            }
