import warnings


warnings.warn("This import is deprecated. Please use core.messaging.upstash_redis_queue", DeprecationWarning)

from core.messaging.upstash_redis_queue import *  # noqa: F403
