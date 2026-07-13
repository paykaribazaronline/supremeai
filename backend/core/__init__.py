# বাংলা মন্তব্য: টেস্ট ও মকিং সহজ করার জন্য সাবমডিউলগুলোকে কোর নেমস্পেসে এক্সপোজ এবং sys.modules এ রেজিস্টার করা হলো
import sys
from core.messaging import event_bus
from core.orchestration import swarm_orchestrator
from core.llm import llm_gateway
from core.messaging import nats_messaging
from core.observability import log_batcher
from core.orchestration import agent_orchestrator
from core.security import auth_middleware
from core.llm import free_tier_tracker
from core.observability import posthog_client
from core.observability import telemetry
from core.security import security_vault

sys.modules["core.event_bus"] = event_bus
sys.modules["core.swarm_orchestrator"] = swarm_orchestrator
sys.modules["core.llm_gateway"] = llm_gateway
sys.modules["core.nats_messaging"] = nats_messaging
sys.modules["core.log_batcher"] = log_batcher
sys.modules["core.agent_orchestrator"] = agent_orchestrator
sys.modules["core.auth_middleware"] = auth_middleware
sys.modules["core.free_tier_tracker"] = free_tier_tracker
sys.modules["core.posthog_client"] = posthog_client
sys.modules["core.telemetry"] = telemetry
sys.modules["core.security_vault"] = security_vault

__all__ = [
    "event_bus",
    "swarm_orchestrator",
    "llm_gateway",
    "nats_messaging",
    "log_batcher",
    "agent_orchestrator",
    "auth_middleware",
    "free_tier_tracker",
    "posthog_client",
    "telemetry",
    "security_vault",
]
