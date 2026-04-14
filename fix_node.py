import os, glob

for f in glob.glob('.github/workflows/*.yml'):
    with open(f, 'r', encoding='utf-8') as file:
        content = file.read()
    content = content.replace("NODE_VERSION: '24'", "NODE_VERSION: '20'")
    content = content.replace("node-version: '24'", "node-version: '20'")
    content = content.replace("FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true", "FORCE_JAVASCRIPT_ACTIONS_TO_NODE20: true")
    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)
