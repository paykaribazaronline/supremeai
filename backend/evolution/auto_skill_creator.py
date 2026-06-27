import os
import time
from datetime import datetime
from datetime import timezone
from typing import Any

from loguru import logger
from skills.installer import SkillInstaller

from core.tenant_db import TenantAwareFirestore
from evolution.fitness_engine import FitnessEngine
from tools.fuzz_sandbox import SecurityError

# আমাদের হার্ডেনড স্যান্ডবক্স গেটকিপার ইম্পোর্ট
from tools.fuzz_sandbox import run_sandbox_ast_check


class AutoSkillCreator:
    """
    Self-Evolution Engine Core.
    Autonomously generates, validates, and provisions dynamic AI skills/tools on-the-fly.
    """

    def __init__(self, db: TenantAwareFirestore | None = None, **kwargs: Any):
        # 🛡️ এখন আর সরাসরি firestore.Client() কল হবে না!
        self.db = db
        self.skills_ref = None
        if db is not None:
            self.skills_ref = self.db.collection("supreme_dynamic_skills")
        else:
            # Try to obtain Firestore client; fall back to mock if unavailable
            try:
                from core.gcp_firestore import get_firestore_client

                client = get_firestore_client()
                if client is not None:
                    self.skills_ref = client.collection("supreme_dynamic_skills")
            except Exception:
                pass
            if self.skills_ref is None:

                class MockDoc:
                    def set(self, *args, **kwargs):
                        pass

                class MockRef:
                    def document(self, *args, **kwargs):
                        return MockDoc()

                self.skills_ref = MockRef()
        # Initialize FitnessEngine for telemetry
        self.fitness_engine = FitnessEngine(db=self.db)
        self._model = None

    def _get_model(self):
        if self._model is None:
            import google.generativeai as genai

            # Gemini API configuration (Secret Vault injects key into env)
            genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
            self._model = genai.GenerativeModel("gemini-1.5-pro")
        return self._model

    async def generate_and_deploy_skill(self, user_demand: str, skill_name: str) -> dict:
        import json
        import shutil
        import uuid

        start_time = time.time()
        from pathlib import Path

        from skills.schema import UniversalSkillSchema

        logger.info(f"🧠 Self-Evolution Triggered: Designing skill '{skill_name}' for demand: '{user_demand}'")

        trace_id = uuid.uuid4().hex
        generation_timestamp = datetime.now(timezone.utc).isoformat()

        # ১. এআই ডিরেক্টিভ প্রম্পট - যা সুনির্দিষ্ট ও কঠোর JSON ফরম্যাটে কোড ও USS জেনারেট করবে
        system_prompt = (
            "You are the SupremeAI 2.0 Self-Evolution Engine. Your job is to output a single structured JSON object "
            "representing a dynamic skill and its metadata schema.\n\n"
            "You must return ONLY a valid JSON block. No markdown explanations outside the JSON block.\n\n"
            "The JSON structure must match this template exactly:\n"
            "{\n"
            '  "code": "python code containing a class matching the skill_name. '
            "The class must implement an async def execute(self, kwargs) -> dict method. "
            "Do not use banned keywords like eval, exec, compile, getattr, setattr, "
            'globals, locals.",\n'
            '  "schema": {\n'
            '    "metadata": {\n'
            '      "name": "skill_name",\n'
            '      "version": "1.0.0",\n'
            '      "description": "description of what the skill does",\n'
            '      "author": "supremeai_agent_id",\n'
            '      "tags": []\n'
            "    },\n"
            '    "interface": {\n'
            '      "input_schema": {\n'
            '        "type": "object",\n'
            '        "properties": {},\n'
            '        "required": []\n'
            "      },\n"
            '      "output_schema": {\n'
            '        "type": "object",\n'
            '        "properties": {}\n'
            "      }\n"
            "    },\n"
            '    "execution": {\n'
            '      "runtime": "python3.11",\n'
            '      "entry_point": "main.execute",\n'
            '      "dependencies": [],\n'
            '      "timeout_seconds": 30\n'
            "    },\n"
            '    "validation": {\n'
            '      "tests": [\n'
            "        {\n"
            '          "input": {},\n'
            '          "expected_output": {}\n'
            "        }\n"
            "      ],\n"
            '      "security_level": "sandboxed"\n'
            "    }\n"
            "  }\n"
            "}\n\n"
            f"Requirements:\n"
            f"- User Demand: {user_demand}\n"
            f"- Skill Name / Class Name: {skill_name}\n"
        )

        # Base directories path setup
        base_dir = Path(__file__).resolve().parent.parent.parent
        quarantine_dir = base_dir / "skills" / "quarantine" / skill_name

        try:
            # ২. অন-দি-ফ্লাই কোড জেনারেশন
            response = self._get_model().generate_content(system_prompt)
            raw_content = response.text.strip()

            # Extract JSON block
            if "```json" in raw_content:
                json_str = raw_content.split("```json")[1].split("```")[0].strip()
            elif "```" in raw_content:
                json_str = raw_content.split("```")[1].split("```")[0].strip()
            else:
                json_str = raw_content

            # Parse generated JSON
            data = json.loads(json_str)
            code_block = data.get("code", "")
            schema_dict = data.get("schema", {})

            # Traceability enhancements
            schema_dict["metadata"]["tags"] = schema_dict["metadata"].get("tags", []) + [f"trace_id:{trace_id}"]
            schema_dict["metadata"]["author"] = f"supremeai_agent_id:{trace_id}"
            schema_dict["metadata"]["description"] = schema_dict["metadata"].get("description", "") + f" (Generated at {generation_timestamp})"

            # 🛡️ ৩. দ্য আলটিমেট স্যান্ডবক্স গেটকিপার ভ্যালিডেশন (The Iron Cage Check)
            try:
                is_safe = run_sandbox_ast_check(code_block)
                if not is_safe:
                    raise SecurityError("Generated code failed AST layout normalization.")
            except SecurityError as sec_err:
                logger.critical(f"🚨 [EVOLUTION BLOCKED] AI generated a dangerous skill payload! Threat defused: {str(sec_err)}")
                return {
                    "success": False,
                    "error": f"Security Sandbox Violation: {str(sec_err)}",
                }

            # ৪. USS Pydantic Schema Validation
            try:
                uss = UniversalSkillSchema(**schema_dict)
            except Exception as e:
                logger.error(f"❌ USS Validation failed: {e}")
                return {
                    "success": False,
                    "error": f"USS Validation Exception: {str(e)}",
                }

            # ৫. Quarantine Zone & Automated Testing Loop
            quarantine_dir.mkdir(parents=True, exist_ok=True)
            entry_file = quarantine_dir / "main.py"
            schema_file = quarantine_dir / "schema.json"

            with open(entry_file, "w", encoding="utf-8") as f:
                f.write(code_block)
            with open(schema_file, "w", encoding="utf-8") as f:
                json.dump(schema_dict, f, indent=4)

            # Load module from quarantine and execute validation tests
            import importlib.util

            # Isolated import to run test validation loop
            spec = importlib.util.spec_from_file_location(f"skills.quarantine.{skill_name}", str(entry_file))
            mod = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(mod)

            cls = getattr(mod, skill_name)
            instance = cls()

            # Execute validation tests loop
            for idx, test in enumerate(uss.validation.tests):
                logger.info(f"Running validation test case {idx + 1}/{len(uss.validation.tests)}...")
                result = await instance.execute(test.input)
                if result != test.expected_output:
                    raise ValueError(f"Validation test {idx + 1} failed. Expected {test.expected_output}, got {result}")

            logger.info(f"✅ All {len(uss.validation.tests)} validation tests passed for skill '{skill_name}'!")

            # ৬. Finalize Registration & Storage Deployment
            installer = SkillInstaller()
            ok = installer.install_skill_from_source(
                name=skill_name,
                code=code_block,
                version=uss.metadata.version,
                description=uss.metadata.description,
                dependencies=uss.execution.dependencies,
                uss=schema_dict,
            )

            if not ok:
                raise RuntimeError("Failed to register and install validated skill.")

            # Clean up quarantine directory
            if quarantine_dir.exists():
                shutil.rmtree(quarantine_dir)

            # Firestore live deployment
            now = datetime.now(timezone.utc)
            skill_meta = {
                "skill_name": skill_name,
                "demand_justification": user_demand,
                "generated_code": code_block,
                "status": "ACTIVE",
                "deployed_at": now,
                "uss": schema_dict,
            }
            self.skills_ref.document(skill_name).set(skill_meta)
            logger.info(f"🏆 Deployed dynamic skill '{skill_name}' into Firestore. Ready for live orchestration!")

            latency = time.time() - start_time
            self.fitness_engine.track_execution(skill_name, success=True, latency=latency)
            return {
                "success": True,
                "skill_name": skill_name,
                "message": "Autonomous evolution loop successfully completed. Skill is live.",
            }

        except Exception as e:
            logger.error(f"❌ Self-Evolution loop crashed: {str(e)}")
            latency = time.time() - start_time
            self.fitness_engine.track_execution(skill_name, success=False, latency=latency)
            # Cleanup quarantine on failure
            if quarantine_dir.exists():
                shutil.rmtree(quarantine_dir)
            return {"success": False, "error": str(e)}
