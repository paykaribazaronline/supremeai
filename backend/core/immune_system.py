# Immune System static security analysis scanner
# বাংলা মন্তব্য: এআই জেনারেটেড কোডের সিকিউরিটি স্ক্যানার ও এএসটি ভ্যালিডেশন গেটকিপার।

import ast

from loguru import logger


class SecuritySandboxError(Exception):
    """Exception thrown when code violates AST security constraints."""
    pass


class ASTSecurityScanner(ast.NodeVisitor):
    def __init__(self):
        # 🛑 ZERO-GAP: Extended Banned Imports
        self.banned_imports: set[str] = {
            "os", "sys", "subprocess", "pty", "shlex",
            "importlib", "code", "runpy", "multiprocessing",
            "pickle", "marshal", "tempfile", "socket", 
            "urllib", "urllib3", "requests", "http", "ctypes", "builtins"
        }
        
        # 🛑 ZERO-GAP: Banned Built-in Functions for Introspection & Execution
        self.banned_functions: set[str] = {
            "eval", "exec", "compile", "globals", "locals",
            "vars", "dir", "type", "chr", "ord", "breakpoint",
            "__import__", "getattr", "setattr", "delattr", "hasattr", "open"
        }
        
        # 🛑 ZERO-GAP: Prevent Sandbox Escapes via Dunder Attributes
        self.banned_attributes: set[str] = {
            "__class__", "__bases__", "__subclasses__",
            "__globals__", "__builtins__", "__dict__", "__mro__",
            "__code__", "__closure__", "__func__"
        }

    def visit_Import(self, node: ast.Import):
        for alias in node.names:
            base_module = alias.name.split('.')[0]
            if base_module in self.banned_imports:
                raise SecuritySandboxError(f"Banned import detected: {alias.name}")
        self.generic_visit(node)

    def visit_ImportFrom(self, node: ast.ImportFrom):
        if node.module:
            base_module = node.module.split('.')[0]
            if base_module in self.banned_imports:
                raise SecuritySandboxError(f"Banned import detected: {node.module}")
        self.generic_visit(node)

    def visit_Call(self, node: ast.Call):
        # Block direct function calls like eval(), __import__()
        if isinstance(node.func, ast.Name) and node.func.id in self.banned_functions:
            raise SecuritySandboxError(f"Banned function call detected: {node.func.id}")
        
        # Block malicious module methods like importlib.import_module() or os.system()
        elif isinstance(node.func, ast.Attribute) and node.func.attr in {"import_module", "system", "popen", "spawn", "fork"}:
            raise SecuritySandboxError(f"Banned method invocation detected: {node.func.attr}")
        
        self.generic_visit(node)

    def visit_Attribute(self, node: ast.Attribute):
        # Block access to dunder attributes used for sandbox escapes
        if node.attr in self.banned_attributes or node.attr in self.banned_functions:
            raise SecuritySandboxError(f"Sandbox escape pattern blocked: {node.attr}")
        self.generic_visit(node)


class ImmuneSystemScanner:
    """
    Scans generated python code using AST parser to block execution of unsafe or malicious code before execution.
    """
    def __init__(self):
        # Preserve public interface configs if needed by test suite or other modules
        self.scanner = ASTSecurityScanner()

    def scan_code(self, code: str) -> dict:
        """
        Parses code string to check for banned keywords and modules.
        Returns a dict: {"safe": bool, "error": str | None}
        """
        try:
            tree = ast.parse(code)
            self.scanner.visit(tree)
            logger.info("AST Static code scan passed successfully. Code is safe for execution.")
            return {"safe": True, "error": None}
        except SecuritySandboxError as sse:
            logger.critical(f"🚨 [IMMUNE SYSTEM] Security threat defused: {sse}")
            # বাংলা মন্তব্য: টেস্ট কেসের প্রত্যাশিত আউটপুট ম্যাচ করানোর সাথে কাস্টম এক্সপশন মাস্কিং বজায় রাখা হলো
            error_msg = str(sse)
            if "Banned import" in error_msg:
                user_error = "Security validation failed: Banned root import detected and blocked."
            elif "Banned function" in error_msg:
                user_error = "Security validation failed: Reference to banned security identifier blocked."
            elif "Sandbox escape" in error_msg:
                user_error = "Security validation failed: Banned attribute or dunder reflection access blocked."
            else:
                user_error = "Security validation failed: Payload rejected by Immune System."
            return {"safe": False, "error": user_error}

        except SyntaxError as se:
            logger.error(f"Syntax validation failed: {se}")
            return {"safe": False, "error": f"SyntaxError: {str(se)}"}
        except Exception as e:
            logger.error(f"Unexpected error during static analysis: {e}")
            return {"safe": False, "error": f"AnalysisException: {str(e)}"}
