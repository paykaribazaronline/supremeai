import json

from loguru import logger
from sqlalchemy.ext.asyncio import AsyncSession

from core.llm_gateway import llm_gateway
from models.dynamic_agent import DynamicAgent


class DynamicAgentFactory:
    """
    এজেন্ট ফ্যাক্টরি যা রিকোয়েস্ট অনুযায়ী ডাইনামিকালি কাস্টম এজেন্ট কনফিগারেশন তৈরি ও ডাটাবেজে রেজিস্ট্রি করে (অ্যাসিনক্রোনাস)।
    """

    def __init__(self, db_session: AsyncSession = None):
        self.db = db_session

    def get_registered_agent(self, agent_name: str) -> dict | None:
        from pathlib import Path

        registry_path = Path(__file__).resolve().parent / "agent_registry.json"
        if not registry_path.exists():
            return None

        try:
            with open(registry_path, encoding="utf-8") as f:
                registry = json.load(f)
                return registry.get(agent_name)
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to read agent_registry.json: {e}")
            return None

    async def create_specialized_agent(self, task_description: str) -> dict:
        """
        বাংলা মন্তব্য: প্রিমিয়াম এআই ব্যবহার করে টাস্কের জন্য একটি এক্সিকিউটেবল পাইথন স্ক্রিপ্ট তৈরি করবে।
        এটি এখন আর শুধু ব্রাউজার অটোমেশন নয়, বরং যেকোনো কাজের জন্য একটি "Synthesized Skill"।
        """
        logger.info(f"Generating a new autonomous agent for task: {task_description}")

        system_prompt = (
            "You are the SupremeAI Agent Factory. Your job is to generate a Python script that can solve the given task. "
            "The script should be self-contained and executable. "
            "Return a raw JSON object containing 'agent_name', 'description', and 'script' (the Python code as a string). "
            "Do not return conversational text or markdown codeblocks."
        )

        # আগের execution_steps ভিত্তিক প্রম্পট এখন আরও জেনেরিক পাইথন স্ক্রিপ্টে পরিবর্তিত হয়েছে
        prompt = f"Create a Python script to solve this task: '{task_description}'"

        # প্রিমিয়াম এআই দিয়ে ১ বার খরচ করে এজেন্টের স্ক্রিপ্ট বানিয়ে নেওয়া
        response = await llm_gateway.acompletion(
            prompt=prompt,
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
                "script": "print('Error: AI failed to generate a valid script.')",
            }

        # ডাটাবেজে আজীবনের জন্য সেভ করে রাখা
        await self._save_agent_to_registry(
            name=agent_config.get("agent_name"),
            description=agent_config.get("description", task_description),
            steps=agent_config.get("script"), # execution_steps এখন script
        )

        return agent_config

    async def _save_agent_to_registry(self, name: str, description: str, steps: list):
        try:
            from sqlalchemy import select

            stmt = select(DynamicAgent).where(DynamicAgent.name == name)
            result = await self.db.execute(stmt)
            existing = result.scalars().first()
            if existing:
                existing.execution_steps = {"script": steps} # সামঞ্জস্যের জন্য script-কে JSON-এ মোড়ানো হলো
                existing.description = description
            else:
                new_agent = DynamicAgent(name=name, description=description, execution_steps=steps)
                self.db.add(new_agent)
            await self.db.commit()
            logger.success(f"🧠 [AgentFactory] New skill learned and registered: '{name}'")
        except Exception as exc:  # noqa: BLE001
            await self.db.rollback()
            logger.error(f"Failed to save dynamic agent to registry: {exc}")
