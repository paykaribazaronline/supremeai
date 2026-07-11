# 📄 ফাইল: backend/core/security_utils.py

**প্রকার:** .py  
**সাইজ:** 841 বাইট  
**আপডেট:** 2026-07-11T11:29:21.194917

---

## কোড

```py
import ipaddress
import socket
from urllib.parse import urlparse

from loguru import logger


def is_safe_url(url: str) -> bool:
    try:
        parsed = urlparse(url)
        hostname = parsed.hostname
        if not hostname:
            return False
        if hostname == "169.254.169.254" or hostname.endswith(".local"):
            return False
        ip = socket.gethostbyname(hostname)
        ip_obj = ipaddress.ip_address(ip)
        return not (ip_obj.is_private or ip_obj.is_loopback or ip_obj.is_link_local)
    except (ValueError, socket.gaierror, OSError) as e:
        # সুনির্দিষ্ট URL পার্সিং বা সকেট-সম্পর্কিত ত্রুটি ক্যাচ করা হলো
        logger.warning(f"URL safety check failed for '{url}': {e}")
        return False

```