import warnings


warnings.warn("This import is deprecated. Please use core.security.honeypot_middleware", DeprecationWarning, stacklevel=2)

from core.security.honeypot_middleware import *  # noqa: E402  # noqa: F403
