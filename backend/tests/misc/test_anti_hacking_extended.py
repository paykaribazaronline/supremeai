"""
Extended tests for middleware/anti_hacking.py
Covers _octet3 helper and AntiHackingContextMiddleware.dispatch edge cases.
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock

import pytest

from middleware.anti_hacking import AntiHackingContextMiddleware, _octet3

# ── _octet3 helper ───────────────────────────────────────────────────────────


def test_octet3_ipv4():
    assert _octet3("192.168.1.42") == "192.168.1"


def test_octet3_ipv6_returns_full():
    assert _octet3("::1") == "::1"


def test_octet3_empty_returns_empty():
    assert _octet3("") == ""


# ── Middleware dispatch ───────────────────────────────────────────────────────


@pytest.mark.anyio
async def test_dispatch_no_admin_sets_signal_only():
    middleware = AntiHackingContextMiddleware(app=MagicMock())
    request = MagicMock()
    request.headers = {
        "x-forwarded-for": "1.2.3.4",
        "cf-ipcountry": "BD",
        "user-agent": "UA",
        "x-device-fingerprint": "FP",
    }
    request.state = MagicMock()
    request.state.user = None
    call_next = AsyncMock(return_value=MagicMock())
    response = await middleware.dispatch(request, call_next)
    assert response == call_next.return_value
    assert request.state.security_signal["ip"] == "1.2.3.4"
