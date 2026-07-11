# 📄 ফাইল: backend/tests/core/test_swarm_pubsub.py

**প্রকার:** .py  
**সাইজ:** 12,586 বাইট  
**আপডেট:** 2026-07-11T18:21:34.960188

---

## কোড

```py
# backend/tests/core/test_swarm_pubsub.py
# বাংলা মন্তব্য: SwarmPubSub-এর জন্য comprehensive unit tests।
# Redis mock করা হয়েছে — actual Redis dependency ছাড়াই।

import asyncio
import json
from unittest.mock import AsyncMock
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from core.swarm_pubsub import SwarmPubSub


# -------------------- Fixtures --------------------


@pytest.fixture
def swarm_pubsub():
    """SwarmPubSub ইনস্ট্যান্স ফেরত দেয়।"""
    with patch("redis.asyncio.from_url", return_value=AsyncMock()):
        pubsub = SwarmPubSub()
        return pubsub


@pytest.fixture
def mock_pubsub():
    """Mock Redis pubsub instance।"""
    pubsub = AsyncMock()
    pubsub.subscribe = AsyncMock()
    pubsub.unsubscribe = AsyncMock()
    pubsub.close = AsyncMock()
    pubsub.get_message = AsyncMock()
    return pubsub


@pytest.fixture
def mock_redis(mock_pubsub):
    """Mock Redis client।"""
    redis = AsyncMock()
    redis.pubsub = MagicMock(return_value=mock_pubsub)
    redis.publish = AsyncMock()
    return redis


# -------------------- Tests: __init__ --------------------


class TestSwarmPubSubInit:
    """বাংলা মন্তব্য: Initialization টেস্ট।"""

    def test_creates_redis_connection(self):
        """বাংলা মন্তব্য: Redis connection create হয়।"""
        with patch("redis.asyncio.from_url") as mock_from_url:
            mock_redis = AsyncMock()
            mock_from_url.return_value = mock_redis

            pubsub = SwarmPubSub()

            mock_from_url.assert_called_once_with("redis://localhost")
            assert pubsub.redis is mock_redis


# -------------------- Tests: subscribe --------------------


class TestSubscribe:
    """বাংলা মন্তব্য: subscribe() async generator method টেস্ট।"""

    @pytest.mark.asyncio
    async def test_subscribe_creates_pubsub(self, swarm_pubsub, mock_pubsub, mock_redis):
        """বাংলা মন্তব্য: subscribe() call করলে pubsub create হয়।"""
        swarm_pubsub.redis = mock_redis
        mock_redis.pubsub.return_value = mock_pubsub

        # Start the generator
        gen = swarm_pubsub.subscribe()
        await gen.__anext__()

        mock_redis.pubsub.assert_called_once()
        mock_pubsub.subscribe.assert_called_once_with("swarm_stream")

    @pytest.mark.asyncio
    async def test_subscribe_yields_messages(self, swarm_pubsub, mock_pubsub, mock_redis):
        """বাংলা মন্তব্য: subscribe() messages yield করে।"""
        swarm_pubsub.redis = mock_redis
        mock_redis.pubsub.return_value = mock_pubsub

        # Mock message data
        test_messages = [
            {"data": b"message1"},
            {"data": b"message2"},
            {"data": b"message3"},
        ]

        message_index = 0

        async def mock_get_message(**kwargs):
            nonlocal message_index
            if message_index < len(test_messages):
                msg = test_messages[message_index]
                message_index += 1
                return msg
            return None

        mock_pubsub.get_message = mock_get_message

        gen = swarm_pubsub.subscribe()
        received = []

        # Collect messages
        for _ in range(3):
            try:
                msg = await asyncio.wait_for(gen.__anext__(), timeout=0.5)
                received.append(msg)
            except TimeoutError:
                break

        assert len(received) == 3
        assert received[0] == "message1"
        assert received[1] == "message2"
        assert received[2] == "message3"

    @pytest.mark.asyncio
    async def test_subscribe_handles_cancelled_error(self, swarm_pubsub, mock_pubsub, mock_redis):
        """বাংলা মন্তব্য: CancelledError handle করে cleanup করে।"""
        swarm_pubsub.redis = mock_redis
        mock_redis.pubsub.return_value = mock_pubsub

        gen = swarm_pubsub.subscribe()

        # Simulate cancellation
        with pytest.raises(asyncio.CancelledError):
            mock_pubsub.get_message = AsyncMock(side_effect=asyncio.CancelledError())
            await gen.__anext__()

        # Verify cleanup
        mock_pubsub.unsubscribe.assert_called_once_with("swarm_stream")
        mock_pubsub.close.assert_called_once()

    @pytest.mark.asyncio
    async def test_subscribe_sleeps_between_messages(self, swarm_pubsub, mock_pubsub, mock_redis):
        """বাংলা মন্তব্য: Message polling-এর between-এ sleep হয়।"""
        swarm_pubsub.redis = mock_redis
        mock_redis.pubsub.return_value = mock_pubsub

        call_count = 0

        async def mock_get_message(**kwargs):
            nonlocal call_count
            call_count += 1
            # First call: no message (returns None, triggers sleep)
            if call_count == 1:
                return None
            # Second call: return a message
            if call_count == 2:
                return {"data": b"test"}
            # Third call: stop the loop
            raise asyncio.CancelledError()

        mock_pubsub.get_message = mock_get_message

        with patch("asyncio.sleep", new_callable=AsyncMock) as mock_sleep:
            gen = swarm_pubsub.subscribe()
            try:
                msg = await gen.__anext__()
                assert msg == "test"
            except asyncio.CancelledError:
                pass

            # Sleep should have been called at least once (after first None)
            assert mock_sleep.call_count >= 1
            mock_sleep.assert_any_call(0.01)

    @pytest.mark.asyncio
    async def test_subscribe_ignores_subscribe_messages(self, swarm_pubsub, mock_pubsub, mock_redis):
        """বাংলা মন্তব্য: Subscribe confirmation messages ignore করা হয়।"""
        swarm_pubsub.redis = mock_redis
        mock_redis.pubsub.return_value = mock_pubsub

        # Mock get_message to return subscribe confirmation first, then actual message
        call_count = 0

        async def mock_get_message(**kwargs):
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                # Subscribe confirmation - Redis filters this with ignore_subscribe_messages=True
                # So we return None instead
                return None
            elif call_count == 2:
                # Actual message
                return {"data": b"real message"}
            # Stop after getting the real message
            raise asyncio.CancelledError()

        mock_pubsub.get_message = mock_get_message

        with patch("asyncio.sleep", new_callable=AsyncMock):
            gen = swarm_pubsub.subscribe()
            received = []

            try:
                # First call should get real message (subscribe messages are filtered by Redis)
                msg1 = await gen.__anext__()
                received.append(msg1)
            except asyncio.CancelledError:
                pass

            # Should only receive the real message
            assert received == ["real message"]


# -------------------- Tests: broadcast --------------------


class TestBroadcast:
    """বাংলা মন্তব্য: broadcast() method টেস্ট।"""

    @pytest.mark.asyncio
    async def test_broadcast_publishes_message(self, swarm_pubsub, mock_redis):
        """বাংলা মন্তব্য: broadcast() call করলে Redis-এ publish হয়।"""
        swarm_pubsub.redis = mock_redis

        await swarm_pubsub.broadcast("test_event", {"key": "value"})

        mock_redis.publish.assert_called_once()
        call_args = mock_redis.publish.call_args
        assert call_args.args[0] == "swarm_stream"

        # Verify message format
        published_data = json.loads(call_args.args[1])
        assert published_data["type"] == "test_event"
        assert published_data["data"] == {"key": "value"}

    @pytest.mark.asyncio
    async def test_broadcast_with_different_event_types(self, swarm_pubsub, mock_redis):
        """বাংলা মন্তব্য: Different event types correctly publish হয়।"""
        swarm_pubsub.redis = mock_redis

        event_types = ["agent_started", "task_completed", "error_occurred"]

        for event_type in event_types:
            await swarm_pubsub.broadcast(event_type, {"test": "data"})

        assert mock_redis.publish.call_count == 3

        # Verify each event type
        for i, event_type in enumerate(event_types):
            call_args = mock_redis.publish.call_args_list[i]
            published_data = json.loads(call_args.args[1])
            assert published_data["type"] == event_type

    @pytest.mark.asyncio
    async def test_broadcast_with_complex_payload(self, swarm_pubsub, mock_redis):
        """বাংলা মন্তব্য: Complex payload correctly serialize হয়।"""
        swarm_pubsub.redis = mock_redis

        complex_payload = {
            "task_id": "123",
            "agent_id": "agent-456",
            "results": [{"step": 1, "status": "done"}, {"step": 2, "status": "pending"}],
            "metadata": {"timestamp": "2024-01-01T00:00:00Z", "priority": "high"},
        }

        await swarm_pubsub.broadcast("complex_event", complex_payload)

        call_args = mock_redis.publish.call_args
        published_data = json.loads(call_args.args[1])
        assert published_data["data"] == complex_payload


# -------------------- Tests: Global Instance --------------------


class TestGlobalInstance:
    """বাংলা মন্তব্য: Global swarm_streamer instance টেস্ট।"""

    def test_global_instance_exists(self):
        """বাংলা মন্তব্য: Global instance create করা আছে।"""
        from core.swarm_pubsub import swarm_streamer

        assert isinstance(swarm_streamer, SwarmPubSub)

    def test_global_instance_has_redis_connection(self):
        """বাংলা মন্তব্য: Global instance-এ Redis connection আছে।"""
        from core.swarm_pubsub import swarm_streamer

        assert swarm_streamer.redis is not None


# -------------------- Tests: Integration --------------------


class TestSwarmPubSubIntegration:
    """বাংলা মন্তব্য: Integration-style tests for realistic scenarios।"""

    @pytest.mark.asyncio
    async def test_pub_sub_workflow(self, mock_pubsub, mock_redis):
        """বাংলা মন্তব্য: Publish এবং subscribe এর সম্পূর্ণ workflow।"""
        pubsub = SwarmPubSub()
        pubsub.redis = mock_redis
        mock_redis.pubsub.return_value = mock_pubsub

        # Broadcast a message
        test_payload = {"event": "task_complete", "task_id": "task-123"}
        await pubsub.broadcast("task_complete", test_payload)

        # Verify it was published
        mock_redis.publish.assert_called_once()
        published_msg = json.loads(mock_redis.publish.call_args.args[1])
        assert published_msg["type"] == "task_complete"
        assert published_msg["data"] == test_payload

    @pytest.mark.asyncio
    async def test_multiple_broadcasts(self, mock_redis):
        """বাংলা মন্তব্য: Multiple broadcasts sequentially correctly handle হয়।"""
        pubsub = SwarmPubSub()
        pubsub.redis = mock_redis

        # Broadcast multiple messages
        for i in range(5):
            await pubsub.broadcast(f"event_{i}", {"index": i})

        assert mock_redis.publish.call_count == 5

    @pytest.mark.asyncio
    async def test_subscribe_generator_cleanup(self, mock_pubsub, mock_redis):
        """বাংলা মন্তব্য: Subscribe generator properly cleanup হয়।"""
        pubsub = SwarmPubSub()
        pubsub.redis = mock_redis
        mock_redis.pubsub.return_value = mock_pubsub

        gen = pubsub.subscribe()

        # Cancel the generator by raising CancelledError
        mock_pubsub.get_message = AsyncMock(side_effect=asyncio.CancelledError())

        try:
            await gen.__anext__()
        except asyncio.CancelledError:
            pass

        # Verify cleanup was called
        mock_pubsub.unsubscribe.assert_called_once_with("swarm_stream")
        mock_pubsub.close.assert_called_once()

```