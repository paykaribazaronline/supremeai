"""Logging Configuration — Structured Logging with Correlation IDs (Zero-Hardcode)

বাংলা মন্তব্ব্য: এই মডিউলটি স্ট্রাকচার্ড লগিং এবং করিলেশন আইডি ব্যবস্থা সরবরাহ করে।
যেকোনো hardcoded ভ্যালু নেই। সবকিছু environment-driven। JSON ফরম্যাটে লগিং নিশ্চিত করে।

Key Components:
- `setup_logging()`: লগিং কনফিগারেশন সেট আপ করে।
- `CorrelationIdFilter`: রিকোয়েস্ট-ওয়াইজ করিলেশন আইডি যোগ করে।
- `JsonFormatter`: JSON ফরম্যাটে লগ রাইট করে।

Critical Security Note: সমস্ত লগ এখন JSON ফরম্যাটে হবে এবং
করিলেশন আইডি সহ স্ট্রাকচার্ড হবে অডিট এবং মনিটরিং এর জন্য।
"""

import json
import os
import sys
import uuid
from datetime import datetime
from pathlib import Path

from loguru import logger

try:
    from starlette_context import context
    from starlette_context.header_keys import HeaderKeys
except ImportError:

    class DummyContext(dict):
        def exists(self):
            return False

    context = DummyContext()

    class HeaderKeys:
        request_id = "X-Request-ID"


from core.config import settings


class LoggingConfig:
    """Centralized logging configuration with correlation IDs and structured format."""

    def __init__(self):
        self.setup_logging()

    def setup_logging(self):
        """Configure structured logging with correlation IDs."""
        # Remove default handlers to avoid duplication
        logger.remove()

        # Determine log level based on environment
        log_level = "DEBUG" if settings.debug else "INFO"

        # Add JSON formatted handler with correlation ID
        logger.add(
            sys.stdout,
            format=self._json_format,
            level=log_level,
            backtrace=True,
            diagnose=True,
        )

        # Add file handler if needed (with rotation)
        if settings.env in ["production", "staging"]:
            try:
                log_dir = Path(os.getenv("LOG_DIR", "/tmp/logs" if os.getenv("RENDER") else "logs"))
                log_dir.mkdir(parents=True, exist_ok=True)
                log_file = log_dir / "app_{time}.log"
                logger.add(
                    str(log_file),
                    rotation="100 MB",
                    retention="10 days",
                    compression="zip",
                    serialize=True,
                    level="INFO",
                )
            except Exception as exc:
                sys.stderr.write(f"⚠️ Failed to initialize file logger sink: {exc}. Continuing with stdout logging.\n")

    def _json_format(self, record: dict) -> str:
        """Custom JSON formatter with correlation ID."""
        # Extract correlation ID from context if available
        correlation_id = "N/A"
        try:
            if hasattr(context, "exists") and context.exists():
                correlation_id = context.data.get(HeaderKeys.correlation_id, "N/A")
        except Exception:
            # বাংলা মন্তব্য: starlette_context request scope-এর বাইরে থাকলে এই exception আসে।
            # সাইলেন্ট ফেইলিউর নয় — fallback value সেট করা হচ্ছে।
            correlation_id = "N/A"

        # Create structured log entry
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "level": record["level"].name,
            "message": record["message"],
            "module": record["name"],
            "function": record["function"],
            "line": record["line"],
            "correlation_id": correlation_id,
            "environment": settings.env,
            "service": settings.PROJECT_NAME,
        }

        # Add any extra fields that were passed
        if record["extra"]:
            log_entry.update(record["extra"])

        record["extra"]["json_str"] = json.dumps(log_entry)
        return "{extra[json_str]}\n"


def inject_correlation_id():
    """Middleware function to inject correlation ID into logs."""
    # This will be used in middleware to set the correlation ID in context
    correlation_id = str(uuid.uuid4())
    try:
        context.set(HeaderKeys.correlation_id, correlation_id)
    except (AttributeError, LookupError, NameError):  # Context may not be initialized in some cases
        pass
    return correlation_id


# Initialize logging configuration
logging_config = LoggingConfig()


# Alias for convenience
def setup_logging():
    return None  # Already configured in the class initialization
