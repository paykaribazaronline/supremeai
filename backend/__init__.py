# backend/__init__.py
"""SupremeAI Master Package.

Living, Self-Evolving Autonomous AI Engine.
"""

import os
import sys

_backend_dir = os.path.dirname(os.path.abspath(__file__))
if _backend_dir not in sys.path:
    sys.path.insert(0, _backend_dir)

try:
    from core.factory import SupremeAIFactory, get_ai, get_factory
    from core.integration_layer import SupremeAIIntegrator, get_integrator
    from config.settings import get_settings
except ImportError:
    from backend.core.factory import SupremeAIFactory, get_ai, get_factory
    from backend.core.integration_layer import SupremeAIIntegrator, get_integrator
    from backend.config.settings import get_settings

__version__ = "4.2.0-wired"
__all__ = [
    "SupremeAIFactory",
    "SupremeAIIntegrator",
    "get_ai",
    "get_factory",
    "get_integrator",
    "get_settings",
]
