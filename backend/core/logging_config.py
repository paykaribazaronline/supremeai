"""This module centralizes the configuration of the application's logging system using Loguru. It establishes structured logging practices by directing output to both the console (stdout) for real-time monitoring and to a rotating, compressed file for persistent storage and detailed debugging. This setup is crucial for maintaining observability across the SupremeAI ecosystem, aiding in development, operational monitoring, and efficient troubleshooting of its FastAPI backend and various agentic tools.

Key Components:
- `setup_logging()`: Configures the Loguru logger to output structured logs to both the console and a rotating file, ensuring comprehensive logging for the SupremeAI application.

Dependencies:
- `sys`: For accessing standard system streams, specifically `sys.stdout` for console logging.
- `loguru`: The primary external library used for flexible, structured, and production-ready logging."""

import sys

from loguru import logger


def setup_logging():
    """
    Configures Loguru for structured logging.
    In production, this could also push to OpenTelemetry.
    """
    logger.remove()
    # বাংলা মন্তব্য: pytest চালানোর সময় sys.stdout ক্লোজ হয়ে যাওয়ার কারণে "I/O operation on closed file" এরর হয়। এটি এড়াতে lambda ব্যবহার করে ডাইনামিকালি sys.stdout রিড করা হচ্ছে।  # noqa: E501
    logger.add(
        lambda msg: sys.stdout.write(msg),
        colorize=True,
        format=(
            "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | "
            "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
            "<level>{message}</level>"
        ),
        level="INFO",
    )
    logger.add(
        "logs/supremeai.log",
        rotation="10 MB",
        retention="10 days",
        compression="zip",
        level="DEBUG",
    )
