# 📄 ফাইল: backend/tools/local_code_executor.py

**প্রকার:** .py  
**সাইজ:** 3,011 বাইট  
**আপডেট:** 2026-07-11T14:41:19.380775

---

## কোড

```py
import asyncio
import os
from loguru import logger
from tools.docker_sandbox import DockerSandbox  # আমাদের এক্সিস্টিং সুনির্দিষ্ট টুল


class LocalCodeExecutor:
    """বাংলা মন্তব্য: Cohesion আপগ্রেড — লোকাল ডকার ও সাবপ্রসেস এক্সিকিউশনের একক দায়িত্ব।"""

    def __init__(self, use_docker: bool = True):
        self.use_docker = use_docker
        self.docker_sandbox = DockerSandbox() if use_docker else None

    async def execute_local_code(self, code: str, timeout_seconds: int = 30) -> dict:
        env = os.getenv("ENV", "development").lower()
        allow_fallback = os.getenv("ALLOW_LOCAL_SANDBOX_FALLBACK", "false").lower() == "true"

        if self.use_docker and self.docker_sandbox:
            try:
                logger.info("🐳 Running code inside tight Docker Sandbox Container...")
                if hasattr(self.docker_sandbox, "run_secure"):
                    res = await self.docker_sandbox.run_secure(code, timeout=timeout_seconds)
                    if res and (res.get("success") or isinstance(res, dict) and "success" not in res):
                        if "stdout" in res and "output" not in res:
                            res["output"] = res["stdout"]
                        if "stderr" in res and "error" not in res:
                            res["error"] = res["stderr"]
                        return res
            except Exception as exc:  # noqa: BLE001
                logger.warning(f"🐳 Docker execution failure: {exc}")

        if env == "production" and not allow_fallback:
            return {
                "success": False,
                "error": "local execution is disabled for safety",
                "stderr": "local execution is disabled for safety",
                "stdout": "",
                "output": "",
            }

        logger.info("🔌 Falling back to secure Host Subprocess execution layer...")
        return await self._run_host_subprocess(code, timeout_seconds)

    async def _run_host_subprocess(self, code: str, timeout: int) -> dict:
        logger.warning("⚠️ CRITICAL SECURITY NOTE: Running code directly on Host Subprocess!")
        try:
            # অসিঙ্ক্রোনাসভাবে লোকাল হোস্ট প্রসেস এক্সিকিউট করা হচ্ছে
            proc = await asyncio.create_subprocess_exec("python", "-c", code, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)
            stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout=timeout)
            return {"success": proc.returncode == 0, "output": stdout.decode().strip(), "error": stderr.decode().strip()}
        except TimeoutError:
            logger.error("🔴 Host subprocess timed out!")
            return {"success": False, "error": "Execution TimeoutExpired"}

```