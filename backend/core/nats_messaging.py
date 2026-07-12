import warnings


warnings.warn("This import is deprecated. Please use core.messaging.nats_messaging", DeprecationWarning)

from core.messaging.nats_messaging import *  # noqa: F403
