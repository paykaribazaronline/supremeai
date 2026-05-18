import os, re

debt_file = 'docs/status/TECHNICAL_DEBT.md'
files = []
with open(debt_file, 'r') as f:
    for line in f:
        line = line.strip()
        if line and not line.startswith('We need'):
            files.append(line)

results = {}
pattern = re.compile(r'(TODO|FIXME|hardcoded|hard-coded|mock|dummy|placeholder)', re.IGNORECASE)

for file in files:
    if os.path.exists(file):
        try:
            with open(file, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()
                for i, line in enumerate(lines):
                    if pattern.search(line):
                        if file not in results:
                            results[file] = []
                        results[file].append('Line ' + str(i+1) + ': ' + line.strip())
        except Exception as e:
            pass

report_path = 'docs/status/TECHNICAL_DEBT_DETAILED.md'
with open(report_path, 'w', encoding='utf-8') as out:
    out.write('# Technical Debt & Placeholders Report\n\n')
    out.write('This document lists all the features or implementations in the code that are currently mocked, hardcoded, or marked as TODO/FIXME, which users or admins might face issues with if they try to use them in production.\n\n')
    
    for file, matches in results.items():
        out.write('## File: `' + file + '`\n')
        for match in matches:
            out.write('- ' + match + '\n')
        out.write('\n')

print('Report generated at ' + report_path)
