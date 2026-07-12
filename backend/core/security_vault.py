import warnings


warnings.warn("This import is deprecated. Please use core.security.security_vault", DeprecationWarning, stacklevel=2)

from core.security.security_vault import *  # noqa: E402, F403
