"""SupremeAI 2.0 — Entry point. Handles ENV bootstrap, signal handling, and Uvicorn launch.

বাংলা: রুট এন্ট্রি পয়েন্ট। ENV সেটআপ, সিগন্যাল হ্যান্ডলিং এবং সার্ভার লঞ্চ।
"""

import os
import signal
import sys
from typing import Any

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

# বাংলা মন্তব্য (ROOT-CAUSE FIX): আগে এখানে `from core.app import app` করে মডিউল-লেভেলে
# সম্পূর্ণ (all-routers) অ্যাপ তৈরি হতো। কিন্তু production-এ uvicorn আবার
# `core.app_user:app` / `core.app_admin:app` ইম্পোর্ট করে দ্বিতীয় FastAPI অ্যাপ
# বানাত। ফলে একই প্রসেসে দুইটা সম্পূর্ণ অ্যাপ (~70 router × 2, middleware chain × 2)
# লোড হতো — Render free tier-এর 512MB RAM-এ এটি OOM/অতিরিক্ত ধীর বুট ঘটাত এবং
# deploy `update_failed` + /health timeout দিত।
# এখন `app` লেজি `__getattr__`-এর মাধ্যমে দেওয়া হয়: শুধু যারা সত্যিই
# `from main import app` করে (schema_exporter, generate_openapi, legacy tests)
# তারাই full app তৈরি করবে; সার্ভার বুট পথে এটি আর তৈরি হবে না।
__all__ = ["app"]


def __getattr__(name: str) -> Any:
    """Lazy re-export of `core.app:app` for backward compatibility.

    বাংলা: `from main import app` আগের মতোই কাজ করবে, কিন্তু সার্ভার বুটের সময়
    অপ্রয়োজনীয়ভাবে দ্বিতীয় অ্যাপ ইনস্ট্যান্স তৈরি হবে না।
    """
    if name == "app":
        from core.app import app as _app

        return _app
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


from core.config import settings
from core.logging_config import setup_logging

setup_logging()


def _handle_sigterm(signum: int, frame: object) -> None:
    """SIGTERM/SIGINT handler.

    SupremeAI FastAPI shutdown is handled by Uvicorn + `lifespan.app_lifespan`.
    This handler must NOT force `sys.exit()` because that can bypass lifespan teardown.
    """
    logger.info(f"🚨 Signal received ({signum}). Initiating graceful shutdown via Uvicorn/FastAPI lifespan...")
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
        "0.0.0.0" if os.getenv("RENDER") or os.getenv("PORT") or not is_local else settings.host
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
        logger.critical(f"Server failed to start (port/bind error on {settings.host}:{port}): {exc}")
        if settings.sentry_dsn:
            try:
                import sentry_sdk

                sentry_sdk.capture_exception(exc)
            except Exception as sentry_exc:
                logger.warning(f"Failed to report error to Sentry: {sentry_exc}")
        sys.exit(1)


if __name__ == "__main__":
    run_server()
