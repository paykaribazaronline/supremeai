# backend/api/__init__.py
"""SupremeAI 2.0 — API Package Bootstrap.

Centralized router registration with ErrorEventBus integration.
No router is loaded silently; all failures are captured and reported.
"""

from __future__ import annotations

import importlib
import logging

from core.config import settings  # noqa  # noqa
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from fastapi import FastAPI

logger = logging.getLogger("SupremeAI.API")


def register_router(
    app: FastAPI,
    router_module: str,
    prefix: str = "",
    *,
    optional: bool = False,
) -> None:
    """Lazy-load a router module and include it on the FastAPI app.

    Args:
        app: The FastAPI application instance.
        router_module: Dotted import path to the router module.
        prefix: URL prefix for the router.
        optional: If True, missing/optional routers are logged as warnings
                  instead of crashing the process.
    """
    try:
        module = importlib.import_module(router_module)
        router = getattr(module, "router", None)
        if router is None:
            raise AttributeError(f"Module {router_module!r} has no 'router' attribute.")
        app.include_router(router, prefix=prefix)
        logger.debug(f"Router registered: {router_module!r} -> prefix={prefix!r}")
    except ImportError as exc:
        msg = f"Optional router {router_module!r} not found: {exc}"
        if optional:
            logger.warning(msg)
            error_event_bus.emit(
                ErrorEvent(
                    module="api_bootstrap",
                    error_type="ROUTER_NOT_FOUND",
                    message=str(exc)[:200],
                    severity="WARNING",
                    structured_context=ErrorContext(module="api_bootstrap"),
                    context={"router_module": router_module},
                ),
            )
        else:
            logger.critical(msg)
            error_event_bus.emit(
                ErrorEvent(
                    module="api_bootstrap",
                    error_type="ROUTER_LOAD_FAILED",
                    message=str(exc)[:500],
                    severity="CRITICAL",
                    structured_context=ErrorContext(module="api_bootstrap"),
                    context={"router_module": router_module},
                ),
            )
            raise
    except (AttributeError, TypeError) as exc:
        msg = f"Critical error loading router {router_module!r}: {exc}"
        if optional:
            logger.warning(msg)
        else:
            logger.critical(msg)
            error_event_bus.emit(
                ErrorEvent(
                    module="api_bootstrap",
                    error_type="ROUTER_LOAD_FAILED",
                    message=str(exc)[:500],
                    severity="CRITICAL",
                    structured_context=ErrorContext(module="api_bootstrap"),
                    context={"router_module": router_module},
                ),
            )
            raise


__all__ = ["register_router"]
