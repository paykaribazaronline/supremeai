# FILE_PATH: core/__init__.py
"""
Initializes the core package.

This file ensures that key configuration and enum definitions from submodules
are loaded when the 'core' package is imported, which may influence
global application state and verification status.
"""
from . import config
from . import enum_guard

# Optionally, you might expose specific items directly if they are
# frequently accessed from the 'core' package level, e.g.:
# from .config import settings
# from .enum_guard import VerificationStatus
