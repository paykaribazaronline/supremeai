import warnings


warnings.warn("This import is deprecated. Please use core.security.auth_middleware", DeprecationWarning, stacklevel=2)

from core.security.auth_middleware import *  # noqa: E402  # noqa: F403
