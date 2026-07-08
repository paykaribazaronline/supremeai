# 📄 ফাইল: coverage.toml

**প্রকার:** .toml  
**সাইজ:** 534 বাইট  
**আপডেট:** 2026-07-08T09:53:36.716707

---

## কোড

```toml
[tool.coverage.run]
branch = true
omit = [
    "*/site-packages/*",
    "*/distutils/*",
    "*/tests/*",
    "*/__pycache__/*"
]

[tool.coverage.report]
exclude_lines = [
    "pragma: no cover",
    "def __repr__",
    "if self.debug",
    "if settings.DEBUG",
    "raise AssertionError",
    "raise NotImplementedError",
    "if 0:",
    "if __name__ == .__main__.",
    "if TYPE_CHECKING:",
    "@abstractmethod",
    "@abstractproperty"
]

[tool.coverage.html]
directory = "htmlcov"

[tool.coverage.json]
output = "coverage.json"

```