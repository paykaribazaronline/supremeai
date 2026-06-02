import re
import sys
import os

content = open('build/reports/jacoco/test/jacocoTestReport.xml', encoding='utf-8').read()
matches = re.findall(r'type="LINE" missed="(\d+)" covered="(\d+)"', content)
m, c = int(matches[-1][0]), int(matches[-1][1])
pct = round(c / (m + c) * 100, 1)
print(f"Current Line coverage: {pct}%  ({c} covered / {m+c} total)")

prev_pct = 0.0

if "PREVIOUS_COVERAGE_PERCENTAGE" in os.environ:
    try:
        prev_pct = float(os.environ["PREVIOUS_COVERAGE_PERCENTAGE"])
    except ValueError:
        print("Warning: Could not parse PREVIOUS_COVERAGE_PERCENTAGE environment variable.")

print(f"Previous Line coverage target: {prev_pct}%")

temp_coverage_file = 'new_coverage_percentage.txt'
with open(temp_coverage_file, 'w') as f:
    f.write(str(pct))
print(f"New coverage percentage {pct}% saved to {temp_coverage_file}")

if pct >= 100.0:
    print("🎉 100% target reached: PASS")
    sys.exit(0)

if pct >= prev_pct:
    print(f"✅ Progress check: PASS (Maintained or increased: {prev_pct}% -> {pct}%)")
    sys.exit(0)
else:
    print(f"🚨 Progress check: FAIL (Coverage dropped! Was {prev_pct}%, now {pct}%. You must maintain or increase coverage.)")
    sys.exit(1)
