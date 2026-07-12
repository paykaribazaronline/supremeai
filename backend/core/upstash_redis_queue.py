import warnings


warnings.warn("This import is deprecated. Please use core.messaging.upstash_redis_queue", DeprecationWarning, stacklevel=2)

from core.messaging.upstash_redis_queue import *  # noqa: E402, F403
