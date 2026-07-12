import warnings


warnings.warn("This import is deprecated. Please use core.health.health_probes", DeprecationWarning, stacklevel=2)

from core.health.health_probes import *  # noqa: E402, F403
