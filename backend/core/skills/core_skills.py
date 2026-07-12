# backend/core/skills/core_skills.py
from typing import Any

from core.llm_gateway import llm_gateway
from core.skills.base import BaseSkill
from models.shared_workspace import SharedWorkspace


class SystemDesignSkill(BaseSkill):
    """
    বাংলা মন্তব্য: সিস্টেম আর্কিটেকচার ডিজাইন করার জন্য একটি কোর স্কিল।
    """

    @property
    def name(self) -> str:
        return "SystemDesignSkill"

    async def execute(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash", **kwargs: Any) -> Any:
        workspace.log(f"{self.name}: Starting system architecture layout analysis...")
        sys_prompt = "You are a lead system architect. Define file structures, component breakdown, and database schemas."
        user_prompt = f"Design architecture for task: {workspace.original_prompt}"

        messages = [{"role": "system", "content": sys_prompt}, {"role": "user", "content": user_prompt}]
        resp = await llm_gateway.acompletion(model=model_name, messages=messages, user_id=user_id)
        return resp.get("choices", [{}])[0].get("message", {}).get("content", "")
