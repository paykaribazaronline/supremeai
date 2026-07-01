# বাংলা কমেন্ট: সুপ্রিম-এআই এর ডাইনামিক স্কিল ইনজেক্টর ইঞ্জিন।
# এটি জেনারেটেড কোডকে স্যান্ডবক্সে ভেরিফাই করে সফল হলে সিস্টেমে ইনজেক্ট করে, আর ফেইল করলে কোয়ারেন্টাইনে পাঠায়।

import importlib
import importlib.util
import os
import sys
from datetime import datetime

from backend.evolution.security_sandbox import execute_secure_sandbox
from core.logging_config import logger


class DynamicSkillInjector:
    def __init__(self):
        self.skills_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../skills/dynamic"))
        self.quarantine_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../skills/quarantine"))
        os.makedirs(self.skills_dir, exist_ok=True)
        os.makedirs(self.quarantine_dir, exist_ok=True)

    def inject_skill(self, skill_name: str, code_content: str) -> dict:
        """
        জিরো-গ্যাপ ভেরিফিকেশন সহ নতুন স্কিল ইনজেক্ট বা আপডেট করে।
        """
        logger.info(f"🚀 Attempting dynamic injection for skill: {skill_name}")
        
        # স্যান্ডবক্স ভেরিফিকেশন
        sandbox_result = execute_secure_sandbox(code_content)
        
        if sandbox_result["status"] != "SUCCESS":
            # ফেইল করলে কোয়ারেন্টাইনে মুভ করা (০% গ্যাপ পলিসি)
            self._quarantine_code(skill_name, code_content, sandbox_result.get("reason", "Unknown sandbox error"))
            return {"status": "FAILED", "reason": sandbox_result.get("reason")}

        # সাকসেস হলে ডাইনামিক ফোল্ডারে রাইট করা
        file_path = os.path.join(self.skills_dir, f"{skill_name}.py")
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(code_content)
            
        # মডিউল লোড ও রি-লোড মেকানিজম (মেমোরি ক্যাশ ইনভ্যালিডেশন গ্যাপ ফিক্স)
        module_name = f"skills.dynamic.{skill_name}"
        
        try:
            if module_name in sys.modules:
                # মডিউল আগে থেকেই থাকলে রিলোড করা
                module = sys.modules[module_name]
                importlib.reload(module)
                logger.success(f"🔄 Module {module_name} successfully reloaded into memory.")
            else:
                # নতুন মডিউল ইম্পোর্ট করা
                spec = importlib.util.spec_from_file_location(module_name, file_path)
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)
                logger.success(f"✅ Module {module_name} successfully injected into memory.")
                
            return {"status": "SUCCESS", "module": module}
            
        except Exception as e:
            logger.critical(f"🔥 FATAL: Failed to load module {module_name} after injection -> {str(e)}")
            self._quarantine_code(skill_name, code_content, str(e))
            return {"status": "FAILED", "reason": str(e)}

    def _quarantine_code(self, skill_name: str, code_content: str, reason: str):
        """
        রিজেক্টেড বা এরর থাকা কোড কোয়ারেন্টাইনে পাঠায় সেলফ-কারেকশনের জন্য।
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        safe_name = f"{skill_name}_{timestamp}_blocked.py"
        file_path = os.path.join(self.quarantine_dir, safe_name)
        
        quarantine_content = f"# BLOCKED REASON: {reason}\n\n{code_content}"
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(quarantine_content)
            
        logger.warning(f"🔒 Skill {skill_name} isolated to quarantine zone -> {safe_name}")

dynamic_injector = DynamicSkillInjector()
