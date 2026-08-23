from pathlib import Path
import re

file_path = Path(r"f:\supremeai\backend\api\routes\__init__.py")
content = file_path.read_text(encoding="utf-8")

# Remove the Any = None
content = re.sub(r'(\s+)([a-zA-Z0-9_]+router): Any = None', r'\1\2 = None  # type: ignore', content)

file_path.write_text(content, encoding="utf-8")
print("Done fixing __init__.py again")
