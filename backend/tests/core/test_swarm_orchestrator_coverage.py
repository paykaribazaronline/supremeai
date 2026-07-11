# backend/tests/core/test_swarm_orchestrator_coverage.py
# বাংলা মন্তব্য: SwarmOrchestrator এবং CircuitBreaker-এর জন্য comprehensive unit tests।
# Agent methods mock করা হয়েছে — actual agent execution ছাড়াই।

import time
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from core.swarm_orchestrator import CircuitBreaker
from core.swarm_orchestrator import CircuitBreakerOpenError
from core.swarm_orchestrator import CircuitBreakerState
from core.swarm_orchestrator import SwarmOrchestrator


# -------------------- Fixtures --------------------


@pytest.fixture
def circuit_breaker():
    """CircuitBreaker ইনস্ট্যান্স ফেরত দেয়।"""
    return CircuitBreaker(failure_threshold=3, recovery_timeout=30.0)


@pytest.fixture
def mock_workspace():
    """Mock SharedWorkspace।"""
    workspace = MagicMock()
    workspace.log = MagicMock()
    workspace.add_error = MagicMock()
    return workspace


# -------------------- Tests: CircuitBreakerState --------------------


class TestCircuitBreakerState:
    """বাংলা মন্তব্য: CircuitBreakerState constants টেস্ট।"""

    def test_closed_state(self):
        assert CircuitBreakerState.CLOSED == "CLOSED"

    def test_open_state(self):
        assert CircuitBreakerState.OPEN == "OPEN"

    def test_half_open_state(self):
        assert CircuitBreakerState.HALF_OPEN == "HALF_OPEN"


# -------------------- Tests: CircuitBreakerOpenError --------------------


class TestCircuitBreakerOpenError:
    """বাংলা মন্তব্য: CircuitBreakerOpenError exception টেস্ট।"""

    def test_can_be_raised(self):
        with pytest.raises(CircuitBreakerOpenError, match="Service temporarily unavailable"):
            raise CircuitBreakerOpenError("Service temporarily unavailable — circuit breaker OPEN")

    def test_is_exception(self):
        with pytest.raises(Exception):
            raise CircuitBreakerOpenError("Test error")


# -------------------- Tests: CircuitBreaker --------------------


class TestCircuitBreakerInit:
    """বাংলা মন্তব্য: CircuitBreaker initialization টেস্ট।"""

    def test_default_initialization(self):
        cb = CircuitBreaker()
        assert cb.failure_threshold == 5
        assert cb.recovery_timeout == 30.0
        assert cb.failures == 0
        assert cb.last_failure_time is None
        assert cb.state == CircuitBreakerState.CLOSED

    def test_custom_initialization(self):
        cb = CircuitBreaker(failure_threshold=10, recovery_timeout=60.0)
        assert cb.failure_threshold == 10
        assert cb.recovery_timeout == 60.0

    def test_initial_state_is_closed(self):
        cb = CircuitBreaker()
        assert cb.state == CircuitBreakerState.CLOSED


