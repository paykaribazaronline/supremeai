import re
import sys
import os

content = open('build/reports/jacoco/test/jacocoTestReport.xml', encoding='utf-8').read()
matches = re.findall(r'type="LINE" missed="(\d+)" covered="(\d+)"', content)
m, c = int(matches[-1][0]), int(matches[-1][1])
pct = round(c / (m + c) * 100, 1)
print(f"Current Line coverage: {pct}%  ({c} covered / {m+c} total)")

state_file = 'scripts/tools/.last_coverage'
prev_pct = 0.0

if os.path.exists(state_file):
    try:
        prev_pct = float(open(state_file).read().strip())
    except ValueError:
        pass

print(f"Previous Line coverage: {prev_pct}%")

if pct >= 100.0:
    print("🎉 100% target reached: PASS")
    open(state_file, 'w').write(str(pct))
    sys.exit(0)

if pct >= prev_pct:
    print(f"✅ Progress check: PASS (Maintained or increased: {prev_pct}% -> {pct}%)")
    open(state_file, 'w').write(str(pct))
    sys.exit(0)
else:
    print(f"🚨 Progress check: FAIL (Coverage dropped! Was {prev_pct}%, now {pct}%)")
    sys.exit(1)
