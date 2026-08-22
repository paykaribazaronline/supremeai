"""Collection-time import guards.

এই ফাইলে আমরা এমন external deps-এর জন্য minimal sys.modules stubs তৈরি করি
যেগুলো ইনস্টল না থাকলেও test collection যেন ভেঙে না যায়।
"""

import sys
from unittest.mock import MagicMock


def _stub_module(name: str) -> None:
    if name in sys.modules:
        return
    m = MagicMock()
    m.__spec__ = None
    sys.modules[name] = m


# NATS is optional for unit tests in this repo.
_stub_module("nats")
_stub_module("nats.errors")
_stub_module("nats.js")
_stub_module("nats.js.errors")

# Provide attributes referenced by code.
sys.modules["nats"].connect = MagicMock()
sys.modules["nats.errors"].NoServersError = type("NoServersError", (Exception,), {})
sys.modules["nats.js.errors"].KeyValueError = type("KeyValueError", (Exception,), {})

# Google genai is optional; stub so agents package init doesn't fail.
_stub_module("google")
_stub_module("google.genai")
_stub_module("google.genai.types")

# Ensure `from google import genai` works
sys.modules["google"].genai = sys.modules["google.genai"]


class _DummyClient:
    def __init__(self, *args, **kwargs):
        self.models = MagicMock()


sys.modules["google.genai"].Client = _DummyClient
