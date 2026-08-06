import asyncio

from loguru import logger


class FreebuffClient:
    """বাংলা মন্তব্ট: Cohesion আপগ্রেড — এক্সটার্নাল CLI টুল ডেলিগেশনের একক দায়িত্ব।"""

    def __init__(self, binary_path: str = "freebuff", timeout: int = 30):
        self.binary_path = binary_path
        self.timeout = timeout

    async def delegate_task(self, command_args: list) -> dict:
        logger.info(f"📡 Delegating asynchronous workload to external CLI tool: {self.binary_path}")
        try:
            proc = await asyncio.create_subprocess_exec(
                self.binary_path,
                *command_args,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )
            # Security Fix: Add timeout to prevent indefinite hang if the
            # external CLI tool never terminates.
            stdout, stderr = await asyncio.wait_for(
                proc.communicate(),
                timeout=self.timeout,
            )
            return {
                "exit_code": proc.returncode,
                "stdout": stdout.decode().strip(),
                "stderr": stderr.decode().strip(),
            }
        except asyncio.TimeoutError:
            logger.error(
                f"🔴 Freebuff CLI execution timed out after {self.timeout}s. "
                "Killing process."
            )
            try:
                proc.kill()
                await proc.wait()
            except Exception as kill_err:
                logger.warning(f"Process kill cleanup warning: {kill_err}")
            return {
                "exit_code": -1,
                "stdout": "",
                "stderr": f"Execution timed out after {self.timeout}s",
            }
        except Exception as e:
            logger.error(f"🔴 Freebuff CLI execution failed: {e!s}")
            return {"success": False, "error": str(e)}
