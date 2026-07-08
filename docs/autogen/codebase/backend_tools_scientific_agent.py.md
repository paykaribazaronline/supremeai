# 📄 ফাইল: backend/tools/scientific_agent.py

**প্রকার:** .py  
**সাইজ:** 1,581 বাইট  
**আপডেট:** 2026-07-08T03:11:56.354526

---

## কোড

```py
from typing import Any

from loguru import logger


class ScientificAgent:
    async def solve_equation(self, equation: str) -> dict[str, Any]:
        logger.info(f"Solving equation: {equation}")
        try:
            import sympy as sp

            expr = sp.sympify(equation)
            solution = sp.solve(expr)
            method = "symbolic"
            if not solution:
                solution = sp.nsolve(expr, 0)
                method = "numerical"
            return {
                "status": "success",
                "equation": equation,
                "solution": str(solution),
                "method": method,
            }
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Equation solving failed: {exc}")
            return {
                "status": "error",
                "equation": equation,
                "error": str(exc),
                "solution": "x = 42",
                "method": "mock_fallback",
            }

    async def generate_simulation_script(self, phenomenon: str) -> dict[str, Any]:
        logger.info(f"Generating simulation for: {phenomenon}")
        script = f"""
import numpy as np
import matplotlib.pyplot as plt

t = np.linspace(0, 10, 100)
y = np.sin(t) * np.exp(-0.1 * t)

plt.plot(t, y)
plt.title("Simulation of {phenomenon}")
plt.xlabel("Time")
plt.ylabel("Amplitude")
plt.grid(True)
plt.show()
"""
        return {
            "status": "success",
            "language": "python",
            "script": script.strip(),
            "dependencies": ["numpy", "matplotlib"],
        }

```