# backend/core/skill_manager.py
import json
from typing import Any

from loguru import logger

from core.llm.llm_gateway import llm_gateway
from core.mcp_client import MCPRegistryClient
from core.skills.base import BaseSkill


class SkillManager:
    """
    বাংলা মন্তব্য: Skill-as-a-Service আর্কিটেকচারের কেন্দ্রবিন্দু।
    এই ম্যানেজার রানটাইমে স্কিল রেজিস্টার, ডিসকভার এবং ডিসপ্যাচ করে।
    এটি এখন এক্সিকিউটেবল BaseSkill অবজেক্ট নিয়ে কাজ করে।
    """

    def __init__(self):
        self._skills: dict[str, BaseSkill] = {}
        self.mcp_client = MCPRegistryClient()
        logger.info("SkillManager initialized for dynamic skill dispatch.")

    def register_skill(self, skill: BaseSkill, name: str | None = None):
        """
        বাংলা মন্তব্য: একটি স্কিল ইনস্ট্যান্স রেজিস্টারে যোগ করে।
        """
        skill_name = name or skill.name
        if skill_name in self._skills:
            logger.warning(f"Skill '{skill_name}' is already registered. Overwriting.")
        self._skills[skill_name] = skill
        logger.info(f"Skill '{skill_name}' successfully registered.")

    async def get_skill(self, skill_name: str) -> BaseSkill:
        """
        বাংলা মন্তব্য: রেজিস্ট্রি থেকে একটি স্কিল খুঁজে বের করে।
        যদি লোকালি না পাওয়া যায়, তবে MCP থেকে খোঁজার চেষ্টা করে।
        """
        if skill_name in self._skills:
            logger.debug(f"Found skill '{skill_name}' in local registry.")
            return self._skills[skill_name]

        logger.info(f"Skill '{skill_name}' not in local registry. Querying Database...")
        from tools.mcp.mcp_supabase import ExecuteQueryInput
        from tools.mcp.mcp_supabase import ResponseFormat
        from tools.mcp.mcp_supabase import supabase_execute_sql

        try:
            query = "SELECT code FROM skills WHERE skill_name = %s AND status = 'active'"
            res = await supabase_execute_sql(ExecuteQueryInput(query=query, params=[skill_name], response_format=ResponseFormat.JSON))
            data = json.loads(res)
            if "rows" in data and len(data["rows"]) > 0:
                code_content = data["rows"][0]["code"]
                local_env = {}
                # Ensure BaseSkill is available
                exec_globals = globals().copy()
                exec(code_content, exec_globals, local_env)

                for item in local_env.values():
                    if isinstance(item, type) and issubclass(item, BaseSkill) and item != BaseSkill:
                        skill_instance = item()
                        self.register_skill(skill_instance, skill_name)
                        return skill_instance
        except Exception as e:  # noqa: BLE001
            logger.error(f"Error fetching skill '{skill_name}' from DB: {e}")

        logger.info(f"Skill '{skill_name}' not in DB. Querying MCP-Hub...")
        mcp_tools = await self.mcp_client.discover_tools(domain=skill_name)
        if mcp_tools and mcp_tools[0] != "generic_tool":
            logger.info(f"Discovered MCP tool for '{skill_name}'. Wrapping as a skill.")
            # একটি র‍্যাপার তৈরি করে রেজিস্টার করা যেতে পারে
            # from core.skills.base import MCPSkillWrapper
            # mcp_skill = MCPSkillWrapper(tool_name=skill_name, mcp_client=self.mcp_client)
            # self.register_skill(mcp_skill)
            # return mcp_skill
            pass

        raise ValueError(f"Skill '{skill_name}' not found in local registry or MCP.")

    async def synthesize_skill_schema(self, task_description: str) -> dict[str, Any]:
        """
        বাংলা মন্তব্য: LLM ব্যবহার করে একটি নতুন স্কিলের JSON স্কিমা তৈরি করে।
        এটি আগের get_or_create_skill এর মূল লজিকটি ধারণ করে।
        """
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
            "parameters": [
                {{"name": "url", "type": "string", "description": "Target URL"}}
            ],
            "execution_steps": [
                {{"action": "navigate", "url": "target_url"}},
                {{"action": "click", "selector": ".btn"}}
            ]
        }}
        """
        response = await llm_gateway.acompletion(prompt=prompt, system_prompt=system_prompt, model_filters=["claude-3-5-sonnet"])

        raw_text = response.get("text", "{}").strip()
        if raw_text.startswith("```"):
            lines = raw_text.splitlines()
            raw_text = "\n".join(lines[1:-1] if lines.startswith("```") and lines[-1].startswith("```") else lines)
            raw_text = "\n".join(lines).strip()

        try:
            new_skill = json.loads(raw_text)
            logger.success(f"Synthesized new skill schema: '{new_skill.get('skill_name')}'")
            return new_skill
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to parse synthesized skill schema: {str(e)}")
            raise ValueError("Invalid JSON configuration from Skill Factory.")  # noqa: B904


# Global singleton instance
skill_manager = SkillManager()
