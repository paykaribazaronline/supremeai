import os, re

regexes = [
    re.compile(r'os\.getenv\([\'\"]([A-Z0-9_]+)[\'\"]\)', re.IGNORECASE),
    re.compile(r'os\.environ\.get\([\'\"]([A-Z0-9_]+)[\'\"]\)', re.IGNORECASE),
    re.compile(r'os\.environ\[[\'\"]([A-Z0-9_]+)[\'\"]\]', re.IGNORECASE),
    re.compile(r'process\.env\.([A-Z0-9_]+)', re.IGNORECASE),
    re.compile(r'import\.meta\.env\.([A-Z0-9_]+)', re.IGNORECASE),
]

found_keys = set()
for root, dirs, files in os.walk('.'):
    if '.git' in root or 'node_modules' in root or 'venv' in root:
        continue
    for f in files:
        if f.endswith(('.py', '.js', '.ts', '.tsx', '.jsx', '.yaml', '.yml')):
            path = os.path.join(root, f)
            try:
                with open(path, 'r', encoding='utf-8') as file:
                    content = file.read()
                    for r in regexes:
                        for match in r.findall(content):
                            found_keys.add(match)
            except Exception:
                pass

for k in sorted(list(found_keys)):
    print(k)
