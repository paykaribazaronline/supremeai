import re
from pathlib import Path

file_path = Path("f:/supremeai/backend/core/competitive_kit.py")
content = file_path.read_text(encoding="utf-8")
lines = content.splitlines()

# 1. Add imports at the top
imports = "import math\nimport requests\ntry:\n    import requests\n    HAS_REQUESTS = True\nexcept ImportError:\n    HAS_REQUESTS = False\n"
content = imports + content

lines = content.splitlines()

for i, line in enumerate(lines):
    if line.strip() == "if violation_score > config[\"strictness\"]:":
        lines[i] = line.replace('config["strictness"]', 'float(config["strictness"])')
    elif line.strip() == 'return SAFETY_CONFIGS[self.current_level]["system_addition"]':
        lines[i] = line.replace('return', 'return str(') + ')'
    elif line.strip() == 'counts = {}' and "def _count_by_rule" in "\n".join(lines[max(0, i-5):i]):
        lines[i] = line.replace('counts = {}', 'counts: Dict[str, int] = {}')
    elif line.strip() == 'tag_counts = {}' and "def _get_top_tags" in "\n".join(lines[max(0, i-5):i]):
        lines[i] = line.replace('tag_counts = {}', 'tag_counts: Dict[str, int] = {}')
    elif "def score_response(self, query: str, response: str, sources: List[Dict] = None)" in line:
        lines[i] = line.replace('sources: List[Dict] = None', 'sources: List[Dict] | None = None')
        # We also need to inject sources = sources or []
        lines.insert(i+1, "        sources = sources or []")
    elif "self.total_tokens_used += message[\"tokens\"]" in line:
        lines[i] = line.replace('message["tokens"]', 'int(message["tokens"])')
    elif "def _estimate_tokens(self, text: str) -> int:" in line:
        # Instead of replacing signature, we will cast the return
        pass
    elif "return len(text.split()) * 1.3" in line:
        lines[i] = line.replace("return len", "return int(len") + ")"

file_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("Done fixing competitive_kit")
