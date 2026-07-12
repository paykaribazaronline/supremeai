import warnings


warnings.warn("This import is deprecated. Please use core.security.rbac", DeprecationWarning, stacklevel=2)

from core.security.rbac import *  # noqa: E402  # noqa: F403
