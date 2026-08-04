import os
import re

test_dir = "C:/Users/n/supremeai/supremeai_2.0/backend/tests"
pattern = re.compile(r"^(\s+)yield\s*$", re.MULTILINE)

for root, dirs, files in os.walk(test_dir):
    for file in files:
        if file.endswith(".py"):
            filepath = os.path.join(root, file)
            try:
                with open(filepath, "r", encoding="utf-8") as f:
                    content = f.read()
            except UnicodeDecodeError:
                with open(filepath, "r", encoding="latin-1") as f:
                    content = f.read()
            original = content
            content = pattern.sub(r"\1yield\n\1return", content)
            if content != original:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(content)
                print(f"Fixed: {filepath}")