class TestCircuitBreakerCall:
    """বাংলা মন্তব্য: CircuitBreaker.call() method-এর logic টেস্ট।"""

    @pytest.mark.asyncio
    async def test_successful_call_in_closed_state(self, circuit_breaker):
        """বাংলা মন্তব্য: CLOSED state-এ সফল call result return করে।"""
        mock_coro = AsyncMock(return_value="success")

        result = await circuit_breaker.call(mock_coro, "arg1", key="value")

        assert result == "success"
        assert circuit_breaker.state == CircuitBreakerState.CLOSED
        assert circuit_breaker.failures == 0

    @pytest.mark.asyncio
    async def test_successful_call_resets_failures(self, circuit_breaker):
        """বাংলা মন্তব্য: Success হলে failures reset হয়।"""
        circuit_breaker.failures = 2
        circuit_breaker.state = CircuitBreakerState.HALF_OPEN  # Need to be in HALF_OPEN state
        mock_coro = AsyncMock(return_value="success")

        await circuit_breaker.call(mock_coro)

        assert circuit_breaker.failures == 0
        assert circuit_breaker.state == CircuitBreakerState.CLOSED

    @pytest.mark.asyncio
    async def test_failure_increments_counter(self, circuit_breaker):
        """বাংলা মন্তব্য: Failure হলে failure counter increment হয়।"""
        mock_coro = AsyncMock(side_effect=RuntimeError("Service error"))

        with pytest.raises(RuntimeError):
            await circuit_breaker.call(mock_coro)

        assert circuit_breaker.failures == 1
        assert circuit_breaker.last_failure_time is not None

    @pytest.mark.asyncio
    async def test_multiple_failures_under_threshold(self, circuit_breaker):
        """বাংলা মন্তব্য: Threshold-এর নিচে failures থাকলে state CLOSED থাকে।"""
        mock_coro = AsyncMock(side_effect=RuntimeError("Service error"))

        for i in range(2):
            with pytest.raises(RuntimeError):
                await circuit_breaker.call(mock_coro)

        assert circuit_breaker.failures == 2
        assert circuit_breaker.state == CircuitBreakerState.CLOSED

    @pytest.mark.asyncio
    async def test_failures_exceed_threshold_opens_circuit(self, circuit_breaker):
        """বাংলা মন্তব্য: Threshold cross করলে circuit OPEN হয়।"""
        mock_coro = AsyncMock(side_effect=RuntimeError("Service error"))

        for i in range(3):
            with pytest.raises(RuntimeError):
                await circuit_breaker.call(mock_coro)

        assert circuit_breaker.failures == 3
        assert circuit_breaker.state == CircuitBreakerState.OPEN

    @pytest.mark.asyncio
    async def test_open_circuit_rejects_calls(self, circuit_breaker):
        """বাংলা মন্তব্য: OPEN state-এ calls reject হয়।"""
        circuit_breaker.state = CircuitBreakerState.OPEN
        circuit_breaker.last_failure_time = time.time()

        mock_coro = AsyncMock(return_value="success")

        with pytest.raises(CircuitBreakerOpenError, match="Service temporarily unavailable"):
            await circuit_breaker.call(mock_coro)

    @pytest.mark.asyncio
    async def test_open_circuit_transitions_to_half_open_after_timeout(self, circuit_breaker):
        """বাংলা মন্তব্য: Recovery timeout পরে OPEN থেকে HALF_OPEN হয়।"""
        circuit_breaker.state = CircuitBreakerState.OPEN
        circuit_breaker.last_failure_time = time.time() - 31.0  # 31 seconds ago

        mock_coro = AsyncMock(return_value="success")

        result = await circuit_breaker.call(mock_coro)

        assert result == "success"
        # After successful call in HALF_OPEN, it transitions to CLOSED
        assert circuit_breaker.state == CircuitBreakerState.CLOSED

    @pytest.mark.asyncio
    async def test_open_circuit_stays_open_before_timeout(self, circuit_breaker):
        """বাংলা মন্তব্য: Timeout আগে OPEN state maintain করে।"""
        circuit_breaker.state = CircuitBreakerState.OPEN
        circuit_breaker.last_failure_time = time.time() - 10.0  # 10 seconds ago

        mock_coro = AsyncMock(return_value="success")

        with pytest.raises(CircuitBreakerOpenError):
            await circuit_breaker.call(mock_coro)

        assert circuit_breaker.state == CircuitBreakerState.OPEN

    @pytest.mark.asyncio
    async def test_half_open_success_closes_circuit(self, circuit_breaker):
        """বাংলা মন্তব্য: HALF_OPEN state-এ success হলে CLOSED হয়।"""
        circuit_breaker.state = CircuitBreakerState.HALF_OPEN
        circuit_breaker.failures = 2

        mock_coro = AsyncMock(return_value="success")

        result = await circuit_breaker.call(mock_coro)

        assert result == "success"
        assert circuit_breaker.state == CircuitBreakerState.CLOSED
        assert circuit_breaker.failures == 0

    @pytest.mark.asyncio
    async def test_half_open_failure_reopens_circuit(self, circuit_breaker):
        """বাংলা মন্তব্য: HALF_OPEN state-এ failure হলে আবার OPEN হয়।"""
        circuit_breaker.state = CircuitBreakerState.HALF_OPEN
        circuit_breaker.failures = 2

        mock_coro = AsyncMock(side_effect=RuntimeError("Still failing"))

        with pytest.raises(RuntimeError):
            await circuit_breaker.call(mock_coro)

        assert circuit_breaker.state == CircuitBreakerState.OPEN
        assert circuit_breaker.failures == 3

    @pytest.mark.asyncio
    async def test_call_with_args_and_kwargs(self, circuit_breaker):
        """বাংলা মন্তব্য: args এবং kwargs correctly coroutine-এ pass হয়।"""
        mock_coro = AsyncMock(return_value="result")

        result = await circuit_breaker.call(mock_coro, "arg1", "arg2", key1="val1", key2="val2")

        mock_coro.assert_called_once_with("arg1", "arg2", key1="val1", key2="val2")
        assert result == "result"


