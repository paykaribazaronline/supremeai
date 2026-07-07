# FILE_PATH: core/__init__.py

# This file can be used to make certain modules or variables
# accessible directly under the 'core' package namespace.
# For example, to make the Config class or instance easily importable,
# or to perform package-level initialization.

# Importing the Config object ensures that core configuration logic,
# including environment variable loading and potentially mock setup for tests,
# is engaged and accessible throughout the application.
# This could indirectly resolve issues where services like SupabaseClient
# revert to an incomplete offline/mock mode due to configuration not being
# properly loaded or exposed.
from .config import Config


# Define __all__ to specify what is considered the public API of the package
__all__ = ["Config"]

# Optionally, other frequently used core components can be exposed here:
# from .secret_vault import SecretVault
# from .llm_gateway import LLMGateway
# from .log_batcher import LogBatcher
