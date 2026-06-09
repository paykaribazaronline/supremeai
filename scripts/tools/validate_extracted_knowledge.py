#!/usr/bin/env python3
"""
Knowledge Security Validator
Scans extracted git knowledge for PII, secrets, and high-entropy strings.
"""

import json
import re
import math
import os
import sys

INPUT_FILE = "scripts/tools/.extracted_git_knowledge.json"

# Patterns for common sensitive data
PATTERNS = {
    "email": r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}',
    "secret_assignment": r'(?:api_key|apikey|secret|token|password|passwd|pwd|private_key)\s*[:=]\s*["\'][a-zA-Z0-9._%-]{8,}["\']',
    "bearer_token": r'Bearer\s+[a-zA-Z0-9._-]+',
    "env_secret": r'export\s+[A-Z_]+_SECRET\s*=',
}

def calculate_entropy(s):
    """Calculates the Shannon entropy of a string to detect potential keys."""
    if not s or len(s) < 8: return 0
    prob = [float(s.count(c)) / len(s) for c in dict.fromkeys(list(s))]
    return - sum([p * math.log(p, 2) for p in prob])

def validate():
    if not os.path.exists(INPUT_FILE):
        print(f"⚠️  Input file {INPUT_FILE} not found. Skipping validation.")
        return

    with open(INPUT_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)

    vulnerabilities = []
    print(f"🛡️  Scanning {len(data)} knowledge entries for vulnerabilities...")

    for entry in data:
        content = entry.get("content", "")
        entry_id = entry.get("id", "unknown")
        
        # 1. Regex Pattern Matching
        for name, pattern in PATTERNS.items():
            matches = re.findall(pattern, content, re.IGNORECASE)
            if matches:
                vulnerabilities.append({
                    "id": entry_id,
                    "type": name,
                    "snippet": matches[0][:50] + "..."
                })

        # 2. Entropy Analysis for long alphanumeric strings
        words = re.findall(r'[a-zA-Z0-9]{20,}', content)
        for word in words:
            if calculate_entropy(word) > 3.8:
                vulnerabilities.append({
                    "id": entry_id,
                    "type": "high_entropy_string",
                    "snippet": word[:10] + "..." + word[-5:]
                })

    if vulnerabilities:
        print(f"❌ Found {len(vulnerabilities)} potential security issues:")
        for v in vulnerabilities:
            print(f"  - [{v['type']}] in Entry {v['id']}: {v['snippet']}")
        
        print("\n🚨 Action Required: Sanitize the git history or the extracted JSON before ingestion.")
        # Create a sanitized version
        sanitized_file = INPUT_FILE + ".sanitized"
        sanitized_data = [e for e in data if e['id'] not in [v['id'] for v in vulnerabilities]]
        
        with open(sanitized_file, 'w', encoding='utf-8') as f:
            json.dump(sanitized_data, f, indent=2)
        
        print(f"✅ Created sanitized version: {sanitized_file}")
        sys.exit(1)
    else:
        print("✅ No security vulnerabilities detected in the extracted knowledge.")
        sys.exit(0)

if __name__ == "__main__":
    validate()