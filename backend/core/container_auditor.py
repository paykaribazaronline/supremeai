import asyncio
import json
import subprocess

from loguru import logger

from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus


class ContainerAuditor:
    """বাংলা মন্তব্য: রিয়েল-টাইম কন্টেইনার মেমরি অডিট। 
    OOM অ্যাবিউস ঠেকাতে ৮০% এ অ্যালার্ট এবং ৯৫% এ কিল চেইন ট্রিগার করবে।
    Stateless invocation for Cron, no while sleep loop."""

    def __init__(self):
        pass

    def get_container_stats(self) -> list:
        try:
            cmd = ["docker", "stats", "--no-stream", "--format", "{{ json . }}"]
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=10, check=False)
            if result.returncode != 0:
                logger.error(f"Failed to fetch docker stats: {result.stderr}")
                error_event_bus.emit(
                    ErrorEvent(
                        module="container_auditor",
                        error_type="DOCKER_STATS_FAILED",
                        message=result.stderr[:200],
                        severity="WARNING"
                    )
                )
                return []

            stats = []
            for line in result.stdout.strip().split("\n"):
                if line:
                    stats.append(json.loads(line))
            return stats
        except Exception as e:
            logger.error(f"Error executing docker stats: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="container_auditor",
                    error_type="DOCKER_STATS_EXEC_ERROR",
                    message=str(e)[:200],
                    severity="ERROR"
                )
            )
            return []

    def parse_memory_percent(self, mem_perc_str: str) -> float:
        try:
            return float(mem_perc_str.replace("%", "").strip())
        except ValueError:
            return 0.0

    async def audit_cycle(self):
        """Single stateless audit cycle"""
        logger.info("🛡️ Running Container Audit Cycle...")
        try:
            stats = await asyncio.to_thread(self.get_container_stats)
            for container in stats:
                name = container.get("Name")
                mem_perc_str = container.get("MemPerc", "0.00%")
                mem_perc = self.parse_memory_percent(mem_perc_str)

                if mem_perc >= 95.0:
                    logger.error(f"🚨 OOM Kill Chain Triggered: Container {name} is at {mem_perc}%. Terminating...")
                    try:
                        subprocess.run(["docker", "kill", name], capture_output=True, timeout=5, check=False)
                    except Exception as e:
                        logger.error(f"Failed to kill container {name}: {e}")
                        error_event_bus.emit(
                            ErrorEvent(
                                module="container_auditor",
                                error_type="DOCKER_KILL_FAILED",
                                message=str(e)[:200],
                                severity="CRITICAL",
                                context={"container_name": name}
                            )
                        )
                elif mem_perc >= 80.0:
                    logger.warning(f"⚠️ Memory Warning: Container {name} is nearing capacity at {mem_perc}%.")
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"Error in container audit cycle: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="container_auditor",
                    error_type="AUDIT_CYCLE_FAILED",
                    message=str(e)[:200],
                    severity="ERROR"
                )
            )

if __name__ == "__main__":
    auditor = ContainerAuditor()
    asyncio.run(auditor.audit_cycle())
