import warnings


warnings.warn("This import is deprecated. Please use core.messaging.event_bus", DeprecationWarning, stacklevel=2)

from core.messaging.event_bus import *  # noqa: E402, F403
