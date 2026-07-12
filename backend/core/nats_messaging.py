import warnings


warnings.warn("This import is deprecated. Please use core.messaging.nats_messaging", DeprecationWarning, stacklevel=2)

from core.messaging.nats_messaging import *  # noqa: E402  # noqa: F403
