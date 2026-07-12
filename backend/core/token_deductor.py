import warnings


warnings.warn("This import is deprecated. Please use core.llm.token_deductor", DeprecationWarning, stacklevel=2)

from core.llm.token_deductor import *  # noqa: E402, F403