# -------------------- Tests: SwarmOrchestrator --------------------


class TestSwarmOrchestratorInit:
    """বাংলা মন্তব্য: SwarmOrchestrator initialization টেস্ট।"""

    def test_initialization(self):
        """বাংলা মন্তব্য: Orchestrator initialize হয় with all agents।"""
        orchestrator = SwarmOrchestrator()

        assert orchestrator.architect is not None
        assert orchestrator.coder is not None
        assert orchestrator.qa is not None
        assert orchestrator.circuit_breaker is not None

    def test_circuit_breaker_default_config(self):
        """বাংলা মন্তব্য: Circuit breaker default configuration দিয়ে create হয়।"""
        orchestrator = SwarmOrchestrator()
        assert orchestrator.circuit_breaker.failure_threshold == 3
        assert orchestrator.circuit_breaker.recovery_timeout == 30.0


class TestSwarmOrchestratorExecuteTask:
    """বাংলা মন্তব্য: execute_task() method-এর orchestration logic টেস্ট।"""

    @pytest.mark.asyncio
    async def test_successful_task_execution(self, mock_workspace):
        """বাংলা মন্তব্য: সব agents successfully execute হলে completed workspace return হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock):
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock):
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock):
                    with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                        result = await orchestrator.execute_task("Build a REST API", "user123")

                        assert result is mock_workspace
                        mock_workspace.log.assert_any_call("SwarmOrchestrator: Multi-Agent execution graph completed successfully.")

    @pytest.mark.asyncio
    async def test_task_creates_unique_task_id(self, mock_workspace):
        """বাংলা মন্তব্য: প্রতিটি task-এর unique task_id হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock):
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock):
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock):
                    with patch("core.swarm_orchestrator.SharedWorkspace") as mock_ws_class:
                        mock_ws_class.return_value = mock_workspace

                        await orchestrator.execute_task("Task 1", "user1")
                        call1_task_id = mock_ws_class.call_args[1]["task_id"]

                        await orchestrator.execute_task("Task 2", "user2")
                        call2_task_id = mock_ws_class.call_args[1]["task_id"]

                        assert call1_task_id != call2_task_id

    @pytest.mark.asyncio
    async def test_task_logs_initialization(self, mock_workspace):
        """বাংলা মন্তব্য: Task initialization log হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock):
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock):
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock):
                    with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                        await orchestrator.execute_task("Test task", "user123")

                        # Verify initialization log was called
                        mock_workspace.log.assert_called()
                        # Check that the log contains the expected message
                        log_calls = [str(call) for call in mock_workspace.log.call_args_list]
                        assert any("Initialized swarm department" in call for call in log_calls)

    @pytest.mark.asyncio
    async def test_circuit_breaker_open_returns_workspace(self, mock_workspace):
        """বাংলা মন্তব্য: Circuit breaker OPEN হলে workspace return হয় with error।"""
        orchestrator = SwarmOrchestrator()

        # Make circuit breaker open
        orchestrator.circuit_breaker.state = CircuitBreakerState.OPEN
        orchestrator.circuit_breaker.last_failure_time = time.time()

        with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
            result = await orchestrator.execute_task("Test task", "user123")

            assert result is mock_workspace
            # Check that log was called with circuit breaker message
            mock_workspace.log.assert_called()
            log_calls = [str(call) for call in mock_workspace.log.call_args_list]
            assert any("Circuit breaker OPEN" in call for call in log_calls)
            mock_workspace.add_error.assert_called_once()

    @pytest.mark.asyncio
    async def test_architecture_phase_failure(self, mock_workspace):
        """বাংলা মন্তব্য: Architecture phase fail করলে circuit breaker trigger হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock, side_effect=RuntimeError("Design failed")):
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock):
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock):
                    with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                        with pytest.raises(RuntimeError):
                            await orchestrator.execute_task("Test task", "user123")

                        # Circuit breaker should have recorded the failure
                        assert orchestrator.circuit_breaker.failures == 1

    @pytest.mark.asyncio
    async def test_code_generation_phase_failure(self, mock_workspace):
        """বাংলা মন্তব্য: Code generation phase fail করলে circuit breaker trigger হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock):
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock, side_effect=RuntimeError("Code gen failed")):
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock):
                    with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                        with pytest.raises(RuntimeError):
                            await orchestrator.execute_task("Test task", "user123")

                        assert orchestrator.circuit_breaker.failures == 1

    @pytest.mark.asyncio
    async def test_qa_phase_failure(self, mock_workspace):
        """বাংলা মন্তব্য: QA phase fail করলে circuit breaker trigger হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock):
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock):
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock, side_effect=RuntimeError("QA failed")):
                    with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                        with pytest.raises(RuntimeError):
                            await orchestrator.execute_task("Test task", "user123")

                        assert orchestrator.circuit_breaker.failures == 1

    @pytest.mark.asyncio
    async def test_default_user_id(self, mock_workspace):
        """বাংলা মন্তব্য: Default user_id 'default_user_session' ব্যবহার হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock) as mock_design:
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock):
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock):
                    with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                        await orchestrator.execute_task("Test task")

                        # Verify architect.design was called with default user_id
                        mock_design.assert_called_once()
                        call_args = mock_design.call_args
                        assert call_args[0][1] == "default_user_session"


# -------------------- Tests: Integration --------------------


class TestSwarmOrchestratorIntegration:
    """বাংলা মন্তব্য: Integration-style tests for realistic scenarios।"""

    @pytest.mark.asyncio
    async def test_full_successful_execution_flow(self, mock_workspace):
        """বাংলা মন্তব্য: সম্পূর্ণ successful execution flow।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(orchestrator.architect, "design", new_callable=AsyncMock) as mock_design:
            with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock) as mock_code:
                with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock) as mock_verify:
                    with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                        result = await orchestrator.execute_task("Build a microservice", "user456")

                        # All three phases should be called
                        mock_design.assert_called_once()
                        mock_code.assert_called_once()
                        mock_verify.assert_called_once()

                        # All should be called with workspace and user_id
                        for mock_method in [mock_design, mock_code, mock_verify]:
                            call_args = mock_method.call_args
                            assert call_args[0][0] is mock_workspace
                            assert call_args[0][1] == "user456"

    @pytest.mark.asyncio
    async def test_circuit_breaker_prevents_cascading_failures(self, mock_workspace):
        """বাংলা মন্তব্য: Circuit breaker cascading failures prevent করে।"""
        orchestrator = SwarmOrchestrator()

        # Simulate multiple failures to open circuit
        for i in range(3):
            with patch.object(orchestrator.architect, "design", new_callable=AsyncMock, side_effect=RuntimeError("Service down")):
                with patch.object(orchestrator.coder, "generate_code", new_callable=AsyncMock):
                    with patch.object(orchestrator.qa, "verify", new_callable=AsyncMock):
                        with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
                            with pytest.raises(RuntimeError):
                                await orchestrator.execute_task(f"Task {i}", "user1")

        # Circuit should now be open
        assert orchestrator.circuit_breaker.state == CircuitBreakerState.OPEN

        # Next call should be rejected immediately
        with patch("core.swarm_orchestrator.SharedWorkspace", return_value=mock_workspace):
            result = await orchestrator.execute_task("Task after open", "user1")
            # Should return workspace with error, not raise
            assert result is mock_workspace
            mock_workspace.add_error.assert_called_once()
