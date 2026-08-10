# Implementation Plan: Fix Cascading Errors

This plan addresses the three root causes identified for the 44 test failures.

## Proposed Changes

1. **swarm_orchestrator.py**: Renamed MorphicOrchestrator to SwarmOrchestrator directly and removed the alias to prevent ImportError and isinstance issues.
2. **circuit_breaker.py**: Initialized self.opened_at = None in __init__ to fix the AttributeError.
3. **conftest.py**: Verified the DB bootstrap logic. The setup_test_database fixture already calls Base.metadata.create_all(bind=engine).

### 1. backend/core/orchestration/swarm_orchestrator.py
`python
# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import asyncio
import uuid

from pydantic import BaseModel
from pydantic import ConfigDict
from pydantic import Field

from core.agent_factory import DynamicAgentFactory
from core.mcp_client import MCPRegistryClient
from core.orchestration.crew_departments import ArchitectureAgent
from core.orchestration.crew_departments import CodeGeneratorAgent
from core.orchestration.crew_departments import GuardianAgent
from core.orchestration.crew_departments import IntegrationAgent
from core.orchestration.crew_departments import QAAgent
from core.orchestration.crew_departments import ReflectionAgent
from core.orchestration.crew_departments import ResearchAgent
from core.orchestration.crew_departments import ToolExecutorAgent
from core.orchestration.crew_departments import ToolSynthesizerAgent
from core.resilience.circuit_breaker import CircuitBreaker
from core.skill_manager import skill_manager
from core.skills.core_skills import CodeGenerationSkill
from core.skills.core_skills import ExperiencePersistenceSkill
from core.skills.core_skills import ResearchSkill
from core.skills.core_skills import StaticAnalysisSkill
from core.skills.core_skills import SystemDesignSkill
from core.skills.core_skills import ToolExecutionSkill
from core.skills.core_skills import ToolSynthesisSkill
from core.skills.integrations import GithubSyncSkill
from core.skills.integrations import NotionSyncSkill
from core.skills.integrations import SlackIntegrationSkill
from models.shared_workspace import SharedWorkspace


class ExecutionResult(BaseModel):
    model_config = ConfigDict(arbitrary_types_allowed=True)
    task_id: str = Field(..., description="Unique Master Task ID")
    status: str = Field(..., description="Execution status")
    workspace: SharedWorkspace
    errors: list[str] = Field(default_factory=list)


# Alias for backward compatibility
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
        self.circuit_breaker = CircuitBreaker(name="swarm_orch", failure_threshold=3, recovery_timeout=30.0)
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
        return {"executor": [], "researcher": ["executor"], "reflection": ["researcher"]}

    async def _synthesize_tool(self, intent: str, user_id: str) -> dict | None:
        """
        বাংলা মন্তব্য: ফেজ ২ - Zero-Shot Synthesis (The Morphic Engine).
        যদি কোনো টুল MCP সার্ভারে না থাকে, সিস্টেম নিজেই একটি টুল তৈরি করে নেবে।
        """
        workspace = SharedWorkspace(task_id=str(uuid.uuid4()), original_prompt=f"Synthesize a capability for intent: {intent}")
        workspace.log(f"SwarmOrchestrator: Synthesizing new capability for intent '{intent}' using DynamicAgentFactory.")

        synthesized_capability = await self.agent_factory.create_specialized_agent(f"Create a Python script for the task: {intent}")
        if synthesized_capability and "script" in synthesized_capability:
            workspace.log(f"SwarmOrchestrator: Successfully synthesized new capability: {synthesized_capability.get('agent_name')}")
            return synthesized_capability
        return None

    async def execute_task(self, prompt: str, user_id: str = "default_user_session") -> ExecutionResult:
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

        workspace.log(f"SwarmOrchestrator: Classified intent as '{workspace.intent}' (tier={route.get('tier')}, provider={best_provider})")
        # Store best_provider in workspace for agent consumption
        workspace.work_product["best_provider"] = best_provider

        # 2. Universal Glue: MCP থেকে টুলস ডিসকভার করা
        domain = workspace.intent
        available_mcp_tools = await self.mcp_client.discover_tools(domain)
        workspace.log(f"SwarmOrchestrator: Discovered MCP tools for domain '{domain}': {available_mcp_tools}")
        workspace.work_product["available_tools"] = available_mcp_tools

        # 3. Dynamic Synthesis: যদি টুল না পাওয়া যায়, তবে নতুন টুল তৈরি করা
        if not available_mcp_tools or "generic_tool" in available_mcp_tools:
            workspace.log("SwarmOrchestrator: No specific tool found. Attempting Zero-Shot Synthesis...")
            new_tool = await self._synthesize_tool(workspace.intent, user_id)
            if new_tool:
                workspace.work_product["available_tools"].append(new_tool)

        # 3. Get Dynamic DAG based on intent
        workspace = await self.run_dag_for_workspace(workspace, user_id)
        status = "error" if workspace.errors else "success"
        return ExecutionResult(task_id=workspace.task_id, status=status, workspace=workspace, errors=workspace.errors)

    async def run_dag_for_workspace(self, workspace: SharedWorkspace, user_id: str = "default_user_session") -> SharedWorkspace:
        task_graph = await self._get_dag_for_intent(workspace.intent)
        workspace.log(f"SwarmOrchestrator: Constructed DAG with nodes: {list(task_graph.keys())}")

        completed_tasks = set()

        async def _execute_dag():
            # Standard DAG execution for non-loop parts
            while len(completed_tasks) < len(task_graph):
                ready_tasks = [task for task, deps in task_graph.items()
                               if task not in completed_tasks and all(d in completed_tasks for d in deps)]
                if not ready_tasks:
                    raise RuntimeError(f"DAG execution error: No ready tasks found, but not all tasks are complete. Completed: {completed_tasks}")

                runnable = [task for task in ready_tasks if task in self.agents]
                missing = set(ready_tasks) - set(runnable)
                if missing:
                    # ❗ আগে silently completed মার্ক হতো — এখন স্পষ্ট error, সিস্টেম জানবে সে কিছু মিস করছে
                    raise RuntimeError(
                        f"SwarmOrchestrator: DAG references unregistered agent(s): {missing}. "
                        f"Registered agents: {list(self.agents.keys())}"
                    )

                coros = [self.agents[task].run(workspace, user_id) for task in runnable]
                results = await asyncio.gather(*coros, return_exceptions=True)

                failures = [(task, r) for task, r in zip(runnable, results, strict=False) if isinstance(r, Exception)]
                if failures:
                    failed_names = ", ".join(f"{t}: {e}" for t, e in failures)
                    raise RuntimeError(f"SwarmOrchestrator: task(s) failed in this batch — {failed_names}")

                completed_tasks.update(runnable)   # শুধু যেগুলো সত্যিই সফলভাবে রান হয়েছে

            # Special Handling for 'code_generation' intent's refinement loop
            if workspace.intent == "code_generation":
                max_refinements = 3
                guardian_agent = self.agents.get("guardian")
                coder_agent = self.agents.get("coder")

                if not guardian_agent or not coder_agent:
                    workspace.log("SwarmOrchestrator: Guardian or Coder agent missing for code generation loop.")
                else:
                    for i in range(max_refinements):
                        workspace.log(f"SwarmOrchestrator: Starting Guardian/QA refinement loop, iteration {i + 1}/{max_refinements}")

                        # Guardian validation
                        is_approved, feedback = await guardian_agent.validate(workspace, user_id)

                        if is_approved:
                            workspace.log("SwarmOrchestrator: Code APPROVED by Guardian. Exiting refinement loop.")
                            break

                        workspace.log("SwarmOrchestrator: Code FAILED Guardian validation. Triggering refinement.")

                        # Refinement by CodeGeneratorAgent
                        await coder_agent.refine(workspace, feedback, user_id)
                    else:  # This else belongs to the for loop, executes if loop finishes without break
                        workspace.log("SwarmOrchestrator: Max refinement attempts reached. Proceeding with current code.")

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

            with trace_span("morphic_orchestrator.run_dag_for_workspace", attributes=attributes):
                await self.circuit_breaker.acall(_execute_dag)

        except Exception as e:  # noqa: BLE001
            # বাংলা মন্তব্য: অর্কেস্ট্রেটরের টপ-লেভেলে সব এরর ক্যাচ করার জন্য Exception ব্যবহার করা হয়েছে এবং ট্রেসব্যাক লগ করা হচ্ছে।
            from loguru import logger
            logger.opt(exception=True).error(f"DAG execution failed: {e}")

            from core.resilience.circuit_breaker import CircuitBreakerOpenError

            if isinstance(e, CircuitBreakerOpenError) or "is OPEN" in str(e):
                workspace.log(f"SwarmOrchestrator: Circuit breaker OPEN — {e}")
                workspace.add_error(str(e))
                return workspace

            workspace.log(f"SwarmOrchestrator: An unexpected error occurred during DAG execution: {e}")
            workspace.add_error(str(e))

            # বাংলা মন্তব্য: এরর হলেও রিফ্লেকশন চালানোর চেষ্টা করা হবে, যাতে সিস্টেম শিখতে পারে, তবে রিফ্লেকশনে এরর হলে তা মেইন ফ্লো কে ব্লক করবে না।
            if "reflection" not in completed_tasks and "reflection" in self.agents:
                try:
                    await self.agents["reflection"].reflect_and_persist(workspace, user_id)
                except Exception as reflection_error:  # noqa: BLE001
                    workspace.log(f"SwarmOrchestrator: Failed to run reflection after error: {reflection_error}")
            return workspace

        workspace.log("SwarmOrchestrator: Multi-Agent DAG execution completed successfully.")
        return workspace

`

