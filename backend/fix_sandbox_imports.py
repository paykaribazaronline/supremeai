import re

filepath = 'tools/docker_sandbox.py'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

if 'import os' not in content:
    content = re.sub(r'^(import |from )', r'import os\n\1', content, count=1, flags=re.MULTILINE)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Added import os to docker_sandbox.py")
