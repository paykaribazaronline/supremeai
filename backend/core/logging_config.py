"""This module centralizes the configuration of the application's logging system using Loguru. It establishes structured logging practices by directing output to both the console (stdout) for real-time monitoring and to a rotating, compressed file for persistent storage and detailed debugging. This setup is crucial for maintaining observability across the SupremeAI ecosystem, aiding in development, operational monitoring, and efficient troubleshooting of its FastAPI backend and various agentic tools.

Key Components:
- `setup_logging()`: Configures the Loguru logger to output structured logs to both the console and a rotating file, ensuring comprehensive logging for the SupremeAI application.

Dependencies:
- `sys`: For accessing standard system streams, specifically `sys.stdout` for console logging.
- `loguru`: The primary external library used for flexible, structured, and production-ready logging.
"""  # noqa: E501

import os
import sys

from loguru import logger


def setup_logging():
    """
    Configures Loguru for structured logging with JSON serialization in production.
    Falls back to human-readable format in local development.
    """
    logger.remove()
    env = os.getenv("ENV", "local").lower()
    is_prod = env in ("production", "staging")

    if is_prod:
        # JSON format for production — easier to parse by log aggregators
        logger.add(
            lambda msg: sys.stdout.write(msg),
            colorize=False,
            serialize=True,
            format="{time} | {level} | {message} | {extra}",
            level="INFO",
        )
    else:
        # Human-readable colorized format for local development
        logger.add(
            lambda msg: sys.stdout.write(msg),
            colorize=True,
            format=(
                "<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | "
                "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
                "<level>{message}</level>"
            ),
            level="DEBUG",
        )

    # File logging — always human-readable for local debugging
    logger.add(
        "logs/supremeai.log",
        rotation="10 MB",
        retention="10 days",
        compression="zip",
        level="DEBUG",
    )
