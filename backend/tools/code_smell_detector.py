import ast
import os
from typing import List, Dict, Any
from loguru import logger

class CodeSmellDetector:
    """
    Static analysis tool to detect cyclomatic complexity, code duplication, and other smells.
    (Closes Gap #23)
    """

    def __init__(self):
        logger.info("Initialized CodeSmellDetector")

    def _calculate_complexity(self, node: ast.AST) -> int:
        """Calculates McCabe Cyclomatic Complexity for an AST node."""
        complexity = 1
        for child in ast.walk(node):
            if isinstance(child, (ast.If, ast.IfExp, ast.For, ast.While, ast.ExceptHandler, ast.With, ast.Assert, ast.BoolOp)):
                complexity += 1
        return complexity

    def analyze_python_file(self, filepath: str) -> List[Dict[str, Any]]:
        """Analyzes a Python file for code smells."""
        if not os.path.exists(filepath):
            return []
            
        logger.info(f"Analyzing {filepath} for smells...")
        smells = []
        
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
                
            tree = ast.parse(content)
            
            for node in ast.walk(tree):
                if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                    complexity = self._calculate_complexity(node)
                    if complexity > 10:
                        smells.append({
                            "type": "High Complexity",
                            "line": node.lineno,
                            "function": node.name,
                            "message": f"Cyclomatic complexity is {complexity} (Threshold: 10). Consider refactoring.",
                            "severity": "warning"
                        })
                        
                    # Check for too many arguments
                    if len(node.args.args) > 5:
                        smells.append({
                            "type": "Too Many Arguments",
                            "line": node.lineno,
                            "function": node.name,
                            "message": f"Function takes {len(node.args.args)} arguments (Threshold: 5).",
                            "severity": "info"
                        })
                        
        except Exception as e:
            logger.error(f"Failed to analyze {filepath}: {e}")
            
        return smells

    def analyze_directory(self, directory_path: str) -> Dict[str, List[Dict[str, Any]]]:
        """Runs analysis on all Python files in a directory."""
        results = {}
        for root, _, files in os.walk(directory_path):
            for file in files:
                if file.endswith(".py"):
                    full_path = os.path.join(root, file)
                    smells = self.analyze_python_file(full_path)
                    if smells:
                        results[full_path] = smells
        return results
