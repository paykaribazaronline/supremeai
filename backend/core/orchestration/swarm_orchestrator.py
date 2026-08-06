# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import asyncio
import uuid

from core.agent_factory import DynamicAgentFactory
from core.mcp_client import MCPRegistryClient
from core.orchestration.crew_departments import (ArchitectureAgent,
                                                 CodeGeneratorAgent,
                                                 GuardianAgent,
                                                 IntegrationAgent, QAAgent,
                                                 ReflectionAgent,
                                                 ResearchAgent,
                                                 ToolExecutorAgent,
                                                 ToolSynthesizerAgent)
from core.resilience.circuit_breaker import CircuitBreaker
from core.skill_manager import skill_manager
from core.skills.core_skills import (CodeGenerationSkill,
                                     ExperiencePersistenceSkill, ResearchSkill,
                                     StaticAnalysisSkill, SystemDesignSkill,
                                     ToolExecutionSkill, ToolSynthesisSkill)
from core.skills.integrations import (GithubSyncSkill, NotionSyncSkill,
                                      SlackIntegrationSkill)
from models.shared_workspace import SharedWorkspace
from pydantic import BaseModel, ConfigDict, Field


class ExecutionResult(BaseModel):
    model_config = ConfigDict(arbitrary_types_allowed=True)
    task_id: str = Field(..., description="Unique Master Task ID")
    status: str = Field(..., description="Execution status")
    workspace: SharedWorkspace
    errors: list[str] = Field(default_factory=list)


