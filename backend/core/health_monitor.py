import asyncio
import time
from typing import Any

from loguru import logger


try:
    from prometheus_client import Gauge
    from prometheus_client import Histogram
    from prometheus_client import start_http_server

    _PROMETHEUS_AVAILABLE = True
except ImportError:
    _PROMETHEUS_AVAILABLE = False


class HealthMonitor:
    """
    Monitors system health with Prometheus metrics export for Grafana dashboards.
    """

    def __init__(self, metrics_port: int = 9090):
        self.start_time = time.time()
        self.cpu_threshold = 85.0
        self.mem_threshold = 90.0
        self.metrics_port = metrics_port
        if _PROMETHEUS_AVAILABLE:
            self._setup_metrics()
            try:
                start_http_server(metrics_port)
                logger.info(f"Prometheus metrics server started on port {metrics_port}")
            except OSError as exc:
                logger.warning(f"Could not start metrics server: {exc}")

    def _setup_metrics(self):
        self.uptime_seconds = Gauge("supremeai_uptime_seconds", "Server uptime in seconds")
        self.cpu_usage_percent = Gauge("supremeai_cpu_usage_percent", "CPU usage percentage")
        self.memory_usage_percent = Gauge("supremeai_memory_usage_percent", "Memory usage percentage")
        self.memory_available_mb = Gauge("supremeai_memory_available_mb", "Available memory in MB")
        self.request_duration_seconds = Histogram(
            "supremeai_request_duration_seconds",
            "HTTP request latency in seconds",
            buckets=[0.1, 0.2, 0.3, 0.5, 0.75, 1.0, 2.5, 5.0],
        )
        self.active_tasks = Gauge("supremeai_active_tasks", "Number of active asyncio tasks")
        self.status = Gauge("supremeai_health_status", "Health status (1=healthy, 0=degraded)")

    async def get_system_metrics(self) -> dict[str, Any]:
        import psutil

        cpu_percent = psutil.cpu_percent(interval=0.1)
        mem = psutil.virtual_memory()
        status = "healthy"
        if cpu_percent > self.cpu_threshold or mem.percent > self.mem_threshold:
            status = "degraded"
        result = {
            "status": status,
            "uptime_seconds": int(time.time() - self.start_time),
            "cpu_usage_percent": cpu_percent,
            "memory_usage_percent": mem.percent,
            "memory_available_mb": mem.available / (1024 * 1024),
            "active_tasks": len(asyncio.all_tasks()),
        }
        if _PROMETHEUS_AVAILABLE:
            try:
                self.uptime_seconds.set(result["uptime_seconds"])
                self.cpu_usage_percent.set(result["cpu_usage_percent"])
                self.memory_usage_percent.set(result["memory_usage_percent"])
                self.memory_available_mb.set(result["memory_available_mb"])
                self.active_tasks.set(result["active_tasks"])
                self.status.set(1 if status == "healthy" else 0)
            except Exception as exc:
                logger.debug(f"Prometheus metrics update failed: {exc}")
        return result

    async def is_ready(self) -> bool:
        metrics = await self.get_system_metrics()
        if metrics["status"] == "degraded":
            logger.warning("Readiness probe: system degraded")
            return True
        return True

    async def is_live(self) -> bool:
        return True

    def record_request_duration(self, duration_seconds: float) -> None:
        if _PROMETHEUS_AVAILABLE:
            try:
                self.request_duration_seconds.observe(duration_seconds)
            except Exception as exc:
                logger.debug(f"Failed to record request duration: {exc}")


health_monitor = HealthMonitor()
