# backend/tests/core/test_event_bus_coverage.py
# বাংলা মন্তব্য: ErrorEventBus-এর জন্য comprehensive unit tests।
# Async event bus with DLQ, listeners, এবং structured context।

import asyncio
import json
from datetime import UTC
from datetime import datetime
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from core.messaging.event_bus import DeadLetterQueueItem
from core.messaging.event_bus import ErrorContext
from core.messaging.event_bus import ErrorEvent
from core.messaging.event_bus import ErrorEventBus
from core.messaging.event_bus import error_event_bus


# -------------------- Fixtures --------------------


@pytest.fixture
def event_bus():
    """Fresh ErrorEventBus instance for each test।"""
    return ErrorEventBus()


@pytest.fixture
def sample_error_event():
    """Sample ErrorEvent for testing।"""
    return ErrorEvent(
        module="test_module",
        error_type="TestError",
        message="This is a test error message",
        severity="ERROR",
        context={"request_id": "req-123"},
        structured_context=ErrorContext(
            module="test_module",
            user_id="user-456",
            task_id="task-789",
            request_id="req-123",
            env="test",
        ),
    )


@pytest.fixture
def sample_dlq_item(sample_error_event):
    """Sample DeadLetterQueueItem for testing।"""
    return DeadLetterQueueItem(
        event_type="TestError",
        handler_name="test_handler",
        error="Handler failed",
        timestamp=datetime.now(UTC),
        retry_count=0,
        original_event=sample_error_event,
    )


# -------------------- Tests: ErrorContext --------------------


class TestErrorContext:
    """বাংলা মন্তব্য: ErrorContext model টেস্ট।"""

    def test_create_with_all_fields(self):
        ctx = ErrorContext(
            module="api",
            user_id="user-123",
            task_id="task-456",
            request_id="req-789",
            env="production",
            extra={"key": "value"},
        )
        assert ctx.module == "api"
        assert ctx.user_id == "user-123"
        assert ctx.task_id == "task-456"
        assert ctx.request_id == "req-789"
        assert ctx.env == "production"
        assert ctx.extra == {"key": "value"}

    def test_create_with_defaults(self):
        ctx = ErrorContext(module="test")
        assert ctx.module == "test"
        assert ctx.user_id is None
        assert ctx.task_id is None
        assert ctx.request_id is None
        assert ctx.env == "unknown"
        assert ctx.extra == {}


# -------------------- Tests: ErrorEvent --------------------


class TestErrorEvent:
    """বাংলা মন্তব্য: ErrorEvent model টেস্ট।"""

    def test_create_error_event(self, sample_error_event):
        assert sample_error_event.module == "test_module"
        assert sample_error_event.error_type == "TestError"
        assert sample_error_event.message == "This is a test error message"
        assert sample_error_event.severity == "ERROR"
        assert sample_error_event.context == {"request_id": "req-123"}
        assert sample_error_event.structured_context is not None
        assert sample_error_event.structured_context.user_id == "user-456"

    def test_timestamp_auto_generated(self):
        event = ErrorEvent(
            module="test",
            error_type="TestError",
            message="Test",
            severity="INFO",
        )
        assert isinstance(event.timestamp, datetime)
        assert event.timestamp.tzinfo is not None  # Should be UTC


# -------------------- Tests: DeadLetterQueueItem --------------------


class TestDeadLetterQueueItem:
    """বাংলা মন্তব্য: DeadLetterQueueItem model টেস্ট।"""

    def test_create_dlq_item(self, sample_dlq_item, sample_error_event):
        assert sample_dlq_item.event_type == "TestError"
        assert sample_dlq_item.handler_name == "test_handler"
        assert sample_dlq_item.error == "Handler failed"
        assert sample_dlq_item.retry_count == 0
        assert sample_dlq_item.original_event is sample_error_event

    def test_default_retry_count(self, sample_error_event):
        dlq_item = DeadLetterQueueItem(
            event_type="TestError",
            handler_name="handler",
            error="Error",
            timestamp=datetime.now(UTC),
            original_event=sample_error_event,
        )
        assert dlq_item.retry_count == 0


# -------------------- Tests: ErrorEventBus Init --------------------


class TestErrorEventBusInit:
    """বাংলা মন্তব্য: ErrorEventBus initialization টেস্ট।"""

    def test_initialization(self, event_bus):
        assert event_bus._listeners == []
        assert event_bus._dead_letter_handlers == []
        assert event_bus._total_emitted == 0
        assert event_bus._total_dlq_items == 0
        assert event_bus.dead_letter_queue_size == 0

    def test_dlq_max_size(self, event_bus):
        """বাংলা মন্তব্য: DLQ bounded with maxsize=1000।"""
        assert event_bus._dlq.maxsize == 1000


