from pathlib import Path
import re

file_path = Path(r"f:\supremeai\backend\core\__init__.py")
content = file_path.read_text(encoding="utf-8")

content = content.replace(") = (None,) * 11", ") = (None,) * 11  # type: ignore")
content = content.replace("from core.lifespan import app_lifespan", "from core.lifespan import app_lifespan  # type: ignore")

file_path.write_text(content, encoding="utf-8")
print("Done fixing core/__init__.py")