### 2. backend/core/resilience/circuit_breaker.py
`python
"""Circuit Breaker — Resilience pattern for preventing cascading failures.

বাংলা: সার্কিট ব্রেকার — ক্যাসকেডিং ফেইলিওর প্রতিরোধের জন্য রেজিলিয়েন্স প্যাটার্ন।

Tracks failure/success counts and opens the circuit when threshold exceeded.
After cooldown, transitions to half-open state for recovery testing.
"""
from __future__ import annotations

import threading
import time
from collections.abc import Awaitable
from collections.abc import Callable
from enum import Enum
from typing import Any
from typing import TypeVar

from loguru import logger

from core.config import settings


T = TypeVar("T")


class CircuitBreakerState(str, Enum):
    """Circuit breaker states."""

    CLOSED = "CLOSED"  # Normal operation — requests pass through
    OPEN = "OPEN"  # Failing — requests are rejected immediately
    HALF_OPEN = "HALF_OPEN"  # Testing — limited requests allowed


class CircuitBreakerOpenError(Exception):
    """Raised when the circuit breaker is OPEN and a request is rejected.

    বাংলা: সার্কিট ব্রেকার OPEN থাকলে রিকোয়েস্ট রিজেক্ট হলে এই এক্সেপশন রেইজ হয়।
    """

    def __init__(self, name: str, state: CircuitBreakerState) -> None:
        self.name = name
        self.state = state
        super().__init__(f"Circuit breaker '{name}' is {state.value}. Request rejected.")


class CircuitBreaker:
    """Circuit breaker for a specific operation or service.

    বাংলা: নির্দিষ্ট অপারেশন বা সার্ভিসের জন্য সার্কিট ব্রেকার।

    Attributes:
        name: Identifier for this breaker (e.g., service name).
        failure_threshold: Number of consecutive failures to open the circuit.
        recovery_timeout: Seconds to wait before transitioning to HALF_OPEN.
        state: Current circuit state.
        failure_count: Current consecutive failure count.
        success_count: Current consecutive success count (for half-open recovery).
        last_failure_time: Timestamp of the last failure.
        last_success_time: Timestamp of the last success.
    """

    def __init__(
        self,
        name: str,
        failure_threshold: int | None = None,
        recovery_timeout: float | None = None,
        **kwargs: Any,
    ) -> None:
        self.name = name
        self.failure_threshold = failure_threshold or settings.circuit_breaker_failure_threshold
        self.recovery_timeout = float(recovery_timeout or settings.circuit_breaker_cooldown_period)

        self.state: CircuitBreakerState = CircuitBreakerState.CLOSED
        self.failure_count: int = 0
        self.success_count: int = 0
        self.last_failure_time: float | None = None
        self.last_success_time: float | None = None
        self._recovery_in_progress: bool = False
        self.opened_at: float | None = None
        self._lock = threading.Lock()

    def __repr__(self) -> str:
        with self._lock:
            return (
                f"CircuitBreaker(name='{self.name}', state={self.state.value}, "
                f"failures={self.failure_count}, successes={self.success_count})"
            )

    @property
    def is_open(self) -> bool:
        """Check if the circuit is currently open.

        বাংলা: সার্কিট বর্তমানে OPEN কিনা চেক করে।
        """
        with self._lock:
            return self.state == CircuitBreakerState.OPEN

    def _should_attempt_recovery(self) -> bool:
        """Check if enough time has passed to attempt recovery.

        বাংলা: রিকভারি চেষ্টা করার জন্য যথেষ্ট সময় পেরিয়েছে কিনা চেক করে।
        """
        if self.last_failure_time is None:
            return True
        return (time.monotonic() - self.last_failure_time) >= self.recovery_timeout

    def call(self, func: Callable[..., T], *args: Any, **kwargs: Any) -> T:
        """Execute a function with circuit breaker protection (sync).

        বাংলা: সার্কিট ব্রেকার প্রোটেকশন সহ সিঙ্ক্রোনাস ফাংশন এক্সিকিউট করে।

        Raises:
            CircuitBreakerOpenError: If circuit is OPEN and not ready for recovery.
        """
        with self._lock:
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_recovery():
                    logger.info(f"Circuit breaker '{self.name}' transitioning to HALF_OPEN for recovery test")
                    self.state = CircuitBreakerState.HALF_OPEN
                    self._recovery_in_progress = True
                else:
                    raise CircuitBreakerOpenError(self.name, self.state)
            elif self.state == CircuitBreakerState.HALF_OPEN:
                if self._recovery_in_progress:
                    raise CircuitBreakerOpenError(self.name, self.state)
                self._recovery_in_progress = True

        try:
            result = func(*args, **kwargs)
            self._mark_success()
            return result
        except (ConnectionError, TimeoutError, OSError) as exc:
            logger.warning(f"Circuit breaker '{self.name}' caught recoverable error: {exc}")
            self._mark_failure()
            raise
        except Exception:
            logger.opt(exception=True).error(f"Circuit breaker '{self.name}' caught unexpected error")
            self._mark_failure()
            raise

    async def acall(self, func: Callable[..., Awaitable[T]], *args: Any, **kwargs: Any) -> T:
        """Execute an async function with circuit breaker protection.

        বাংলা: সার্কিট ব্রেকার প্রোটেকশন সহ অ্যাসিঙ্ক্রোনাস ফাংশন এক্সিকিউট করে।

        Raises:
            CircuitBreakerOpenError: If circuit is OPEN and not ready for recovery.
        """
        with self._lock:
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_recovery():
                    logger.info(f"Circuit breaker '{self.name}' transitioning to HALF_OPEN for recovery test")
                    self.state = CircuitBreakerState.HALF_OPEN
                    self._recovery_in_progress = True
                else:
                    raise CircuitBreakerOpenError(self.name, self.state)
            elif self.state == CircuitBreakerState.HALF_OPEN:
                if self._recovery_in_progress:
                    raise CircuitBreakerOpenError(self.name, self.state)
                self._recovery_in_progress = True

        try:
            result = await func(*args, **kwargs)
            self._mark_success()
            return result
        except (ConnectionError, TimeoutError, OSError) as exc:
            logger.warning(f"Circuit breaker '{self.name}' caught recoverable error: {exc}")
            self._mark_failure()
            raise
        except Exception:
            logger.opt(exception=True).error(f"Circuit breaker '{self.name}' caught unexpected error")
            self._mark_failure()
            raise

    def _mark_success(self) -> None:
        """Record a successful call and potentially close the circuit.

        বাংলা: সফল কল রেকর্ড করে এবং সম্ভবত সার্কিট বন্ধ করে।
        """
        with self._lock:
            self.success_count += 1
            self.failure_count = 0
            self.last_success_time = time.monotonic()

            if self.state == CircuitBreakerState.HALF_OPEN:
                logger.info(f"Circuit breaker '{self.name}' recovered — transitioning to CLOSED")
                self.state = CircuitBreakerState.CLOSED
                self._recovery_in_progress = False

    def _mark_failure(self) -> None:
        """Record a failed call and potentially open the circuit.

        বাংলা: ব্যর্থ কল রেকর্ড করে এবং সম্ভবত সার্কিট খোলে।
        """
        with self._lock:
            self.failure_count += 1
            self.success_count = 0
            self.last_failure_time = time.monotonic()

            if self.failure_count >= self.failure_threshold and self.state != CircuitBreakerState.OPEN:
                logger.warning(
                    f"Circuit breaker '{self.name}' opened after {self.failure_count} consecutive failures"
                )
                self.state = CircuitBreakerState.OPEN
                self._recovery_in_progress = False

    def reset(self) -> None:
        """Manually reset the circuit breaker to CLOSED state.

        বাংলা: ম্যানুয়ালি সার্কিট ব্রেকারকে CLOSED স্টেটে রিসেট করে।
        """
        with self._lock:
            logger.info(f"Circuit breaker '{self.name}' manually reset")
            self.state = CircuitBreakerState.CLOSED
            self.failure_count = 0
            self.success_count = 0
            self.last_failure_time = None
            self.last_success_time = None
            self._recovery_in_progress = False

    def get_metrics(self) -> dict[str, Any]:
        """Get current metrics for monitoring.

        বাংলা: মনিটরিংয়ের জন্য বর্তমান মেট্রিক্স রিটার্ন করে।
        """
        with self._lock:
            state_val = 0
            if self.state == CircuitBreakerState.OPEN:
                state_val = 2
            elif self.state == CircuitBreakerState.HALF_OPEN:
                state_val = 1

            return {
                f'circuit_breaker_state{{name="{self.name}"}}': state_val,
                f'circuit_breaker_failures_total{{name="{self.name}"}}': self.failure_count,
                f'circuit_breaker_successes_total{{name="{self.name}"}}': self.success_count,
            }

    def __call__(self, func: Callable[..., Any]) -> Callable[..., Any]:
        """Allow CircuitBreaker to be used as a decorator."""
        import asyncio
        import functools
        if asyncio.iscoroutinefunction(func):
            @functools.wraps(func)
            async def async_wrapper(*args: Any, **kwargs: Any) -> Any:
                return await self.acall(func, *args, **kwargs)
            return async_wrapper
        else:
            @functools.wraps(func)
            def sync_wrapper(*args: Any, **kwargs: Any) -> Any:
                return self.call(func, *args, **kwargs)
            return sync_wrapper

    def allow_request(self) -> bool:
        """Check if request is allowed through the breaker.

        বাংলা: রিকোয়েস্ট সার্কিট দিয়ে পাস হতে পারবে কিনা চেক করে।
        """
        with self._lock:
            if self.state == CircuitBreakerState.OPEN:
                if self._should_attempt_recovery():
                    logger.info(f"Circuit breaker '{self.name}' transitioning to HALF_OPEN for recovery test")
                    self.state = CircuitBreakerState.HALF_OPEN
                    self._recovery_in_progress = True
                    return True
                return False
            elif self.state == CircuitBreakerState.HALF_OPEN:
                if self._recovery_in_progress:
                    return False
                self._recovery_in_progress = True
                return True
            return True

    def mark_success(self) -> None:
        """Record successful request."""
        self._mark_success()

    def mark_failure(self) -> None:
        """Record failed request."""
        self._mark_failure()

`

