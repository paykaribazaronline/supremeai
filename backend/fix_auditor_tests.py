import re


filepath = "tests/core/test_container_auditor.py"
with open(filepath, encoding="utf-8") as f:
    content = f.read()

# Remove init arguments in fixture
content = content.replace("ContainerAuditor(check_interval_seconds=1)", "ContainerAuditor()")
content = content.replace("ContainerAuditor(check_interval_seconds=10)", "ContainerAuditor()")

# Remove TestContainerAuditorInit class
pattern_init = re.compile(r"class TestContainerAuditorInit:[\s\S]*?(?=# -------------------- Tests: get_container_stats --------------------)")
content = pattern_init.sub("", content)

# Remove TestRun class
pattern_run = re.compile(r"class TestRun:[\s\S]*?(?=# -------------------- Tests: Integration --------------------)")
content = pattern_run.sub("", content)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("test_container_auditor.py fixed.")
