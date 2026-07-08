# 📄 ফাইল: backend/core/skill_manager.py

**প্রকার:** .py  
**সাইজ:** 5,406 বাইট  
**আপডেট:** 2026-07-08T11:32:31.849676

---

## কোড

```py
# backend/core/skill_manager.py
import json

from loguru import logger

from core.llm_gateway import llm_gateway  # আপনার কোডের এক্সিস্টিং গেটওয়ে


class DynamicSkillManager:
    def __init__(self, chroma_client=None):
        # এখানে আপনার ক্রোমাডিবি বা লোকাল ভেক্টর ডিবি ক্লায়েন্ট ইনজেক্ট হবে
        self.db = chroma_client
        self._temporary_memory_registry = {}

    async def get_or_create_skill(self, task_description: str) -> dict:
        """লোকাল রেজিস্ট্রি চেক করবে, মিস হলে প্রিমিয়াম এআই দিয়ে ১ বার নতুন স্কিল জেনারেট করবে।"""

        # ১. লোকাল স্কিল ডাটাবেজে সিমান্টিক সার্চ (MOCK - আপনার ডিবি অনুযায়ী কানেক্ট করবেন)
        existing_skill = await self._search_local_registry(task_description)
        if existing_skill:
            logger.info(f"🎯 Local Skill Hit! Using existing tool recipe for: '{task_description}'")
            return existing_skill

        # ২. লোকাল ডাটাবেজে না থাকলে (Registry Miss) -> প্রিমিয়াম এআই দিয়ে ১ বার স্কিল জেনারেট
        logger.warning("🚀 New unique task detected. Generating a reusable skill via Premium LLM...")

        system_prompt = (
            "You are SupremeAI's Skill Architect. Your sole job is to generate a reusable, structural "
            "step-by-step automation blueprint for a Playwright browser agent based on user request. "
            "You must return ONLY a raw valid JSON object. No conversation, no markdown codeblocks."
        )

        prompt = f"""
        Create a functional automation extraction schema for the following task: '{task_description}'.
        The output format must strictly be JSON matching this shape:
        {{
            "skill_name": "UniqueCamelCaseName",
            "description": "Detailed summary of what this skill does for future semantic lookup",
            "execution_steps": [
                {{"action": "navigate", "url": "target_url"}},
                {{"action": "type", "selector": "#input-id", "value": "context_data"}},
                {{"action": "click", "selector": ".submit-btn"}},
                {{"action": "extract", "selector": "table.results"}}
            ]
        }}
        """

        response = await llm_gateway.acompletion(
            prompt=prompt,
            system_prompt=system_prompt,
            model_filters=["claude-3-5-sonnet"]
        )

        try:
            raw_text = response.get("text", "{}").strip()

            # CRITICAL FIX (LLM Markdown Trap):
            # ক্লড (Claude) বা জিপিটি অনেক সময় উত্তরের শুরুতে/শেষে ```json জুড়ে দেয়।
            # সরাসরি json.loads() করলে এটি ক্র্যাশ করবে, তাই পার্স করার আগে স্ট্রিংটি ক্লিন করা বাধ্যতামূলক।
            if raw_text.startswith("```"):
                lines = raw_text.splitlines()
                if lines[0].startswith("```"):
                    lines = lines[1:]
                if lines and lines[-1].startswith("```"):
                    lines = lines[:-1]
                raw_text = "\n".join(lines).strip()

            new_skill = json.loads(raw_text)
            # ৩. ভবিষ্যতের জন্য এই নতুন স্কিল বা টুলটি সেভ করে রাখা (০ কস্ট লেয়ার সক্রিয় করা)
            await self._save_skill_to_registry(new_skill)
            logger.success(f"💾 Successfully registered new skill: {new_skill.get('skill_name')} to Zero-Cost Pool.")
            return new_skill
        except json.JSONDecodeError as je:
            logger.error(f"LLM returned invalid JSON string: {raw_text}")
            raise ValueError("Failed to parse AI generated skill due to formatting.") from je
        except Exception as e:  # noqa
            logger.error(f"Failed to parse or register dynamic skill: {str(e)}")
            raise ValueError("Invalid configuration returned from Skill Factory.") from e

    async def _search_local_registry(self, description: str):
        # টাস্কের ডেসক্রিপশন যদি আগে তৈরি করা কোনো স্কিলের ডেসক্রিপশনের সাথে মিলে যায়
        for _, data in self._temporary_memory_registry.items():
            if data.get("description", "").lower() in description.lower() or description.lower() in data.get("description", "").lower():
                return data
        return None

    async def _save_skill_to_registry(self, skill_data: dict):
        # ইন-মেমোরি ডিকশনারিতে সেভ করে রাখা
        name = skill_data.get("skill_name", "UnknownSkill")
        self._temporary_memory_registry[name] = skill_data

```