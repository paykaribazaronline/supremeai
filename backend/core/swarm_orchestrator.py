# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import asyncio
import uuid
from core.orchestrators.crew_departments import (ArchitectureAgent, CodeGeneratorAgent, GuardianAgent, QAAgent,
                                                 ReflectionAgent, ResearchAgent)
from models.shared_workspace import SharedWorkspace


class CircuitBreakerState:
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"

class CircuitBreakerOpenError(Exception):
    pass

class CircuitBreaker:
    def __init__(self, failure_threshold: int = 5, recovery_timeout: float = 30.0):
        from core.circuit_breaker import CircuitBreaker as UnifiedCB
        self._cb = UnifiedCB(name="swarm_orch", failure_threshold=failure_threshold, recovery_timeout=recovery_timeout)
        self.state = self._cb.state

    async def call(self, coro, *args, **kwargs):
        self.state = self._cb.state if hasattr(self._cb, "state") else CircuitBreakerState.CLOSED
        if not self._cb.allow_request():
            self.state = CircuitBreakerState.OPEN
            raise CircuitBreakerOpenError("Service temporarily unavailable — circuit breaker OPEN")
        try:
            result = await coro(*args, **kwargs)
            self._cb.mark_success()
            self.state = self._cb.state
            return result
        except Exception:  # noqa: BLE001
            self._cb.mark_failure()
            self.state = self._cb.state
            raise

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
            "qa": QAAgent(),
            "guardian": GuardianAgent(),
            "reflection": ReflectionAgent(),
        }
        self.circuit_breaker = CircuitBreaker(failure_threshold=3, recovery_timeout=30.0)

    async def _get_dag_for_intent(self, intent: str) -> dict[str, list[str]]:
        """
        বাংলা মন্তব্য: ইউজারের ইনটেন্ট অনুযায়ী ডাইনামিক DAG তৈরি করে।
        এটি সিস্টেমকে কোডিং-এর বাইরেও রিসার্চ বা অ্যানালাইসিসের মতো কাজ করার ক্ষমতা দেয়।
        """
        if intent == "code_generation":
            return {
                "architect": [],
                "coder": ["architect"],
                "guardian_qa_loop": ["coder"],
                "reflection": ["guardian_qa_loop"],
            }
        elif intent == "research_analysis":
            return {
                "researcher": [],
                "reflection": ["researcher"],
            }
        # Default DAG for general tasks
        return {
            "researcher": [],
            "reflection": ["researcher"],
        }

    async def execute_task(self, prompt: str, user_id: str = "default_user_session") -> SharedWorkspace:
        task_id = str(uuid.uuid4())
        workspace = SharedWorkspace(task_id=task_id, original_prompt=prompt)
        workspace.log(f"SwarmOrchestrator: Initialized swarm DAG for task {task_id}")
        
        # 1. Classify Intent
        # A simple keyword-based classifier for demonstration
        # In a real system, this would be a more sophisticated NLP model.
        prompt_lower = prompt.lower()
        if any(keyword in prompt_lower for keyword in ["code", "script", "program", "fastapi", "python"]):
            workspace.intent = "code_generation"
        elif any(keyword in prompt_lower for keyword in ["research", "summarize", "analyze", "report"]):
            workspace.intent = "research_analysis"
        else:
            workspace.intent = "general_task"
        workspace.log(f"SwarmOrchestrator: Classified intent as '{workspace.intent}'")

        # 2. Get Dynamic DAG based on intent
        task_graph = await self._get_dag_for_intent(workspace.intent)
        workspace.log(f"SwarmOrchestrator: Constructed DAG with nodes: {list(task_graph.keys())}")
        
        completed_tasks = set()
        
        try:
            while len(completed_tasks) < len(task_graph):
                ready_tasks = [
                    task for task, deps in task_graph.items()
                    if task not in completed_tasks and all(d in completed_tasks for d in deps)
                ]

                if not ready_tasks:
                    # বাংলা মন্তব্য: যদি কোনো রেডি টাস্ক না থাকে, কিন্তু সব টাস্ক শেষ না হয়, তাহলে সম্ভবত একটি সাইকেল বা ভুল গ্রাফ আছে।
                    raise RuntimeError(f"DAG execution error: No ready tasks found, but not all tasks are complete. Completed: {completed_tasks}")

                # বাংলা মন্তব্য: asyncio.gather ব্যবহার করে সব রেডি টাস্ক প্যারালালি এক্সিকিউট করা হচ্ছে।
                await asyncio.gather(
                    *(self.agents[task].run(workspace, user_id) for task in ready_tasks if task in self.agents)
                )
                
                completed_tasks.update(ready_tasks)

        except CircuitBreakerOpenError as e:
            workspace.log(f"SwarmOrchestrator: Circuit breaker OPEN — {e}")
            workspace.add_error(str(e))
            return workspace
        except Exception as e:  # noqa: BLE001
            workspace.log(f"SwarmOrchestrator: An unexpected error occurred during DAG execution: {e}")
            workspace.add_error(str(e))
            # বাংলা মন্তব্য: এরর হলেও রিফ্লেকশন চালানোর চেষ্টা করা হবে, যাতে সিস্টেম শিখতে পারে।
            if "reflection" not in completed_tasks:
                await self.agents["reflection"].reflect_and_persist(workspace, user_id)
            return workspace

        workspace.log("SwarmOrchestrator: Multi-Agent DAG execution completed successfully.")
        return workspace
