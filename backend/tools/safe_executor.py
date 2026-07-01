"""backend.tools.safe_executor
================================
A thin wrapper around **RestrictedPython** that provides a safe execution
environment for dynamic Python code.

Only a curated whitelist of built‑ins is exposed; everything else (e.g.
``open``, ``__import__``) is blocked. The function returns the locals dictionary
so callers can retrieve a ``result`` variable if they set one.
"""

import ast
import logging
from typing import Any

from RestrictedPython import compile_restricted
from RestrictedPython.Eval import default_globals

logger = logging.getLogger(__name__)

# Define a minimal safe builtins whitelist. Adjust as needed for the
# application – currently only ``range`` and ``len`` are allowed because the
# CoT reasoner does not rely on any other built‑ins.
SAFE_BUILTINS = {
    "range": range,
    "len": len,
    "abs": abs,
    "round": round,
    "min": min,
    "max": max,
    "sum": sum,
    "int": int,
    "float": float,
    "str": str,
    "list": list,
    "dict": dict,
    "set": set,
    # Add more safe functions here if required.
}

ALLOWED_NODE_TYPES = {
    ast.Module,
    ast.Assign,
    ast.AugAssign,
    ast.Name,
    ast.Constant,
    ast.Expr,
    ast.BinOp,
    ast.UnaryOp,
    ast.Compare,
    ast.BoolOp,
    ast.If,
    ast.List,
    ast.Dict,
    ast.Set,
    ast.Tuple,
    ast.Subscript,
    ast.Slice,
    ast.Pass,
    ast.Call,
    ast.Load,
    ast.Store,
    ast.Del,
    ast.Add,
    ast.Sub,
    ast.Mult,
    ast.Div,
    ast.Mod,
    ast.Pow,
    ast.LShift,
    ast.RShift,
    ast.BitOr,
    ast.BitXor,
    ast.BitAnd,
    ast.FloorDiv,
    ast.And,
    ast.Or,
    ast.Not,
    ast.Invert,
    ast.UAdd,
    ast.USub,
    ast.Eq,
    ast.NotEq,
    ast.Lt,
    ast.LtE,
    ast.Gt,
    ast.GtE,
    ast.Is,
    ast.IsNot,
    ast.In,
    ast.NotIn,
}


def validate_ast(source: str) -> None:
    """Validate that the source code contains only whitelisted AST nodes and safe calls."""
    try:
        tree = ast.parse(source)
    except SyntaxError as e:
        raise ValueError(f"Syntax error during validation: {e}")

    for node in ast.walk(tree):
        # 1. Check node type
        if type(node) not in ALLOWED_NODE_TYPES:
            raise ValueError(
                f"Security error: AST node type {type(node).__name__} is not allowed"
            )

        # 2. Prevent import statements entirely (Imports are not in ALLOWED_NODE_TYPES, but check as safety net)
        if isinstance(node, (ast.Import, ast.ImportFrom)):
            raise ValueError("Security error: Import statements are strictly forbidden")

        # 3. Restrict attribute access to prevent double underscore tricks (e.g. __class__)
        if isinstance(node, ast.Attribute) and node.attr.startswith("__"):
            raise ValueError(
                "Security error: Access to private/dunder attributes is forbidden"
            )

        # 4. Restrict Calls to only SAFE_BUILTINS
        if isinstance(node, ast.Call):
            if isinstance(node.func, ast.Name):
                func_name = node.func.id
                if func_name not in SAFE_BUILTINS:
                    raise ValueError(
                        f"Security error: Function call '{func_name}' is not in the allowed list"
                    )
            else:
                raise ValueError(
                    "Security error: Non-name function calls are not allowed"
                )


def run_restricted(
    source: str, locals_: dict[str, Any] | None = None
) -> tuple[bool, dict[str, Any] | str]:
    """Execute *source* in a RestrictedPython sandbox after AST validation.

    Parameters
    ----------
    source:
        The Python code to execute. It should be a string containing a valid
        Python block (e.g. ``"result = 2 + 2"``).
    locals_:
        Optional dictionary that will be used as the locals namespace. If not
        provided a fresh dict is created.

    Returns
    -------
    tuple[bool, dict[str, Any] | str]
        A tuple containing a success flag and either the locals dictionary
        on success or an error message string on failure.
    """
    if locals_ is None:
        locals_ = {}

    try:
        # Pre-execution AST verification
        validate_ast(source)

        # Compile the code with RestrictedPython. The ``compile_restricted``
        # function returns a code object that can be safely ``exec``ed.
        byte_code = compile_restricted(source, "<string>", "exec")

        # Merge the default globals with our safe builtins whitelist.
        globals_ = dict(default_globals)
        globals_["__builtins__"] = SAFE_BUILTINS

        # Execute the sandboxed code. RestrictedPython will raise an exception if
        # the code attempts to use disallowed operations.
        exec(byte_code, globals_, locals_)
        return True, locals_

    except (ValueError, SyntaxError, NameError, TypeError) as e:
        error_message = f"Restricted execution failed: {type(e).__name__}: {e}"
        logger.error(error_message)
        return False, error_message
    except Exception as e:
        error_message = f"An unexpected error occurred during restricted execution: {e}"
        logger.error(error_message)
        return False, error_message
