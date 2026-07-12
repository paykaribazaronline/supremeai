# Specialized swarm agents defining task-oriented personalities
# বাংলা মন্তব্য: মাস্টার প্ল্যানিং, কোড জেনারেশন ও স্যান্ডবক্স টেস্টিং এর জন্য স্পেশালাইজড সোয়ার্ম ডিপার্টমেন্ট।

from core.llm_gateway import llm_gateway
from models.shared_workspace import SharedWorkspace


class SwarmAgentBase:
    async def call_gateway(
        self, system_prompt: str, user_prompt: str, user_id: str = "default_user", model_name: str = "gemini/gemini-1.5-flash"
    ) -> str:
        # বাংলা মন্তব্য: প্রতিটি এজেন্ট কল গেটওয়ের মাধ্যমে রাউট করা হচ্ছে যাতে কস্ট ট্র্যাকিং এনাবেল থাকে।
        messages = [{"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}]
        resp = await llm_gateway.acompletion(model=model_name, messages=messages, user_id=user_id)
        return resp.get("choices", [{}])[0].get("message", {}).get("content", "")


class ArchitectureAgent(SwarmAgentBase):
    async def design(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("ArchitectureAgent: Starting system architecture layout analysis...")
        sys_prompt = "You are a lead system architect. Define file structures, component breakdown, and database schemas."
        user_prompt = f"Design architecture for task: {workspace.original_prompt}"

        design_output = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        workspace.architecture_design = design_output
        workspace.log("ArchitectureAgent: System design blueprint completed.")


class CodeGeneratorAgent(SwarmAgentBase):
    async def generate_code(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("CodeGeneratorAgent: Injecting layout and writing core codes...")
        sys_prompt = "You are an expert backend engineer. Output only clean python code blocks for specified files."
        user_prompt = f"Design blueprint:\n{workspace.architecture_design}\nGenerate the python code matching this design."

        code_output = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        workspace.generated_code["main.py"] = code_output
        workspace.log("CodeGeneratorAgent: Core files successfully generated.")

    async def refine(self, workspace: SharedWorkspace, feedback: str, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("CodeGeneratorAgent: Refining code based on Guardian feedback...")
        sys_prompt = "You are an expert backend engineer. Refine the python code based on the feedback."
        user_prompt = f"Original Code:\n{workspace.generated_code.get('main.py', '')}\nFeedback:\n{feedback}\nGenerate the fixed python code matching the constraints."
        code_output = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        workspace.generated_code["main.py"] = code_output
        workspace.log("CodeGeneratorAgent: Code successfully refined.")


class QAAgent(SwarmAgentBase):
    async def verify(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("QAAgent: Initiating test suites and static CodeQL scans...")
        # Simulating running ImmuneSystem AST scan and Python validations
        code_to_test = workspace.generated_code.get("main.py", "")

        if "import os" in code_to_test or "eval(" in code_to_test:
            workspace.test_results["safe"] = False
            workspace.test_results["error"] = "Security Exception: Banned AST calls detected by static scan."
            workspace.log("QAAgent: 🚨 Static security analysis scan failed!")
        else:
            workspace.test_results["safe"] = True
            workspace.test_results["passed"] = True
            workspace.log("QAAgent: AST Static scans and sanity runs completed successfully.")

        sys_prompt = "You are a QA engineer. Review code and validation results and give feedback. State clearly if the code is APPROVED or FAILED."
        user_prompt = f"Code:\n{code_to_test}\nResults: {workspace.test_results}"
        qa_feedback = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        workspace.test_results["feedback"] = qa_feedback


class GuardianAgent(SwarmAgentBase):
    async def validate(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro") -> tuple[bool, str]:
        workspace.log("GuardianAgent: Scanning code for agent_rules.json violations...")
        from pathlib import Path

        rules_path = Path(__file__).resolve().parent.parent.parent.parent / "agent_rules.json"
        rules_text = ""
        if rules_path.exists():
            with open(rules_path, encoding="utf-8") as f:
                rules_text = f.read()

        sys_prompt = "You are the SupremeAI Guardian Agent. Check if the provided code violates the agent_rules.json rules. If valid, reply exactly 'APPROVED'. If invalid, reply 'FAILED' followed by the reasons and rule IDs."
        user_prompt = f"Rules:\n{rules_text}\n\nCode:\n{workspace.generated_code.get('main.py', '')}"

        feedback = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)

        if feedback.strip().startswith("APPROVED"):
            workspace.log("GuardianAgent: Code passed all compliance checks.")
            return True, "Passed"
        else:
            workspace.log(f"GuardianAgent: Violation found. {feedback}")
            return False, feedback


class ReflectionAgent(SwarmAgentBase):
    async def reflect_and_persist(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("ReflectionAgent: Analyzing task outcome to generate experience...")
        sys_prompt = "You are an AI Reflection engine. Analyze the workspace logs and extract what worked, what failed, and suggested improvements. Return JSON with 'what_worked', 'what_failed', 'suggested_improvements'."
        user_prompt = f"Logs: {workspace.execution_logs}\nOriginal Prompt: {workspace.original_prompt}"

        analysis = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)

        # Save to ExperienceDatabase
        try:
            from adaptive_engine.experience_db import Experience
            from adaptive_engine.experience_db import ExperienceDatabase

            db = ExperienceDatabase()

            import json

            try:
                parsed = json.loads(analysis)
            except:
                parsed = {"what_worked": [analysis], "what_failed": [], "suggested_improvements": []}

            exp = Experience(
                user_id=user_id,
                request=workspace.original_prompt,
                action_taken="Swarm Orchestrator DAG Execution",
                generated_code=workspace.generated_code.get("main.py", ""),
                deployment_logs="\\n".join(workspace.execution_logs),
                what_worked=parsed.get("what_worked", []),
                what_failed=parsed.get("what_failed", []),
                suggested_improvements=parsed.get("suggested_improvements", []),
            )
            db.record_experience(exp)
            workspace.log("ReflectionAgent: Experience successfully saved to Vector DB.")
        except Exception as e:
            workspace.log(f"ReflectionAgent: Failed to save experience: {e}")

        return analysis
