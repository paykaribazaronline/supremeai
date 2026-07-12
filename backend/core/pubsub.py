import warnings


warnings.warn("This import is deprecated. Please use core.messaging.pubsub", DeprecationWarning)

from core.messaging.pubsub import *  # noqa: F403
