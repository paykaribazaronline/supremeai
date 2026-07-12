import warnings


warnings.warn("This import is deprecated. Please use core.messaging.gcp_pubsub_queue", DeprecationWarning)

from core.messaging.gcp_pubsub_queue import *  # noqa: F403
