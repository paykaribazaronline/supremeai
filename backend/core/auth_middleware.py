import warnings


warnings.warn("This import is deprecated. Please use core.security.auth_middleware", DeprecationWarning)

from core.security.auth_middleware import *  # noqa: F403
