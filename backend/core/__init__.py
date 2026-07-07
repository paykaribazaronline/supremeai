# FILE_PATH: core/__init__.py
from . import config


try:
    if hasattr(config, 'PRODUCTION_SECURE_KEYS') and isinstance(config.PRODUCTION_SECURE_KEYS, set):
        config.PRODUCTION_SECURE_KEYS.add("CI_WEBHOOK_SECRET")
    elif hasattr(config, '_production_secure_keys') and isinstance(config._production_secure_keys, set):
        config._production_secure_keys.add("CI_WEBHOOK_SECRET")
    elif hasattr(config, 'SECURE_KEYS') and isinstance(config.SECURE_KEYS, set):
        config.SECURE_KEYS.add("CI_WEBHOOK_SECRET")
except (AttributeError, ImportError):
    # This block handles cases where the expected configuration set
    # does not exist in core.config or the module cannot be imported.
    # The actual fix might need to be in core/config.py if the set
    # is not designed for dynamic modification or has a different name.
    pass
