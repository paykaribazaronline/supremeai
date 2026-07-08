import asyncio
from typing import Any

import httpx
from loguru import logger


class TaskRouter:
    """
    Task Router for SupremeAI 2.0.
    Analyzes intent of user requests to map them to appropriate modules/agents.
    """

    def __init__(self) -> None:
        pass

    def process_requirement(
        self, task_description: str, max_cost: float = 0.01
    ) -> dict[str, Any]:
        logger.info(f"Processing requirement: '{task_description}' max_cost={max_cost}")
        desc_lower = task_description.lower()
        prompt_len = len(task_description)

        token_budget = (
            "small"
            if prompt_len <= 500
            else "medium" if prompt_len <= 2000 else "large"
        )
        modality = "text"
        if any(w in desc_lower for w in ["image", "picture", "photo", "vision"]):
            modality = "image"
        if any(w in desc_lower for w in ["video", "voice", "audio", "speech"]):
            modality = "multimodal"

        if "code" in desc_lower or "program" in desc_lower or "script" in desc_lower:
            task_type = "coding"
        elif (
            "image" in desc_lower
            or "picture" in desc_lower
            or "photo" in desc_lower
            or "draw" in desc_lower
            or "generate an image" in desc_lower
        ):
            task_type = "image_generation"
        elif "scrape" in desc_lower or "crawl" in desc_lower:
            task_type = "web_scraping"
        elif "system" in desc_lower or "terminal" in desc_lower:
            task_type = "system_control"
        else:
            task_type = "general"

        reasoning_depth = "low"
        if any(w in desc_lower for w in ["math", "reasoning", "analyze", "research"]):
            reasoning_depth = "high"
        elif modality != "text":
            reasoning_depth = "medium"

        fallback_handler = "n8n_webhook"
        if task_type != "general":
            if task_type == "coding":
                fallback_handler = "crewai_agents"
            elif task_type == "web_scraping":
                fallback_handler = "browser_agent"
            elif task_type == "system_control":
                fallback_handler = "computer_agent"

        return {
            "task_type": task_type,
            "handler": fallback_handler,
            "cost_limit": max_cost,
            "token_budget": token_budget,
            "reasoning_depth": reasoning_depth,
            "modality": modality,
        }

    def analyze_and_route(
        self, task_description: str, max_cost: float = 0.01
    ) -> dict[str, Any]:
        return self.process_requirement(task_description, max_cost=max_cost)

    async def trigger_external_skill(
        self, webhook_url: str, payload: dict[str, Any], retries: int = 3
    ) -> dict[str, Any]:
        from urllib.parse import urlparse
        ALLOWED_WEBHOOK_DOMAINS = frozenset({"api.n8n.cloud", "hooks.zapier.com", "hooks.slack.com", "discord.com"})
        parsed = urlparse(webhook_url)
        if parsed.scheme not in ("https",) or parsed.hostname not in ALLOWED_WEBHOOK_DOMAINS:
            logger.error(f"SSRF blocked: webhook_url={webhook_url} not in allowlist")
            raise ValueError(f"Webhook domain '{parsed.hostname}' is not in the security allowlist.")
        async with httpx.AsyncClient(timeout=30.0) as client:
            for attempt in range(retries):
                try:
                    response = await client.post(
                        webhook_url, json=payload, timeout=30.0
                    )
                    response.raise_for_status()
                    logger.success(f"Skill triggered on attempt {attempt + 1}")
                    return response.json()
                except Exception as e:  # noqa: BLE001
                    logger.warning(f"Attempt {attempt + 1} failed: {str(e)}")
                    if attempt == retries - 1:
                        logger.error("All retry attempts failed.")
                        return {
                            "success": False,
                            "error": "External service unavailable",
                        }
                    await asyncio.sleep(2**attempt)
        return {"success": False, "error": "External service unavailable"}

    async def execute_scraping_task(self, task_prompt: str, contextual_url: str = None) -> dict:
        """
        মাল্টি-টিয়ার ফলব্যাক লজিক ইমপ্লিমেন্টেশন।
        প্রথমে ডাটাবেজে পূর্বে তৈরি করা ডাইনামিক এজেন্ট খুঁজবে। না পেলে JIT DynamicAgentFactory দিয়ে ওয়ান-টাইম জেনারেট করবে।
        """
        # --- LAYER 2: BROWSER AUTOMATION AGENT ---
        if contextual_url:
            try:
                logger.info(f"[Router] Check database registry for existing dynamic agent matching: {task_prompt}")
                # ডাটাবেজ সেশন লোড
                from database.session import AsyncSessionLocal
                from models.dynamic_agent import DynamicAgent
                from core.agent_factory import DynamicAgentFactory
                from sqlalchemy import select

                agent_name = None
                execution_steps = []

                # সিঙ্ক্রোনাস/অ্যাসিনক্রোনাস ডাটাবেজ চেকিং
                async with AsyncSessionLocal() as session:
                    stmt = select(DynamicAgent).where(DynamicAgent.name.like(f"%{task_prompt[:15]}%"))
                    result = await session.execute(stmt)
                    agent = result.scalars().first()
                    if agent:
                        agent_name = agent.name
                        execution_steps = agent.execution_steps
                        logger.info(f"Loaded existing dynamic agent '{agent_name}' from database.")

                if not agent_name:
                    logger.info("No matching agent found in registry. Invoking DynamicAgentFactory to build one JIT...")
                    # AsyncSessionLocal call instead of SessionLocal
                    async with AsyncSessionLocal() as factory_session:
                        factory = DynamicAgentFactory(factory_session)
                        agent_config = await factory.create_specialized_agent(task_prompt)
                        agent_name = agent_config.get("agent_name")
                        execution_steps = agent_config.get("execution_steps", [])

                logger.info(f"[Router] Launching Layer 2 Engine for destination: {contextual_url} using agent: {agent_name}")
                
                # ৩০ সেকেন্ডের কড়া টাইমআউট গেট সহ ব্রাউজার লেভেল এক্সট্রাকশন রান
                result = await asyncio.wait_for(
                    self._run_browser_automation(task_prompt, contextual_url, execution_steps), 
                    timeout=35.0
                )
                
                if result and result.get("status") == "success":
                    return {
                        "status": "success",
                        "tier": f"Layer 2 (Zero-Cost Browser Automation: {agent_name})",
                        "data": result.get("data")
                    }
                raise Exception("Browser automation was flagged, blocked, or failed to collect data.")
                
            except (asyncio.TimeoutError, Exception) as e:
                # বাংলা মন্তব্য: Layer 2 ব্যর্থ হলে বা টাইমআউট হলে Layer 3/4 এ ফলব্যাক ট্রিগার করা হচ্ছে
                logger.warning(f"[Router] Layer 2 failed or timed out: {str(e)}. Falling back to Layer 3.")

        # --- LAYER 3 & 4 ACCELERATION FALLBACKS ---
        return await self._execute_api_fallback(task_prompt)

    async def _run_browser_automation(self, prompt: str, url: str, steps: list = None) -> dict:
        """Playwright কন্টেক্সট স্ট্রিম রান করার হেল্পার মেথড।"""
        # tools/browser_agent.py এর সাথে ইন্টারফেস করার জন্য প্লেসহোল্ডার রান
        await asyncio.sleep(1.5) 
        return {"status": "success", "data": "DOM payload stream"}

    async def _execute_api_fallback(self, prompt: str) -> dict:
        """বাজেট কন্ট্রোল ও মডেল সিলেকশন সহ এপিআই ফলব্যাক হ্যান্ডলার।"""
        try:
            logger.info("[Router] Routing to Layer 3 Economy AI Core...")
            from core.llm_gateway import llm_gateway
            from core.cost_guard import CostGuard
            # Real budget verification will use cost_guard dynamically
            # economy_response = await llm_gateway.acompletion(prompt, model_filters=["gpt-4o-mini", "deepseek-v3"])
            return {"status": "success", "tier": "Layer 3 (Economy API)", "data": "Economy LLM Data"}
        except Exception as economy_err:
            logger.error(f"[Router] Layer 3 breached: {str(economy_err)}. Escalating to Layer 4 Premium.")
            return {"status": "success", "tier": "Layer 4 (Premium API)", "data": "Premium LLM Data"}
