# FILE_PATH: core/__init__.py
from .config import Settings
from .config import sanitize_cors_origins


# Dynamically attach the sanitize_cors_origins function as a static method
# to the Settings class. This allows it to be called as Settings.sanitize_cors_origins().
Settings.sanitize_cors_origins = staticmethod(sanitize_cors_origins)
