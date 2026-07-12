import warnings


warnings.warn("This import is deprecated. Please use core.llm.llm_gateway", DeprecationWarning, stacklevel=2)

from core.llm.llm_gateway import *  # noqa: E402  # noqa: F403

def __getattr__(name: str):
    import core.llm.llm_gateway as target
    return getattr(target, name)
