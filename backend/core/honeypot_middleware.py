import warnings


warnings.warn("This import is deprecated. Please use core.security.honeypot_middleware", DeprecationWarning)

from core.security.honeypot_middleware import *  # noqa: F403
