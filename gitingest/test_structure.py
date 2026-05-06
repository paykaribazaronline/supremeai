"""
Test script to verify Gitingest code structure
Run with: python3 test_structure.py
"""
import sys
import os

base_dir = "/home/nazifarabbu/OneDrive/supremeai/gitingest"

def test_file_exists(filepath, description):
    """Test if a file exists"""
    if os.path.exists(filepath):
        print(f"✓ {description}: {filepath}")
        return True
    else:
        print(f"✗ Missing: {filepath}")
        return False

def test_python_syntax(filepath):
    """Test if a Python file has valid syntax"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            compile(f.read(), filepath, 'exec')
        print(f"  ✓ Syntax OK: {os.path.basename(filepath)}")
        return True
    except SyntaxError as e:
        print(f"  ✗ Syntax Error in {os.path.basename(filepath)}: {e}")
        return False
    except Exception as e:
        print(f"  ? Could not check {os.path.basename(filepath)}: {e}")
        return False

print("=" * 60)
print("Gitingest Code Structure Test")
print("=" * 60)
print()

files = [
    ("__init__.py", "Package init"),
    ("config.py", "Configuration"),
    ("models.py", "Pydantic models"),
    ("cli.py", "CLI entry point"),
    ("core/__init__.py", "Core module init"),
    ("core/parser.py", "URL parser"),
    ("core/cloner.py", "Git cloner"),
    ("core/ignore.py", "Ignore patterns"),
    ("core/walker.py", "Directory walker"),
    ("core/formatter.py", "Output formatter"),
    ("core/reader.py", "File reader"),
    ("api/main.py", "FastAPI server"),
    ("cache/__init__.py", "Cache module"),
    ("utils/__init__.py", "Utils module"),
    ("ui/templates/base.html", "Base template"),
    ("ui/templates/index.html", "Index template"),
    ("pyproject.toml", "Package config"),
    ("README.md", "README"),
    (".env.example", "Env example"),
]

all_passed = True
for filepath, desc in files:
    full_path = os.path.join(base_dir, filepath)
    if not test_file_exists(full_path, desc):
        all_passed = False

print()
print("Testing Python syntax...")
print()

# Test all Python files
python_files = []
for root, dirs, files in os.walk(base_dir):
    if 'node_modules' in root or 'venv' in root or '.git' in root:
        continue
    for file in files:
        if file.endswith('.py'):
            python_files.append(os.path.join(root, file))

passed = 0
failed = 0
for py_file in python_files:
    if test_python_syntax(py_file):
        passed += 1
    else:
        failed += 1

print()
print("=" * 60)
print(f"Syntax Test Results: {passed} passed, {failed} failed")
print("=" * 60)

if failed > 0:
    sys.exit(1)
else:
    print("All syntax checks passed!")
    sys.exit(0)
