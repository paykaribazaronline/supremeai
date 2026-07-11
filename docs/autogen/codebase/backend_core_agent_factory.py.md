# 📄 ফাইল: backend/core/agent_factory.py

**প্রকার:** .py  
**সাইজ:** 3,563 বাইট  
**আপডেট:** 2026-07-11T11:29:21.194219

---

## কোড

```py
import json

from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession

from core.llm_gateway import llm_gateway
from models.dynamic_agent import DynamicAgent


class DynamicAgentFactory:
    """
    এজেন্ট ফ্যাক্টরি যা রিকোয়েস্ট অনুযায়ী ডাইনামিকালি কাস্টম এজেন্ট কনফিগারেশন তৈরি ও ডাটাবেজে রেজিস্ট্রি করে (অ্যাসিনক্রোনাস)।
    """

    def __init__(self, db_session: AsyncSession):
        self.db = db_session

    async def create_specialized_agent(self, task_description: str) -> dict:
        """
        প্রিমিয়াম এআই ব্যবহার করে ওয়ান-টাইম এজেন্ট স্ক্রিপ্ট বানাবে।
        """
        logger.info(f"Generating a new autonomous agent for task: {task_description}")

        system_prompt = (
            "You are the SupremeAI Agent Factory. Your job is to output a raw JSON configuration "
            "and structural flow steps that a Python Playwright browser can execute locally. "
            "Do not return conversational text, return only valid JSON containing 'agent_name', "
            "'description', and 'execution_steps' (a list of actions)."
        )

        # প্রিমিয়াম এআই দিয়ে ১ বার খরচ করে এজেন্টের স্ক্রিপ্ট বানিয়ে নেওয়া
        response = await llm_gateway.acompletion(
            prompt=f"Create a custom browser extraction script for: {task_description}",
            system_prompt=system_prompt,
            model_filters=["claude-3-5-sonnet"],
        )

        try:
            agent_config = json.loads(response.get("text"))
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to parse AI generated agent configuration JSON: {e}")
            import time

            agent_config = {
                "agent_name": f"AutoAgent_{int(time.time())}",
                "description": task_description,
                "execution_steps": [{"action": "navigate", "value": "contextual_url"}],
            }

        # ডাটাবেজে আজীবনের জন্য সেভ করে রাখা
        await self._save_agent_to_registry(
            name=agent_config.get("agent_name"),
            description=agent_config.get("description", task_description),
            steps=agent_config.get("execution_steps", []),
        )

        return agent_config

    async def _save_agent_to_registry(self, name: str, description: str, steps: list):
        try:
            from sqlalchemy import select

            stmt = select(DynamicAgent).where(DynamicAgent.name == name)
            result = await self.db.execute(stmt)
            existing = result.scalars().first()
            if existing:
                existing.execution_steps = steps
                existing.description = description
            else:
                new_agent = DynamicAgent(name=name, description=description, execution_steps=steps)
                self.db.add(new_agent)
            await self.db.commit()
            logger.success(f"🧠 [AgentFactory] New skill learned and registered: '{name}'")
        except Exception as exc:  # noqa: BLE001
            await self.db.rollback()
            logger.error(f"Failed to save dynamic agent to registry: {exc}")

```