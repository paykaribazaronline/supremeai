# Multi-Agent Swarm Orchestrator Engine
# বাংলা মন্তব্য: মাল্টি-এজেন্ট সিকোয়েন্সিয়াল সোয়ার্ম কোঅর্ডিনেটর ও টাস্ক রানার।

import time
import uuid

import asyncio
from core.orchestrators.crew_departments import ArchitectureAgent
from core.orchestrators.crew_departments import CodeGeneratorAgent
from core.orchestrators.crew_departments import QAAgent
from core.orchestrators.crew_departments import GuardianAgent
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
        self.architect = ArchitectureAgent()
        self.coder = CodeGeneratorAgent()
        self.qa = QAAgent()
        self.guardian = GuardianAgent()
        self.reflection = ReflectionAgent()
        self.circuit_breaker = CircuitBreaker(failure_threshold=3, recovery_timeout=30.0)

    async def execute_task(self, prompt: str, user_id: str = "default_user_session") -> SharedWorkspace:
        task_id = str(uuid.uuid4())
        workspace = SharedWorkspace(task_id=task_id, original_prompt=prompt)
        workspace.log(f"SwarmOrchestrator: Initialized swarm DAG for task {task_id}")

        try:
            # 1. Architecture Phase
            await self.circuit_breaker.call(self.architect.design, workspace, user_id)
            
            # 2. Code Generation & Guardian Loop
            max_retries = 3
            retries = 0
            valid = False
            feedback = ""
            
            await self.circuit_breaker.call(self.coder.generate_code, workspace, user_id)
            
            while not valid and retries < max_retries:
                valid, feedback = await self.circuit_breaker.call(self.guardian.validate, workspace, user_id)
                if not valid:
                    workspace.log(f"SwarmOrchestrator: Guardian rejected code. Triggering Coder refine (Attempt {retries+1}/{max_retries})")
                    await self.circuit_breaker.call(self.coder.refine, workspace, feedback, user_id)
                    retries += 1
                    
            if not valid:
                workspace.log("SwarmOrchestrator: Maximum refine retries reached. Proceeding with warnings.")
                
            # 3. QA Phase
            await self.circuit_breaker.call(self.qa.verify, workspace, user_id)
            
            # 4. Reflection Phase (ZTO Learning Engine)
            await self.circuit_breaker.call(self.reflection.reflect_and_persist, workspace, user_id)
            
        except CircuitBreakerOpenError as e:
            workspace.log(f"SwarmOrchestrator: Circuit breaker OPEN — {e}")
            workspace.add_error(str(e))
            return workspace

        workspace.log("SwarmOrchestrator: Multi-Agent DAG execution completed successfully.")
        return workspace
