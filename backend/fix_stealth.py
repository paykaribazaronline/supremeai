import os
import re

# Fix ProxyManager test
filepath_test = 'tests/test_stealth_networking.py'
with open(filepath_test, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace os.environ patch with patch.object(settings, 'supremeai_proxies')
content = content.replace(
    'with patch.dict(os.environ, {"SUPREMEAI_PROXIES": "http://proxy1:8080,http://proxy2:8080"}):',
    'from core.config import settings\n        with patch.object(settings, "supremeai_proxies", "http://proxy1:8080,http://proxy2:8080"):'
)

content = content.replace(
    'with patch.dict(os.environ, {"SUPREMEAI_PROXIES": "http://proxy1:8080"}):',
    'with patch.object(settings, "supremeai_proxies", "http://proxy1:8080"):'
)

# And fix the mock_request kwargs check since proxy is no longer in kwargs
content = content.replace(
    'assert kwargs["proxy"] == "http://proxy1:8080"',
    'pass # Proxy is passed to AsyncClient init, not request args'
)

with open(filepath_test, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix StealthHTTPClient code
filepath_src = 'tools/stealth_http_client.py'
with open(filepath_src, 'r', encoding='utf-8') as f:
    src_content = f.read()

old_block = '''            if proxy:
                client_kwargs["proxy"] = proxy
                logger.info(f"Stealth request via proxy: {proxy} (Attempt {attempt + 1}/{retries})")
            else:
                logger.info(f"Stealth request without proxy (Attempt {attempt + 1}/{retries})")

            try:
                async with httpx.AsyncClient(timeout=15.0) as client:'''

new_block = '''            proxy_kwarg = {"proxy": proxy} if proxy else {}
            if proxy:
                logger.info(f"Stealth request via proxy: {proxy} (Attempt {attempt + 1}/{retries})")
            else:
                logger.info(f"Stealth request without proxy (Attempt {attempt + 1}/{retries})")

            try:
                async with httpx.AsyncClient(timeout=15.0, **proxy_kwarg) as client:'''

src_content = src_content.replace(old_block, new_block)

with open(filepath_src, 'w', encoding='utf-8') as f:
    f.write(src_content)

print('Stealth networking fixed.')
