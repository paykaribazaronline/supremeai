# scripts/sync_github_secrets.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি কমান্ড লাইন আর্গুমেন্টে Secret না পাঠিয়ে নিরাপদ STDIN প্রসেস পাইপিংয়ের মাধ্যমে
# GitHub Actions Repository Secrets সেট করে, যাতে সিস্টেমে প্রসেস ট্র্যাকিং থেকে Secret লিকেজ রোধ করা যায়।

import os
import re
import subprocess
import sys

from dotenv import load_dotenv

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

load_dotenv(".env")

valid_key_pattern = re.compile(r"^[a-zA-Z_][a-zA-Z0-9_.-]*$")

env_vars = {}
with open(".env", "r", encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        k = k.strip()
        v = v.strip().strip('"').strip("'")
        if valid_key_pattern.match(k):
            env_vars[k] = v

print(
    f"Found {len(env_vars)} valid keys in .env. Updating GitHub Secrets securely via STDIN..."
)

env = os.environ.copy()
env.pop("GITHUB_TOKEN", None)

success = 0
failed = 0

for k, v in env_vars.items():
    if not v:
        continue
    # Secure stdin piping without passing secret values as CLI arguments
    p = subprocess.run(
        ["gh", "secret", "set", k, "--repo", "paykaribazaronline/supremeai"],
        input=v,
        text=True,
        capture_output=True,
        env=env,
    )
    if p.returncode == 0:
        success += 1
        print(f"[OK] Set GitHub Secret: {k}", flush=True)
    else:
        failed += 1
        print(f"[FAIL] Failed to set {k}: {p.stderr.strip()}", flush=True)

print(f"\nDone! Updated {success} secrets ({failed} failed).")
