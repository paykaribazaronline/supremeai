import re

filepath = '.github/workflows/supreme-core-ci.yml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Update deploy-frontend-prod if condition
content = re.sub(
    r"(deploy-frontend-prod:.*?if: \|\n.*?always\(\) && \n.*?github.ref == 'refs/heads/main' &&\n.*?needs.frontend-core.result != 'failure' && needs.frontend-core.result != 'cancelled')",
    r"\1 && needs.frontend-core.result != 'skipped'",
    content,
    flags=re.DOTALL
)

# Update deploy-to-vercel if condition
content = re.sub(
    r"(deploy-to-vercel:.*?if: \|\n.*?always\(\) && \n.*?github.ref == 'refs/heads/main' &&\n.*?needs.frontend-core.result != 'failure' && needs.frontend-core.result != 'cancelled')",
    r"\1 && needs.frontend-core.result != 'skipped'",
    content,
    flags=re.DOTALL
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Deploy fixes applied")
