import re

with open("pyproject.toml", encoding="utf-8") as f:
    content = f.read()
content = re.sub(
    r"pydantic-extra-types\s*=\s*\".*?\"", 'pydantic-extra-types = "*"', content
)
with open("pyproject.toml", "w", encoding="utf-8") as f:
    f.write(content)
