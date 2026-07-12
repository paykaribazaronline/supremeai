import warnings


warnings.warn("This import is deprecated. Please use core.health.self_healer", DeprecationWarning, stacklevel=2)

from core.health.self_healer import *  # noqa: E402, F403
