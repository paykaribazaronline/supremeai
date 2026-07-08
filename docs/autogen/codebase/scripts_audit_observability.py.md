# 📄 ফাইল: scripts/audit_observability.py

**প্রকার:** .py  
**সাইজ:** 2,813 বাইট  
**আপডেট:** 2026-07-08T12:17:29.831032

---

## কোড

```py
import ast
import json
import os
from pathlib import Path

def audit_directory(base_dir: str):
    report = {
        "silent_exceptions": [],
        "print_statements": []
    }
    
    base_path = Path(base_dir)
    for filepath in base_path.rglob("*.py"):
        if "venv" in str(filepath) or ".venv" in str(filepath):
            continue
            
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
                
            tree = ast.parse(content, filename=str(filepath))
            
            for node in ast.walk(tree):
                # Check for print statements
                if isinstance(node, ast.Call):
                    if isinstance(node.func, ast.Name) and node.func.id == "print":
                        report["print_statements"].append({
                            "file": str(filepath.relative_to(base_path)),
                            "line": node.lineno
                        })
                        
                # Check for silent exceptions (except: pass)
                elif isinstance(node, ast.Try):
                    for handler in node.handlers:
                        # handler.body is a list of statements
                        # Check if the only statement is 'pass'
                        if len(handler.body) == 1 and isinstance(handler.body[0], ast.Pass):
                            # It's a silent except
                            error_type = "Exception"
                            if handler.type:
                                if isinstance(handler.type, ast.Name):
                                    error_type = handler.type.id
                                elif isinstance(handler.type, ast.Attribute):
                                    error_type = handler.type.attr
                            report["silent_exceptions"].append({
                                "file": str(filepath.relative_to(base_path)),
                                "line": handler.lineno,
                                "type": error_type
                            })
                            
        except Exception as e:
            print(f"Failed to parse {filepath}: {e}")
            
    return report

if __name__ == "__main__":
    backend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "backend"))
    report_data = audit_directory(backend_dir)
    
    report_path = os.path.join(os.path.dirname(__file__), "observability_report.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report_data, f, indent=4)
        
    print(f"Audit completed. Found {len(report_data['silent_exceptions'])} silent exceptions and {len(report_data['print_statements'])} print statements.")
    print(f"Report saved to {report_path}")

```