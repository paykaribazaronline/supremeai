import re
content = open('build/reports/jacoco/test/jacocoTestReport.xml', encoding='utf-8').read()
matches = re.findall(r'type="LINE" missed="(\d+)" covered="(\d+)"', content)
m, c = int(matches[-1][0]), int(matches[-1][1])
pct = round(c / (m + c) * 100, 1)
print(f"Line coverage: {pct}%  ({c} covered / {m+c} total)")
print(f"70% threshold: {'PASS' if pct >= 70 else 'FAIL - need more tests'}")