### 3. backend/tests/conftest.py
`python
import os
import sys

from loguru import logger

# বাংলা মন্তব্য: pytest কালেকশনের সময় loguru-এর ডিফল্ট stderr হ্যান্ডলার যেন I/O error না দেয়, তাই প্রথমেই সেটি রিমুভ করা হলো।
logger.remove()

os.environ["ENCRYPTION_KEY"] = "9llmzMU2XSRhbAS-R__JMW1XLZzc0ll7obD_RqaVwno="
os.environ["ENCRYPTION_KEY"] = "9llmzMU2XSRhbAS-R__JMW1XLZzc0ll7obD_RqaVwno="
os.environ["STRIPE_API_KEY"] = "sk_test_dummy"
os.environ["STRIPE_WEBHOOK_SECRET"] = "whsec_dummy"
os.environ["OPENROUTER_API_KEY"] = "sk-or-v1-dummy"
os.environ["GEMINI_API_KEY"] = "AIzaSy_dummy"
os.environ["CI_WEBHOOK_SECRET"] = "dummy_ci"
os.environ["ENV"] = "test"
os.environ["DOCS_PASSWORD"] = "dummy_pass"
os.environ["SUPREMEAI_ADMIN_PASSWORD_HASH"] = "dummy_admin_hash"
import sys
import matplotlib

matplotlib.use("Agg")


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if ROOT not in sys.path:
    sys.path.insert(0, ROOT)
# Also add repository root and scripts/ directory so tests can import moved modules
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SCRIPTS_DIR = os.path.join(REPO_ROOT, "scripts")
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)
if os.path.isdir(SCRIPTS_DIR) and SCRIPTS_DIR not in sys.path:
    sys.path.insert(0, SCRIPTS_DIR)
os.environ.setdefault("OPENROUTER_API_KEY", "mock-key-value")

# বাংলা মন্তব্য: টেস্ট রান করার সময় রিয়াল ডাটাবেস এড়াতে এবং লক হওয়া রোধ করতে ইন-মেমোরি ডাটাবেস সেট করা হলো
os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"
os.environ["SUPABASE_DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"
os.environ["SUPABASE_DATABASE_URL_POOLER"] = "sqlite+aiosqlite:///:memory:"


# Mock Google Auth credentials and services globally during tests
from unittest.mock import MagicMock


try:
    import google.auth

    google.auth.default = lambda *args, **kwargs: (MagicMock(), "mock-project-id")
except ImportError:
    sys.modules["google.auth"] = MagicMock()

try:
    import google.cloud.firestore

    google.cloud.firestore.Client = MagicMock
except ImportError:
    sys.modules["google.cloud.firestore"] = MagicMock()

try:
    import google.cloud.secretmanager

    google.cloud.secretmanager.SecretManagerServiceClient = MagicMock
except ImportError:
    sys.modules["google.cloud.secretmanager"] = MagicMock()

os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = "/dev/null"

import contextlib

import pytest

from core.security.rbac import RoleBasedAccessControl


_TEST_ENV_DEFAULTS = {
    "ENV": "test",
    "OPENROUTER_API_KEY": "mock_openrouter",
    "HF_API_KEY": "mock_hf",
    "GEMINI_API_KEY": "mock_gemini",
    "DEEPSEEK_API_KEY": "mock_deepseek",
    "GROQ_API_KEY": "mock_groq",
    "NVIDIA_API_KEY": "mock_nvidia",
    "FIRECRAWL_API_KEY": "mock_firecrawl",
    "OLLAMA_URL": "http://127.0.0.1:11434",
    "SUPREMEAI_API_KEY": "",
    "SENTRY_DSN": "",
    "GCP_PROJECT_ID": "",
    "GCP_REGION": "",
    "SUPABASE_DATABASE_URL": "sqlite+aiosqlite:///:memory:",
    "SUPABASE_DATABASE_URL_POOLER": "sqlite+aiosqlite:///:memory:",
    "GITHUB_TOKEN": "mock_dummy_token",
    "RENDER_API_KEY": "mock_render_key",
    "ADMIN_AUTHORIZED": "false",
    "RAILWAY_TOKEN": "mock_railway_token",
    "ORACLE_CLOUD_API_KEY": "mock_oracle_key",
    "AUTOFIX_AUTHORIZED": "false",
    "EXPERIENCE_DB_PATH": f"data/test_experience_{os.getpid()}.db",
    "LITELLM_DISABLE_ASYNC_CLIENT_CLEANUP": "True",
}


@pytest.fixture
def rbac():
    return RoleBasedAccessControl()


@pytest.fixture(autouse=True)
def isolate_env(monkeypatch: pytest.MonkeyPatch):
    import core.config

    for key, value in _TEST_ENV_DEFAULTS.items():
        monkeypatch.setenv(key, value)
        try:
            import brain.model_router
            if hasattr(brain.model_router.ModelRouter, "_breakers"):
                brain.model_router.ModelRouter._breakers.clear()
        except ImportError:
            pass
        try:
            if hasattr(core.config.settings, key.lower()):
                setattr(core.config.settings, key.lower(), value)
            elif hasattr(core.config.settings, key):
                setattr(core.config.settings, key, value)
            elif getattr(core.config.settings.model_config, "extra", "ignore") == "allow":
                setattr(core.config.settings, key.lower(), value)
        except AttributeError:
            pass


@pytest.fixture(autouse=True, scope="session")
def bypass_jwt_auth():
    from unittest.mock import patch

    patches = []
    targets = [
        "backend.middleware.auth_middleware.verify_token",
        "middleware.auth_middleware.verify_token",
        "backend.core.security.verify_token",
        "core.security.verify_token",
        "core.security.auth_middleware._decode_jwt",
    ]
    for target in targets:
        try:
            p = patch(target)
            mock = p.start()
            mock.return_value = {"sub": "test_admin@supremeai.com", "role": "admin"}
            patches.append(p)
        except Exception as e:  # noqa: BLE001
            import logging

            logging.warning(f"Exception suppressed: {e}")
    yield
    for p in patches:
        with contextlib.suppress(Exception):
            p.stop()


@pytest.fixture(autouse=True)
def configure_litellm():
    """টেস্টের জন্য litellm সেটিংস কনফিগার করুন"""
    # বাংলা মন্তব্য: লিটেলএলএম প্রক্সি এবং টেলিমেট্রি সেটিংস নিশ্চিত করা
    try:
        import threading

        result = {}

        def _import():
            try:
                import litellm

                result["module"] = litellm
            except Exception as e:  # noqa: BLE001
                result["error"] = e

        t = threading.Thread(target=_import, daemon=True)
        t.start()
        t.join(timeout=8)
        if t.is_alive():
            import logging

            logging.warning("litellm import timed out; skipping configuration")
        elif "error" in result:
            import logging

            logging.warning(f"Exception suppressed: {result['error']}")
        else:
            litellm = result["module"]
            litellm.use_litellm_proxy = False
            litellm.drop_params = True
            litellm.telemetry = False
    except Exception as e:  # noqa: BLE001
        import logging

        logging.warning(f"Exception suppressed: {e}")
    yield


@pytest.fixture
def mock_production_env(monkeypatch):
    monkeypatch.setenv("ENV", "production")
    monkeypatch.setenv("OPENROUTER_API_KEY", "sk-mock-123")
    monkeypatch.setenv("GEMINI_API_KEY", "mock-key")


import pytest_asyncio

pytest_plugins = ["pytest_asyncio"]


@pytest_asyncio.fixture(autouse=True, scope="session")
async def setup_test_database():
    from sqlalchemy.ext.compiler import compiles
    from sqlalchemy.dialects.postgresql import JSONB
    from sqlalchemy.types import JSON
    import sqlalchemy.dialects.sqlite as sqlite_dialect

    @compiles(JSONB, "sqlite")
    def compile_jsonb_sqlite(type_, compiler, **kw):
        return "JSON"

    from database.session import engine
    from models.base import Base
    import importlib
    import pkgutil
    import models

    # Import all modules in the models package so they are registered with Base
    for _, module_name, _ in pkgutil.iter_modules(models.__path__):
        importlib.import_module(f"models.{module_name}")

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest_asyncio.fixture
async def async_session():
    from unittest.mock import AsyncMock

    yield AsyncMock()

`
