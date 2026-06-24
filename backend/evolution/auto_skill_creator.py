import os
from typing import Optional, Any
import google.generativeai as genai
from loguru import logger
from datetime import datetime, timezone

# আমাদের হার্ডেনড স্যান্ডবক্স গেটকিপার ইম্পোর্ট
try:
    from fuzz_sandbox import run_sandbox_ast_check, SecurityError
    from backend.core.tenant_db import TenantAwareFirestore
except ImportError:
    try:
        from backend.tools.fuzz_sandbox import run_sandbox_ast_check, SecurityError
    except ImportError:
        from tools.fuzz_sandbox import run_sandbox_ast_check, SecurityError
    try:
        from backend.core.tenant_db import TenantAwareFirestore
    except ImportError:
        from core.tenant_db import TenantAwareFirestore

class AutoSkillCreator:
    """
    Self-Evolution Engine Core.
    Autonomously generates, validates, and provisions dynamic AI skills/tools on-the-fly.
    """
    def __init__(self, db: Optional[TenantAwareFirestore] = None, **kwargs: Any):
        # 🛡️ এখন আর সরাসরি firestore.Client() কল হবে না!
        self.db = db
        if db is not None:
            self.skills_ref = self.db.collection("supreme_dynamic_skills")
        else:
            # Fallback mock or default
            try:
                from core.gcp_firestore import get_firestore_client
                client = get_firestore_client()
                if client is not None:
                    self.skills_ref = client.collection("supreme_dynamic_skills")
                else:
                    class MockRef:
                        def document(self, *args, **kwargs):
                            class MockDoc:
                                def set(self, *args, **kwargs):
                                    pass
                            return MockDoc()
                    self.skills_ref = MockRef()
            except Exception:
                class MockRef:
                    def document(self, *args, **kwargs):
                        class MockDoc:
                            def set(self, *args, **kwargs):
                                pass
                        return MockDoc()
                self.skills_ref = MockRef()
        
        # জেমিনি এপিআই কনফিগারেশন (Secret Vault থেকে মেমরিতে ইনজেক্টেড)
        genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
        self.model = genai.GenerativeModel("gemini-1.5-pro")

    async def generate_and_deploy_skill(self, user_demand: str, skill_name: str) -> dict:
        logger.info(f"🧠 Self-Evolution Triggered: Designing skill '{skill_name}' for demand: '{user_demand}'")
        
        # ১. এআই ডিরেক্টিভ প্রম্পট - যা সুনির্দিষ্ট ও কঠোর পাইথন সিনট্যাক্স মেনে কোড তৈরি করবে
        system_prompt = (
            "You are the SupremeAI 2.0 Self-Evolution Engine. Your job is to output pure Python code "
            "for a dynamic skill class. You must return ONLY valid executable Python code wrapped in "
            "```python ... \n``` blocks. No markdown explanations outside the block, no variables initialization "
            "using banned keys like eval, exec, compile, or dunder reflection attributes.\n\n"
            f"The skill must achieve: {user_demand}\n"
            f"The class name must be strictly: {skill_name}\n"
            "The class must implement an async def execute(self, kwargs) -> dict method."
        )

        try:
            # ২. অন-দি-ফ্লাই কোড জেনারেশন
            response = self.model.generate_content(system_prompt)
            raw_content = response.text
            
            # এক্সট্রাক্ট পিওর পাইথন কোড
            if "```python" in raw_content:
                code_block = raw_content.split("```python")[1].split("```")[0].strip()
            else:
                code_block = raw_content.strip()

            # 🛡️ ৩. দ্য আলটিমেট স্যান্ডবক্স গেটকিপার ভ্যালিডেশন (The Iron Cage Check)
            # ডাটাবেসে সেভ হওয়ার আগেই আমাদের হার্ডেনড AST লজিক দিয়ে কোডটি স্ক্যান করা হচ্ছে
            try:
                is_safe = run_sandbox_ast_check(code_block)
                if not is_safe:
                    raise SecurityError("Generated code code failed AST layout normalization.")
            except SecurityError as sec_err:
                logger.critical(f"🚨 [EVOLUTION BLOCKED] AI generated a dangerous skill payload! Threat defused: {str(sec_err)}")
                return {"success": False, "error": f"Security Sandbox Violation: {str(sec_err)}"}

            # ৪. ফায়ারস্টোরে লাইভ ডেপ্লয়মেন্ট ও সিনক্রোনাইজেশন
            now = datetime.now(timezone.utc)
            skill_meta = {
                "skill_name": skill_name,
                "demand_justification": user_demand,
                "generated_code": code_block,
                "status": "ACTIVE",
                "deployed_at": now
            }
            
            self.skills_ref.document(skill_name).set(skill_meta)
            logger.info(f"🏆 Deployed dynamic skill '{skill_name}' into Firestore. Ready for live orchestration!")
            
            return {
                "success": True,
                "skill_name": skill_name,
                "message": "Autonomous evolution loop successfully completed. Skill is live."
            }

        except Exception as e:
            logger.error(f"❌ Self-Evolution loop crashed: {str(e)}")
            return {"success": False, "error": str(e)}