# -------------------- Tests: register_listener --------------------


class TestRegisterListener:
    """বাংলা মন্তব্য: register_listener() method টেস্ট।"""

    def test_register_single_listener(self, event_bus):
        def listener(event):
            pass

        event_bus.register_listener(listener)
        assert len(event_bus._listeners) == 1
        assert event_bus._listeners[0] is listener

    def test_register_multiple_listeners(self, event_bus):
        def listener1(event):
            pass

        def listener2(event):
            pass

        event_bus.register_listener(listener1)
        event_bus.register_listener(listener2)
        assert len(event_bus._listeners) == 2

    def test_register_async_listener(self, event_bus):
        async def async_listener(event):
            pass

        event_bus.register_listener(async_listener)
        assert len(event_bus._listeners) == 1


# -------------------- Tests: register_dead_letter_handler --------------------


class TestRegisterDeadLetterHandler:
    """বাংলা মন্তব্য: register_dead_letter_handler() method টেস্ট।"""

    def test_register_single_handler(self, event_bus):
        def handler(item):
            pass

        event_bus.register_dead_letter_handler(handler)
        assert len(event_bus._dead_letter_handlers) == 1

    def test_register_multiple_handlers(self, event_bus):
        def handler1(item):
            pass

        def handler2(item):
            pass

        event_bus.register_dead_letter_handler(handler1)
        event_bus.register_dead_letter_handler(handler2)
        assert len(event_bus._dead_letter_handlers) == 2


# -------------------- Tests: emit --------------------


