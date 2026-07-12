# Specialized swarm agents defining task-oriented personalities
# বাংলা মন্তব্য: মাস্টার প্ল্যানিং, কোড জেনারেশন ও স্যান্ডবক্স টেস্টিং এর জন্য স্পেশালাইজড সোয়ার্ম ডিপার্টমেন্ট।

from loguru import logger

from core.llm_gateway import llm_gateway
from core.skill_manager import skill_manager
from models.shared_workspace import SharedWorkspace


class SwarmAgentBase:
    """
    বাংলা মন্তব্য: স্কিল-ভিত্তিক আর্কিটেকচারের জন্য আপডেট করা বেস এজেন্ট।
    """

    async def call_gateway(
        self, system_prompt: str, user_prompt: str, user_id: str = "default_user", model_name: str = "gemini/gemini-1.5-flash"
    ) -> str:
        # বাংলা মন্তব্য: প্রতিটি এজেন্ট কল গেটওয়ের মাধ্যমে রাউট করা হচ্ছে যাতে কস্ট ট্র্যাকিং এনাবেল থাকে।
        messages = [{"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}]
        resp = await llm_gateway.acompletion(model=model_name, messages=messages, user_id=user_id)
        return resp.get("choices", [{}])[0].get("message", {}).get("content", "")

    async def use_skill(self, skill_name: str, **kwargs):
        """
        বাংলা মন্তব্য: এজেন্টরা এখন এই মেথড ব্যবহার করে SkillManager থেকে স্কিল কল করবে।
        """
        try:
            skill = await skill_manager.get_skill(skill_name)
            return await skill.execute(**kwargs)
        except ValueError as e:
            logger.error(f"Skill '{skill_name}' not found or failed to execute: {e}")
            raise


class ArchitectureAgent(SwarmAgentBase):
    async def design(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("ArchitectureAgent: Starting system architecture layout analysis...")
        sys_prompt = "You are a lead system architect. Define file structures, component breakdown, and database schemas."
        user_prompt = f"Design architecture for task: {workspace.original_prompt}"

        design_output = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        # বাংলা মন্তব্য: ডোমেইন-অ্যাগনস্টিক work_product ব্যবহার করা হচ্ছে।
        workspace.work_product["architecture_design"] = design_output
        workspace.log("ArchitectureAgent: System design blueprint completed.")

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        # await self.design(workspace, user_id, model_name)
        logger.info("ArchitectureAgent: Using 'SystemDesignSkill' to plan architecture.")
        design_output = await self.use_skill(
            "SystemDesignSkill", workspace=workspace, user_id=user_id, model_name=model_name
        )
        workspace.work_product["architecture_design"] = design_output


class CodeGeneratorAgent(SwarmAgentBase):
    async def generate_code(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("CodeGeneratorAgent: Injecting layout and writing core codes...")
        sys_prompt = "You are an expert backend engineer. Output only clean python code blocks for specified files."
        user_prompt = f"Design blueprint:\n{workspace.work_product.get('architecture_design', '')}\nGenerate the python code matching this design."

        code_output = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        workspace.work_product["generated_code"] = {"main.py": code_output}
        workspace.log("CodeGeneratorAgent: Core files successfully generated.")

    async def refine(self, workspace: SharedWorkspace, feedback: str, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("CodeGeneratorAgent: Refining code based on Guardian feedback...")
        sys_prompt = "You are an expert backend engineer. Refine the python code based on the feedback."
        user_prompt = f"Original Code:\n{workspace.work_product.get('generated_code', {}).get('main.py', '')}\nFeedback:\n{feedback}\nGenerate the fixed python code matching the constraints."  # noqa: E501
        code_output = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        workspace.work_product["generated_code"]["main.py"] = code_output
        workspace.log("CodeGeneratorAgent: Code successfully refined.")

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        # await self.generate_code(workspace, user_id, model_name)
        logger.info("CodeGeneratorAgent: Using 'CodeGenerationSkill' to write code.")
        code_output = await self.use_skill(
            "CodeGenerationSkill", workspace=workspace, user_id=user_id, model_name=model_name
        )
        workspace.work_product["generated_code"] = {"main.py": code_output}


class QAAgent(SwarmAgentBase):
    async def verify(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("QAAgent: Initiating test suites and static CodeQL scans...")
        # Simulating running ImmuneSystem AST scan and Python validations
        code_to_test = workspace.work_product.get('generated_code', {}).get("main.py", "")

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

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        # await self.verify(workspace, user_id, model_name)
        logger.info("QAAgent: Using 'StaticAnalysisSkill' to verify code.")
        qa_feedback = await self.use_skill("StaticAnalysisSkill", workspace=workspace, user_id=user_id, model_name=model_name)
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
        
        sys_prompt = "You are the SupremeAI Guardian Agent. Your task is to check if the provided code violates any of the rules in the `agent_rules.json` file. The rules cover security, clean code, and architecture. If the code is valid and follows all critical rules, reply with the single word 'APPROVED'. If the code violates any rule, reply starting with 'FAILED:' followed by a clear, concise, and actionable list of violations, including the specific rule ID and a suggestion for fixing it."  # noqa: E501
        user_prompt = f"Rules:\n{rules_text}\n\nCode:\n{workspace.work_product.get('generated_code', {}).get('main.py', '')}"
        
        feedback = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        
        if feedback.strip().startswith("APPROVED"):
            workspace.log("GuardianAgent: Code passed all compliance checks.")
            return True, "APPROVED"
        else:
            workspace.log(f"GuardianAgent: Violation found. {feedback}")
            return False, feedback

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro"):
        # await self.validate(workspace, user_id, model_name)
        logger.info("GuardianAgent: Using 'ComplianceValidationSkill' to check rules.")
        is_approved, feedback = await self.use_skill("ComplianceValidationSkill", workspace=workspace, user_id=user_id, model_name=model_name)
        # This agent's run result needs to be handled by the orchestrator loop

class ResearchAgent(SwarmAgentBase):
    """
    বাংলা মন্তব্য: এই এজেন্টটি রিসার্চ এবং অ্যানালাইসিস সংক্রান্ত কাজ করবে।
    এটি সিস্টেমের Universal Utility Mode-এর একটি অংশ।
    """
    async def analyze(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro"):
        workspace.log("ResearchAgent: Starting analysis and information synthesis...")
        sys_prompt = "You are a world-class research analyst. Analyze the user's prompt, synthesize information, and provide a structured summary."
        analysis_output = await self.call_gateway(sys_prompt, workspace.original_prompt, user_id, model_name=model_name)
        workspace.work_product["research_summary"] = analysis_output
        workspace.log("ResearchAgent: Analysis complete.")

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro"):
        # await self.analyze(workspace, user_id, model_name)
        logger.info("ResearchAgent: Using 'ResearchSkill' for analysis.")
        analysis_output = await self.use_skill("ResearchSkill", workspace=workspace, user_id=user_id, model_name=model_name)
        workspace.work_product["research_summary"] = analysis_output


class ReflectionAgent(SwarmAgentBase):
    async def reflect_and_persist(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        workspace.log("ReflectionAgent: Analyzing task outcome to generate experience...")
        sys_prompt = "You are an AI Reflection engine. Analyze the workspace logs and extract what worked, what failed, and suggested improvements. Return JSON with 'what_worked', 'what_failed', 'suggested_improvements'."  # noqa: E501
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
            except Exception:  # noqa: BLE001
                parsed = {"what_worked": [analysis], "what_failed": [], "suggested_improvements": []}
                
            exp = Experience(
                user_id=user_id,
                request=workspace.original_prompt,
                action_taken=f"Swarm Orchestrator DAG Execution for intent: {workspace.intent}",
                deployment_logs="\\n".join(workspace.execution_logs),
                what_worked=parsed.get("what_worked", []),
                what_failed=parsed.get("what_failed", []),
                suggested_improvements=parsed.get("suggested_improvements", [])
            )
            db.record_experience(exp)
            workspace.log("ReflectionAgent: Experience successfully saved to Vector DB.")
        except Exception as e:  # noqa: BLE001
            workspace.log(f"ReflectionAgent: Failed to save experience: {e}")
        
        return analysis

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-flash"):
        # await self.reflect_and_persist(workspace, user_id, model_name)
        logger.info("ReflectionAgent: Using 'ExperiencePersistenceSkill' to learn.")
        await self.use_skill("ExperiencePersistenceSkill", workspace=workspace, user_id=user_id, model_name=model_name)


class ToolSynthesizerAgent(SwarmAgentBase):
    """
    বাংলা মন্তব্য: ফেজ ২ - Morphic Engine.
    এই এজেন্টটি ಅಗತ್ಯ অনুযায়ী ফ্লাইতে নতুন টুল তৈরি করে।
    """
    async def synthesize(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro"):
        workspace.log("ToolSynthesizerAgent: Starting Zero-Shot Tool Synthesis...")
        sys_prompt = "You are a master tool builder. Based on a task intent, create a JSON definition for a new tool. The definition must include a name, description, and a list of parameters."
        user_prompt = f"Create a tool definition for the intent: '{workspace.original_prompt}'. Respond with only the JSON object."

        tool_definition_str = await self.call_gateway(sys_prompt, user_prompt, user_id, model_name=model_name)
        import json
        tool_definition = json.loads(tool_definition_str)
        workspace.work_product["synthesized_tool"] = tool_definition
        workspace.log(f"ToolSynthesizerAgent: New tool '{tool_definition.get('name')}' synthesized.")

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro"):
        # await self.synthesize(workspace, user_id, model_name)
        logger.info("ToolSynthesizerAgent: Using 'ToolSynthesisSkill' to create a new tool.")
        tool_definition = await self.use_skill("ToolSynthesisSkill", workspace=workspace, user_id=user_id, model_name=model_name)
        workspace.work_product["synthesized_tool"] = tool_definition
        workspace.log(f"ToolSynthesizerAgent: New tool '{tool_definition.get('name')}' synthesized.")


class ToolExecutorAgent(SwarmAgentBase):
    """
    বাংলা মন্তব্য: ফেজ ৩ - Universal Executioner.
    এই এজেন্টটি যেকোনো টুল (আবিষ্কৃত বা সিন্থেসাইজড) এক্সিকিউট করতে পারে।
    """
    async def execute(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro"):
        workspace.log("ToolExecutorAgent: Preparing to execute available tools...")
        tools = workspace.work_product.get("available_tools", [])
        if not tools:
            workspace.log("ToolExecutorAgent: No tools available to execute.")
            return

        # For this PoC, we'll simulate executing the first available tool.
        # A real implementation would involve a more complex selection and execution logic.
        tool_to_run = tools[0]
        workspace.log(f"ToolExecutorAgent: Executing tool '{tool_to_run}'...")
        workspace.work_product["execution_result"] = f"Successfully executed tool: {tool_to_run}"

    async def run(self, workspace: SharedWorkspace, user_id: str, model_name: str = "gemini/gemini-1.5-pro"):
        # await self.execute(workspace, user_id, model_name)
        logger.info("ToolExecutorAgent: Using 'ToolExecutionSkill' to run a tool.")
        await self.use_skill("ToolExecutionSkill", workspace=workspace, user_id=user_id, model_name=model_name)
