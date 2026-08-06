# backend/tests/core/test_swarm_orchestrator_coverage.py
# বাংলা মন্তব্য: SwarmOrchestrator এবং CircuitBreaker-এর জন্য comprehensive unit tests।
# Agent methods mock করা হয়েছে — actual agent execution ছাড়াই।

import time
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from core.orchestration.swarm_orchestrator import SwarmOrchestrator
from core.resilience.circuit_breaker import (CircuitBreaker,
                                             CircuitBreakerOpenError,
                                             CircuitBreakerState)

# -------------------- Fixtures --------------------


@pytest.fixture(autouse=True)
def mock_llm_gateway():
    """Mock the LLM gateway to prevent real network calls."""
    # বাংলা মন্তব্য: রিমোট এবং লোকাল উভয় ধরনের মকিং একসাথে করা হল যাতে টেস্টগুলো নেটওয়ার্ক কল ছাড়াই কাজ করে।
    with patch(
        "core.llm.llm_gateway.get_llm_gateway", new_callable=MagicMock
    ) as mock_gateway_factory:
        mock_gateway = AsyncMock()
        mock_gateway.acompletion = AsyncMock(
            return_value={
                "choices": [
                    {
                        "message": {
                            "content": '{"name": "mocked_tool", "description": "A mocked tool"}'
                        }
                    }
                ]
            }
        )
        mock_gateway_factory.return_value = mock_gateway
        with patch(
            "core.llm.llm_gateway.LLMGateway.acompletion", new_callable=AsyncMock
        ) as mock_acompletion:
            mock_acompletion.return_value = {
                "text": '{"agent_name": "mocked_agent", "script": "print(\'mock\')"}',
                "success": True,
                "choices": [
                    {
                        "message": {
                            "content": '{"name": "mocked_tool", "description": "A mocked tool"}'
                        }
                    }
                ],
            }
            with patch("litellm.acompletion", new_callable=AsyncMock) as mock_litellm:
                mock_litellm.return_value = MagicMock()
                yield mock_gateway


@pytest.fixture(autouse=True)
def mock_mcp_and_agent_factory():
    """Mock MCP client and agent factory to prevent network calls and hanging."""
    with patch(
        "core.mcp_client.MCPRegistryClient.discover_tools",
        new_callable=AsyncMock,
        return_value=[],
    ):
        with patch(
            "core.agent_factory.DynamicAgentFactory.create_specialized_agent",
            new_callable=AsyncMock,
            return_value=None,
        ):
            yield


@pytest.fixture
def circuit_breaker():
    """CircuitBreaker ইনস্ট্যান্স ফেরত দেয়।"""
    return CircuitBreaker(
        name="test_fixture", failure_threshold=3, recovery_timeout=0.1
    )


