"""SupremeAI 2.0 — Entry point. Handles ENV bootstrap, signal handling, and Uvicorn launch.

বাংলা: রুট এন্ট্রি পয়েন্ট। ENV সেটআপ, সিগন্যাল হ্যান্ডলিং এবং সার্ভার লঞ্চ।
"""

import os
import signal
import sys

if not os.getenv("ENV"):
    os.environ["ENV"] = os.getenv("SUPREMEAI_DEFAULT_ENV", "local")

# Initialize global silent catcher before any other imports that might spawn threads
from core.intelligent_silent_catcher import setup_silent_catcher

setup_silent_catcher()

import uvicorn
from loguru import logger

# বাংলা মন্তব্য: টেস্ট এনভায়রনমেন্টে সম্পূর্ণ অ্যাপ এবং প্রোডাকশনে রোল অনুযায়ী ইউজার/অ্যাডমিন এন্ট্রি পয়েন্ট লোড করা হচ্ছে
# বাংলা: _APP_IMPORT_STRING ট্র্যাক করা হয় যাতে uvicorn.run()-এ app object-এর বদলে
# import string পাস করা যায় — reload=True বা workers>1 উভয় ক্ষেত্রেই সঠিকভাবে কাজ করে।
if "pytest" in sys.modules:
    _APP_IMPORT_STRING = "core.app:app"
else:
    role = os.getenv("SERVICE_ROLE", "user").lower()
    if role == "admin":
        _APP_IMPORT_STRING = "core.app_admin:app"
    else:
        _APP_IMPORT_STRING = "core.app_user:app"

# বাংলা মন্তব্য: ব্যাকওয়ার্ড কম্প্যাটিবিলিটি এবং টেস্টের সুবিধার জন্য core.app থেকে app এক্সপোর্ট করা হচ্ছে
from core.app import app

__all__ = ["app"]

from core.config import settings
from core.logging_config import setup_logging

setup_logging()


def _handle_sigterm(signum: int, frame: object) -> None:
    """SIGTERM/SIGINT handler.

    SupremeAI FastAPI shutdown is handled by Uvicorn + `lifespan.app_lifespan`.
    This handler must NOT force `sys.exit()` because that can bypass lifespan teardown.
    """
    logger.info(
        f"🚨 Signal received ({signum}). Initiating graceful shutdown via Uvicorn/FastAPI lifespan..."
    )
    # Best-effort observability: let operators know shutdown intent was triggered.
    os.environ["UVICORN_SHUTDOWN_REQUESTED"] = "1"
    # Do not block here; return control to Uvicorn so it can run shutdown hooks.
    return


signal.signal(signal.SIGTERM, _handle_sigterm)
signal.signal(signal.SIGINT, _handle_sigterm)


def run_server() -> None:
    """Boot the Uvicorn server with config-driven settings.

    বাংলা: কনফিগ-ড্রিভেন সেটিংস দিয়ে Uvicorn সার্ভার বুট।
    """
    is_local = settings.env == "local"
    port = int(os.getenv("PORT", str(settings.port)))
    host = os.getenv("HOST") or (
        "0.0.0.0"
        if os.getenv("RENDER") or os.getenv("PORT") or not is_local
        else settings.host
    )
    uvicorn_kwargs: dict = {
        "host": host,
        "port": port,
        "log_level": os.getenv("UVICORN_LOG_LEVEL", "info"),
        "access_log": os.getenv("UVICORN_ACCESS_LOG", "true").lower() == "true",
        "timeout_keep_alive": int(os.getenv("UVICORN_KEEP_ALIVE_TIMEOUT", "30")),
    }
    if is_local:
        uvicorn_kwargs["reload"] = True
    else:
        uvicorn_kwargs["reload"] = False
        # বাংলা: UVICORN_WORKERS env var ব্যবহার করা হয়, GUNICORN_WORKERS deprecated
        workers = int(os.getenv("UVICORN_WORKERS", "1"))
        if workers > 1:
            uvicorn_kwargs["workers"] = workers

    try:
        # বাংলা: আগে app object সরাসরি পাস হতো — reload=True বা workers>1 হলে uvicorn
        # 'must pass import string' ওয়ার্নিং দিত এবং port bind না করেই exit হতো (status 3)।
        # import string ব্যবহারে reload ও multi-worker দুটো ক্ষেত্রেই নির্ভরযোগ্যভাবে কাজ করে।
        uvicorn.run(_APP_IMPORT_STRING, **uvicorn_kwargs)
    except RuntimeError as exc:
        logger.critical(f"Server failed to start (configuration error): {exc}")
        if settings.sentry_dsn:
            try:
                import sentry_sdk

                sentry_sdk.capture_exception(exc)
            except Exception as sentry_exc:
                logger.warning(f"Failed to report error to Sentry: {sentry_exc}")
        sys.exit(1)
    except OSError as exc:
        logger.critical(
            f"Server failed to start (port/bind error on {settings.host}:{port}): {exc}"
        )
        if settings.sentry_dsn:
            try:
                import sentry_sdk

                sentry_sdk.capture_exception(exc)
            except Exception as sentry_exc:
                logger.warning(f"Failed to report error to Sentry: {sentry_exc}")
        sys.exit(1)


if __name__ == "__main__":
    run_server()
