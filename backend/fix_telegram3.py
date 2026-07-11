import re
import os

filepath_bot = 'tools/telegram_bot.py'
with open(filepath_bot, 'r', encoding='utf-8') as f:
    content = f.read()

if 'import os' not in content:
    # find first import
    content = re.sub(r'^(import |from )', r'import os\n\1', content, count=1, flags=re.MULTILINE)

with open(filepath_bot, 'w', encoding='utf-8') as f:
    f.write(content)

print("Added import os to telegram_bot.py.")
