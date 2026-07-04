# 📜 SupremeAI 2.0 Centralized Changelog

# 📋 Auto-Fix Commit b4d7886c

## Commit Stats
```
commit b4d7886c6d700161a71e1c9e330eef8ebd85f11d
Author: SupremeAI Bot <bot@supremeai.dev>
Date:   2026-07-04 11:12:17 UTC

    fix: auto-fix applied for CI failure

    File: code_smell_detector.py
```

## 🤖 AI Auto-Fix Context

| Field | Value |
|-------|-------|
| **Fixed File** | `tools/code_smell_detector.py` |
| **AI Model Used** | `gemini/gemini-2.5-flash` |
| **Timestamp** | 2026-07-04 11:12:17 UTC |
| **Branch** | `main` |
| **Commit** | [`b4d7886c`](https://github.com/paykaribazaronline/supremeai/commit/b4d7886c6d700161a71e1c9e330eef8ebd85f11d) |

## Error Log (Truncated)
```
FFFFF....F....F.FFFFFFFFFFFFFFFF                                         [100%]
=================================== FAILURES ===================================
_____________________ test_parse_admin_
```

## Diff Detail
```diff
diff --git a/backend/tools/code_smell_detector.py b/backend/tools/code_smell_detector.py
index 48c2e6a..6a53494 100644
--- a/backend/tools/code_smell_detector.py
+++ b/backend/tools/code_smell_detector.py
@@ -1,3 +1,4 @@
+# FILE_PATH: tools/code_smell_detector.py
 import ast
 import os
 import subprocess
@@ -16,9 +17,7 @@ class CodeSmellDetector:
     def __init__(self):
         self.radon_available = self._check_radon()
         self.pylint_available = self._check_pylint()
-        logger.info(
-            f"CodeSmellDetector initialized (radon={self.radon_available}, pylint={self.pylint_available})"
-        )
+        logger.info(f"CodeSmellDetector initialized (radon={self.radon_available}, pylint={self.pylint_available})")
 
     def _check_radon(self) -> bool:
         try:
@@ -56,9 +55,7 @@ class CodeSmellDetector:
                 complexity += len(child.values) - 1
         return complexity
 
-    def analyze_python_file(
-        self, filepath: str, thresholds: dict[str, int] | None = None
-    ) -> list[dict[str, Any]]:
+    def analyze_python_file(self, filepath: str, thresholds: dict[str, int] | None = None) -> list[dict[str, Any]]:
         if not os.path.exists(filepath):
             return []
 
@@ -129,9 +126,7 @@ class CodeSmellDetector:
                             }
                         )
 
-                    return_count = sum(
-                        1 for child in ast.walk(node) if isinstance(child, ast.Return)
-                    )
+                    return_count = sum(1 for child in ast.walk(node) if isinstance(child, ast.Return))
                     if return_count > 7:
                         smells.append(
                             {
@@ -146,11 +141,7 @@ class CodeSmellDetector:
                         )
 
                 if isinstance(node, ast.ClassDef):
-                    methods = sum(
-                        1
-                        for child in ast.iter_child_nodes(node)
-                        if isinstance(child, (ast.FunctionDef, ast.AsyncFunctionDef))
-                    )
+                    methods = sum(1 for child in ast.iter_child_nodes(node) if isinstance(child, (ast.FunctionDef, ast.AsyncFunctionDef)))
                     if methods > class_methods_threshold:
                         smells.append(
                             {
@@ -181,32 +172,31 @@ class CodeSmellDetector:
         except Exception as e:
             logger.error(f"Failed to analyze {filepath}: {e}")
 
-        if self.radon_available:
-            try:
-                smells.extend(self._analyze_radon(filepath, tree, complexity_threshold))
-            except Exception as e:
-                logger.warning(f"Radon analysis failed for {filepath}: {e}")
+        if tree is not None:  # Added check for tree being not None
+            if self.radon_available:
+                try:
+                    smells.extend(self._analyze_radon(filepath, tree, complexity_threshold))
+                except Exception as e:
+                    logger.warning(f"Radon analysis failed for {filepath}: {e}")
 
-            coupling = self.compute_coupling_metrics(tree, filepath)
-            if coupling.get("unique_modules", 0) > 15:
-                smells.append(
-                    {
-                        "type": "High Coupling",
-                        "line": 1,
-                        "message": (
-                            f"Module imports {coupling['unique_modules']} unique packages "
-                            f"(fan_out={coupling['fan_out']}). Consider facade/wrapper layers."
-                        ),
-                        "severity": "warning",
-                        "coupling": coupling,
-                    }
-                )
+                coupling = self.compute_coupling_metrics(tree, filepath)
+                if coupling.get("unique_modules", 0) > 15:
+                    smells.append(
+                        {
+                            "type": "High Coupling",
+                            "line": 1,
+                            "message": (
+                                f"Module imports {coupling['unique_modules']} unique packages "
+                                f"(fan_out={coupling['fan_out']}). Consider facade/wrapper layers."
+                            ),
+                            "severity": "warning",
+                            "coupling": coupling,
+                        }
+                    )
 
         return smells
 
-    def _detect_duplicate_functions(
-        self, tree: ast.AST, filepath: str
-    ) -> list[dict[str, Any]]:
+    def _detect_duplicate_functions(self, tree: ast.AST, filepath: str) -> list[dict[str, Any]]:
         smells: list[dict[str, Any]] = []
         bodies: dict[str, list[dict[str, Any]]] = {}
         for node in ast.walk(tree):
@@ -236,9 +226,7 @@ class CodeSmellDetector:
                 )
         return smells
 
-    def _detect_broad_exceptions(
-        self, tree: ast.AST, file_
```

---
_Auto-generated by SupremeAI CI Auto-Fix Engine v3_


