import os
import re

filepath = r'c:\Users\n\supremeai\supremeai_2.0\backend\core\orchestration\swarm_orchestrator.py'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove local CircuitBreaker classes
content = re.sub(r'class CircuitBreakerState:.*?class MorphicOrchestrator:', 'from core.resilience.circuit_breaker import CircuitBreaker\n\n\nclass MorphicOrchestrator:', content, flags=re.DOTALL)

# 2. Update initialization
content = content.replace(
    'self.circuit_breaker = CircuitBreaker(failure_threshold=3, recovery_timeout=30.0)',
    'self.circuit_breaker = CircuitBreaker(name="swarm_orch", failure_threshold=3, recovery_timeout=30.0)'
)

# 3. Update exception catching
content = content.replace(
    'except CircuitBreakerOpenError as e:',
    'except RuntimeError as e:\n            if "is open" not in str(e):\n                raise\n            workspace.log(f"MorphicOrchestrator: Circuit breaker OPEN — {e}")\n            workspace.add_error(str(e))\n            return workspace'
)

# Also remove the duplicate workspace.log that was under except CircuitBreakerOpenError as e
content = re.sub(r'except RuntimeError as e:\s+if "is open" not in str\(e\):\s+raise\s+workspace\.log\(f"MorphicOrchestrator: Circuit breaker OPEN — \{e\}"\)\s+workspace\.add_error\(str\(e\)\)\s+return workspace\s+workspace\.log.*?return workspace', 'except RuntimeError as e:\n            if "is open" not in str(e):\n                raise\n            workspace.log(f"MorphicOrchestrator: Circuit breaker OPEN — {e}")\n            workspace.add_error(str(e))\n            return workspace', content, flags=re.DOTALL)


with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
