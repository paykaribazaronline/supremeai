# 📄 ফাইল: backend/tools/freebuff_client.py

**প্রকার:** .py  
**সাইজ:** 1,071 বাইট  
**আপডেট:** 2026-07-11T13:38:55.739349

---

## কোড

```py
import asyncio
from loguru import logger


class FreebuffClient:
    """বাংলা মন্তব্য: Cohesion আপগ্রেড — এক্সটার্নাল CLI টুল ডেলিগেশনের একক দায়িত্ব।"""

    def __init__(self, binary_path: str = "freebuff"):
        self.binary_path = binary_path

    async def delegate_task(self, command_args: list) -> dict:
        logger.info(f"📡 Delegating asynchronous workload to external CLI tool: {self.binary_path}")
        try:
            proc = await asyncio.create_subprocess_exec(
                self.binary_path, *command_args, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE
            )
            stdout, stderr = await proc.communicate()
            return {"exit_code": proc.returncode, "stdout": stdout.decode().strip(), "stderr": stderr.decode().strip()}
        except Exception as e:  # noqa: BLE001
            logger.error(f"🔴 Freebuff CLI execution failed: {str(e)}")
            return {"success": False, "error": str(e)}

```