@pytest.fixture
def mock_workspace():
    """Mock SharedWorkspace।"""
    from core.orchestration.swarm_orchestrator import SharedWorkspace

    workspace = SharedWorkspace(
        task_id="test-task",
        original_prompt="test",
        intent="general",
        tools=[],
        metadata={},
    )
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
        with pytest.raises(CircuitBreakerOpenError):
            raise CircuitBreakerOpenError(
                name="test_service", state=CircuitBreakerState.OPEN
            )

    def test_is_exception(self):
        with pytest.raises(
            Exception
        ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
            raise CircuitBreakerOpenError(
                name="test_service", state=CircuitBreakerState.OPEN
            )


# -------------------- Tests: CircuitBreaker --------------------


class TestCircuitBreakerInit:
    """বাংলা মন্তব্য: CircuitBreaker initialization টেস্ট।"""

    def test_default_initialization(self):
        cb = CircuitBreaker(name="test_service")
        assert cb.name == "test_service"
        assert cb.failure_threshold == 3
        assert cb.recovery_timeout == 60.0
        assert cb.failure_count == 0
        assert cb.last_failure_time is None
        assert cb.state == CircuitBreakerState.CLOSED

    def test_custom_initialization(self):
        cb = CircuitBreaker(
            name="test_service", failure_threshold=10, recovery_timeout=60.0
        )
        assert cb.name == "test_service"
        assert cb.failure_threshold == 10
        assert cb.recovery_timeout == 60.0

    def test_initial_state_is_closed(self):
        cb = CircuitBreaker(name="test_service")
        assert cb.state == CircuitBreakerState.CLOSED


class TestCircuitBreakerCall:
    """বাংলা মন্তব্য: CircuitBreaker.call() method-এর logic টেস্ট।"""

    @pytest.mark.asyncio
    async def test_successful_call_in_closed_state(self, circuit_breaker):
        """বাংলা মন্তব্য: CLOSED state-এ সফল call result return করে।"""
        mock_coro = AsyncMock(return_value="success")

        result = await circuit_breaker.acall(mock_coro, "arg1", key="value")

        assert result == "success"
        assert circuit_breaker.state == CircuitBreakerState.CLOSED
        assert circuit_breaker.failure_count == 0

    @pytest.mark.asyncio
    async def test_successful_call_resets_failures(self, circuit_breaker):
        """বাংলা মন্তব্য: Success হলে failures reset হয়।"""
        circuit_breaker.failure_count = 2
        circuit_breaker.state = CircuitBreakerState.HALF_OPEN
        mock_coro = AsyncMock(return_value="success")

        await circuit_breaker.acall(mock_coro)

        assert circuit_breaker.failure_count == 0
        assert circuit_breaker.state == CircuitBreakerState.CLOSED

    @pytest.mark.asyncio
    async def test_failure_increments_counter(self, circuit_breaker):
        """বাংলা মন্তব্য: Failure হলে failure counter increment হয়।"""
        mock_coro = AsyncMock(side_effect=RuntimeError("Service error"))

        with pytest.raises(RuntimeError):
            await circuit_breaker.acall(mock_coro)

        assert circuit_breaker.failure_count == 1
        assert circuit_breaker.last_failure_time is not None

    @pytest.mark.asyncio
    async def test_multiple_failures_under_threshold(self, circuit_breaker):
        """বাংলা মন্তব্য: Threshold-এর নিচে failures থাকলে state CLOSED থাকে।"""
        mock_coro = AsyncMock(side_effect=RuntimeError("Service error"))

        for _i in range(2):
            with pytest.raises(RuntimeError):
                await circuit_breaker.acall(mock_coro)

        assert circuit_breaker.failure_count == 2
        assert circuit_breaker.state == CircuitBreakerState.CLOSED

    @pytest.mark.asyncio
    async def test_failures_exceed_threshold_opens_circuit(self, circuit_breaker):
        """বাংলা মন্তব্য: Threshold cross করলে circuit OPEN হয়।"""
        mock_coro = AsyncMock(side_effect=RuntimeError("Service error"))

        for _i in range(3):
            with pytest.raises(RuntimeError):
                await circuit_breaker.acall(mock_coro)

        assert circuit_breaker.failure_count == 3
        assert circuit_breaker.state == CircuitBreakerState.OPEN

    @pytest.mark.asyncio
    async def test_open_circuit_rejects_calls(self, circuit_breaker):
        """বাংলা মন্তব্য: OPEN state-এ calls reject হয়।"""
        circuit_breaker.state = CircuitBreakerState.OPEN
        circuit_breaker.opened_at = time.monotonic()
        circuit_breaker.last_failure_time = time.time()

        mock_coro = AsyncMock(return_value="success")

        with pytest.raises(CircuitBreakerOpenError):
            await circuit_breaker.acall(mock_coro)

    @pytest.mark.asyncio
    async def test_open_circuit_transitions_to_half_open_after_timeout(
        self, circuit_breaker
    ):
        """বাংলা মন্তব্য: Recovery timeout পরে OPEN থেকে HALF_OPEN হয়।"""
        circuit_breaker.state = CircuitBreakerState.OPEN
        circuit_breaker.last_failure_time = time.monotonic() - 61.0  # 61 seconds ago

        mock_coro = AsyncMock(return_value="success")

        result = await circuit_breaker.acall(mock_coro)

        assert result == "success"
        # After successful call in HALF_OPEN, it transitions to CLOSED
        assert circuit_breaker.state == CircuitBreakerState.CLOSED

    @pytest.mark.asyncio
    async def test_open_circuit_stays_open_before_timeout(self, circuit_breaker):
        """বাংলা মন্তব্য: Timeout আগে OPEN state maintain করে।"""
        circuit_breaker.state = CircuitBreakerState.OPEN
        circuit_breaker.opened_at = time.monotonic()
        circuit_breaker.last_failure_time = (
            time.monotonic()
        )  # 0 seconds ago (timeout is 0.1)

        mock_coro = AsyncMock(return_value="success")

        with pytest.raises(CircuitBreakerOpenError):
            await circuit_breaker.acall(mock_coro)

        assert circuit_breaker.state == CircuitBreakerState.OPEN

    @pytest.mark.asyncio
    async def test_half_open_success_closes_circuit(self, circuit_breaker):
        """বাংলা মন্তব্য: HALF_OPEN state-এ success হলে CLOSED হয়।"""
        circuit_breaker.state = CircuitBreakerState.HALF_OPEN
        circuit_breaker.failure_count = 2

        mock_coro = AsyncMock(return_value="success")

        result = await circuit_breaker.acall(mock_coro)

        assert result == "success"
        assert circuit_breaker.state == CircuitBreakerState.CLOSED
        assert circuit_breaker.failure_count == 0

    @pytest.mark.asyncio
    async def test_half_open_failure_reopens_circuit(self, circuit_breaker):
        """বাংলা মন্তব্য: HALF_OPEN state-এ failure হলে আবার OPEN হয়।"""
        circuit_breaker.state = CircuitBreakerState.HALF_OPEN
        circuit_breaker.failure_count = 2

        mock_coro = AsyncMock(side_effect=RuntimeError("Still failing"))

        with pytest.raises(RuntimeError):
            await circuit_breaker.acall(mock_coro)

        assert circuit_breaker.state == CircuitBreakerState.OPEN
        assert circuit_breaker.failure_count == 3

    @pytest.mark.asyncio
    async def test_call_with_args_and_kwargs(self, circuit_breaker):
        """বাংলা মন্তব্য: args এবং kwargs correctly coroutine-এ pass হয়।"""
        mock_coro = AsyncMock(return_value="result")

        result = await circuit_breaker.acall(
            mock_coro, "arg1", "arg2", key1="val1", key2="val2"
        )

        mock_coro.assert_called_once_with("arg1", "arg2", key1="val1", key2="val2")
        assert result == "result"


# -------------------- Tests: SwarmOrchestrator --------------------


class TestSwarmOrchestratorInit:
    """বাংলা মন্তব্য: SwarmOrchestrator initialization টেস্ট।"""

    def test_initialization(self):
        """বাংলা মন্তব্য: Orchestrator initialize হয় with all agents।"""
        orchestrator = SwarmOrchestrator()

        assert orchestrator.agents.get("architect") is not None
        assert orchestrator.agents.get("coder") is not None
        assert orchestrator.agents.get("qa") is not None
        assert orchestrator.circuit_breaker is not None

    def test_circuit_breaker_default_config(self):
        """বাংলা মন্তব্য: Circuit breaker default configuration দিয়ে create হয়।"""
        orchestrator = SwarmOrchestrator()
        assert getattr(orchestrator, "circuit_breaker", None) is not None
        assert orchestrator.circuit_breaker.recovery_timeout == 30.0


class TestSwarmOrchestratorExecuteTask:
    """বাংলা মন্তব্য: execute_task() method-এর orchestration logic টেস্ট।"""

    @pytest.mark.asyncio
    async def test_successful_task_execution(self, mock_workspace):
        """বাংলা মন্তব্য: সব agents successfully execute হলে completed workspace return হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(
            orchestrator.agents["architect"], "run", new_callable=AsyncMock
        ):
            with patch.object(
                orchestrator.agents["coder"], "run", new_callable=AsyncMock
            ):
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    return_value=(True, ""),
                ):
                    with patch.object(
                        orchestrator.agents["reflection"], "run", new_callable=AsyncMock
                    ):
                        with patch(
                            "core.orchestration.swarm_orchestrator.SharedWorkspace",
                            return_value=mock_workspace,
                        ):
                            result_exec = await orchestrator.execute_task(
                                "Build a python REST API", "user123"
                            )
                            result = result_exec.workspace
                        assert result is mock_workspace
                        assert (
                            "SwarmOrchestrator: Multi-Agent DAG execution completed successfully."
                            in mock_workspace.execution_logs
                        )

    @pytest.mark.asyncio
    async def test_task_creates_unique_task_id(self, mock_workspace):
        """বাংলা মন্তব্য: প্রতিটি task-এর unique task_id হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(
            orchestrator.agents["architect"], "run", new_callable=AsyncMock
        ):
            with patch.object(
                orchestrator.agents["coder"], "run", new_callable=AsyncMock
            ):
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    return_value=(True, ""),
                ):
                    with patch(
                        "core.orchestration.swarm_orchestrator.SharedWorkspace"
                    ) as mock_ws_class:
                        mock_ws_class.return_value = mock_workspace

                        res1 = await orchestrator.execute_task("python Task 1", "user1")
                        res1 = res1.workspace
                        call1_task_id = (
                            mock_ws_class.call_args_list[0][1].get("task_id")
                            or mock_ws_class.call_args_list[0][0][0]
                        )

                        res2 = await orchestrator.execute_task("python Task 2", "user2")
                        res2 = res2.workspace
                        call2_task_id = (
                            mock_ws_class.call_args_list[1][1].get("task_id")
                            or mock_ws_class.call_args_list[1][0][0]
                        )

                        assert call1_task_id != call2_task_id

    @pytest.mark.asyncio
    async def test_task_logs_initialization(self, mock_workspace):
        """বাংলা মন্তব্য: Task initialization log হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(
            orchestrator.agents["architect"], "run", new_callable=AsyncMock
        ):
            with patch.object(
                orchestrator.agents["coder"], "run", new_callable=AsyncMock
            ):
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    return_value=(True, ""),
                ):
                    with patch(
                        "core.orchestration.swarm_orchestrator.SharedWorkspace",
                        return_value=mock_workspace,
                    ):
                        res = await orchestrator.execute_task(
                            "Test python task", "user123"
                        )
                        res = res.workspace

                        # Verify initialization log was called
                        assert len(mock_workspace.execution_logs) > 0
                        # Check that the log contains the expected message
                        assert any(
                            "Initialized swarm DAG" in call
                            for call in mock_workspace.execution_logs
                        )

    @pytest.mark.asyncio
    async def test_circuit_breaker_open_returns_workspace(self, mock_workspace):
        """বাংলা মন্তব্য: Circuit breaker OPEN হলে workspace return হয় with error।"""
        orchestrator = SwarmOrchestrator()

        # Make circuit breaker open
        orchestrator.circuit_breaker.state = CircuitBreakerState.OPEN
        orchestrator.circuit_breaker.opened_at = time.monotonic()
        orchestrator.circuit_breaker.last_failure_time = time.monotonic()

        with patch(
            "core.orchestration.swarm_orchestrator.SharedWorkspace",
            return_value=mock_workspace,
        ):
            result_exec = await orchestrator.execute_task("Test python task", "user123")
            result = result_exec.workspace

            assert result is mock_workspace
            # Check that log was called with circuit breaker message
            assert len(mock_workspace.execution_logs) > 0
            assert any(
                "Circuit breaker" in call for call in mock_workspace.execution_logs
            )
            assert any("Circuit breaker" in str(err) for err in mock_workspace.errors)

    @pytest.mark.asyncio
    async def test_architecture_phase_failure(self, mock_workspace):
        """বাংলা মন্তব্য: Architecture phase fail করলে circuit breaker trigger হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(
            orchestrator.agents["architect"],
            "run",
            new_callable=AsyncMock,
            side_effect=RuntimeError("Design failed"),
        ):
            with patch.object(
                orchestrator.agents["coder"], "run", new_callable=AsyncMock
            ):
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    return_value=(True, ""),
                ):
                    with patch(
                        "core.orchestration.swarm_orchestrator.SharedWorkspace",
                        return_value=mock_workspace,
                    ):
                        # The SwarmOrchestrator will catch RuntimeError, run reflection, and return workspace
                        # To test circuit breaker, we just check if failure was recorded.
                        res = await orchestrator.execute_task(
                            "Test python task", "user123"
                        )
                        res = res.workspace

                        # Circuit breaker should have recorded the failure
                        assert orchestrator.circuit_breaker.failure_count == 1

    @pytest.mark.asyncio
    async def test_code_generation_phase_failure(self, mock_workspace):
        """বাংলা মন্তব্য: Code generation phase fail করলে circuit breaker trigger হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(
            orchestrator.agents["architect"], "run", new_callable=AsyncMock
        ):
            with patch.object(
                orchestrator.agents["coder"],
                "run",
                new_callable=AsyncMock,
                side_effect=RuntimeError("Code gen failed"),
            ):
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    return_value=(True, ""),
                ):
                    with patch(
                        "core.orchestration.swarm_orchestrator.SharedWorkspace",
                        return_value=mock_workspace,
                    ):
                        res = await orchestrator.execute_task(
                            "Test python task", "user123"
                        )
                        res = res.workspace

                        assert orchestrator.circuit_breaker.failure_count == 1

    @pytest.mark.asyncio
    async def test_qa_phase_failure(self, mock_workspace):
        """বাংলা মন্তব্য: QA phase fail করলে circuit breaker trigger হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(
            orchestrator.agents["architect"], "run", new_callable=AsyncMock
        ):
            with patch.object(
                orchestrator.agents["coder"], "run", new_callable=AsyncMock
            ):
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    side_effect=RuntimeError("QA failed"),
                ):
                    with patch(
                        "core.orchestration.swarm_orchestrator.SharedWorkspace",
                        return_value=mock_workspace,
                    ):
                        res = await orchestrator.execute_task(
                            "Test python task", "user123"
                        )
                        res = res.workspace

                        assert orchestrator.circuit_breaker.failure_count == 1

    @pytest.mark.asyncio
    async def test_default_user_id(self, mock_workspace):
        """বাংলা মন্তব্য: Default user_id 'default_user_session' ব্যবহার হয়।"""
        orchestrator = SwarmOrchestrator()

        with patch.object(
            orchestrator.agents["architect"], "run", new_callable=AsyncMock
        ) as mock_design:
            with patch.object(
                orchestrator.agents["coder"], "run", new_callable=AsyncMock
            ):
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    return_value=(True, ""),
                ):
                    with patch(
                        "core.orchestration.swarm_orchestrator.SharedWorkspace",
                        return_value=mock_workspace,
                    ):
                        res = await orchestrator.execute_task("Test python task")
                        res = res.workspace

                        # Verify architect.run was called with default user_id
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

        # Override Intent logic so it uses standard DAG
        mock_workspace.intent = "standard_code_generation"

        with patch.object(
            orchestrator.agents["architect"], "run", new_callable=AsyncMock
        ) as mock_design:
            with patch.object(
                orchestrator.agents["coder"], "run", new_callable=AsyncMock
            ) as mock_code:
                with patch.object(
                    orchestrator.agents["guardian"],
                    "validate",
                    new_callable=AsyncMock,
                    return_value=(True, ""),
                ) as mock_guardian:
                    with patch(
                        "core.orchestration.swarm_orchestrator.SharedWorkspace",
                        return_value=mock_workspace,
                    ):
                        await orchestrator.execute_task(
                            "Build a python microservice", "user456"
                        )

                        # All three phases should be called
                        mock_design.assert_called_once()
                        mock_code.assert_called_once()
                        mock_guardian.assert_called_once()

                        # All should be called with workspace and user_id
                        for mock_method in [mock_design, mock_code, mock_guardian]:
                            call_args = mock_method.call_args
                            assert call_args[0][0] is mock_workspace
                            assert call_args[0][1] == "user456"

    @pytest.mark.asyncio
    async def test_circuit_breaker_prevents_cascading_failures(self, mock_workspace):
        """বাংলা মন্তব্য: Circuit breaker cascading failures prevent করে।"""
        orchestrator = SwarmOrchestrator()
        mock_workspace.intent = "standard"

        # Simulate multiple failures to open circuit
        for i in range(3):
            with patch.object(
                orchestrator.agents["architect"],
                "run",
                new_callable=AsyncMock,
                side_effect=RuntimeError("Service down"),
            ):
                with patch.object(
                    orchestrator.agents["coder"], "run", new_callable=AsyncMock
                ):
                    with patch.object(
                        orchestrator.agents["guardian"],
                        "validate",
                        new_callable=AsyncMock,
                        return_value=(True, ""),
                    ):
                        with patch(
                            "core.orchestration.swarm_orchestrator.SharedWorkspace",
                            return_value=mock_workspace,
                        ):
                            # SwarmOrchestrator intercepts it and adds error to workspace
                            await orchestrator.execute_task(f"Task python {i}", "user1")

        # Circuit should now be open
        assert orchestrator.circuit_breaker.state == CircuitBreakerState.OPEN

        # Next call should be rejected immediately
        with patch(
            "core.orchestration.swarm_orchestrator.SharedWorkspace",
            return_value=mock_workspace,
        ):
            result = await orchestrator.execute_task("Task python after open", "user1")
            # Should return workspace with error, not raise
            assert result.workspace is mock_workspace
            assert len(mock_workspace.errors) > 0
