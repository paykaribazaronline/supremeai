#!/usr/bin/env python3
import os
import re

controllers_dir = "/home/nazifarabbu/supremeai/src/main/java/com/supremeai/controller"

print("🔍 Starting Auto-Audit of @RestController DTO Validation (S-02)...")
print("=" * 80)

missing_valid_count = 0
total_request_bodies = 0
controller_count = 0

for root, _, files in os.walk(controllers_dir):
    for file in files:
        if file.endswith(".java"):
            controller_count += 1
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()

            # Find all method definitions containing @RequestBody
            lines = content.split("\n")
            for i, line in enumerate(lines):
                if "@RequestBody" in line:
                    # Ignore standard generic maps/lists/strings where field validation is not possible
                    if any(
                        x in line
                        for x in ["Map<", "String ", "String>", "List<", "Object "]
                    ):
                        continue

                    total_request_bodies += 1

                    # We will reconstruct the parameter context (current line + 2 lines around it)
                    context_lines = lines[max(0, i - 1) : min(len(lines), i + 3)]
                    context = " ".join(context_lines)

                    if "@Valid" not in context:
                        missing_valid_count += 1
                        method_name = "Unknown"

                        # Simple extraction of method name
                        for c_line in lines[max(0, i - 2) : min(len(lines), i + 2)]:
                            match = re.search(
                                r"(public|protected|private)\s+[\w<>]+\s+(\w+)\s*\(",
                                c_line,
                            )
                            if match:
                                method_name = match.group(2)
                                break

                        print(f"⚠️ Custom DTO Validation Missing in [ {file} ]")
                        print(f"   Method: {method_name}() on line {i+1}")
                        print(f"   Code snippet: {line.strip()}")
                        print("-" * 80)

print("=" * 80)
print(f"📊 Custom DTO Audit Summary:")
print(f"   - Total Controllers Scanned: {controller_count}")
print(f"   - Custom DTOs as Request Bodies: {total_request_bodies}")
print(f"   - Missing @Valid Annotations: {missing_valid_count}")
if total_request_bodies > 0:
    coverage = (
        (total_request_bodies - missing_valid_count) / total_request_bodies
    ) * 100
    print(f"   - Custom DTO Validation Coverage: {coverage:.2f}%")
else:
    print(f"   - Custom DTO Validation Coverage: 100.00%")
print("=" * 80)
