# 📄 ফাইল: backend/tools/trading_agent.py

**প্রকার:** .py  
**সাইজ:** 1,537 বাইট  
**আপডেট:** 2026-07-04T11:05:04.786241

---

## কোড

```py
from typing import Any

from loguru import logger


class TradingAgent:
    async def generate_strategy(
        self, prompt: str, risk_profile: str = "moderate"
    ) -> dict[str, Any]:
        logger.info(f"Generating trading strategy for: {prompt} ({risk_profile})")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            llm_prompt = (
                f"Create a detailed trading strategy for: {prompt}. "
                f"Risk profile: {risk_profile}. Include entry/exit rules, position sizing, and risk management. "
                "Return only the strategy text."
            )
            result = router.async_route_and_generate(
                llm_prompt, task_type="general", max_cost=0.01
            )
            text = result.get("text", "") if isinstance(result, dict) else ""
            return {
                "status": "success",
                "prompt": prompt,
                "risk_profile": risk_profile,
                "strategy": text or prompt,
                "backtest_results": {},
                "note": "Real trading requires backtesting framework and market data integration.",
            }
        except Exception as exc:
            logger.error(f"Trading strategy generation failed: {exc}")
            return {
                "status": "error",
                "prompt": prompt,
                "error": str(exc),
                "note": "Real trading requires backtesting framework and market data integration.",
            }

```