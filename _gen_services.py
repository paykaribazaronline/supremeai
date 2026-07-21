import base64
import json
import os
import sys

base = r"c:\Users\n\supremeai\supremeai_2.0\backend\services"


def w(p, b):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w", encoding="utf-8").write(base64.b64decode(b).decode("utf-8"))
    print(f"Written: {os.path.basename(p)}")


files = json.loads(sys.argv[1])
for f, b in files.items():
    w(os.path.join(base, f), b)
print("Done")
