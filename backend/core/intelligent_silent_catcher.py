# backend/core/intelligent_silent_catcher.py
# বাংলা মন্তব্য: গ্লোবাল এক্সেপশন ও থ্রেড ক্র্যাশ ক্যাচার। রানটাইমে হওয়া যেকোনো সাইলেন্ট বা আনহ্যান্ডেলড
# ক্র্যাশ সনাক্ত করে তা Intelligent Error Bus-এ পাঠিয়ে দেওয়া হয়।

import sys
import threading
import traceback

from loguru import logger

from core.error_bus import with_error_bus
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus


@with_error_bus("handle_unhandled_exception")
def handle_unhandled_exception(exc_type, exc_value, exc_tb):
    """Custom sys.excepthook to catch silent/unhandled crashes globally.

    বাংলা মন্তব্য: রানটাইমে কোনো আনহ্যান্ডেলড এক্সেপশন বা থ্রেড ক্র্যাশ হলে এটি কল হবে এবং এরর বাসে ইভেন্ট এমিট করবে।
    """
    if issubclass(exc_type, KeyboardInterrupt):
        sys.__excepthook__(exc_type, exc_value, exc_tb)
        return

    error_msg = f"UNHANDLED_RUNTIME_EXCEPTION: {exc_value}"
    tb_str = "".join(traceback.format_tb(exc_tb))

    logger.error(error_msg, traceback=tb_str)

    # Try to find the originating module
    module = "intelligent_silent_catcher"
    if exc_tb:
        frame = exc_tb
        while frame.tb_next:
            frame = frame.tb_next
        module = frame.tb_frame.f_globals.get("__name__", "intelligent_silent_catcher")

    # Emit to Intelligent Error Bus
    error_event_bus.emit(
        ErrorEvent(
            module=module,
            error_type="SILENT_RUNTIME_CRASH_DETECTED",
            message=error_msg,
            severity="CRITICAL",
            structured_context=ErrorContext(module=module, env="production"),
            context={"traceback": tb_str, "exception_type": str(exc_type)},
        )
    )

    # Call the original excepthook to preserve default behavior (like printing to stderr)
    sys.__excepthook__(exc_type, exc_value, exc_tb)


def thread_target_wrapper(target):
    """Wrapper to catch silent thread crashes.

    বাংলা মন্তব্য: থ্রেডের ভেতরের সাইলেন্ট ক্র্যাশ ক্যাচ করার জন্য থ্রেড টার্গেটের চারপাশে একটি ট্রাই-ক্যাচ র্যাপার।
    """

    def wrapper(*args, **kwargs):
        try:
            return target(*args, **kwargs)
        except Exception as e:
            handle_unhandled_exception(type(e), e, e.__traceback__)

    return wrapper


original_thread_init = threading.Thread.__init__


def patched_thread_init(self, *args, **kwargs):
    original_thread_init(self, *args, **kwargs)
    if hasattr(self, "_target") and self._target:
        self._target = thread_target_wrapper(self._target)


def install_excepthook():
    """Install the custom exception hook to catch all unhandled exceptions."""
    sys.excepthook = handle_unhandled_exception
    threading.Thread.__init__ = patched_thread_init
    logger.info("🛡️ Intelligent Silent Catcher hooks installed.")


# Alias for backward-compatibility
setup_silent_catcher = install_excepthook
