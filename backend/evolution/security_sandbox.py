# বাংলা কমেন্ট: সুপ্রিম-এআই এর জন্য আলটিমেট সিকিউরিটি স্যান্ডবক্স ও এএসটি গেটকিপার ইঞ্জিন।
# এটি ডাইনামিক কোড এক্সিকিউশনের আগে পাইথনের AST (Abstract Syntax Tree) লেভেলে আরসিই (RCE) ভেক্টর ব্লক করে।

import ast
import sys
from core.logging_config import logger

class ASTGatekeeper(ast.NodeVisitor):
    """
    হোয়াইটলিস্ট-বেসড কড়া AST গেটকিপার। যেকোনো ব্ল্যাকলিস্টেড ফাংশন, 
    মডিউল ইম্পোর্ট বা ইন্টারনাল অ্যাট্রিবিউট ডাইভার্সন দেখলেই এটি এক্সিকিউশন ডিসেবল করে।
    """
    # বাংলা কমেন্ট: শুধুমাত্র নিরাপদ পাইথন নোডগুলোর হোয়াইটলিস্ট
    ALLOWED_NODES = {
        ast.Module, ast.Expr, ast.Load, ast.Store, ast.Name,
        ast.Num, ast.Str, ast.Constant, ast.BinOp, ast.UnaryOp,
        ast.Add, ast.Sub, ast.Mult, ast.Div, ast.Mod, ast.Pow,
        ast.List, ast.Dict, ast.Tuple, ast.Set, ast.Compare,
        ast.Eq, ast.NotEq, ast.Lt, ast.LtE, ast.Gt, ast.GtE,
        ast.If, ast.Assign, ast.AugAssign, ast.Pass,
        ast.Call, ast.keyword, ast.FunctionDef, ast.arguments, ast.arg, ast.Return
    }

    # বাংলা কমেন্ট: মারাত্মক আরসিই (RCE) ভেক্টরের ব্ল্যাকলিস্ট
    FORBIDDEN_BUILTINS = {
        'eval', 'exec', 'compile', 'open', '__import__', 'globals', 
        'locals', 'getattr', 'setattr', 'delattr', 'hasattr', 'input'
    }

    FORBIDDEN_ATTRIBUTES = {
        '__subclasses__', '__builtins__', '__globals__', '__code__', 
        '__dict__', '__class__', '__base__', '__bases__'
    }

    def generic_visit(self, node):
        # বাংলা কমেন্ট: নোডটি হোয়াইটলিস্টে না থাকলে সরাসরি Fail-Closed মেকানিজমে রিজেক্ট করা হবে।
        if type(node) not in self.ALLOWED_NODES:
            logger.critical(f"🔥 SECURITY VIOLATION: Forbidden AST Node type detected -> {type(node).__name__}")
            raise SecurityException(f"Forbidden syntax component: {type(node).__name__}")
        super().generic_visit(node)

    def visit_Name(self, node):
        # বিল্ট-ইন ফাংশন হাইজ্যাকিং প্রতিরোধ
        if node.id in self.FORBIDDEN_BUILTINS:
            logger.critical(f"🔥 SECURITY VIOLATION: Blocked execution of dangerous built-in function: '{node.id}'")
            raise SecurityException(f"Access to dangerous built-in '{node.id}' is blocked.")
        self.generic_visit(node)

    def visit_Attribute(self, node):
        # অবফাসকেটেড ইন্টারনাল অবজেক্ট হ্যাকিং প্রতিরোধ (যেমন: obj.__class__.__base__)
        if node.attr in self.FORBIDDEN_ATTRIBUTES:
            logger.critical(f"🔥 SECURITY VIOLATION: Dunder attribute bypass attempted -> '{node.attr}'")
            raise SecurityException(f"Access to dangerous internal attribute '{node.attr}' is blocked.")
        self.generic_visit(node)

    def visit_Import(self, node):
        logger.critical("🔥 SECURITY VIOLATION: Raw module import attempt inside sandbox blocked.")
        raise SecurityException("Direct module imports are strictly prohibited inside the runtime sandbox.")

    def visit_ImportFrom(self, node):
        logger.critical("🔥 SECURITY VIOLATION: Raw 'from ... import ...' statement blocked.")
        raise SecurityException("Segmented module imports are strictly prohibited inside the runtime sandbox.")


class SecurityException(Exception):
    """কাস্টম সিকিউরিটি ভায়োলেশন এক্সেপশন।"""
    pass


def execute_secure_sandbox(code_source: str, local_scope: dict = None) -> dict:
    """
    AST ভেরিফিকেশন সম্পন্ন করার পর কোডকে একটি নিরাপদ আইসোলেটেড এনভায়রনমেন্টে রান করায়।
    """
    if local_scope is None:
        local_scope = {}
        
    try:
        # বাংলা কমেন্ট: স্ট্রিং সোর্স কোডকে AST তে রূপান্তর করে গেটকিপার দিয়ে স্ক্যান করানো হচ্ছে
        parsed_ast = ast.parse(code_source)
        gatekeeper = ASTGatekeeper()
        gatekeeper.visit(parsed_ast)
        
        # বাংলা কমেন্ট: সম্পূর্ণ ফাকা গ্লোবাল ডিকশনারি দিয়ে exec রান করা হচ্ছে যাতে বিল্ট-ইন এক্সেস না পায় (০% গ্যাপ পলিসি)
        safe_globals = {"__builtins__": {
            'print': print, 'range': range, 'len': len, 'int': int, 
            'str': str, 'float': float, 'list': list, 'dict': dict, 'abs': abs
        }}
        
        # কোড এক্সিকিউশন
        exec(code_source, safe_globals, local_scope)
        return {"status": "SUCCESS", "output": local_scope}
        
    except SecurityException as sec_err:
        return {"status": "BLOCKED", "reason": str(sec_err)}
    except Exception as run_err:
        logger.error(f"⚠️ Runtime compilation error inside sandbox: {str(run_err)}")
        return {"status": "FAILED", "reason": str(run_err)}
