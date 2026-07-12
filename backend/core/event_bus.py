import warnings


warnings.warn("This import is deprecated. Please use core.messaging.event_bus", DeprecationWarning)

from core.messaging.event_bus import *  # noqa: F403
