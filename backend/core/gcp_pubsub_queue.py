import warnings


warnings.warn("This import is deprecated. Please use core.messaging.gcp_pubsub_queue", DeprecationWarning, stacklevel=2)

from core.messaging.gcp_pubsub_queue import *  # noqa: E402, F403
