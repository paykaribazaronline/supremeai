#!/usr/bin/env python3
# scripts/audit_observability.py
# বাংলা মন্তব্য: এই স্ক্রিপ্টটি কোডবেসে (বিশেষ করে backend ডিরেক্টরিতে) কোনো সাইলেন্ট exception
# (যেমন except Exception: pass) অথবা প্রিন্ট স্টেটমেন্ট (print) আছে কিনা তা static analysis এর মাধ্যমে
# চেক করে। যদি পাওয়া যায় তবে বিল্ড ফেইল (exit code 1) করায়।

import ast
import sys
import os
from pathlib import Path

# Force UTF-8 stdout encoding where supported (e.g., Windows console)
if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

def safe_print(msg: str):
    try:
        print(msg)
    except Exception:
        # Fallback to writing bytes directly to stdout if encoding fails
        try:
            sys.stdout.buffer.write((msg + '\n').encode('utf-8', errors='replace'))
            sys.stdout.buffer.flush()
        except Exception:
            # Absolute fallback: strip non-ASCII
            clean_msg = "".join(c if ord(c) < 128 else '?' for c in msg)
            print(clean_msg)

class SilentErrorDetector(ast.NodeVisitor):
    def __init__(self, filepath: str):
        self.filepath = filepath
        self.violations: list[str] = []
        self.current_function: str | None = None
        self.in_main_block = False

    def visit_FunctionDef(self, node: ast.FunctionDef):
        old_function = self.current_function
        self.current_function = node.name
        self.generic_visit(node)
        self.current_function = old_function

    def visit_AsyncFunctionDef(self, node: ast.AsyncFunctionDef):
        old_function = self.current_function
        self.current_function = node.name
        self.generic_visit(node)
        self.current_function = old_function

    def visit_If(self, node: ast.If):
        # Detect: if __name__ == "__main__":
        is_main = False
        if isinstance(node.test, ast.Compare):
            if isinstance(node.test.left, ast.Name) and node.test.left.id == '__name__':
                if len(node.test.comparators) == 1 and isinstance(node.test.comparators[0], ast.Constant):
                    if node.test.comparators[0].value == '__main__':
                        is_main = True

        old_in_main = self.in_main_block
        if is_main:
            self.in_main_block = True
        self.generic_visit(node)
        self.in_main_block = old_in_main

    def visit_ExceptHandler(self, node: ast.ExceptHandler):
        # Check strictly for `except Exception` or bare `except:`
        if node.type is None or (isinstance(node.type, ast.Name) and node.type.id == 'Exception'):
            # Check if body is strictly silent (pass, ellipsis, or empty)
            is_silent = all(
                isinstance(stmt, ast.Pass) or
                (isinstance(stmt, ast.Constant) and stmt.value is ...) or
                (isinstance(stmt, ast.Expr) and isinstance(stmt.value, ast.Constant) and stmt.value is ...)
                for stmt in node.body
            )

            # Check for silent return True or masked success without logging
            has_unlogged_return_success = False
            has_logger_call = any(
                isinstance(stmt, ast.Expr) and isinstance(stmt.value, ast.Call) and (
                    (isinstance(stmt.value.func, ast.Attribute) and 'log' in stmt.value.func.attr.lower()) or
                    (isinstance(stmt.value.func, ast.Name) and 'log' in stmt.value.func.id.lower())
                )
                for stmt in node.body
            )
            
            if not has_logger_call:
                for stmt in node.body:
                    if isinstance(stmt, ast.Return) and isinstance(stmt.value, ast.Constant) and stmt.value is True:
                        has_unlogged_return_success = True
                        break

            normalized_path = self.filepath.replace('\\', '/')
            is_test_file = (
                '/tests/' in normalized_path or
                normalized_path.endswith('conftest.py') or
                '/test_' in normalized_path
            )
            if not is_test_file:
                if is_silent:
                    self.violations.append(f"{self.filepath}:{node.lineno} - Silent exception handler (`except Exception: pass`)")
                elif has_unlogged_return_success:
                    self.violations.append(f"{self.filepath}:{node.lineno} - Masked success exception handler (`except Exception: return True` without logging)")
        self.generic_visit(node)

    def visit_Call(self, node: ast.Call):
        # Flag unsafe print() calls in backend
        if isinstance(node.func, ast.Name) and node.func.id == 'print':
            # Allow prints in scripts/, tests/, and scratch/ directories
            # Normalize path delimiters for Windows vs Unix compatibility
            normalized_path = self.filepath.replace('\\', '/')
            if 'backend/' in normalized_path and 'scripts/' not in normalized_path and 'tests/' not in normalized_path and 'scratch/' not in normalized_path:
                # Allow prints in demo functions and inside if __name__ == "__main__":
                if self.in_main_block or (self.current_function and any(x in self.current_function.lower() for x in ('demo', 'sample', 'simulate', 'test'))):
                    pass
                else:
                    self.violations.append(f"{self.filepath}:{node.lineno} - Unsafe `print()` statement in backend logic")
        self.generic_visit(node)

def run_audit():
    # Find the backend directory relative to this script directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, ".."))
    backend_path = Path(os.path.join(project_root, "backend"))

    total_violations = 0

    safe_print("🔍 Running Observability & Silent Error Audit...")
    for py_file in backend_path.rglob("*.py"):
        # Skip virtual env directories if present inside project root
        if ".venv" in py_file.parts or "venv" in py_file.parts:
            continue
        try:
            tree = ast.parse(py_file.read_text(encoding='utf-8'))
            detector = SilentErrorDetector(str(py_file))
            detector.visit(tree)
            for v in detector.violations:
                safe_print(f"❌ FAIL: {v}")
                total_violations += 1
        except Exception as e:
            safe_print(f"⚠️ Could not parse {py_file}: {e}")

    if total_violations > 0:
        safe_print(f"\n🚨 Audit Failed: {total_violations} violations found.")
        sys.exit(1)
    else:
        safe_print("\n✅ Audit Passed: Zero silent exceptions or unsafe prints detected.")
        sys.exit(0)

if __name__ == "__main__":
    run_audit()
