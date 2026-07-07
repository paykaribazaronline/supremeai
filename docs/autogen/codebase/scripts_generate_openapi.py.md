# 📄 ফাইল: scripts/generate_openapi.py

**প্রকার:** .py  
**সাইজ:** 805 বাইট  
**আপডেট:** 2026-07-07T22:11:19.719499

---

## কোড

```py
#!/usr/bin/env python3
"""
SupremeAI - OpenAPI Schema Extractor
This script extracts the OpenAPI schema from the FastAPI app and writes it to API-swagger.yaml.
"""
import sys
import yaml
import json
from pathlib import Path

# Add backend directory to path so we can import the app
sys.path.append('backend')

try:
    from main import app
except ImportError as e:
    print(f"Failed to import FastAPI app from backend.main: {e}")
    sys.exit(1)

def generate_openapi():
    openapi_schema = app.openapi()
    
    output_path = Path("API-swagger.yaml")
    with open(output_path, "w", encoding="utf-8") as f:
        yaml.dump(openapi_schema, f, sort_keys=False)
        
    print(f"Successfully generated OpenAPI schema at {output_path.absolute()}")

if __name__ == "__main__":
    generate_openapi()

```