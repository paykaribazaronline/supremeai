import base64
import os


def w(p, c):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w", encoding="utf-8").write(c)
    print(f"OK: {os.path.basename(p)}")


print("Generator ready")