class TestEmit:
    """বাংলা মন্তব্য: emit() synchronous method টেস্ট।"""

    def test_emit_increments_counter(self, event_bus, sample_error_event):
        event_bus.emit(sample_error_event)
        assert event_bus._total_emitted == 1

    def test_emit_without_listeners(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Listeners না থাকলেও emit works।"""
        with patch("core.messaging.event_bus.logger") as mock_logger:
            event_bus.emit(sample_error_event)
            # Should log the event
            mock_logger.error.assert_called_once()

    @pytest.mark.asyncio
    async def test_emit_with_successful_listener(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Successful listener-এ event dispatch হয়।"""
        received_events = []

        def listener(event):
            received_events.append(event)

        event_bus.register_listener(listener)
        await event_bus.emit_async(sample_error_event)

        assert len(received_events) == 1
        assert received_events[0] is sample_error_event

    @pytest.mark.asyncio
    async def test_emit_with_failing_listener_adds_to_dlq(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Failing listener-এ DLQ-তে item add হয়।"""

        def failing_listener(event):
            raise RuntimeError("Listener failed")

        event_bus.register_listener(failing_listener)
        await event_bus.emit_async(sample_error_event)

        assert event_bus._total_dlq_items == 1
        assert event_bus.dead_letter_queue_size == 1

    def test_emit_no_running_loop(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Running loop না থাকলে sync log works।"""
        with patch("core.messaging.event_bus.logger") as mock_logger:
            event_bus.emit(sample_error_event)
            # Should complete without error
            assert event_bus._total_emitted == 1
            mock_logger.debug.assert_called_once()


# -------------------- Tests: emit_async --------------------


class TestEmitAsync:
    """বাংলা মন্তব্য: emit_async() async method টেস্ট।"""

    @pytest.mark.asyncio
    async def test_emit_async_increments_counter(self, event_bus, sample_error_event):
        await event_bus.emit_async(sample_error_event)
        assert event_bus._total_emitted == 1

    @pytest.mark.asyncio
    async def test_emit_async_dispatches_to_listeners(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: emit_async listeners-এ dispatch করে।"""
        received_events = []

        async def async_listener(event):
            received_events.append(event)

        event_bus.register_listener(async_listener)
        await event_bus.emit_async(sample_error_event)

        assert len(received_events) == 1
        assert received_events[0] is sample_error_event

    @pytest.mark.asyncio
    async def test_emit_async_with_multiple_listeners(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Multiple listeners-এ concurrently dispatch হয়।"""
        received = []

        async def listener1(event):
            received.append("listener1")

        async def listener2(event):
            received.append("listener2")

        event_bus.register_listener(listener1)
        event_bus.register_listener(listener2)
        await event_bus.emit_async(sample_error_event)

        assert len(received) == 2


# -------------------- Tests: _safe_invoke --------------------


class TestSafeInvoke:
    """বাংলা মন্তব্য: _safe_invoke() method টেস্ট।"""

    @pytest.mark.asyncio
    async def test_safe_invoke_sync_listener_success(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Sync listener successfully invoke হয়।"""

        def sync_listener(event):
            return "success"

        result = await event_bus._safe_invoke(sync_listener, sample_error_event)
        assert result == "success"

    @pytest.mark.asyncio
    async def test_safe_invoke_async_listener_success(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Async listener successfully invoke হয়।"""

        async def async_listener(event):
            return "async_success"

        result = await event_bus._safe_invoke(async_listener, sample_error_event)
        assert result == "async_success"

    @pytest.mark.asyncio
    async def test_safe_invoke_returns_exception(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Exception return হয়, suppress হয় না।"""

        def failing_listener(event):
            raise RuntimeError("Listener error")

        result = await event_bus._safe_invoke(failing_listener, sample_error_event)
        assert isinstance(result, RuntimeError)

    @pytest.mark.asyncio
    async def test_safe_invoke_cancelled_error_raised(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: CancelledError re-raise হয়।"""

        async def cancelled_listener(event):
            raise asyncio.CancelledError()

        with pytest.raises(asyncio.CancelledError):
            await event_bus._safe_invoke(cancelled_listener, sample_error_event)


# -------------------- Tests: _dispatch_async --------------------


class TestDispatchAsync:
    """বাংলা মন্তব্য: _dispatch_async() method টেস্ট।"""

    @pytest.mark.asyncio
    async def test_dispatch_with_no_listeners(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Listeners না থাকলে early return হয়।"""
        await event_bus._dispatch_async(sample_error_event)
        # Should complete without error

    @pytest.mark.asyncio
    async def test_dispatch_with_successful_listeners(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: সব listeners successfully invoke হয়।"""
        results = []

        async def listener1(event):
            results.append(1)

        async def listener2(event):
            results.append(2)

        event_bus.register_listener(listener1)
        event_bus.register_listener(listener2)
        await event_bus._dispatch_async(sample_error_event)

        assert len(results) == 2

    @pytest.mark.asyncio
    async def test_dispatch_with_failing_listener_adds_to_dlq(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: Failing listener-এ DLQ-তে item add হয়।"""

        def failing_listener(event):
            raise RuntimeError("Handler failed")

        event_bus.register_listener(failing_listener)
        await event_bus._dispatch_async(sample_error_event)

        assert event_bus._total_dlq_items == 1
        assert event_bus.dead_letter_queue_size == 1

    @pytest.mark.asyncio
    async def test_dispatch_calls_dead_letter_handlers(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: DLQ item add হলে dead letter handlers call হয়।"""
        handler_calls = []

        def failing_listener(event):
            raise RuntimeError("Failed")

        def dlq_handler(item):
            handler_calls.append(item)

        event_bus.register_listener(failing_listener)
        event_bus.register_dead_letter_handler(dlq_handler)
        await event_bus._dispatch_async(sample_error_event)

        assert len(handler_calls) == 1
        assert handler_calls[0].handler_name == "failing_listener"

    @pytest.mark.asyncio
    async def test_dispatch_dlq_full_logs_critical(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: DLQ full হলে critical log হয়।"""

        def failing_listener(event):
            raise RuntimeError("Failed")

        event_bus.register_listener(failing_listener)

        # Fill the DLQ
        for _ in range(1000):
            event_bus._dlq.put_nowait(
                DeadLetterQueueItem(
                    event_type="dummy",
                    handler_name="dummy",
                    error="dummy",
                    timestamp=datetime.now(UTC),
                )
            )

        with patch("core.messaging.event_bus.logger") as mock_logger:
            await event_bus._dispatch_async(sample_error_event)
            # Should log critical error
            mock_logger.critical.assert_called_once()

    @pytest.mark.asyncio
    async def test_dispatch_isolates_listener_failures(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: একটি listener failure অন্য listeners-কে affect করে না।"""
        results = []

        def failing_listener(event):
            raise RuntimeError("Failed")

        async def successful_listener(event):
            results.append("success")

        event_bus.register_listener(failing_listener)
        event_bus.register_listener(successful_listener)
        await event_bus._dispatch_async(sample_error_event)

        # Successful listener should still have been called
        assert len(results) == 1
        assert event_bus._total_dlq_items == 1


# -------------------- Tests: _log_event --------------------


class TestLogEvent:
    """বাংলা মন্তব্য: _log_event() method টেস্ট।"""

    def test_log_critical_severity(self, event_bus):
        event = ErrorEvent(
            module="critical_module",
            error_type="CriticalError",
            message="Critical failure",
            severity="CRITICAL",
        )
        with patch("core.messaging.event_bus.logger") as mock_logger:
            event_bus._log_event(event)
            mock_logger.critical.assert_called_once()

    def test_log_error_severity(self, event_bus):
        event = ErrorEvent(
            module="error_module",
            error_type="Error",
            message="Error occurred",
            severity="ERROR",
        )
        with patch("core.messaging.event_bus.logger") as mock_logger:
            event_bus._log_event(event)
            mock_logger.error.assert_called_once()

    def test_log_warning_severity(self, event_bus):
        event = ErrorEvent(
            module="warn_module",
            error_type="Warning",
            message="Warning message",
            severity="WARNING",
        )
        with patch("core.messaging.event_bus.logger") as mock_logger:
            event_bus._log_event(event)
            mock_logger.warning.assert_called_once()

    def test_log_info_severity(self, event_bus):
        event = ErrorEvent(
            module="info_module",
            error_type="Info",
            message="Info message",
            severity="INFO",
        )
        with patch("core.messaging.event_bus.logger") as mock_logger:
            event_bus._log_event(event)
            mock_logger.info.assert_called_once()

    def test_log_with_structured_context(self, event_bus):
        event = ErrorEvent(
            module="test",
            error_type="TestError",
            message="Test",
            severity="ERROR",
            structured_context=ErrorContext(
                module="test",
                user_id="user-123",
                task_id="task-456",
                request_id="req-789",
                env="production",
            ),
        )
        with patch("core.messaging.event_bus.logger") as mock_logger:
            event_bus._log_event(event)
            log_msg = mock_logger.error.call_args[0][0]
            assert "user-123" in log_msg
            assert "task-456" in log_msg
            assert "req-789" in log_msg


# -------------------- Tests: process_dead_letter_queue --------------------


class TestProcessDeadLetterQueue:
    """বাংলা মন্তব্য: process_dead_letter_queue() method টেস্ট।"""

    @pytest.mark.asyncio
    async def test_process_empty_dlq(self, event_bus):
        """বাংলা মন্তব্য: Empty DLQ-এ return হয় empty list।"""
        result = await event_bus.process_dead_letter_queue()
        assert result == []

    @pytest.mark.asyncio
    async def test_process_dlq_items(self, event_bus, sample_dlq_item):
        """বাংলা মন্তব্য: DLQ থেকে items process হয়।"""
        event_bus._dlq.put_nowait(sample_dlq_item)

        result = await event_bus.process_dead_letter_queue()

        assert len(result) == 1
        assert result[0] is sample_dlq_item
        assert result[0].retry_count == 1

    @pytest.mark.asyncio
    async def test_process_dlq_respects_max_items(self, event_bus):
        """বাংলা মন্তব্য: max_items respect করে।"""
        # Add 5 items
        for i in range(5):
            item = DeadLetterQueueItem(
                event_type=f"Error{i}",
                handler_name="handler",
                error="error",
                timestamp=datetime.now(UTC),
            )
            event_bus._dlq.put_nowait(item)

        result = await event_bus.process_dead_letter_queue(max_items=3)
        assert len(result) == 3

    @pytest.mark.asyncio
    async def test_process_dlq_increments_retry_count(self, event_bus):
        """বাংলা মন্তব্য: Process করার সময় retry_count increment হয়।"""
        item = DeadLetterQueueItem(
            event_type="TestError",
            handler_name="handler",
            error="error",
            timestamp=datetime.now(UTC),
            retry_count=2,
        )
        event_bus._dlq.put_nowait(item)

        result = await event_bus.process_dead_letter_queue()

        assert result[0].retry_count == 3


# -------------------- Tests: Properties --------------------


class TestProperties:
    """বাংলা মন্তব্য: EventBus properties টেস্ট।"""

    def test_dead_letter_queue_size(self, event_bus, sample_dlq_item):
        assert event_bus.dead_letter_queue_size == 0
        event_bus._dlq.put_nowait(sample_dlq_item)
        assert event_bus.dead_letter_queue_size == 1

    def test_stats_property(self, event_bus, sample_error_event):
        """বাংলা মন্তব্য: stats property correct information return করে।"""
        event_bus.emit(sample_error_event)
        event_bus._total_dlq_items = 1  # Manually set for testing

        # Add a DLQ item
        event_bus._dlq.put_nowait(
            DeadLetterQueueItem(
                event_type="TestError",
                handler_name="handler",
                error="error",
                timestamp=datetime.now(UTC),
            )
        )

        stats = event_bus.stats
        assert stats["total_emitted"] == 1
        assert stats["total_dlq_items"] == 1
        assert stats["dlq_current_size"] == 1
        assert stats["registered_listeners"] == 0


# -------------------- Tests: Global Instance --------------------


class TestGlobalInstance:
    """বাংলা মন্তব্য: Global error_event_bus instance টেস্ট।"""

    def test_global_instance_exists(self):
        """বাংলা মন্তব্য: Global instance create করা আছে।"""
        assert isinstance(error_event_bus, ErrorEventBus)

    def test_global_instance_is_singleton(self):
        """বাংলা মন্তব্য: Global instance singleton pattern follow করে।"""
        from core.messaging.event_bus import error_event_bus as instance1
        from core.messaging.event_bus import error_event_bus as instance2

        assert instance1 is instance2
