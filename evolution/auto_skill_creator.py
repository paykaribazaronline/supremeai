from __future__ import annotations

import os
import subprocess
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
# বাংলা মন্তব্য: প্যাকেজ ও ডিরেক্টরি ইম্পোর্টের সামঞ্জস্যতা নিশ্চিত করতে ট্রাই-ক্যাচ ব্লক ব্যবহার করা হলো
try:
    from .evolution_react_agent import EvolutionReActAgent
except ImportError:
    try:
        from evolution.evolution_react_agent import EvolutionReActAgent
    except ImportError:
        import sys
        import os
        sys.path.append(os.path.dirname(os.path.abspath(__file__)))
        from evolution_react_agent import EvolutionReActAgent


class AutoSkillCreator:
    def __init__(self, rules_engine: Optional[Any] = None):
        self.rules_engine = rules_engine
        self.skills_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "skills")
        # Initialize ReAct Agent
        self.react_agent = EvolutionReActAgent()

    def analyze_demand_patterns(self, task_history: List[Dict[str, Any]]) -> List[str]:
        pattern_source = []
        if self.rules_engine and hasattr(self.rules_engine, "rules"):
            pattern_source.extend(self.rules_engine.rules.get("patterns", {}).get("repeated_tasks", []))
        failed = list({t.get("task") for t in task_history if t.get("success") is False})
        return list(set(pattern_source + failed))

    def generate_skill_code(self, skill_name: str, requirement: str = "") -> str:
        """
        Uses the ReAct agent to autonomously generate code. Falls back to static template on failure.
        """
        req = requirement or f"Generate a helper module for executing repeated task: {skill_name}"
        result = self.react_agent.generate_skill(skill_name, req)
        if result["success"]:
            return result["code"]
        
        # Fallback to static template
        class_name = "".join(part.capitalize() for part in skill_name.split("_"))
        return (
            f"class {class_name}:\n"
            f"    def __init__(self):\n"
            f"        self.name = \"{skill_name}\"\n\n"
            f"    def run(self, payload: dict) -> dict:\n"
            f"        return {{'skill': '{skill_name}', 'status': 'ok', 'result': payload}}\n"
        )

    def register_new_skill(self, skill: Dict[str, Any]) -> Dict[str, Any]:
        # বাংলা মন্তব্য: কোড সরাসরি ফাইলে সেভ না করে সাময়িকভাবে কম্পাইল টেস্ট করা হচ্ছে
        from models.pending_tasks import create_pending_task, TaskType
        skill_name = skill.get("skill_name", "unknown")
        filename = f"{skill_name}.py"
        code = skill.get("generated_code") or self.generate_skill_code(skill_name, skill.get("requirement", ""))
        
        # Write to temporary file for testing
        temp_filename = f".temp_{skill_name}.py"
        os.makedirs(self.skills_dir, exist_ok=True)
        temp_path = os.path.join(self.skills_dir, temp_filename)
        with open(temp_path, "w", encoding="utf-8") as f:
            f.write(code)
            
        test_result = self.test_new_skill(temp_path)
        
        # Clean up temp file
        if os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except Exception:
                pass
                
        if test_result["passed"]:
            # বাংলা মন্তব্য: টেস্ট পাস হলে সরাসরি রাইট না করে HITL Approval Manager-এ পেন্ডিং কিউতে পাঠানো হচ্ছে
            payload = {
                "skill_name": skill_name,
                "generated_code": code,
                "requirement": skill.get("requirement", "")
            }
            
            # বাংলা মন্তব্য: সফল প্রম্পট ও কোড প্যাটার্ন এক্সপেরিয়েন্স ডাটাবেসে ভেক্টরাইজড আকারে সেভ করা হলো
            try:
                from adaptive_engine.experience_db import ExperienceDatabase, Experience
                db = ExperienceDatabase()
                db.record_experience(Experience(
                    request=skill.get("requirement", "") or f"Generate skill: {skill_name}",
                    generated_code=code,
                    result="success"
                ))
            except Exception as e:
                import loguru
                loguru.logger.error(f"Failed to record verified skill experience: {e}")

            task = create_pending_task(
                task_type=TaskType.SKILL_GENERATION,
                payload=payload,
                created_by="auto_skill_creator"
            )
            return {
                "skill_name": skill_name,
                "filename": filename,
                "status": "pending_approval",
                "task_id": task.task_id,
                "registered_at": datetime.now(timezone.utc).isoformat(),
                "test_passed": True,
                "test_result": test_result
            }
        else:
            return {
                "skill_name": skill_name,
                "filename": filename,
                "status": "failed_verification",
                "registered_at": datetime.now(timezone.utc).isoformat(),
                "test_passed": False,
                "test_result": test_result
            }

    def test_new_skill(self, skill_path: str) -> Dict[str, Any]:
        if not os.path.exists(skill_path):
            return {"passed": False, "reason": "file not found"}
        try:
            result = subprocess.run(
                ["python", "-m", "py_compile", skill_path],
                capture_output=True,
                text=True,
                timeout=30,
            )
            if result.returncode == 0:
                return {"passed": True, "skill_path": skill_path}
            return {"passed": False, "reason": result.stderr}
        except Exception as exc:
            return {"passed": False, "reason": str(exc)}

    def generate_from_failures(self, task_history: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        patterns = self.analyze_demand_patterns(task_history)
        created = []
        for pattern in patterns:
            skill_name = f"auto_{pattern.strip().replace(' ', '_').lower()}"
            requirement = f"Create an automated skill that handles resolving task failure related to: {pattern}"
            
            proposal = {
                "skill_name": skill_name,
                "source_pattern": pattern,
                "requirement": requirement,
                "status": "generated",
                "generated_at": datetime.now(timezone.utc).isoformat(),
            }
            res = self.register_new_skill(proposal)
            created.append(res)
        return created
