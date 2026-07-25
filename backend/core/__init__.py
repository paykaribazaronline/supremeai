"""
SupremeAI 2.0 — Core Package.

বাংলা মন্তব্য: কোর প্যাকেজটি এখন লেজি ইম্পোর্ট প্যাটার্ন ব্যবহার করে।
সমস্ত সাবমডিউল প্রথম ব্যবহারের সময় ইম্পোর্ট হবে (Lazy Import)।
এতে স্টার্টআপ টাইম কমে, ইম্পোর্ট সাইকেল এড়ানো যায় এবং টেস্ট আইসোলেশন উন্নত হয়।

Backward Compatibility:
- পুরানো কোড বা টেস্ট যদি `from core import event_bus` করে, তাহলে `__getattr__`
  ডাইনামিকালি সঠিক মডিউল লোড করবে (ব্যাকগ্রাউন্ড কম্প্যাটিবল)।
- sys.modules-এ রেজিস্ট্রেশন প্রয়োজন নেই — এটি কেবল পুরানো ইম্পোর্ট প্যাটার্ন ভাঙতে পারে।
"""

import importlib
import sys
from typing import Any

# বাংলা মন্তব্য: Lazy Import Mapping — মডিউল নাম → প্যাকেজ পাথ
_LAZY_IMPORT_MAP: dict[str, str] = {
    "event_bus": "core.messaging.event_bus",
    "swarm_orchestrator": "core.orchestration.swarm_orchestrator",
    "llm_gateway": "core.llm.llm_gateway",
    "nats_messaging": "core.messaging.nats_messaging",
    "agent_orchestrator": "core.orchestration.agent_orchestrator",
    "auth_middleware": "core.security.auth_middleware",
    "free_tier_tracker": "core.llm.free_tier_tracker",
    "posthog_client": "core.observability.posthog_client",
    "telemetry": "core.observability.telemetry",
    "security_vault": "core.security.security_vault",
    "evolution": "core.evolution",
    "auto_skill_creator": "core.evolution.auto_skill_creator",
    "self_evolution_agent": "core.evolution.self_evolution_agent",
}

# বাংলা মন্তব্য: ক্যাশ — ইতিমধ্যে লোড করা মডিউল সংরক্ষণ করে
_load_cache: dict[str, Any] = {}


def __getattr__(name: str) -> Any:
    """
    বাংলা মন্তব্য: ডাইনামিক লেজি ইম্পোর্ট — যখন কোনো অ্যাট্রিবিউট প্রথমবার অ্যাক্সেস করা হয়,
    তখন এটি __getattr__ ট্রিগার করে এবং সঠিক মডিউল ইম্পোর্ট করে।
    """
    if name in _load_cache:
        return _load_cache[name]

    module_path = _LAZY_IMPORT_MAP.get(name)
    if module_path:
        try:
            module = importlib.import_module(module_path)
            _load_cache[name] = module
            return module
        except ImportError as exc:
            # বাংলা মন্তব্য: ইম্পোর্ট ব্যর্থ হলে AttributeError রেইজ করি — ডিবাগিং সহজ হয়
            raise AttributeError(
                f"Failed to lazy-import '{name}' from '{module_path}': {exc}"
            ) from exc

    # বাংলা মন্তব্য: পুরানো sys.modules রেজিস্ট্রেশন চেক — backward compatibility
    if name in sys.modules:
        _load_cache[name] = sys.modules[name]
        return sys.modules[name]

    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


def __dir__() -> list[str]:
    """বাংলা মন্তব্য: ডিরেক্টরিতে সব লেজি-লোডেবল অ্যাট্রিবিউট দেখায়।"""
    return list(_LAZY_IMPORT_MAP.keys()) + list(_load_cache.keys())


__all__ = list(_LAZY_IMPORT_MAP.keys())
