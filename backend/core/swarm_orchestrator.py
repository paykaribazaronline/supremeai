# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import asyncio
import uuid

from core.orchestrators.crew_departments import ArchitectureAgent
from core.orchestrators.crew_departments import CodeGeneratorAgent
from core.orchestrators.crew_departments import GuardianAgent
from core.orchestrators.crew_departments import QAAgent
from core.orchestrators.crew_departments import ReflectionAgent
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
    def __init__(self):
        # বাংলা মন্তব্য: এজেন্টদের একটি রেজিস্ট্রি তৈরি করা হচ্ছে, যা ডাইনামিক্যালি কল করা যাবে।
        self.agents = {
            "architect": ArchitectureAgent(),
            "coder": CodeGeneratorAgent(),
            "qa": QAAgent(),
            "guardian": GuardianAgent(),
            "reflection": ReflectionAgent(),
        }
        self.circuit_breaker = CircuitBreaker(failure_threshold=3, recovery_timeout=30.0)

    async def execute_task(self, prompt: str, user_id: str = "default_user_session") -> SharedWorkspace:
        task_id = str(uuid.uuid4())
        workspace = SharedWorkspace(task_id=task_id, original_prompt=prompt)
        workspace.log(f"SwarmOrchestrator: Initialized swarm DAG for task {task_id}")
        
        # বাংলা মন্তব্য: এখানে টাস্কের DAG (Directed Acyclic Graph) ডিফাইন করা হয়েছে।
        # প্রতিটি টাস্কের নাম এবং তার নির্ভরশীলতা (dependencies) উল্লেখ করা আছে।
        task_graph = {
            "architect": [],
            "coder": ["architect"],
            "guardian_qa_loop": ["coder"],
            "reflection": ["guardian_qa_loop"],
        }
        
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
                    *(self.run_node(task, workspace, user_id) for task in ready_tasks)
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
                await self.run_node("reflection", workspace, user_id)
            return workspace

        workspace.log("SwarmOrchestrator: Multi-Agent DAG execution completed successfully.")
        return workspace
