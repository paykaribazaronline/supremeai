import asyncio
from loguru import logger
from tools.docker_sandbox import DockerSandbox  # আমাদের এক্সিস্টিং সুনির্দিষ্ট টুল


class LocalCodeExecutor:
    """বাংলা মন্তব্য: Cohesion আপগ্রেড — লোকাল ডকার ও সাবপ্রসেস এক্সিকিউশনের একক দায়িত্ব।"""

    def __init__(self, use_docker: bool = True):
        self.use_docker = use_docker
        self.docker_sandbox = DockerSandbox() if use_docker else None

    async def execute_local_code(self, code: str, timeout_seconds: int = 30) -> dict:
        if self.use_docker and self.docker_sandbox:
            try:
                logger.info("🐳 Running code inside tight Docker Sandbox Container...")
                import shlex

                loop = asyncio.get_event_loop()
                escaped_code = shlex.quote(code)
                res = await loop.run_in_executor(None, self.docker_sandbox.execute_command, f"python -c {escaped_code}")
                if not res.get("success") and "Sandbox execution failed" in res.get("error", ""):
                    return res
                return res
            except Exception as exc:  # noqa: BLE001
                logger.warning(f"⚠️ Docker failure, falling back to host subprocess: {exc}")

        # ডকার না থাকলে সেফ ফলব্যাক সরাসরি হোস্ট সাবপ্রসেসে
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
