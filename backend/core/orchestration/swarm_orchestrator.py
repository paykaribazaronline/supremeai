# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import asyncio
import uuid

from core.agent_factory import DynamicAgentFactory
from core.mcp_client import MCPRegistryClient
from core.orchestration.crew_departments import ArchitectureAgent
from core.orchestration.crew_departments import CodeGeneratorAgent
from core.orchestration.crew_departments import GuardianAgent
from core.orchestration.crew_departments import QAAgent
from core.orchestration.crew_departments import ReflectionAgent
from core.orchestration.crew_departments import ResearchAgent
from core.orchestration.crew_departments import ToolExecutorAgent
from core.orchestration.crew_departments import ToolSynthesizerAgent
from core.skill_manager import skill_manager
from core.skills.core_skills import SystemDesignSkill
from models.shared_workspace import SharedWorkspace


from core.resilience.circuit_breaker import CircuitBreaker


class MorphicOrchestrator:
    """
    Universal Cognitive Engine (Architecture 2.0)
    এটি এখন শুধু কোড নয়, ইউজারের যেকোনো ইনটেন্ট (Intent) ডিটেক্ট করে DAG তৈরি করে।
    """

    def __init__(self):
        # বাংলা মন্তব্য: এজেন্টদের একটি রেজিস্ট্রি তৈরি করা হচ্ছে, যা ডাইনামিক্যালি কল করা যাবে।
        self.agents = {
            "architect": ArchitectureAgent(),
            "coder": CodeGeneratorAgent(),
            "researcher": ResearchAgent(),
            "synthesizer": ToolSynthesizerAgent(),
            "executor": ToolExecutorAgent(),
            "qa": QAAgent(),
            "guardian": GuardianAgent(),
            "reflection": ReflectionAgent(),
        }
        # বাংলা মন্তব্য: ফেজ ১ - MCP-Hub ইন্টিগ্রেশন। এটি বাইরের জগতের সাথে সংযোগ স্থাপন করবে।
        self.mcp_client = MCPRegistryClient()
        self.circuit_breaker = CircuitBreaker(name="swarm_orch", failure_threshold=3, recovery_timeout=30.0)
        # বাংলা মন্তব্য: হাইব্রিড মডেলের জন্য ডাইনামিক ফ্যাক্টরি ইনিশিয়ালাইজ করা হলো।
        # এখানে কোনো DB সেশন পাস করা হচ্ছে না, কারণ ফ্যাক্টরি আপাতত stateless।
        self.agent_factory = DynamicAgentFactory()

        # বাংলা মন্তব্য: কোর স্কিলগুলো রেজিস্টার করা হচ্ছে।
        skill_manager.register_skill(SystemDesignSkill())

    async def _get_dag_for_intent(self, intent: str) -> dict[str, list[str]]:
        """
        বাংলা মন্তব্য: ইউজারের ইনটেন্ট অনুযায়ী ডাইনামিক DAG তৈরি করে।
        এটি সিস্টেমকে কোডিং-এর বাইরেও রিসার্চ বা অ্যানালাইসিসের মতো কাজ করার ক্ষমতা দেয়।
        """
        if intent == "code_generation":
            return {
                "architect": [],
                "coder": ["architect"],
                # The guardian_qa_loop and reflection are handled specially below
            }
        elif intent == "research_analysis":
            return {
                "researcher": [],
                "reflection": ["researcher"],
            }
        # Default DAG for general tasks
        return {"executor": [], "researcher": ["executor"], "reflection": ["researcher"]}

    async def _synthesize_tool(self, intent: str, user_id: str) -> dict | None:
        """
        বাংলা মন্তব্য: ফেজ ২ - Zero-Shot Synthesis (The Morphic Engine).
        যদি কোনো টুল MCP সার্ভারে না থাকে, সিস্টেম নিজেই একটি টুল তৈরি করে নেবে।
        """
        workspace = SharedWorkspace(task_id=str(uuid.uuid4()), original_prompt=f"Synthesize a capability for intent: {intent}")
        workspace.log(f"MorphicOrchestrator: Synthesizing new capability for intent '{intent}' using DynamicAgentFactory.")

        synthesized_capability = await self.agent_factory.create_specialized_agent(f"Create a Python script for the task: {intent}")
        if synthesized_capability and "script" in synthesized_capability:
            workspace.log(f"MorphicOrchestrator: Successfully synthesized new capability: {synthesized_capability.get('agent_name')}")
            return synthesized_capability
        return None

    async def execute_task(self, prompt: str, user_id: str = "default_user_session") -> SharedWorkspace:
        task_id = str(uuid.uuid4())
        workspace = SharedWorkspace(task_id=task_id, original_prompt=prompt)
        workspace.log(f"MorphicOrchestrator: Initialized swarm DAG for task {task_id}")

        # 1. Classify Intent using Budget-Aware Routing
        from core.orchestration.agent_orchestrator import budget_aware_route
        
        route = budget_aware_route(prompt=prompt, task_type="general")
        intent_map = {
            "coding": "code_generation",
            "reasoning": "code_generation",
            "search": "research_analysis",
            "vision": "research_analysis",
            "general": "general_task",
        }
        workspace.intent = intent_map.get(route["intent"], "general_task")
        best_provider = route.get("best_provider", "default")
        
        workspace.log(
            f"MorphicOrchestrator: Classified intent as '{workspace.intent}' "
            f"(tier={route.get('tier')}, provider={best_provider})"
        )
        # Store best_provider in workspace for agent consumption
        workspace.work_product["best_provider"] = best_provider

        # 2. Universal Glue: MCP থেকে টুলস ডিসকভার করা
        domain = workspace.intent
        available_mcp_tools = await self.mcp_client.discover_tools(domain)
        workspace.log(f"MorphicOrchestrator: Discovered MCP tools for domain '{domain}': {available_mcp_tools}")
        workspace.work_product["available_tools"] = available_mcp_tools

        # 3. Dynamic Synthesis: যদি টুল না পাওয়া যায়, তবে নতুন টুল তৈরি করা
        if not available_mcp_tools or "generic_tool" in available_mcp_tools:
            workspace.log("MorphicOrchestrator: No specific tool found. Attempting Zero-Shot Synthesis...")
            new_tool = await self._synthesize_tool(workspace.intent, user_id)
            if new_tool:
                workspace.work_product["available_tools"].append(new_tool)

        # 3. Get Dynamic DAG based on intent
        task_graph = await self._get_dag_for_intent(workspace.intent)
        workspace.log(f"MorphicOrchestrator: Constructed DAG with nodes: {list(task_graph.keys())}")

        completed_tasks = set()

        async def _execute_dag():
            # Standard DAG execution for non-loop parts
            while len(completed_tasks) < len(task_graph):
                ready_tasks = [task for task, deps in task_graph.items() if task not in completed_tasks and all(d in completed_tasks for d in deps)]
                if not ready_tasks:
                    raise RuntimeError(f"DAG execution error: No ready tasks found, but not all tasks are complete. Completed: {completed_tasks}")

                tasks_to_run = [self.agents[task].run(workspace, user_id) for task in ready_tasks if task in self.agents]
                if tasks_to_run:
                    await asyncio.gather(*tasks_to_run)
                completed_tasks.update(ready_tasks)

            # Special Handling for 'code_generation' intent's refinement loop
            if workspace.intent == "code_generation":
                max_refinements = 3
                for i in range(max_refinements):
                    workspace.log(f"MorphicOrchestrator: Starting Guardian/QA refinement loop, iteration {i + 1}/{max_refinements}")

                    # Guardian validation
                    guardian_agent = self.agents["guardian"]
                    is_approved, feedback = await guardian_agent.validate(workspace, user_id)

                    if is_approved:
                        workspace.log("MorphicOrchestrator: Code APPROVED by Guardian. Exiting refinement loop.")
                        break

                    workspace.log("MorphicOrchestrator: Code FAILED Guardian validation. Triggering refinement.")

                    # Refinement by CodeGeneratorAgent
                    coder_agent = self.agents["coder"]
                    await coder_agent.refine(workspace, feedback, user_id)
                else:  # This else belongs to the for loop, executes if loop finishes without break
                    workspace.log("MorphicOrchestrator: Max refinement attempts reached. Proceeding with current code.")

            # Final reflection step for all intents
            if "reflection" in self.agents:
                await self.agents["reflection"].run(workspace, user_id)

        try:
            from core.observability.telemetry import tracer
            with tracer.start_as_current_span("morphic_orchestrator.execute_task") as span:
                span.set_attribute("user_id", user_id)
                span.set_attribute("intent", workspace.intent)
                if best_provider:
                    span.set_attribute("provider", best_provider)
                
                await self.circuit_breaker.call(_execute_dag)
            # Duplicate log removed here

        except RuntimeError as e:
            if "is open" not in str(e):
                raise
            workspace.log(f"MorphicOrchestrator: Circuit breaker OPEN — {e}")
            workspace.add_error(str(e))
            return workspace
        except Exception as e:  # noqa: BLE001
            workspace.log(f"MorphicOrchestrator: An unexpected error occurred during DAG execution: {e}")
            workspace.add_error(str(e))
            # বাংলা মন্তব্য: এরর হলেও রিফ্লেকশন চালানোর চেষ্টা করা হবে, যাতে সিস্টেম শিখতে পারে।
            if "reflection" not in completed_tasks:
                await self.agents["reflection"].reflect_and_persist(workspace, user_id)
            return workspace

        workspace.log("MorphicOrchestrator: Multi-Agent DAG execution completed successfully.")
        return workspace
