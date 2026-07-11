
filepath_tests = "tests/test_telegram_bot.py"
filepath_bot = "tools/telegram_bot.py"

# 1. Revert tests/test_telegram_bot.py
# First checkout using git to undo the last script we did, or just do it via regex
import subprocess


subprocess.run(["git", "checkout", filepath_tests])

# 2. Update tools/telegram_bot.py to use os.getenv
with open(filepath_bot, encoding="utf-8") as f:
    content = f.read()

if "import os" not in content:
    content = content.replace("import json", "import os\nimport json")

content = content.replace(
    'self.bot_token: str = getattr(settings, "telegram_bot_token", "")',
    'self.bot_token: str = os.getenv("TELEGRAM_BOT_TOKEN") or getattr(settings, "telegram_bot_token", "")',
)

with open(filepath_bot, "w", encoding="utf-8") as f:
    f.write(content)

print("Restored test_telegram_bot.py and patched telegram_bot.py.")
