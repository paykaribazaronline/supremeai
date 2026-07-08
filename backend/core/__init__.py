# FILE_PATH: core/__init__.py

# Importing key modules and components into the package namespace.
# This ensures that crucial components like configuration and security-related modules
# are loaded and potentially initialized when the 'core' package is imported.
# This helps resolve issues where authentication logic might fail due to uninitialized
# dependencies or un-loaded security vault functionality.

from . import config
from . import security_vault
from .auth_middleware import AuthMiddleware

# Further imports can be added for other core modules to ensure they are loaded
# and their module-level logic (e.g., singleton instantiation, environment variable loading)
# is executed, especially for those with 0% test coverage.
# For example:
# from . import llm_gateway
# from . import cost_guard
# from . import enum_guard
# from . import event_bus
# from . import knowledge_base
# from . import pubsub
# from . import swarm_orchestrator
