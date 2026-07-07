# FILE_PATH: core/__init__.py

# This file is part of the 'core' package, designed to expose key components
# and provide a central point for package-level configuration or metadata.
# Its previous emptiness meant that core modules needed to be imported directly,
# and it didn't leverage Python's package structure for simplified access.

# Expose main components for easier and consistent import across the application.
# These imports allow other modules to do 'from core import Settings' instead of
# 'from core.config import Settings', improving readability and modularity.
from .config import Settings
from .cost_guard import CostGuard
from .enum_guard import EnumGuard
from .event_bus import EventBus
from .knowledge_base import KnowledgeBase
from .llm_gateway import LLMGateway
from .log_batcher import LogBatcher
from .pubsub import PubSub
from .security_vault import SecurityVault
from .swarm_orchestrator import SwarmOrchestrator
from .task_router import TaskRouter


# Optional: Define package-level metadata (e.g., version).
# This helps in tracking the package version programmatically.
__version__ = "0.1.0" # Placeholder version, update as needed
