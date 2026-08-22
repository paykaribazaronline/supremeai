"""Tests for tools/cache_cleanup.py — covers all branches:
- scan_keys: success, scan_iter failure fallback, both failures
- clear_stale_cache: REDIS_URL unset, no keys, keys found and deleted
"""

import os
import sys
from unittest.mock import MagicMock, patch

import pytest

# Ensure the project root is on sys.path for coverage tracking
_PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)

import importlib.util

_cache_cleanup_path = os.path.abspath(os.path.join(_PROJECT_ROOT, "..", "tools", "cache_cleanup.py"))
_spec = importlib.util.spec_from_file_location("cache_cleanup", _cache_cleanup_path)
cache_cleanup = importlib.util.module_from_spec(_spec)
sys.modules["tools.cache_cleanup"] = cache_cleanup
_spec.loader.exec_module(cache_cleanup)


@pytest.fixture(autouse=True)
def reset_redis_url():
    """Ensure REDIS_URL is not leaking between tests."""
    old = os.environ.pop("REDIS_URL", None)
    try:
        yield
    finally:
        if old is not None:
            os.environ["REDIS_URL"] = old
        else:
            os.environ.pop("REDIS_URL", None)


# ── scan_keys ──────────────────────────────────────────────────────────


def test_scan_keys_success():
    """scan_iter returns keys normally."""
    client = MagicMock()
    client.scan_iter.return_value = ["key1", "key2", "key3"]
    result = cache_cleanup.scan_keys(client, "temp_cache:*")
    assert result == ["key1", "key2", "key3"]
    client.scan_iter.assert_called_once_with(match="temp_cache:*", count=1000)


def test_scan_keys_fallback_to_keys():
    """scan_iter raises -> falls back to client.keys."""
    client = MagicMock()
    client.scan_iter.side_effect = RuntimeError("scan broke")
    client.keys.return_value = ["fallback_key"]
    result = cache_cleanup.scan_keys(client, "temp_cache:*")
    assert result == ["fallback_key"]
    client.keys.assert_called_once_with("temp_cache:*")


def test_scan_keys_both_fail():
    """Both scan_iter and keys fail -> returns empty list."""
    client = MagicMock()
    client.scan_iter.side_effect = RuntimeError("scan broke")
    client.keys.side_effect = RuntimeError("keys broke")
    result = cache_cleanup.scan_keys(client, "temp_cache:*")
    assert result == []


# ── clear_stale_cache ──────────────────────────────────────────────────


def test_clear_stale_cache_no_redis_url():
    """REDIS_URL not set -> returns 0 immediately."""
    os.environ.pop("REDIS_URL", None)
    assert cache_cleanup.clear_stale_cache() == 0


def test_clear_stale_cache_no_keys_found():
    """REDIS_URL set but no keys match -> returns 0."""
    os.environ["REDIS_URL"] = "redis://localhost:6379/0"
    mock_client = MagicMock()
    mock_client.scan_iter.return_value = []

    with patch.object(cache_cleanup, "redis") as mock_redis:
        mock_redis.from_url.return_value = mock_client
        result = cache_cleanup.clear_stale_cache()

    assert result == 0
    mock_client.delete.assert_not_called()


def test_clear_stale_cache_deletes_keys():
    """REDIS_URL set, keys found -> deletes and returns count."""
    os.environ["REDIS_URL"] = "redis://localhost:6379/0"
    mock_client = MagicMock()
    mock_client.scan_iter.return_value = ["temp_cache:a", "temp_cache:b", "temp_cache:c"]

    with patch.object(cache_cleanup, "redis") as mock_redis:
        mock_redis.from_url.return_value = mock_client
        result = cache_cleanup.clear_stale_cache()

    assert result == 3
    mock_client.delete.assert_called_once_with("temp_cache:a", "temp_cache:b", "temp_cache:c")


def test_clear_stale_cache_scan_fallback():
    """scan_iter fails -> falls back to keys, still deletes."""
    os.environ["REDIS_URL"] = "redis://localhost:6379/0"
    mock_client = MagicMock()
    mock_client.scan_iter.side_effect = RuntimeError("scan broke")
    mock_client.keys.return_value = ["temp_cache:x", "temp_cache:y"]

    with patch.object(cache_cleanup, "redis") as mock_redis:
        mock_redis.from_url.return_value = mock_client
        result = cache_cleanup.clear_stale_cache()

    assert result == 2
    mock_client.delete.assert_called_once_with("temp_cache:x", "temp_cache:y")
