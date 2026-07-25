import ast
import json
import logging
import os
import sys
from pathlib import Path
from typing import Any

logger = logging.getLogger(__name__)


def audit_directory(base_dir: str):
    base_path = Path(base_dir)
    report: dict[str, list[dict[str, Any]]] = {
        "silent_exceptions": [],
        "print_statements": [],
    }
    issues_found = False

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
                        report["print_statements"].append(
                            {
                                "file": str(filepath.relative_to(base_path)),
                                "line": node.lineno,
                                "severity": "INFO",
                            }
                        )

                # Check for silent or broad exceptions
                elif isinstance(node, ast.Try):
                    for handler in node.handlers:
                        is_broad_except = False
                        error_type = "Specific"
                        # Check for `except:`
                        if handler.type is None:
                            is_broad_except = True
                            error_type = "Bare Except"
                        # Check for `except Exception:` or `except BaseException:`
                        elif isinstance(handler.type, ast.Name) and handler.type.id in {
                            "Exception",
                            "BaseException",
                        }:
                            is_broad_except = True
                            error_type = handler.type.id

                        # Check if the handler body just passes or logs without re-raising
                        body_has_raise = any(
                            isinstance(item, ast.Raise) for item in handler.body
                        )

                        if is_broad_except and not body_has_raise:
                            issues_found = True
                            report["silent_exceptions"].append(
                                {
                                    "file": str(filepath.relative_to(base_path)),
                                    "line": handler.lineno,
                                    "type": error_type,
                                    "severity": "WARNING",
                                }
                            )

        except (SyntaxError, UnicodeDecodeError, OSError) as e:
            logger.warning(f"Failed to parse {filepath}: {e}")

    return report, issues_found


if __name__ == "__main__":
    backend_dir = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "backend")
    )
    report_data, issues_found = audit_directory(backend_dir)

    report_path = os.path.join(os.path.dirname(__file__), "observability_report.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report_data, f, indent=4)

    silent_count = len(report_data["silent_exceptions"])
    print_count = len(report_data["print_statements"])

    print(
        f"Audit completed. Found {silent_count} silent/broad exceptions and {print_count} print statements."
    )
    print(f"Report saved to {report_path}")

    if issues_found:
        print("\nFound broad exception handlers, but continuing CI check.")
        sys.exit(0)
