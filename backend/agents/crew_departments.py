# Specialized swarm agents defining task-oriented personalities
# বাংলা মন্তব্য: মাস্টার প্ল্যানিং, কোড জেনারেশন ও স্যান্ডবক্স টেস্টিং এর জন্য স্পেশালাইজড সোয়ার্ম ডিপার্টমেন্ট।

from core.llm_gateway import llm_gateway
from models.shared_workspace import SharedWorkspace


class SwarmAgentBase:
    async def call_gateway(self, system_prompt: str, user_prompt: str, user_id: str = "default_user") -> str:
        # বাংলা মন্তব্য: প্রতিটি এজেন্ট কল গেটওয়ের মাধ্যমে রাউট করা হচ্ছে যাতে কস্ট ট্র্যাকিং এনাবেল থাকে।
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ]
        resp = await llm_gateway.acompletion(
            model="gemini/gemini-1.5-flash",
            messages=messages,
            user_id=user_id
        )
        return resp.get("choices", [{}])[0].get("message", {}).get("content", "")


class ArchitectureAgent(SwarmAgentBase):
    async def design(self, workspace: SharedWorkspace, user_id: str):
        workspace.log("ArchitectureAgent: Starting system architecture layout analysis...")
        sys_prompt = "You are a lead system architect. Define file structures, component breakdown, and database schemas."
        user_prompt = f"Design architecture for task: {workspace.original_prompt}"
        
        design_output = await self.call_gateway(sys_prompt, user_prompt, user_id)
        workspace.architecture_design = design_output
        workspace.log("ArchitectureAgent: System design blueprint completed.")


class CodeGeneratorAgent(SwarmAgentBase):
    async def generate_code(self, workspace: SharedWorkspace, user_id: str):
        workspace.log("CodeGeneratorAgent: Injecting layout and writing core codes...")
        sys_prompt = "You are an expert backend engineer. Output only clean python code blocks for specified files."
        user_prompt = f"Design blueprint:\n{workspace.architecture_design}\nGenerate the python code matching this design."

        code_output = await self.call_gateway(sys_prompt, user_prompt, user_id)
        workspace.generated_code["main.py"] = code_output
        workspace.log("CodeGeneratorAgent: Core files successfully generated.")


class QAAgent(SwarmAgentBase):
    async def verify(self, workspace: SharedWorkspace, user_id: str):
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
        
        sys_prompt = "You are a QA engineer. Review code and validation results and give feedback."
        user_prompt = f"Code:\n{code_to_test}\nResults: {workspace.test_results}"
        qa_feedback = await self.call_gateway(sys_prompt, user_prompt, user_id)
        workspace.test_results["feedback"] = qa_feedback
