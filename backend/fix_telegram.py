
filepath = "tests/test_telegram_bot.py"
with open(filepath, encoding="utf-8") as f:
    content = f.read()

content = content.replace(
    'with patch.dict(os.environ, {"TELEGRAM_BOT_TOKEN": "test-token"}):',
    'from core.config import settings\n    with patch.object(settings, "telegram_bot_token", "test-token", create=True):',
)

content = content.replace(
    'with patch.dict(os.environ, {"TELEGRAM_BOT_TOKEN": "mock_token"}):',
    'with patch.object(settings, "telegram_bot_token", "mock_token", create=True):',
)

content = content.replace("with patch.dict(os.environ, {}, clear=True):", 'with patch.object(settings, "telegram_bot_token", "", create=True):')

content = content.replace(
    'with patch.dict(os.environ, {"TELEGRAM_BOT_TOKEN": "valid-token"}):',
    'with patch.object(settings, "telegram_bot_token", "valid-token", create=True):',
)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("test_telegram_bot.py patched.")
