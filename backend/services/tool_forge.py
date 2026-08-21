# backend/services/tool_forge.py
"""SupremeAI Tool Forge Service (Phase 7.1 - North Star Pillar 3).

Dynamic on-the-fly Python tool synthesis with zero-RCE AST isolation:
- Verifies synthesized Python code using ASTSandboxScanner before execution.
- Blocks dangerous primitives (os, subprocess, eval, exec, socket, dunder traversal).
- Executes verified tools in an ephemeral restricted execution namespace.
"""

from __future__ import annotations

import ast
import time
from dataclasses import dataclass, field
from typing import Any

from loguru import logger

from core.security.ast_sandbox_scanner import ASTSandboxScanner


class ToolForgeError(Exception):
    """Base exception for ToolForge operations."""


class SecurityViolationError(ToolForgeError):
    """Raised when synthesized code fails AST safety validation."""


@dataclass
class ToolSpec:
    name: str
    description: str
    parameters: dict[str, str] = field(default_factory=dict)
    return_type: str = "dict"
    category: str = "dynamic_synthesized"


@dataclass
class SynthesizedTool:
    spec: ToolSpec
    source_code: str
    is_safe: bool = False
    compiled_code: Any = field(default=None, repr=False)
    created_at: float = field(default_factory=time.time)

    def to_dict(self) -> dict[str, Any]:
        return {
            "name": self.spec.name,
            "description": self.spec.description,
            "parameters": self.spec.parameters,
            "return_type": self.spec.return_type,
            "is_safe": self.is_safe,
            "created_at": self.created_at,
        }


# Safe builtins allowed in execution sandbox
SAFE_BUILTINS = {
    "abs": abs,
    "all": all,
    "any": any,
    "bool": bool,
    "dict": dict,
    "enumerate": enumerate,
    "filter": filter,
    "float": float,
    "int": int,
    "isinstance": isinstance,
    "len": len,
    "list": list,
    "map": map,
    "max": max,
    "min": min,
    "pow": pow,
    "range": range,
    "round": round,
    "set": set,
    "sorted": sorted,
    "str": str,
    "sum": sum,
    "tuple": tuple,
    "zip": zip,
    "True": True,
    "False": False,
    "None": None,
}


class ToolForgeService:
    """Synthesizes, audits, and safely executes dynamic tools inside AST-isolated environments."""

    def __init__(self, scanner: ASTSandboxScanner | None = None) -> None:
        self.scanner = scanner or ASTSandboxScanner()
        self._tool_registry: dict[str, SynthesizedTool] = {}

    def verify_code_safety(self, code: str) -> bool:
        """Runs static AST analysis to ensure code contains no dangerous calls or escapes."""
        if not code or not code.strip():
            return False

        try:
            scan_result = self.scanner.scan(code)
            return scan_result.is_safe
        except Exception as exc:
            logger.warning(f"AST scan exception: {exc}")
            return False

    def forge_tool(self, spec: ToolSpec, code: str) -> SynthesizedTool:
        """Synthesizes and registers an audited dynamic tool."""
        cleaned_code = code.strip()

        # 1. AST Static Security Audit
        if not self.verify_code_safety(cleaned_code):
            scan_res = self.scanner.scan(cleaned_code)
            findings = scan_res.findings
            logger.critical(f"ToolForge security violation blocked for '{spec.name}': {findings}")
            raise SecurityViolationError(f"Code violated sandbox safety rules: {findings}")

        # 2. Syntax & Bytecode Compilation
        try:
            compiled = compile(cleaned_code, f"<synthesized_tool_{spec.name}>", "exec")
        except SyntaxError as exc:
            raise ToolForgeError(f"Syntax error in synthesized tool code: {exc}") from exc

        tool = SynthesizedTool(
            spec=spec,
            source_code=cleaned_code,
            is_safe=True,
            compiled_code=compiled,
        )

        self._tool_registry[spec.name] = tool
        logger.info(f"ToolForge: Successfully forged and registered secure tool '{spec.name}'")
        return tool

    def execute_tool(
        self,
        tool: SynthesizedTool,
        params: dict[str, Any],
        timeout_seconds: float = 5.0,
    ) -> Any:
        """Executes a forged tool in a restricted sandbox namespace."""
        if not tool.is_safe or not tool.compiled_code:
            raise SecurityViolationError(f"Tool '{tool.spec.name}' is unverified or unsafe.")

        # Restricted execution scope
        sandbox_globals = {
            "__builtins__": SAFE_BUILTINS,
            "__name__": "__tool_forge__",
        }
        sandbox_locals: dict[str, Any] = {}

        try:
            exec(tool.compiled_code, sandbox_globals, sandbox_locals)

            # Target function matching spec.name or 'main' or 'run' or the only callable
            func = (
                sandbox_locals.get(tool.spec.name)
                or sandbox_locals.get("run")
                or sandbox_locals.get("main")
            )

            if not func:
                callables = [v for v in sandbox_locals.values() if callable(v)]
                if callables:
                    func = callables[0]

            if not callable(func):
                raise ToolForgeError(f"No executable entrypoint found in tool '{tool.spec.name}'")

            # Execute tool logic with parameters
            result = func(**params) if params else func()
            return result

        except Exception as exc:
            logger.error(f"Execution error in tool '{tool.spec.name}': {exc}")
            raise ToolForgeError(f"Tool execution failed: {exc}") from exc

    def get_tool(self, name: str) -> SynthesizedTool | None:
        return self._tool_registry.get(name)

    def list_tools(self) -> list[dict[str, Any]]:
        return [t.to_dict() for t in self._tool_registry.values()]
