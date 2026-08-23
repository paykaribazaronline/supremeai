from pathlib import Path
import re

file_path = Path(r"f:\supremeai\backend\api\routes\__init__.py")
content = file_path.read_text(encoding="utf-8")

if "from typing import Any" not in content:
    content = "from typing import Any\n" + content

# We want to replace `    some_router = None` with `    some_router: Any = None`
content = re.sub(r'(\s+)([a-zA-Z0-9_]+router) = None', r'\1\2: Any = None', content)

file_path.write_text(content, encoding="utf-8")
print("Done fixing __init__.py")
