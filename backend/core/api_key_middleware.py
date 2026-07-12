import warnings


warnings.warn("This import is deprecated. Please use core.security.api_key_middleware", DeprecationWarning)

from core.security.api_key_middleware import *  # noqa: F403
