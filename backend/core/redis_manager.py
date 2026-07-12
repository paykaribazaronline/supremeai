import warnings


warnings.warn("This import is deprecated. Please use core.cache.redis_manager", DeprecationWarning, stacklevel=2)

from core.cache.redis_manager import *  # noqa: E402  # noqa: F403
