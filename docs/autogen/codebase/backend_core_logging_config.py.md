# 📄 ফাইল: backend/core/logging_config.py

**প্রকার:** .py  
**সাইজ:** 1,060 বাইট  
**আপডেট:** 2026-07-11T13:49:08.308228

---

## কোড

```py
import sys

from loguru import logger


def setup_logging():
    """
    Configures Loguru for structured logging.
    In production, this could also push to OpenTelemetry.
    """
    logger.remove()
    # বাংলা মন্তব্য: pytest চালানোর সময় sys.stdout ক্লোজ হয়ে যাওয়ার কারণে "I/O operation on closed file" এরর হয়। এটি এড়াতে lambda ব্যবহার করে ডাইনামিকালি sys.stdout রিড করা হচ্ছে।
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

```