class SwarmOrchestrator:
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
            "integration": IntegrationAgent(),
        }
        # বাংলা মন্তব্য: ফেজ ১ - MCP-Hub ইন্টিগ্রেশন। এটি বাইরের জগতের সাথে সংযোগ স্থাপন করবে।
        self.mcp_client = MCPRegistryClient()
        self.circuit_breaker = CircuitBreaker(
            name="swarm_orch", failure_threshold=3, recovery_timeout=30.0
        )
        # বাংলা মন্তব্য: হাইব্রিড মডেলের জন্য ডাইনামিক ফ্যাক্টরি ইনিশিয়ালাইজ করা হলো।
        # এখানে কোনো DB সেশন পাস করা হচ্ছে না, কারণ ফ্যাক্টরি আপাতত stateless।
        self.agent_factory = DynamicAgentFactory()

        # বাংলা মন্তব্য: কোর স্কিলগুলো রেজিস্টার করা হচ্ছে।
        skill_manager.register_skill(SystemDesignSkill())
        skill_manager.register_skill(CodeGenerationSkill())
        skill_manager.register_skill(StaticAnalysisSkill())
        skill_manager.register_skill(ResearchSkill())
        skill_manager.register_skill(ToolSynthesisSkill())
        skill_manager.register_skill(ToolExecutionSkill())
        skill_manager.register_skill(ExperiencePersistenceSkill())
        skill_manager.register_skill(SlackIntegrationSkill())
        skill_manager.register_skill(NotionSyncSkill())
        skill_manager.register_skill(GithubSyncSkill())

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
        elif intent in ["sync_to_slack", "sync_to_notion", "sync_to_github"]:
            return {
                "integration": [],
            }
        # Default DAG for general tasks
        return {
            "executor": [],
            "researcher": ["executor"],
            "reflection": ["researcher"],
        }

    async def _synthesize_tool(self, intent: str, user_id: str) -> dict | None:
        """
        বাংলা মন্তব্য: ফেজ ২ - Zero-Shot Synthesis (The Morphic Engine).
        যদি কোনো টুল MCP সার্ভারে না থাকে, সিস্টেম নিজেই একটি টুল তৈরি করে নেবে।
        """
        workspace = SharedWorkspace(
            task_id=str(uuid.uuid4()),
            original_prompt=f"Synthesize a capability for intent: {intent}",
        )
        workspace.log(
            f"SwarmOrchestrator: Synthesizing new capability for intent '{intent}' using DynamicAgentFactory."
        )

        synthesized_capability = await self.agent_factory.create_specialized_agent(
            f"Create a Python script for the task: {intent}"
        )
        if synthesized_capability and "script" in synthesized_capability:
            workspace.log(
                f"SwarmOrchestrator: Successfully synthesized new capability: {synthesized_capability.get('agent_name')}"
            )
            return synthesized_capability
        return None

    async def execute_task(
        self, prompt: str, user_id: str = "default_user_session"
    ) -> ExecutionResult:
        task_id = str(uuid.uuid4())
        workspace = SharedWorkspace(task_id=task_id, original_prompt=prompt)
        workspace.log(f"SwarmOrchestrator: Initialized swarm DAG for task {task_id}")

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
            f"SwarmOrchestrator: Classified intent as '{workspace.intent}' (tier={route.get('tier')}, provider={best_provider})"
        )
        # Store best_provider in workspace for agent consumption
        workspace.work_product["best_provider"] = best_provider

        # 2. Universal Glue: MCP থেকে টুলস ডিসকভার করা
        domain = workspace.intent
        available_mcp_tools = await self.mcp_client.discover_tools(domain)
        workspace.log(
            f"SwarmOrchestrator: Discovered MCP tools for domain '{domain}': {available_mcp_tools}"
        )
        workspace.work_product["available_tools"] = available_mcp_tools

        # 3. Dynamic Synthesis: যদি টুল না পাওয়া যায়, তবে নতুন টুল তৈরি করা
        if not available_mcp_tools or "generic_tool" in available_mcp_tools:
            workspace.log(
                "SwarmOrchestrator: No specific tool found. Attempting Zero-Shot Synthesis..."
            )
            new_tool = await self._synthesize_tool(workspace.intent, user_id)
            if new_tool:
                workspace.work_product["available_tools"].append(new_tool)

        # 3. Get Dynamic DAG based on intent
        workspace = await self.run_dag_for_workspace(workspace, user_id)
        status = "error" if workspace.errors else "success"
        return ExecutionResult(
            task_id=workspace.task_id,
            status=status,
            workspace=workspace,
            errors=workspace.errors,
        )

    async def run_dag_for_workspace(
        self, workspace: SharedWorkspace, user_id: str = "default_user_session"
    ) -> SharedWorkspace:
        task_graph = await self._get_dag_for_intent(workspace.intent)
        workspace.log(
            f"SwarmOrchestrator: Constructed DAG with nodes: {list(task_graph.keys())}"
        )

        completed_tasks: set[str] = set()

        async def _execute_dag():
            from core.swarm_pubsub import swarm_streamer

            # Standard DAG execution for non-loop parts
            while len(completed_tasks) < len(task_graph):
                # বাংলা মন্তব্য: প্রতিটি ব্যাচ শুরুর আগে গ্লোবাল emergency-stop চেক করা হয়।
                # /api/v1/swarm/halt কল হলে চলমান নতুন ব্যাচ শুরু হবে না, workspace-এ
                # স্পষ্ট এরর লগ হবে (silent hang নয়)।
                if await swarm_streamer.is_halted():
                    workspace.log(
                        "SwarmOrchestrator: Execution halted by emergency stop (swarm:halt:global)."
                    )
                    raise RuntimeError("Swarm execution halted by emergency stop")

                ready_tasks = [
                    task
                    for task, deps in task_graph.items()
                    if task not in completed_tasks
                    and all(d in completed_tasks for d in deps)
                ]
                if not ready_tasks:
                    raise RuntimeError(
                        f"DAG execution error: No ready tasks found, but not all tasks are complete. Completed: {completed_tasks}"
                    )

                runnable = [task for task in ready_tasks if task in self.agents]
                missing = set(ready_tasks) - set(runnable)
                if missing:
                    # ❗ আগে silently completed মার্ক হতো — এখন স্পষ্ট error, সিস্টেম জানবে সে কিছু মিস করছে
                    raise RuntimeError(
                        f"SwarmOrchestrator: DAG references unregistered agent(s): {missing}. Registered agents: {list(self.agents.keys())}"
                    )

                coros = [self.agents[task].run(workspace, user_id) for task in runnable]
                results = await asyncio.gather(*coros, return_exceptions=True)

                failures = [
                    (task, r)
                    for task, r in zip(runnable, results, strict=False)
                    if isinstance(r, Exception)
                ]
                if failures:
                    failed_names = ", ".join(f"{t}: {e}" for t, e in failures)
                    raise RuntimeError(
                        f"SwarmOrchestrator: task(s) failed in this batch — {failed_names}"
                    )

                completed_tasks.update(
                    runnable
                )  # শুধু যেগুলো সত্যিই সফলভাবে রান হয়েছে

            # Special Handling for 'code_generation' intent's refinement loop
            if workspace.intent == "code_generation":
                max_refinements = 3
                guardian_agent = self.agents.get("guardian")
                coder_agent = self.agents.get("coder")

                if not guardian_agent or not coder_agent:
                    workspace.log(
                        "SwarmOrchestrator: Guardian or Coder agent missing for code generation loop."
                    )
                else:
                    for i in range(max_refinements):
                        workspace.log(
                            f"SwarmOrchestrator: Starting Guardian/QA refinement loop, iteration {i + 1}/{max_refinements}"
                        )

                        # Guardian validation
                        is_approved, feedback = await guardian_agent.validate(
                            workspace, user_id
                        )

                        if is_approved:
                            workspace.log(
                                "SwarmOrchestrator: Code APPROVED by Guardian. Exiting refinement loop."
                            )
                            break

                        workspace.log(
                            "SwarmOrchestrator: Code FAILED Guardian validation. Triggering refinement."
                        )

                        # Refinement by CodeGeneratorAgent
                        await coder_agent.refine(workspace, feedback, user_id)
                    else:  # This else belongs to the for loop, executes if loop finishes without break
                        workspace.log(
                            "SwarmOrchestrator: Max refinement attempts reached. Proceeding with current code."
                        )

            # Final reflection step for all intents
            reflection_agent = self.agents.get("reflection")
            if reflection_agent:
                await reflection_agent.run(workspace, user_id)

        try:
            from core.observability.telemetry import trace_span

            attributes = {
                "user_id": user_id,
                "intent": workspace.intent,
            }
            best_provider = workspace.work_product.get("best_provider")
            if best_provider:
                attributes["provider"] = best_provider

            with trace_span(
                "morphic_orchestrator.run_dag_for_workspace", attributes=attributes
            ):
                await self.circuit_breaker.acall(_execute_dag)

        except Exception as e:
            # বাংলা মন্তব্য: অর্কেস্ট্রেটরের টপ-লেভেলে সব এরর ক্যাচ করার জন্য Exception ব্যবহার করা হয়েছে এবং ট্রেসব্যাক লগ করা হচ্ছে।
            from loguru import logger

            logger.opt(exception=True).error(f"DAG execution failed: {e}")

            from core.resilience.circuit_breaker import CircuitBreakerOpenError

            if isinstance(e, CircuitBreakerOpenError) or "is OPEN" in str(e):
                workspace.log(f"SwarmOrchestrator: Circuit breaker OPEN — {e}")
                workspace.add_error(str(e))
                return workspace

            workspace.log(
                f"SwarmOrchestrator: An unexpected error occurred during DAG execution: {e}"
            )
            workspace.add_error(str(e))

            # বাংলা মন্তব্য: এরর হলেও রিফ্লেকশন চালানোর চেষ্টা করা হবে, যাতে সিস্টেম শিখতে পারে, তবে রিফ্লেকশনে এরর হলে তা মেইন ফ্লো কে ব্লক করবে না।
            if "reflection" not in completed_tasks and "reflection" in self.agents:
                try:
                    await self.agents["reflection"].reflect_and_persist(
                        workspace, user_id
                    )
                except Exception as reflection_error:
                    workspace.log(
                        f"SwarmOrchestrator: Failed to run reflection after error: {reflection_error}"
                    )
            return workspace

        workspace.log(
            "SwarmOrchestrator: Multi-Agent DAG execution completed successfully."
        )
        return workspace
