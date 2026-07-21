import re

with open("tests/test_api_key_middleware.py", encoding="utf-8") as f:
    content = f.read()

# 1. Remove @patch decorator
content = content.replace(
    '@patch("core.security.api_key_middleware.AsyncRateLimiter.acquire", new_callable=AsyncMock)\n',
    "",
)

# 2. Remove mock_acquire argument
content = re.sub(
    r"def test_([a-zA-Z0-9_]+)\(self, mock_acquire\):", r"def test_\1(self):", content
)

# 3. Remove mock_acquire assignment
content = re.sub(r"\s*mock_acquire\.return_value = (True|False)\n", "\n", content)

# 4. Change keys
keys = [
    '"sk-supreme-0101010101abcdef"',
    '"sk-supreme-0202020202abcdef"',
    '"sk-supreme-0303030303abcdef"',
    '"sk-supreme-0404040404abcdef"',
    '"sk-supreme-0505050505abcdef"',
]

parts = content.split('"sk-supreme-1234567890abcdef"')
if len(parts) == 6:
    content = (
        parts[0]
        + keys[0]
        + parts[1]
        + keys[1]
        + parts[2]
        + keys[2]
        + parts[3]
        + keys[3]
        + parts[4]
        + keys[4]
        + parts[5]
    )


# 5. Make test_rate_limit_exceeded do two requests
def rewrite_rl_test(match):
    test_body = match.group(0)
    # We need to change the last assert to expect 429, but first make a successful request.
    test_body = test_body.replace(
        "resp = client.get(",
        'client.get(\n                "/api/test",\n                headers={"x-api-key": "sk-supreme-0505050505abcdef"},\n            )\n            resp = client.get(',
    )
    return test_body


content = re.sub(
    r"    def test_rate_limit_exceeded.*?assert resp\.status_code == 429",
    rewrite_rl_test,
    content,
    flags=re.DOTALL,
)

with open("tests/test_api_key_middleware.py", "w", encoding="utf-8") as f:
    f.write(content)
