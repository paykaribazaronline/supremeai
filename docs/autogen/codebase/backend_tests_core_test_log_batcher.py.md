# 📄 ফাইল: backend/tests/core/test_log_batcher.py

**প্রকার:** .py  
**সাইজ:** 7,444 বাইট  
**আপডেট:** 2026-07-11T11:05:10.227962

---

## কোড

```py
import pytest
from unittest.mock import AsyncMock, patch, MagicMock
import asyncio
from collections import deque
from core.log_batcher import LogBatcherService, batcher


@pytest.fixture
def batcher_service():
    return LogBatcherService(flush_interval=0.1, batch_size=2)


def test_log_batcher_service_init(batcher_service):
    assert batcher_service.flush_interval == 0.1
    assert batcher_service.batch_size == 2
    assert isinstance(batcher_service.queue, asyncio.Queue)
    assert isinstance(batcher_service.buffer, deque)
    assert batcher_service.running is False
    assert batcher_service.task is None
    assert batcher_service._subscribers == {}


@pytest.mark.anyio
async def test_log_batcher_service_start(batcher_service):
    batcher_service.start()
    assert batcher_service.running is True
    assert batcher_service.task is not None
    # Clean up
    await batcher_service.stop()


@pytest.mark.anyio
async def test_log_batcher_service_stop(batcher_service):
    batcher_service.start()
    await batcher_service.stop()
    assert batcher_service.running is False
    assert batcher_service.task is None


@pytest.mark.anyio
async def test_log_batcher_service_emit(batcher_service):
    log_entry = {"session_id": "123", "message": "test"}
    batcher_service.emit(log_entry)
    # The item should be in the queue
    assert not batcher_service.queue.empty()
    item = await batcher_service.queue.get()
    assert item == log_entry


@pytest.mark.anyio
async def test_log_batcher_service_subscribe(batcher_service):
    session_id = "123"
    queue = batcher_service.subscribe(session_id)
    assert session_id in batcher_service._subscribers
    assert queue in batcher_service._subscribers[session_id]
    assert isinstance(queue, asyncio.Queue)


@pytest.mark.anyio
async def test_log_batcher_service_unsubscribe(batcher_service):
    session_id = "123"
    queue = batcher_service.subscribe(session_id)
    batcher_service.unsubscribe(session_id, queue)
    assert session_id not in batcher_service._subscribers


@pytest.mark.anyio
async def test_log_batcher_service_flush(batcher_service):
    # Mock the database session and execution
    mock_session = AsyncMock()
    mock_session.execute.return_value = None
    mock_session.commit.return_value = None

    async def mock_get_db_session():
        yield mock_session

    with patch("core.log_batcher.get_db_session", return_value=mock_get_db_session()):
        # Add some items to the buffer
        batcher_service.buffer.append({"session_id": "123", "message": "test1"})
        batcher_service.buffer.append({"session_id": "123", "message": "test2"})

        await batcher_service._flush()

        # Check that the buffer is cleared
        assert len(batcher_service.buffer) == 0
        # Check that the session was used
        mock_session.execute.assert_called_once()
        mock_session.commit.assert_called_once()


@pytest.mark.anyio
async def test_log_batcher_service_run(batcher_service):
    # We'll test the _run method by mocking the queue and flush interval
    batcher_service.running = True

    # Mock asyncio.wait_for to simulate timeout and then stop
    call_count = 0

    async def mock_wait_for(coro, timeout):
        nonlocal call_count
        call_count += 1
        if call_count >= 2:
            # After two timeouts, we stop the service to break the loop
            batcher_service.running = False
        # On first call, we return a dummy item to add to buffer
        if call_count == 1:
            return {"session_id": "123", "message": "test"}
        else:
            raise TimeoutError()

    with patch("asyncio.wait_for", side_effect=mock_wait_for):
        with patch.object(batcher_service, "_flush", new_callable=AsyncMock) as mock_flush:
            await batcher_service._run()
            # We expect _flush to be called at least once (from the timeout after the first item)
            assert mock_flush.await_count >= 1


# Startup when already running
@pytest.mark.anyio
async def test_log_batcher_service_start_idempotent(batcher_service):
    batcher_service.start()
    first_task = batcher_service.task
    batcher_service.start()
    assert batcher_service.task is first_task
    await batcher_service.stop()


@pytest.mark.anyio
async def test_log_batcher_service_stop_without_task(batcher_service):
    await batcher_service.stop()
    assert batcher_service.running is False
    assert batcher_service.task is None


@pytest.mark.anyio
async def test_log_batcher_service_emit_publishes_to_subscribers():
    batcher_service = LogBatcherService(flush_interval=0.1, batch_size=2)
    session_id = "123"
    queue = batcher_service.subscribe(session_id)
    log_entry = {"session_id": session_id, "message": "test"}
    batcher_service.emit(log_entry)
    await asyncio.sleep(0)  # Allow the background task to publish
    assert not queue.empty()
    item = await queue.get()
    assert item == log_entry
    batcher_service.unsubscribe(session_id, queue)


@pytest.mark.anyio
async def test_log_batcher_service_subscribe_new_session():
    batcher_service = LogBatcherService()
    session_id = "new"
    queue = batcher_service.subscribe(session_id)
    assert session_id in batcher_service._subscribers
    assert queue in batcher_service._subscribers[session_id]


@pytest.mark.anyio
async def test_log_batcher_service_unsubscribe_last_queue():
    batcher_service = LogBatcherService()
    session_id = "only"
    queue = batcher_service.subscribe(session_id)
    batcher_service.unsubscribe(session_id, queue)
    assert session_id not in batcher_service._subscribers


@pytest.mark.anyio
async def test_log_batcher_service_flush_empty_buffer(batcher_service):
    assert len(batcher_service.buffer) == 0
    await batcher_service._flush()
    assert len(batcher_service.buffer) == 0


@pytest.mark.anyio
async def test_log_batcher_service_run_flush_on_exception(batcher_service):
    batcher_service.running = True
    call_count = 0

    async def mock_wait_for(coro, timeout):
        nonlocal call_count
        call_count += 1
        if call_count >= 2:
            batcher_service.running = False
        return {"session_id": "123", "message": "test"}

    with patch("asyncio.wait_for", side_effect=mock_wait_for):
        with patch.object(batcher_service, "_flush", new_callable=AsyncMock) as mock_flush:
            mock_flush.side_effect = Exception("DB error")
            await batcher_service._run()
            assert mock_flush.await_count >= 1
            # After exception, items should be re-queued
            assert not batcher_service.queue.empty()


@pytest.mark.anyio
async def test_log_batcher_service_flush_db_failure_requeue(batcher_service):
    mock_session = AsyncMock()
    mock_session.execute.side_effect = Exception("DB error")
    mock_session.commit.return_value = None

    async def mock_get_db_session():
        yield mock_session

    with patch("core.log_batcher.get_db_session", return_value=mock_get_db_session()):
        batcher_service.buffer.append({"session_id": "123", "message": "test1"})
        batcher_service.buffer.append({"session_id": "123", "message": "test2"})

        await batcher_service._flush()

        assert len(batcher_service.buffer) == 0
        assert batcher_service.queue.qsize() == 2


# Test the global batcher instance
def test_global_batcher_instance():
    assert isinstance(batcher, LogBatcherService)

```