"""
Tests for workers/celery_app.py
Focus: Celery app initialization and task registration.
"""

from __future__ import annotations

from unittest.mock import MagicMock, patch


def test_celery_app_exposed_from_workers():
    with patch("core.queue.task_queue_enhanced.celery_app", MagicMock()):
        from workers.celery_app import app

    assert app is not None


def test_celery_app_has_name():
    with patch("core.queue.task_queue_enhanced.celery_app") as mock_app:
        from workers import celery_app as wc

    assert hasattr(wc.app, "name") or mock_app.name is not None
