import re

with open("tests/test_api_key_middleware.py", encoding="utf-8") as f:
    content = f.read()

# 1. Remove mock_limiter_cls logic
content = re.sub(
    r'\s*patch\("core\.security\.api_key_middleware\.AsyncRateLimiter"\) as mock_limiter_cls,?',
    "",
    content,
)
content = re.sub(
    r"\s*mock_limiter_cls\.return_value\.acquire = AsyncMock\(return_value=(True|False)\)",
    "",
    content,
)

# 2. Add an @patch at the class level to mock the limiter
# We will inject this before `class TestAPIKeyAuthMiddleware:`
if '@patch("core.security.api_key_middleware.AsyncRateLimiter.acquire"' not in content:
    content = content.replace(
        "class TestAPIKeyAuthMiddleware:",
        '@patch("core.security.api_key_middleware.AsyncRateLimiter.acquire", new_callable=AsyncMock)\nclass TestAPIKeyAuthMiddleware:',
    )

# 3. Add `mock_acquire` argument to all test methods
content = re.sub(
    r"def test_([a-zA-Z0-9_]+)\(self\):", r"def test_\1(self, mock_acquire):", content
)


# 4. Set mock_acquire.return_value inside each test method
def set_mock_return(match):
    func_header = match.group(0)
    if "test_rate_limit_exceeded" in func_header:
        return func_header + "\n        mock_acquire.return_value = False"
    else:
        return func_header + "\n        mock_acquire.return_value = True"


content = re.sub(
    r"    def test_([a-zA-Z0-9_]+)\(self, mock_acquire\):", set_mock_return, content
)

with open("tests/test_api_key_middleware.py", "w", encoding="utf-8") as f:
    f.write(content)
