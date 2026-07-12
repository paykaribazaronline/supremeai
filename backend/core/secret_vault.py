import warnings


warnings.warn("This import is deprecated. Please use core.security.secret_vault", DeprecationWarning, stacklevel=2)

from core.security.secret_vault import *  # noqa: E402, F403
