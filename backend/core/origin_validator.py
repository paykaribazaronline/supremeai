import warnings


warnings.warn("This import is deprecated. Please use core.security.origin_validator", DeprecationWarning, stacklevel=2)

from core.security.origin_validator import *  # noqa: E402, F403
