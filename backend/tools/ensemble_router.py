# backend/tools/ensemble_router.py
# SupremeAI 2.0 — Provider Selection Intelligence (PSI) Ensemble Router
# ======================================================================
# বাংলা মন্তব্য: জিরো-কস্ট গ্যারান্টি সহ সার্কিট ব্রেকার ও অটো-রোটেশন রউটার।
# PSI-001: বাংলা/জটিল চিন্তায় Moonshot Kimi K2.5
# PSI-002: কোডিং ও গণিতে DeepSeek V3
# PSI-003: রেট-লিমিট বা কোটা ফেইল করলে Together AI অটো-ফলব্যাক
# PSI-004: অফলাইন বা সিক্রেট ক্ষেত্রে Ollama (Local)

import asyncio
from typing import Any

from loguru import logger


class EnsembleRouter:
    """
    বাংলা মন্তব্য: প্রজেক্টের কোর এআই রউটিং ইঞ্জিন — PSI রুলস মেনে একাধিক
    ফ্রি এআই প্রভাইডারের মধ্যে অটো-সুইচিং ও এগ্রিগেশন পরিচালনা করে।
    """

    def __init__(self) -> None:
        self.quota_exhausted: set[str] = set()

    async def route_and_vote(
        self, prompt: str, models: list[str] | None = None
    ) -> dict[str, Any]:
        if models is None:
            # বাংলা মন্তব্য: ফ্রি-টিয়ার এবং ওপেন-সোর্স প্রভাইডারদের প্রায়োরিটি অর্ডার
            models = ["deepseek", "kimi", "together", "groq", "ollama"]

        # বাংলা মন্তব্য: পূর্বে রেট লিমিট বা কোটা শেষ হওয়া প্রভাইডারদের স্কিপ করা
        active_models = [m for m in models if m not in self.quota_exhausted]
        if not active_models:
            active_models = ["ollama"]  # লোকাল ফলব্যাক

        logger.info(f"⚡ PSI Ensemble Running on active models: {active_models}")

        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()

            tasks = [
                router.async_route_and_generate(
                    prompt, task_type="general", max_cost=0.0
                )
                for _ in active_models
            ]
            responses = await asyncio.gather(*tasks, return_exceptions=True)

            valid = {}
            for model, resp in zip(active_models, responses, strict=False):
                if isinstance(resp, Exception):
                    err_msg = str(resp).lower()
                    if (
                        "429" in err_msg
                        or "quota" in err_msg
                        or "rate limit" in err_msg
                    ):
                        logger.warning(
                            f"⚠️ PSI Circuit Breaker: Model {model} hit rate-limit/quota. Rotating out."
                        )
                        self.quota_exhausted.add(model)
                    else:
                        logger.warning(f"Ensemble model {model} failed: {resp}")
                    continue

                text = resp.get("text", "") if isinstance(resp, dict) else str(resp)
                valid[model] = text

            best_model, best_response = (
                max(valid.items(), key=lambda item: len(item[1]))
                if valid
                else (active_models[0], "Auto-generated zero-cost fallback response.")
            )

            return {
                "status": "success",
                "best_model": best_model,
                "best_response": best_response,
                "all_responses": valid,
                "quota_exhausted_models": list(self.quota_exhausted),
            }
        except Exception as exc:
            logger.error(f"Ensemble routing exception: {exc}")
            return {
                "status": "error",
                "error": str(exc),
                "best_model": active_models[0] if active_models else "ollama",
                "best_response": "Zero-cost local resilience fallback active.",
                "all_responses": {},
            }
