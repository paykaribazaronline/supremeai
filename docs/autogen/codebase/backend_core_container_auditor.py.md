# 📄 ফাইল: backend/core/container_auditor.py

**প্রকার:** .py  
**সাইজ:** 3,001 বাইট  
**আপডেট:** 2026-07-11T13:53:46.536645

---

## কোড

```py
import asyncio
import json
import subprocess

from loguru import logger


class ContainerAuditor:
    """বাংলা মন্তব্য: রিয়েল-টাইম কন্টেইনার মেমরি অডিট চেইন। OOM অ্যাবিউস ঠেকাতে
    ৮০% এ অ্যালার্ট এবং ৯৫% এ কিল চেইন ট্রিগার করবে।"""

    def __init__(self, check_interval_seconds: int = 5):
        self.check_interval = check_interval_seconds
        self.running = False

    def get_container_stats(self) -> list:
        # Use docker stats to get memory usage
        try:
            cmd = ["docker", "stats", "--no-stream", "--format", "{{ json . }}"]
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=10, check=False)
            if result.returncode != 0:
                logger.error(f"Failed to fetch docker stats: {result.stderr}")
                return []

            stats = []
            for line in result.stdout.strip().split("\n"):
                if line:
                    stats.append(json.loads(line))
            return stats
        except Exception as e:  # noqa: BLE001
            logger.error(f"Error executing docker stats: {e}")
            return []

    def parse_memory_percent(self, mem_perc_str: str) -> float:
        try:
            return float(mem_perc_str.replace("%", "").strip())
        except ValueError:
            return 0.0

    async def audit_cycle(self):
        stats = await asyncio.to_thread(self.get_container_stats)
        for container in stats:
            name = container.get("Name")
            mem_perc_str = container.get("MemPerc", "0.00%")
            mem_perc = self.parse_memory_percent(mem_perc_str)

            if mem_perc >= 95.0:
                logger.error(f"🚨 OOM Kill Chain Triggered: Container {name} is at {mem_perc}%. Terminating...")
                try:
                    subprocess.run(["docker", "kill", name], capture_output=True, timeout=5, check=False)
                except Exception as e:  # noqa: BLE001
                    logger.error(f"Failed to kill container {name}: {e}")
            elif mem_perc >= 80.0:
                logger.warning(f"⚠️ Memory Warning: Container {name} is nearing capacity at {mem_perc}%.")

    async def run(self):
        logger.info("🛡️  Starting Live Memory Container Audit Chain...")
        self.running = True
        while self.running:
            try:
                await self.audit_cycle()
            except Exception as e:  # noqa: BLE001
                logger.error(f"Error in container audit cycle: {e}")
            await asyncio.sleep(self.check_interval)

    def stop(self):
        self.running = False
        logger.info("Container Audit Chain stopped.")


if __name__ == "__main__":
    auditor = ContainerAuditor()
    try:
        asyncio.run(auditor.run())
    except KeyboardInterrupt:
        auditor.stop()

```