import warnings


warnings.warn("This import is deprecated. Please use core.messaging.pubsub", DeprecationWarning, stacklevel=2)

from core.messaging.pubsub import *  # noqa: E402, F403
