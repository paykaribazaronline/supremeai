import os
import re
from pathlib import Path


def process_file(filepath: Path):
    try:
        with open(filepath, encoding="utf-8") as f:
            content = f.read()
    except OSError: # বাংলা মন্তব্য: রূফ লিন্ট এরর এড়াতে specific exception catch করা হচ্ছে
        return

    # Skip files that shouldn't use logger
    if "cli.py" in str(filepath) or "scripts" in str(filepath) or "tests" in str(filepath):
        return

    # Replace basic print( with logger.info(
    new_content, count = re.subn(r"(?<!_)print\(", "logger.info(", content)

    if count > 0:
        if "from loguru import logger" not in new_content:
            # find first import
            lines = new_content.split("\n")
            for i, line in enumerate(lines):
                if line.startswith("import ") or line.startswith("from "):
                    lines.insert(i, "from loguru import logger")
                    break
            new_content = "\n".join(lines)

        with open(filepath, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Fixed {count} prints in {filepath}")  # noqa: T201


root = Path(r"c:\Users\n\supremeai\supremeai_2.0\backend")
for root_dir, _, files in os.walk(root):
    if "venv" in root_dir or ".venv" in root_dir:
        continue
    for file in files:
        if file.endswith(".py"):
            process_file(Path(root_dir) / file)
