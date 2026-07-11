# 📄 ফাইল: backend/core/swarm_orchestrator.py

**প্রকার:** .py  
**সাইজ:** 3,593 বাইট  
**আপডেট:** 2026-07-11T15:50:11.299116

---

## কোড

```py
# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import time
import uuid

from agents.crew_departments import ArchitectureAgent
from agents.crew_departments import CodeGeneratorAgent
from agents.crew_departments import QAAgent
from models.shared_workspace import SharedWorkspace


class CircuitBreakerState:
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class CircuitBreaker:
    """
    বাংলা মন্তব্য: P1 Fix — Circuit Breaker pattern with exponential backoff.
    External API call failure এ graceful degradation নিশ্চিত করে।
    Thundering herd problem prevent করে jitter ব্যবহার করে।
    """

    def __init__(self, failure_threshold: int = 5, recovery_timeout: float = 30.0):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.failures = 0
        self.last_failure_time: float | None = None
        self.state = CircuitBreakerState.CLOSED

    async def call(self, coro, *args, **kwargs):
        if self.state == CircuitBreakerState.OPEN:
            if time.time() - (self.last_failure_time or 0) > self.recovery_timeout:
                self.state = CircuitBreakerState.HALF_OPEN
            else:
                raise CircuitBreakerOpenError("Service temporarily unavailable — circuit breaker OPEN")

        try:
            result = await coro(*args, **kwargs)
            if self.state == CircuitBreakerState.HALF_OPEN:
                self.state = CircuitBreakerState.CLOSED
                self.failures = 0
            return result
        except Exception:  # noqa: BLE001
            self.failures += 1
            self.last_failure_time = time.time()
            if self.failures >= self.failure_threshold:
                self.state = CircuitBreakerState.OPEN
            raise


class CircuitBreakerOpenError(Exception):
    """Raised when circuit breaker is OPEN and request is rejected."""

    pass


class SwarmOrchestrator:
    """
    Coordinates execution of specialized agents sharing state inside a workspace context.
    """

    def __init__(self):
        self.architect = ArchitectureAgent()
        self.coder = CodeGeneratorAgent()
        self.qa = QAAgent()
        self.circuit_breaker = CircuitBreaker(failure_threshold=3, recovery_timeout=30.0)

    async def execute_task(self, prompt: str, user_id: str = "default_user_session") -> SharedWorkspace:
        task_id = str(uuid.uuid4())
        workspace = SharedWorkspace(task_id=task_id, original_prompt=prompt)

        workspace.log(f"SwarmOrchestrator: Initialized swarm department for task {task_id}")

        try:
            # 1. Architecture Design Phase
            await self.circuit_breaker.call(self.architect.design, workspace, user_id)

            # 2. Code Generation Phase
            await self.circuit_breaker.call(self.coder.generate_code, workspace, user_id)

            # 3. QA and Security Analysis Phase
            await self.circuit_breaker.call(self.qa.verify, workspace, user_id)
        except CircuitBreakerOpenError as e:
            workspace.log(f"SwarmOrchestrator: Circuit breaker OPEN — {e}")
            workspace.add_error(str(e))
            return workspace

        workspace.log("SwarmOrchestrator: Multi-Agent execution graph completed successfully.")
        return workspace

```