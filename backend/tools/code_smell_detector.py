#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# file >> code_smell_detector.py
# project >> SupremeAI 2.0
# purpose >> Code analysis
# module >> tools
# ============================================================================
import ast
import os
import subprocess
import tempfile
from typing import Dict, Any, List, Optional
from loguru import logger


class CodeSmellDetector:
    """
    Static analysis tool to detect cyclomatic complexity, code duplication, and other smells.
    Closes Gap #23
    """

    def __init__(self):
        self.radon_available = self._check_radon()
        self.pylint_available = self._check_pylint()
        logger.info(f"CodeSmellDetector initialized (radon={self.radon_available}, pylint={self.pylint_available})")

    def _check_radon(self) -> bool:
        try:
            import radon.complexity  # noqa: F401
            return True
        except ImportError:
            return False

    def _check_pylint(self) -> bool:
        try:
            subprocess.run(["pylint", "--version"], capture_output=True, check=False)
            return True
        except FileNotFoundError:
            return False

    def _calculate_complexity(self, node: ast.AST) -> int:
        complexity = 1
        for child in ast.walk(node):
            if isinstance(child, (ast.If, ast.IfExp, ast.For, ast.While, ast.ExceptHandler, ast.With, ast.Assert, ast.BoolOp)):
                complexity += 1
            if isinstance(child, ast.BoolOp):
                complexity += len(child.values) - 1
        return complexity

    def analyze_python_file(self, filepath: str, thresholds: Optional[Dict[str, int]] = None) -> List[Dict[str, Any]]:
        if not os.path.exists(filepath):
            return []

        t = thresholds or {}
        complexity_threshold = t.get("complexity", 10)
        args_threshold = t.get("args", 5)
        lines_threshold = t.get("lines", 50)
        class_methods_threshold = t.get("class_methods", 20)

        logger.info(f"Analyzing {filepath} for smells...")
        smells: List[Dict[str, Any]] = []

        try:
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
                lines = content.splitlines()

            tree = ast.parse(content)

            for node in ast.walk(tree):
                if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                    complexity = self._calculate_complexity(node)
                    if complexity > complexity_threshold:
                        smells.append({
                            "type": "High Cyclomatic Complexity",
                            "line": node.lineno,
                            "end_line": getattr(node, "end_lineno", node.lineno),
                            "function": node.name,
                            "complexity": complexity,
                            "threshold": complexity_threshold,
                            "message": (
                                f"Cyclomatic complexity is {complexity} (threshold: {complexity_threshold}). "
                                "Consider extracting helper functions."
                            ),
                            "severity": "warning",
                        })

                    if len(node.args.args) > args_threshold:
                        smells.append({
                            "type": "Too Many Arguments",
                            "line": node.lineno,
                            "end_line": getattr(node, "end_lineno", node.lineno),
                            "function": node.name,
                            "count": len(node.args.args),
                            "threshold": args_threshold,
                            "message": f"Function takes {len(node.args.args)} arguments (threshold: {args_threshold}).",
                            "severity": "info",
                        })

                    start = node.lineno
                    end = getattr(node, "end_lineno", start)
                    src_lines = end - start + 1 if end >= start else 1
                    if src_lines > lines_threshold:
                        smells.append({
                            "type": "Long Method",
                            "line": start,
                            "end_line": end,
                            "function": node.name,
                            "lines": src_lines,
                            "threshold": lines_threshold,
                            "message": f"Method spans {src_lines} lines (threshold: {lines_threshold}).",
                            "severity": "info",
                        })

                    return_count = sum(1 for child in ast.walk(node) if isinstance(child, ast.Return))
                    if return_count > 7:
                        smells.append({
                            "type": "Too Many Returns",
                            "line": node.lineno,
                            "end_line": getattr(node, "end_lineno", node.lineno),
                            "function": node.name,
                            "return_count": return_count,
                            "message": f"Function has {return_count} return statements. Consider using guard clauses.",
                            "severity": "info",
                        })

                if isinstance(node, ast.ClassDef):
                    methods = sum(1 for child in ast.iter_child_nodes(node) if isinstance(child, (ast.FunctionDef, ast.AsyncFunctionDef)))
                    if methods > class_methods_threshold:
                        smells.append({
                            "type": "Large Class",
                            "line": node.lineno,
                            "end_line": getattr(node, "end_lineno", node.lineno),
                            "class": node.name,
                            "method_count": methods,
                            "threshold": class_methods_threshold,
                            "message": f"Class has {methods} methods (threshold: {class_methods_threshold}).",
                            "severity": "info",
                        })

            smells.extend(self._detect_duplicate_functions(tree, filepath))

        except SyntaxError as e:
            smells.append({
                "type": "Syntax Error",
                "line": e.lineno or 0,
                "message": str(e.msg),
                "severity": "critical",
            })
        except Exception as e:
            logger.error(f"Failed to analyze {filepath}: {e}")

        if self.radon_available:
            try:
                smells.extend(self._analyze_radon(filepath, tree, complexity_threshold))
            except Exception as e:
                logger.warning(f"Radon analysis failed for {filepath}: {e}")

            coupling = self.compute_coupling_metrics(tree, filepath)
            if coupling.get("unique_modules", 0) > 15:
                smells.append({
                    "type": "High Coupling",
                    "line": 1,
                    "message": (
                        f"Module imports {coupling['unique_modules']} unique packages "
                        f"(fan_out={coupling['fan_out']}). Consider facade/wrapper layers."
                    ),
                    "severity": "warning",
                    "coupling": coupling,
                })

        return smells

    def _detect_duplicate_functions(self, tree: ast.AST, filepath: str) -> List[Dict[str, Any]]:
        smells: List[Dict[str, Any]] = []
        bodies: Dict[str, List[Dict[str, Any]]] = {}
        for node in ast.walk(tree):
            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                src = ast.dump(node.body)
                norm = self._normalize(src)
                bodies.setdefault(norm, []).append({
                    "name": node.name,
                    "line": node.lineno,
                    "end_line": getattr(node, "end_lineno", node.lineno),
                })
        for norm, items in bodies.items():
            if len(items) > 1:
                smells.append({
                    "type": "Duplicate Code",
                    "line": items[0]["line"],
                    "end_line": items[0]["end_line"],
                    "function": items[0]["name"],
                    "instances": len(items),
                    "locations": [(i["name"], i["line"]) for i in items],
                    "message": f"Potential duplicate logic detected in {len(items)} locations.",
                    "severity": "warning",
                })
        return smells

    def _normalize(self, dump: str) -> str:
        import re
        dump = re.sub(r"\s+", " ", dump)
        dump = re.sub(r"'([^']*)'", "'<str>'", dump)
        dump = re.sub(r"\d+", "0", dump)
        return dump

    def _analyze_radon(self, filepath: str, tree: Optional[ast.AST], threshold: int) -> List[Dict[str, Any]]:
        try:
            from radon.complexity import cc_visit
            from radon.metrics import mi_visit
            if tree is None:
                with open(filepath, "r", encoding="utf-8") as f:
                    tree = ast.parse(f.read())
            results: List[Dict[str, Any]] = []
            blocks = cc_visit(tree)
            for block in blocks:
                if block.complexity > threshold:
                    results.append({
                        "type": "High Complexity (radon)",
                        "line": block.lineno,
                        "end_line": block.endline,
                        "function": block.name,
                        "complexity": block.complexity,
                        "message": f"Radon complexity {block.complexity} (threshold: {threshold}).",
                        "severity": "warning",
                    })
            try:
                mi = mi_visit(tree, True)
                if mi < 50:
                    results.append({
                        "type": "Low Maintainability",
                        "line": 1,
                        "message": f"Maintainability index is {mi:.1f} (target: > 50).",
                        "severity": "warning",
                    })
            except Exception:
                pass
            return results
        except ImportError:
            return []
        except SyntaxError:
            return []

    def analyze_directory(self, directory_path: str, thresholds: Optional[Dict[str, int]] = None) -> Dict[str, List[Dict[str, Any]]]:
        results: Dict[str, List[Dict[str, Any]]] = {}
        if not os.path.isdir(directory_path):
            return results
        for root, _, files in os.walk(directory_path):
            for file in files:
                if file.endswith(".py"):
                    full_path = os.path.join(root, file)
                    smells = self.analyze_python_file(full_path, thresholds=thresholds)
                    if smells:
                        results[full_path] = smells
                elif file.endswith((".js", ".ts", ".jsx", ".tsx")):
                    full_path = os.path.join(root, file)
                    smells = self.analyze_js_ts_file(full_path, thresholds=thresholds)
                    if smells:
                        results[full_path] = smells
        if self.pylint_available:
            try:
                results.update(self._analyze_pylint_directory(directory_path))
            except Exception as e:
                logger.warning(f"Pylint directory analysis failed: {e}")

        jscpd_report = self.run_jscpd(directory_path)
        if jscpd_report.get("status") == "success" and jscpd_report.get("duplicates"):
            results.setdefault("_jscpd_", []).append(jscpd_report)

        return results

    def analyze_js_ts_file(self, filepath: str, thresholds: Optional[Dict[str, int]] = None) -> List[Dict[str, Any]]:
        if not os.path.exists(filepath):
            return []
        smells: List[Dict[str, Any]] = []
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
                lines = content.splitlines()

            max_lines = thresholds.get("lines", 200) if thresholds else 200
            max_params = thresholds.get("args", 5) if thresholds else 5

            for i, line in enumerate(lines, start=1):
                stripped = line.strip()
                if len(stripped) > 200:
                    smells.append({
                        "type": "Long Line",
                        "line": i,
                        "message": f"Line length {len(stripped)} exceeds 200 characters.",
                        "severity": "info",
                    })

            func_count = 0
            long_funcs = 0
            big_param_funcs = 0
            current_func_lines = 0
            in_func = False
            brace_depth = 0
            for line in lines:
                stripped = line.strip()
                if "function" in stripped or "=>" in stripped:
                    if not in_func:
                        in_func = True
                        current_func_lines = 0
                        func_count += 1
                        if "(" in stripped:
                            params = stripped.split("(")[1].split(")")[0]
                            if len([p.strip() for p in params.split(",") if p.strip()]) > max_params and not stripped.startswith("//"):
                                big_param_funcs += 1
                if in_func:
                    current_func_lines += 1
                    if current_func_lines > max_lines:
                        long_funcs += 1
                        in_func = False
                    if stripped == "}" or (stripped == "});" and "{" not in stripped):
                        in_func = False
                        brace_depth = 0
                    else:
                        brace_depth += stripped.count("{") - stripped.count("}")
                        if brace_depth < 0:
                            brace_depth = 0
                        if brace_depth == 0 and stripped:
                            brace_depth = 0

            if long_funcs:
                smells.append({
                    "type": "Long Function",
                    "line": 1,
                    "message": f"{long_funcs} JS/TS functions exceed {max_lines} lines.",
                    "severity": "info",
                })
            if big_param_funcs:
                smells.append({
                    "type": "Too Many Parameters",
                    "line": 1,
                    "message": f"{big_param_funcs} functions exceed {max_params} parameters.",
                    "severity": "info",
                })

            if "eval(" in content or "Function(" in content:
                smells.append({
                    "type": "Dangerous Patterns",
                    "line": 1,
                    "message": "Use of eval() or Function() detected.",
                    "severity": "critical",
                })
        except Exception as e:
            logger.error(f"Failed to analyze JS/TS file {filepath}: {e}")
        return smells

    def _analyze_pylint_directory(self, directory_path: str) -> Dict[str, List[Dict[str, Any]]]:
        output: Dict[str, List[Dict[str, Any]]] = {}
        with tempfile.TemporaryDirectory() as tmpdir:
            rcfile = os.path.join(tmpdir, ".pylintrc")
            with open(rcfile, "w", encoding="utf-8") as f:
                f.write("[MASTER]\nload-plugins=\n")
            try:
                proc = subprocess.run(
                    ["pylint", "--output-format=json", f"--rcfile={rcfile}", directory_path],
                    capture_output=True,
                    text=True,
                    timeout=120,
                    check=False,
                )
                import json
                for item in json.loads(proc.stdout or "[]"):
                    path = item.get("path")
                    if not path:
                        continue
                    output.setdefault(path, []).append({
                        "type": item.get("symbol") or item.get("message-id", "pylint"),
                        "line": item.get("line", 0),
                        "message": item.get("message", ""),
                        "severity": "warning" if item.get("type") in ("convention", "refactor", "warning") else "critical",
                        "source": "pylint",
                    })
            except subprocess.TimeoutExpired:
                logger.warning("Pylint timed out")
            except Exception as e:
                logger.warning(f"Pylint execution failed: {e}")
        return output

    def compute_coupling_metrics(self, tree: ast.AST, filepath: str) -> Dict[str, Any]:
        """Compute fan-in/fan-out coupling metrics for a Python module."""
        imports: List[str] = []
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    imports.append(alias.name.split(".")[0])
            elif isinstance(node, ast.ImportFrom):
                if node.module:
                    imports.append(node.module.split(".")[0])
        fan_out = len(imports)
        unique_modules = len(set(imports))
        return {
            "file": filepath,
            "fan_out": fan_out,
            "unique_modules": unique_modules,
            "imports": imports,
            "severity": "info",
        }

    def run_jscpd(self, directory_path: str) -> Dict[str, Any]:
        """Run jscpd CLI to detect cross-file code duplication."""
        if not os.path.isdir(directory_path):
            return {"status": "skipped", "reason": "directory not found"}
        try:
            proc = subprocess.run(
                [
                    "jscpd",
                    directory_path,
                    "--silent",
                    "--format",
                    "json",
                    "--min-lines",
                    "5",
                    "--min-tokens",
                    "50",
                ],
                capture_output=True,
                text=True,
                timeout=120,
                check=False,
            )
            import json
            stdout = proc.stdout.strip()
            if not stdout:
                return {"status": "success", "duplicates": []}
            data = json.loads(stdout) if stdout.startswith("{") else {}
            return {
                "status": "success",
                "duplicates": data.get("duplicates", []),
                "statistics": data.get("statistics", {}),
            }
        except FileNotFoundError:
            logger.debug("jscpd not installed; skipping cross-file duplication check")
            return {"status": "skipped", "reason": "jscpd not found"}
        except Exception as exc:
            logger.warning(f"jscpd execution failed: {exc}")
            return {"status": "error", "reason": str(exc)}
