import warnings


warnings.warn("This import is deprecated. Please use core.security.api_key_middleware", DeprecationWarning, stacklevel=2)

from core.security.api_key_middleware import *  # noqa: E402  # noqa: F403
