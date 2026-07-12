import warnings


warnings.warn("This import is deprecated. Please use core.health.health_monitor", DeprecationWarning, stacklevel=2)

from core.health.health_monitor import *  # noqa: E402  # noqa: F403
