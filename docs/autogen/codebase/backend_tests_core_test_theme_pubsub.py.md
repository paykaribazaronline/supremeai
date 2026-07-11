# 📄 ফাইল: backend/tests/core/test_theme_pubsub.py

**প্রকার:** .py  
**সাইজ:** 13,789 বাইট  
**আপডেট:** 2026-07-11T18:21:34.957869

---

## কোড

```py
# backend/tests/core/test_theme_pubsub.py
# বাংলা মন্তব্য: ThemePubSub-এর জন্য comprehensive unit tests।
# In-memory pubsub — no external dependencies।

import asyncio
from unittest.mock import MagicMock
from unittest.mock import patch

import pytest

from core.theme_pubsub import ThemePubSub


# -------------------- Fixtures --------------------


@pytest.fixture
def theme_pubsub():
    """ThemePubSub ইনস্ট্যান্স ফেরত দেয়।"""
    return ThemePubSub()


@pytest.fixture
def mock_queue():
    """Mock asyncio.Queue।"""
    queue = MagicMock(spec=asyncio.Queue)
    queue.put_nowait = MagicMock()
    return queue


# -------------------- Tests: __init__ --------------------


class TestThemePubSubInit:
    """বাংলা মন্তব্য: Initialization টেস্ট।"""

    def test_initializes_empty_subscribers(self):
        """বাংলা মন্তব্য: Initial subscribers dict empty হয়।"""
        pubsub = ThemePubSub()
        assert pubsub._subscribers == {}


# -------------------- Tests: subscribe --------------------


class TestSubscribe:
    """বাংলা মন্তব্য: subscribe() method টেস্ট।"""

    def test_subscribe_creates_new_queue(self, theme_pubsub):
        """বাংলা মন্তব্য: নতুন user subscribe করলে নতুন queue create হয়।"""
        queue = theme_pubsub.subscribe("user1")

        assert isinstance(queue, asyncio.Queue)
        assert "user1" in theme_pubsub._subscribers
        assert len(theme_pubsub._subscribers["user1"]) == 1
        assert theme_pubsub._subscribers["user1"][0] is queue

    def test_subscribe_returns_new_queue_each_time(self, theme_pubsub):
        """বাংলা মন্তব্য: প্রতিটি subscribe call নতুন queue return করে।"""
        queue1 = theme_pubsub.subscribe("user1")
        queue2 = theme_pubsub.subscribe("user1")

        assert queue1 is not queue2
        assert len(theme_pubsub._subscribers["user1"]) == 2

    def test_subscribe_multiple_users(self, theme_pubsub):
        """বাংলা মন্তব্য: Multiple users subscribe করতে পারে independently।"""
        queue1 = theme_pubsub.subscribe("user1")
        queue2 = theme_pubsub.subscribe("user2")
        queue3 = theme_pubsub.subscribe("user3")

        assert len(theme_pubsub._subscribers) == 3
        assert "user1" in theme_pubsub._subscribers
        assert "user2" in theme_pubsub._subscribers
        assert "user3" in theme_pubsub._subscribers

    def test_subscribe_same_user_multiple_times(self, theme_pubsub):
        """বাংলা মন্তব্য: একই user multiple times subscribe করতে পারে।"""
        queues = [theme_pubsub.subscribe("user1") for _ in range(5)]

        assert len(theme_pubsub._subscribers["user1"]) == 5
        assert all(isinstance(q, asyncio.Queue) for q in queues)


# -------------------- Tests: unsubscribe --------------------


class TestUnsubscribe:
    """বাংলা মন্তব্য: unsubscribe() method টেস্ট।"""

    def test_unsubscribe_removes_queue(self, theme_pubsub):
        """বাংলা মন্তব্য: Unsubscribe করলে queue remove হয়।"""
        queue = theme_pubsub.subscribe("user1")
        theme_pubsub.unsubscribe("user1", queue)

        assert "user1" not in theme_pubsub._subscribers

    def test_unsubscribe_one_of_multiple_queues(self, theme_pubsub):
        """বাংলা মন্তব্য: Multiple queues থেকে একটি remove হয়।"""
        queue1 = theme_pubsub.subscribe("user1")
        queue2 = theme_pubsub.subscribe("user1")
        queue3 = theme_pubsub.subscribe("user1")

        theme_pubsub.unsubscribe("user1", queue2)

        assert len(theme_pubsub._subscribers["user1"]) == 2
        assert queue1 in theme_pubsub._subscribers["user1"]
        assert queue2 not in theme_pubsub._subscribers["user1"]
        assert queue3 in theme_pubsub._subscribers["user1"]

    def test_unsubscribe_last_queue_removes_user(self, theme_pubsub):
        """বাংলা মন্তব্য: Last queue unsubscribe হলে user entry remove হয়।"""
        queue = theme_pubsub.subscribe("user1")
        theme_pubsub.unsubscribe("user1", queue)

        assert "user1" not in theme_pubsub._subscribers
        assert len(theme_pubsub._subscribers) == 0

    def test_unsubscribe_nonexistent_user(self, theme_pubsub):
        """বাংলা মন্তব্য: Nonexistent user unsubscribe করলে error নেই।"""
        queue = asyncio.Queue()
        # Should not raise any error
        theme_pubsub.unsubscribe("nonexistent", queue)

    def test_unsubscribe_wrong_queue(self, theme_pubsub):
        """বাংলা মন্তব্য: ভুল queue unsubscribe করলে error নেই।"""
        queue1 = theme_pubsub.subscribe("user1")
        queue2 = asyncio.Queue()

        # Should not raise any error
        theme_pubsub.unsubscribe("user1", queue2)

        # Original queue should still be there
        assert len(theme_pubsub._subscribers["user1"]) == 1
        assert queue1 in theme_pubsub._subscribers["user1"]

    def test_unsubscribe_handles_value_error(self, theme_pubsub):
        """বাংলা মন্তব্য: Queue list-এ না থাকলেও ValueError suppress হয়।"""
        queue = theme_pubsub.subscribe("user1")
        theme_pubsub.unsubscribe("user1", queue)

        # Unsubscribe again - should not raise ValueError
        theme_pubsub.unsubscribe("user1", queue)

        assert "user1" not in theme_pubsub._subscribers


# -------------------- Tests: publish --------------------


class TestPublish:
    """বাংলা মন্তব্য: publish() method টেস্ট।"""

    def test_publish_to_single_subscriber(self, theme_pubsub):
        """বাংলা মন্তব্য: Single subscriber-কে message publish হয়।"""
        queue = theme_pubsub.subscribe("user1")

        with patch.object(queue, "put_nowait") as mock_put:
            theme_pubsub.publish("user1", "dark_mode")

            mock_put.assert_called_once()
            call_args = mock_put.call_args[0][0]
            assert call_args["event"] == "theme_changed"
            assert call_args["theme"] == "dark_mode"

    def test_publish_to_multiple_subscribers(self, theme_pubsub):
        """বাংলা মন্তব্য: Multiple subscribers-কে সবাইকে message publish হয়।"""
        queue1 = theme_pubsub.subscribe("user1")
        queue2 = theme_pubsub.subscribe("user1")
        queue3 = theme_pubsub.subscribe("user1")

        with patch.object(queue1, "put_nowait") as mock_put1:
            with patch.object(queue2, "put_nowait") as mock_put2:
                with patch.object(queue3, "put_nowait") as mock_put3:
                    theme_pubsub.publish("user1", "light_mode")

                    mock_put1.assert_called_once()
                    mock_put2.assert_called_once()
                    mock_put3.assert_called_once()

                    # Verify all got the same message
                    for mock_put in [mock_put1, mock_put2, mock_put3]:
                        call_args = mock_put.call_args[0][0]
                        assert call_args["event"] == "theme_changed"
                        assert call_args["theme"] == "light_mode"

    def test_publish_only_to_specific_user(self, theme_pubsub):
        """বাংলা মন্তব্য: শুধু নির্দিষ্ট user-এর subscribers-কে publish হয়।"""
        queue_user1 = theme_pubsub.subscribe("user1")
        queue_user2 = theme_pubsub.subscribe("user2")

        with patch.object(queue_user1, "put_nowait") as mock_put1:
            with patch.object(queue_user2, "put_nowait") as mock_put2:
                theme_pubsub.publish("user1", "dark_mode")

                mock_put1.assert_called_once()
                mock_put2.assert_not_called()

    def test_publish_to_nonexistent_user(self, theme_pubsub):
        """বাংলা মন্তব্য: Nonexistent user publish করলে error নেই।"""
        # Should not raise any error
        theme_pubsub.publish("nonexistent", "dark_mode")

    def test_publish_logs_correct_message(self, theme_pubsub):
        """বাংলা মন্তব্য: Publish করার সময় সঠিক log message হয়।"""
        queue = theme_pubsub.subscribe("user1")

        with patch.object(queue, "put_nowait"):
            with patch("core.theme_pubsub.logger") as mock_logger:
                theme_pubsub.publish("user1", "dark_mode")

                mock_logger.info.assert_called_once()
                log_msg = mock_logger.info.call_args[0][0]
                assert "Publishing theme update 'dark_mode' for user 'user1'" in log_msg
                assert "1 clients" in log_msg

    def test_publish_logs_correct_client_count(self, theme_pubsub):
        """বাংলা মন্তব্য: Client count correctly log হয়।"""
        for _ in range(3):
            theme_pubsub.subscribe("user1")

        with patch("core.theme_pubsub.logger") as mock_logger:
            theme_pubsub.publish("user1", "dark_mode")

            log_msg = mock_logger.info.call_args[0][0]
            assert "3 clients" in log_msg

    def test_publish_message_format(self, theme_pubsub):
        """বাংলা মন্তব্য: Published message-এর সঠিক format আছে।"""
        queue = theme_pubsub.subscribe("user1")

        with patch.object(queue, "put_nowait") as mock_put:
            theme_pubsub.publish("user1", "dark_mode")

            message = mock_put.call_args[0][0]
            assert message == {"event": "theme_changed", "theme": "dark_mode"}


# -------------------- Tests: Global Instance --------------------


class TestGlobalInstance:
    """বাংলা মন্তব্য: Global theme_pubsub instance টেস্ট।"""

    def test_global_instance_exists(self):
        """বাংলা মন্তব্য: Global instance create করা আছে।"""
        from core.theme_pubsub import theme_pubsub

        assert isinstance(theme_pubsub, ThemePubSub)

    def test_global_instance_is_singleton(self):
        """বাংলা মন্তব্য: Global instance singleton pattern follow করে।"""
        from core.theme_pubsub import theme_pubsub as instance1
        from core.theme_pubsub import theme_pubsub as instance2

        assert instance1 is instance2


# -------------------- Tests: Integration --------------------


class TestThemePubSubIntegration:
    """বাংলা মন্তব্য: Integration-style tests for realistic scenarios।"""

    def test_full_subscribe_publish_workflow(self):
        """বাংলা মন্তব্য: Subscribe এবং publish এর সম্পূর্ণ workflow।"""
        pubsub = ThemePubSub()

        # Subscribe multiple users
        queue1 = pubsub.subscribe("user1")
        queue2 = pubsub.subscribe("user2")

        # Publish to user1
        pubsub.publish("user1", "dark_mode")

        # Verify queue1 received the message
        assert not queue1.empty()
        message = queue1.get_nowait()
        assert message["event"] == "theme_changed"
        assert message["theme"] == "dark_mode"

        # Verify queue2 did not receive anything
        assert queue2.empty()

    def test_multiple_theme_changes(self):
        """বাংলা মন্তব্য: Multiple theme changes correctly broadcast হয়।"""
        pubsub = ThemePubSub()
        queue = pubsub.subscribe("user1")

        themes = ["light", "dark", "system", "high_contrast"]

        for theme in themes:
            pubsub.publish("user1", theme)

        # Verify all messages were queued
        received = []
        while not queue.empty():
            received.append(queue.get_nowait())

        assert len(received) == 4
        for i, theme in enumerate(themes):
            assert received[i]["theme"] == theme

    def test_user_disconnect_cleanup(self):
        """বাংলা মন্তব্য: User unsubscribe হলে properly cleanup হয়।"""
        pubsub = ThemePubSub()

        queue1 = pubsub.subscribe("user1")
        queue2 = pubsub.subscribe("user1")
        queue3 = pubsub.subscribe("user1")

        # User disconnects with queue2
        pubsub.unsubscribe("user1", queue2)

        # Publish should only go to remaining queues
        with patch.object(queue1, "put_nowait") as mock_put1:
            with patch.object(queue3, "put_nowait") as mock_put3:
                pubsub.publish("user1", "dark_mode")

                mock_put1.assert_called_once()
                mock_put3.assert_called_once()

    def test_concurrent_subscribers(self):
        """বাংলা মন্তব্য: Concurrent subscribers correctly handle হয়।"""
        pubsub = ThemePubSub()

        # Simulate multiple concurrent subscriptions
        queues = [pubsub.subscribe(f"user{i}") for i in range(10)]

        assert len(pubsub._subscribers) == 10

        # Publish to all users
        for i in range(10):
            pubsub.publish(f"user{i}", f"theme_{i}")

        # Verify each queue got its message
        for i, queue in enumerate(queues):
            assert not queue.empty()
            message = queue.get_nowait()
            assert message["theme"] == f"theme_{i}"

```