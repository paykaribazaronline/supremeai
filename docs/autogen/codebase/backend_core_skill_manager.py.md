# 📄 ফাইল: backend/core/skill_manager.py

**প্রকার:** .py  
**সাইজ:** 7,493 বাইট  
**আপডেট:** 2026-07-11T13:49:08.305711

---

## কোড

```py
# backend/core/skill_manager.py
import json

from loguru import logger

from core.llm_gateway import llm_gateway

# আমাদের প্রডাকশন ডাটাবেজ বা সুপাবেস ক্লায়েন্ট ইম্পোর্ট করো
from database.supabase_client import db


class DynamicSkillManager:
    def __init__(self):
        # ইন-মেমোরি ক্যাশ বাতিল, এখন সরাসরি সুপাবেস ক্লায়েন্ট কাজ করবে
        self.db = db.client
        # লিগ্যাসি ব্যাকওয়ার্ড কম্প্যাটিবিলিটির জন্য
        self.registry_path = "dummy_registry.json"
        self.skills = {"skills": {}}

    def get_skill(self, skill_name: str) -> dict | None:
        if skill_name in self.skills.get("skills", {}):
            return self.skills["skills"][skill_name]
        return {"skill_name": skill_name, "status": "active"}

    def register_skill(self, *args, **kwargs):
        """লিগ্যাসি কি-ওয়ার্ড আর্গুমেন্ট (name, uss) এবং নতুন ডিকশনারি ইনজেকশন উভয়ই হ্যান্ডেল করবে।"""
        skill_data = {}
        if args and isinstance(args[0], dict):
            skill_data = args[0]
        elif args:
            skill_data["skill_name"] = args[0]
            # Ruff E701 ফিক্স: এক লাইনে একাধিক স্টেটমেন্ট লেখা যাবে না
            if len(args) > 1:
                skill_data["version"] = args[1]
            if len(args) > 2:
                skill_data["description"] = args[2]
            if len(args) > 3:
                skill_data["entry_file"] = args[3]
            if len(args) > 4:
                skill_data["dependencies"] = args[4]
            skill_data.update(kwargs)
        else:
            skill_data = kwargs.get("skill_data") or kwargs

        final_data = skill_data.copy() if skill_data else {}
        if "name" in final_data and "skill_name" not in final_data:
            final_data["skill_name"] = final_data["name"]
        return self._save_skill_to_registry(final_data)

    async def get_or_create_skill(self, task_description: str) -> dict:
        """লোকাল সুপাবেস ডিবি চেক করবে, মিস হলে ১ বার প্রিমিয়াম এআই দিয়ে স্কিল জেনারেট করবে।"""

        # ১. লোকাল ডাটাবেজে সেমান্টিক বা টেক্সট সার্চ (Layer 1.5)
        existing_skill = await self._search_local_registry(task_description)
        if existing_skill:
            logger.info(f"🎯 [DB Hit] Reusable skill found in Supabase: '{existing_skill['skill_name']}'")
            return existing_skill

        # ২. ডাটাবেজে না থাকলে (Registry Miss) -> প্রিমিয়াম এলএলএম কল
        logger.warning("🚀 [DB Miss] Unique task scenario. Escalating to Claude-3.5-Sonnet for Skill Generation...")

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
            "description": "Detailed summary of what this skill does for future lookup",
            "execution_steps": [
                {{"action": "navigate", "url": "target_url"}},
                {{"action": "click", "selector": ".btn"}}
            ]
        }}
        """

        response = await llm_gateway.acompletion(prompt=prompt, system_prompt=system_prompt, model_filters=["claude-3-5-sonnet"])

        # এলএলএম মার্কডাউন কোডব্লক ট্র্যাপ ক্লিনআপ (সাইলেন্ট এরর ফিক্স)
        raw_text = response.get("text", "{}").strip()
        if raw_text.startswith("```"):
            lines = raw_text.splitlines()
            if lines[0].startswith("```"):
                lines = lines[1:]
            if lines[-1].startswith("```"):
                lines = lines[:-1]
            raw_text = "\n".join(lines).strip()

        try:
            new_skill = json.loads(raw_text)
            # ৩. ডাটাবেজে আজীবনের জন্য পারসিস্ট (Save) করা হচ্ছে
            self._save_skill_to_registry(new_skill)
            return new_skill
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to parse or register dynamic skill: {str(e)}")
            raise ValueError("Invalid JSON configuration from Skill Factory.")  # noqa: B904

    async def _search_local_registry(self, description: str):
        """Supabase থেকে ডেসক্রিপশন ম্যাচ করে কাস্টম স্কিল রেসিপি খুঁজবে।"""
        try:
            if not self.db:
                return None
            # প্রডাকশন রানিং কোয়েরি: টেক্সট ম্যাচিং (ভবিষ্যতে ক্রোমাডিবি ভেক্টরে আপগ্রেড হবে)
            response = self.db.table("tools_registry").select("*").ilike("description", f"%{description}%").execute()

            if response.data and len(response.data) > 0:
                # প্রথম ম্যাচিং স্কিলটি রিটার্ন করা হচ্ছে
                skill = response.data[0]
                return {
                    "skill_name": skill["skill_name"],
                    "description": skill["description"],
                    "execution_steps": skill["execution_steps"],  # PostgreSQL অটো JSON ডিকোড করবে
                }
            return None
        except Exception as e:  # noqa: BLE001
            logger.error(f"Supabase read error in Skill Manager: {str(e)}")
            return None

    def _save_skill_to_registry(self, skill_data: dict):
        """নতুন জেনারেট হওয়া স্কিলটি সুপাবেস টেবিলে ইনসার্ট করবে।"""
        try:
            if not self.db:
                return
            payload = {
                "skill_name": skill_data.get("skill_name"),
                "description": skill_data.get("description"),
                "execution_steps": skill_data.get("execution_steps"),  # JSONB ফিল্ডে সরাসরি ম্যাপ হবে
            }
            self.db.table("tools_registry").insert(payload).execute()
            logger.success(f"💾 [Supabase Persisted] Registered '{payload['skill_name']}' to global tools pool.")
        except Exception as e:  # noqa: BLE001
            logger.error(f"Supabase write error in Skill Manager: {str(e)}")

```