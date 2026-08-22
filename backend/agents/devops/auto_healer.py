"""
SupremeAI - AutoHealer Agent 🚑
================================
Purpose: Auto-restart, rollback, or backup provider switch when services go down.
Author: SupremeAI Architecture Team
Date: July 18, 2026
"""

from __future__ import annotations

import datetime
import json
import logging
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from enum import Enum, auto
from pathlib import Path
from typing import Any

import requests

# বাংলা মন্তব্য: উইন্ডোজ টার্মিনালে ইউনিকোড/ইমোজি আউটপুট সাপোর্ট করার জন্য এনকোডিং কনফিগার করা হলো।
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except AttributeError:
        pass

# --- Path Setup (consistent with existing codebase) ---
# বাংলা মন্তব্ব্য: পাথ সেটআপ ও কোর কনফিগ ইম্পোর্ট
try:
    from core.config import settings
except ImportError:
    sys.path.insert(0, str(Path(__file__).resolve().parent / "backend"))
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent))
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from core.config import settings

# --- Configuration ---
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("autohealer")

REQUEST_TIMEOUT = int(os.getenv("HTTP_TIMEOUT_SECONDS") or "10")
HEALTH_CHECK_INTERVAL = int(os.getenv("HEALTH_CHECK_INTERVAL") or "30")  # seconds
MAX_RETRIES = int(os.getenv("AUTOHEAL_MAX_RETRIES") or "3")
CIRCUIT_BREAKER_THRESHOLD = int(os.getenv("CIRCUIT_BREAKER_THRESHOLD") or "5")
CIRCUIT_BREAKER_TIMEOUT = int(os.getenv("CIRCUIT_BREAKER_TIMEOUT") or "300")  # 5 minutes
STATE_FILE = Path(__file__).parent / ".autohealer_state.json"

RENDER_API_KEY = os.getenv("RENDER_API_KEY", "")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP", "")
VERCEL_TOKEN = os.getenv("VERCEL_OIDC_TOKEN", "")
DISCORD_WEBHOOK = getattr(settings, "discord_webhook_url", os.getenv("DISCORD_WEBHOOK_URL", ""))
BACKUP_PROVIDER_URL = os.getenv("BACKUP_PROVIDER_URL", "")


class CircuitState(Enum):
    """Circuit breaker states."""

    CLOSED = auto()  # Normal operation
    OPEN = auto()  # Failure threshold reached, reject requests
    HALF_OPEN = auto()  # Testing if service recovered


class HealingAction(Enum):
    """Possible healing actions."""

    RESTART = "restart"
    ROLLBACK = "rollback"
    SWITCH_PROVIDER = "switch_provider"
    NONE = "none"


@dataclass
class ServiceConfig:
    """একটি সার্ভিসের heal configuration।"""

    name: str
    url: str
    provider: str  # 'render', 'vercel', 'custom'
    service_id: str | None = None
    check_interval: int = 30
    failure_threshold: int = 3
    healing_actions: list[HealingAction] = None
    backup_url: str | None = None

    def __post_init__(self):
        if self.healing_actions is None:
            self.healing_actions = [
                HealingAction.RESTART,
                HealingAction.SWITCH_PROVIDER,
            ]


@dataclass
class HealingRecord:
    """Healing action history record."""

    timestamp: str
    service_name: str
    action: str
    success: bool
    error_message: str | None = None
    duration_seconds: float = 0.0


class CircuitBreaker:
    """Enhanced circuit breaker pattern implementation with adaptive thresholds."""

    def __init__(
        self,
        threshold: int = CIRCUIT_BREAKER_THRESHOLD,
        timeout: int = CIRCUIT_BREAKER_TIMEOUT,
    ):
        self.threshold = threshold
        self.timeout = timeout
        self.failures: dict[str, int] = {}
        self.last_failure_time: dict[str, datetime.datetime] = {}
        self.state: dict[str, CircuitState] = {}
        self.failure_rates: dict[str, float] = {}  # Track failure rates for adaptive behavior
        self.failure_timestamps: dict[str, list[datetime.datetime]] = {}  # Track failure timestamps
        self._load_state()

    def _load_state(self):
        # বাংলা মন্তব্ব্য: সার্কিট ব্রেকার স্টেট ক্যাশ ফাইল থেকে লোড করা।
        if STATE_FILE.exists():
            try:
                data = json.loads(STATE_FILE.read_text(encoding="utf-8"))
                for svc, st in data.get("states", {}).items():
                    self.state[svc] = CircuitState[st]
                self.failures = data.get("failures", {})
                self.failure_rates = data.get("failure_rates", {})
                self.last_failure_time = {
                    k: datetime.datetime.fromisoformat(v) for k, v in data.get("last_failure", {}).items()
                }
                # Load failure timestamps
                timestamps_data = data.get("failure_timestamps", {})
                for svc, timestamps in timestamps_data.items():
                    self.failure_timestamps[svc] = [datetime.datetime.fromisoformat(ts) for ts in timestamps]
            except Exception as e:
                logger.warning(f"⚠️ Failed to load circuit breaker state: {e}")

    def _save_state(self):
        # বাংলা মন্তব্ব্য: সার্কিট ব্রেকার স্টেট ক্যাশ ফাইলে রাইট করা।
        try:
            data = {
                "states": {k: v.name for k, v in self.state.items()},
                "failures": self.failures,
                "failure_rates": self.failure_rates,
                "last_failure": {k: v.isoformat() for k, v in self.last_failure_time.items()},
                "failure_timestamps": {
                    svc: [ts.isoformat() for ts in timestamps]
                    for svc, timestamps in self.failure_timestamps.items()
                },
            }
            STATE_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")
        except Exception as e:
            logger.warning(f"⚠️ Failed to save circuit breaker state: {e}")

    def record_success(self, service_name: str):
        if service_name in self.failures:
            del self.failures[service_name]
        if service_name in self.last_failure_time:
            del self.last_failure_time[service_name]
        if service_name in self.failure_timestamps:
            del self.failure_timestamps[service_name]
        if service_name in self.failure_rates:
            del self.failure_rates[service_name]
        self.state[service_name] = CircuitState.CLOSED
        self._save_state()

    def record_failure(self, service_name: str) -> CircuitState:
        now = datetime.datetime.now(datetime.UTC)
        self.failures[service_name] = self.failures.get(service_name, 0) + 1

        # Track failure timestamps for rate calculation
        if service_name not in self.failure_timestamps:
            self.failure_timestamps[service_name] = []
        self.failure_timestamps[service_name].append(now)

        # Keep only recent failures (last 10 minutes) for rate calculation
        recent_window = datetime.timedelta(minutes=10)
        self.failure_timestamps[service_name] = [
            ts for ts in self.failure_timestamps[service_name]
            if (now - ts) <= recent_window
        ]

        # Calculate failure rate
        total_checks = len(self.failure_timestamps[service_name])
        if total_checks > 0:
            # Approximate total checks as failure count + some success estimates
            estimated_total = max(total_checks, self.failures[service_name])
            self.failure_rates[service_name] = self.failures[service_name] / estimated_total
        else:
            self.failure_rates[service_name] = 0.0

        self.last_failure_time[service_name] = now

        if self.state.get(service_name) == CircuitState.OPEN:
            last_fail = self.last_failure_time.get(service_name)
            if last_fail and (now - last_fail) > datetime.timedelta(seconds=self.timeout):
                self.state[service_name] = CircuitState.HALF_OPEN
                logger.info(f"🔓 Circuit breaker HALF_OPEN for {service_name}")
            return self.state[service_name]

        # Adaptive threshold based on failure rate
        current_threshold = self.threshold
        if service_name in self.failure_rates:
            if self.failure_rates[service_name] > 0.8:  # 80% failure rate
                current_threshold = max(1, int(self.threshold * 0.5))  # Lower threshold for flaky services
            elif self.failure_rates[service_name] < 0.1:  # 10% failure rate
                current_threshold = int(self.threshold * 1.5)  # Higher threshold for stable services

        if self.failures[service_name] >= current_threshold:
            self.state[service_name] = CircuitState.OPEN
            logger.warning(f"🔒 Circuit breaker OPEN for {service_name} ({self.failures[service_name]} failures, threshold: {current_threshold})")
        else:
            self.state[service_name] = CircuitState.CLOSED

        self._save_state()
        return self.state[service_name]

    def can_heal(self, service_name: str) -> bool:
        state = self.state.get(service_name, CircuitState.CLOSED)
        if state in (CircuitState.CLOSED, CircuitState.HALF_OPEN):
            return True
        if state == CircuitState.OPEN:
            last_fail = self.last_failure_time.get(service_name)
            if last_fail and (datetime.datetime.now(datetime.UTC) - last_fail) > datetime.timedelta(
                seconds=self.timeout
            ):
                self.state[service_name] = CircuitState.HALF_OPEN
                self._save_state()
                return True
            return False
        return True


class HealthChecker:
    def __init__(self, timeout: int = REQUEST_TIMEOUT):
        self.timeout = timeout
        self.health_indicators: dict[str, dict] = {}  # Track various health indicators per service

    def check(self, url: str, retries: int = 2) -> tuple[bool, int | None, str | None]:
        # বাংলা মন্তব্ব্য: রিট্রাই সহ সার্ভিস হেলথ চেক মেকানিজম।
        for attempt in range(retries + 1):
            try:
                start_time = time.time()
                resp = requests.get(url, timeout=self.timeout, allow_redirects=True)

                # Calculate response time
                response_time = time.time() - start_time

                # Track health indicators
                service_key = url
                if service_key not in self.health_indicators:
                    self.health_indicators[service_key] = {
                        "response_times": [],
                        "status_codes": [],
                        "last_check": datetime.datetime.now(datetime.UTC)
                    }

                # Store metrics
                self.health_indicators[service_key]["response_times"].append(response_time)
                self.health_indicators[service_key]["status_codes"].append(resp.status_code)

                # Keep only recent metrics (last 10 checks)
                if len(self.health_indicators[service_key]["response_times"]) > 10:
                    self.health_indicators[service_key]["response_times"] = \
                        self.health_indicators[service_key]["response_times"][-10:]
                    self.health_indicators[service_key]["status_codes"] = \
                        self.health_indicators[service_key]["status_codes"][-10:]

                # Define health based on response status and response time
                is_healthy = resp.status_code < 500
                # Consider slow response times (>3 seconds) as degraded health
                if is_healthy and response_time > 3.0:
                    logger.warning(f"⚠️ {url} is responding slowly ({response_time:.2f}s)")

                if is_healthy:
                    return True, resp.status_code, None
                else:
                    if attempt < retries:
                        time.sleep(1)
                        continue
                    return False, resp.status_code, f"HTTP {resp.status_code}"
            except requests.Timeout:
                if attempt < retries:
                    time.sleep(1)
                    continue
                return False, None, "Timeout"
            except requests.ConnectionError as e:
                if attempt < retries:
                    time.sleep(1)
                    continue
                return False, None, f"Connection error: {e}"
            except Exception as e:
                if attempt < retries:
                    time.sleep(1)
                    continue
                return False, None, str(e)
        return False, None, "Unknown error"

    def get_health_summary(self, url: str) -> dict:
        """Get a summary of health metrics for a service."""
        service_key = url
        if service_key not in self.health_indicators:
            return {"error": "No health data available"}

        indicators = self.health_indicators[service_key]
        if not indicators["response_times"]:
            return {"error": "No response time data available"}

        avg_response_time = sum(indicators["response_times"]) / len(indicators["response_times"])
        recent_status_codes = indicators["status_codes"][-5:]  # Last 5 status codes

        return {
            "average_response_time": avg_response_time,
            "recent_status_codes": recent_status_codes,
            "degraded_performance": avg_response_time > 3.0,
            "last_check": indicators["last_check"].isoformat(),
            "sample_size": len(indicators["response_times"])
        }


class RenderHealer:
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.headers = {
            "Authorization": f"Bearer {api_key}",
            "Accept": "application/json",
        }

    def restart_service(self, service_id: str) -> tuple[bool, str]:
        if not self.api_key:
            return False, "RENDER_API_KEY not configured"
        try:
            resp = requests.post(
                f"https://api.render.com/v1/services/{service_id}/deploys",
                headers=self.headers,
                json={"clearCache": "do_not_clear"},
                timeout=REQUEST_TIMEOUT,
            )
            resp.raise_for_status()
            deploy_id = resp.json().get("deploy", {}).get("id", "unknown")
            logger.info(f"🔄 Render service {service_id} restart triggered. Deploy ID: {deploy_id}")
            return True, f"Restart triggered (deploy: {deploy_id})"
        except requests.RequestException as e:
            logger.error(f"❌ Render restart failed for {service_id}: {e}")
            return False, str(e)


class VercelHealer:
    def __init__(self, token: str):
        self.token = token
        self.headers = {"Authorization": f"Bearer {token}"}

    def rollback(self, project_id: str) -> tuple[bool, str]:
        if not self.token:
            return False, "VERCEL_OIDC_TOKEN not configured"
        try:
            resp = requests.get(
                f"https://api.vercel.com/v6/deployments?projectId={project_id}&limit=5&state=READY",
                headers=self.headers,
                timeout=REQUEST_TIMEOUT,
            )
            resp.raise_for_status()
            deployments = resp.json().get("deployments", [])

            if len(deployments) < 2:
                return False, "Not enough stable deployments for rollback"

            target_id = deployments[1].get("uid")
            redeploy_resp = requests.post(
                "https://api.vercel.com/v13/deployments",
                headers=self.headers,
                json={
                    "deploymentId": target_id,
                    "projectId": project_id,
                    "target": "production",
                },
                timeout=REQUEST_TIMEOUT,
            )
            redeploy_resp.raise_for_status()
            new_deploy_id = redeploy_resp.json().get("id", "unknown")
            logger.info(f"⏪ Vercel project {project_id} rolled back to {target_id}. New deploy: {new_deploy_id}")
            return True, f"Rolled back to {target_id} (new deploy: {new_deploy_id})"
        except requests.RequestException as e:
            logger.error(f"❌ Vercel rollback failed for {project_id}: {e}")
            return False, str(e)


class BackupProviderSwitch:
    def __init__(self, backup_url: str):
        self.backup_url = backup_url
        self.active_provider: dict[str, str] = {}
        self._load_state()

    def _load_state(self):
        state_path = Path(__file__).parent / ".provider_state.json"
        if state_path.exists():
            try:
                self.active_provider = json.loads(state_path.read_text(encoding="utf-8"))
            except Exception as e:
                # বাংলা: state ফাইল corrupt/unreadable হলে চুপচাপ খালি অবস্থায় শুরু করা যাবে না —
                # তাহলে কোন সার্ভিস backup provider-এ ছিল সেই তথ্য হারিয়ে যায় এবং AutoHealer
                # ভুল করে down থাকা primary-তে আবার ট্রাফিক পাঠাতে পারে। তাই এটাকে জোরালোভাবে
                # log + alert করা হচ্ছে, এবং করাপ্ট ফাইলটা backup রেখে override হওয়া থেকে বাঁচানো হচ্ছে।
                logger.error(
                    f"❌ Failed to load provider-switch state from {state_path} "
                    f"(failover routing knowledge may be lost): {e}"
                )
                try:
                    corrupt_backup = state_path.with_suffix(".json.corrupt")
                    state_path.replace(corrupt_backup)
                    logger.warning(f"⚠️ Corrupted state file moved to {corrupt_backup} for inspection.")
                except Exception as move_err:
                    logger.error(f"❌ Could not preserve corrupted state file: {move_err}")
                self.active_provider = {}

    def _save_state(self):
        state_path = Path(__file__).parent / ".provider_state.json"
        try:
            state_path.write_text(json.dumps(self.active_provider, indent=2), encoding="utf-8")
        except Exception as e:
            logger.warning(f"⚠️ Failed to save provider state: {e}")

    def switch_to_backup(self, service_name: str, original_url: str) -> tuple[bool, str]:
        if not self.backup_url:
            return False, "BACKUP_PROVIDER_URL not configured"
        self.active_provider[service_name] = self.backup_url
        self._save_state()
        logger.warning(f"🔄 Traffic switched for {service_name}: {original_url} → {self.backup_url}")
        return True, f"Switched to backup provider: {self.backup_url}"

    def switch_to_primary(self, service_name: str, primary_url: str) -> tuple[bool, str]:
        if service_name in self.active_provider:
            del self.active_provider[service_name]
            self._save_state()
        logger.info(f"🔄 Traffic restored for {service_name}: → {primary_url}")
        return True, f"Restored to primary: {primary_url}"


class AlertManager:
    def __init__(self, webhook_url: str):
        self.webhook_url = webhook_url

    def send_healing_alert(self, record: HealingRecord):
        # বাংলা মন্তব্ব্য: ডিসকর্ড ওয়েবহুকে অটো-হিলিং প্রসেসের স্ট্যাটাস এলার্ট পাঠানো।
        if not self.webhook_url:
            return False
        color = 0x00FF00 if record.success else 0xFF0000
        emoji = "✅" if record.success else "❌"
        embed = {
            "title": f"{emoji} AutoHealer Action: {record.action.upper()}",
            "color": color,
            "fields": [
                {"name": "🔧 Service", "value": record.service_name, "inline": True},
                {"name": "⚡ Action", "value": record.action, "inline": True},
                {
                    "name": "⏱️ Duration",
                    "value": f"{record.duration_seconds:.1f}s",
                    "inline": True,
                },
                {
                    "name": "💬 Result",
                    "value": record.error_message or "Success",
                    "inline": False,
                },
            ],
            "footer": {"text": f"SupremeAI AutoHealer | {record.timestamp}"},
        }
        try:
            resp = requests.post(
                self.webhook_url,
                json={"embeds": [embed]},  # type: ignore
                timeout=REQUEST_TIMEOUT,
                headers={"Content-Type": "application/json"},
            )
            resp.raise_for_status()
            return True
        except Exception as e:
            logger.error(f"❌ Discord alert failed: {e}")
            return False


class AutoHealer:
    def __init__(self):
        self.circuit_breaker = CircuitBreaker()
        self.health_checker = HealthChecker()
        self.render_healer = RenderHealer(RENDER_API_KEY)
        self.vercel_healer = VercelHealer(VERCEL_TOKEN)
        self.backup_switch = BackupProviderSwitch(BACKUP_PROVIDER_URL)
        self.alerter = AlertManager(DISCORD_WEBHOOK)
        self.healing_history: list[HealingRecord] = []
        self.metrics = {
            "total_checks": 0,
            "healing_attempts": 0,
            "successful_healings": 0,
            "failed_healings": 0,
            "circuit_breaker_blocks": 0
        }

    def heal_service(self, service: ServiceConfig) -> HealingRecord:
        # Increment total checks metric
        self.metrics["total_checks"] += 1

        # বাংলা মন্তব্ব্য: একটি ডেক্লারেটিভ সার্ভিসকে সেলফ-হিল করার মূল প্রসেস লুপ।
        start_time = time.time()
        timestamp = datetime.datetime.now(datetime.UTC).isoformat()

        if not self.circuit_breaker.can_heal(service.name):
            # Increment circuit breaker blocks metric
            self.metrics["circuit_breaker_blocks"] += 1
            return HealingRecord(
                timestamp=timestamp,
                service_name=service.name,
                action="circuit_breaker_blocked",
                success=False,
                error_message="Circuit breaker is OPEN — too many recent failures",
                duration_seconds=time.time() - start_time,
            )

        # Enhanced health check with additional metrics
        is_healthy, status_code, error = self.health_checker.check(service.url)
        if is_healthy:
            # Record successful health check and reset failure counters
            self.circuit_breaker.record_success(service.name)
            return HealingRecord(
                timestamp=timestamp,
                service_name=service.name,
                action="health_check_pass",
                success=True,
                error_message=f"Healthy (HTTP {status_code})",
                duration_seconds=time.time() - start_time,
            )

        logger.warning(f"💔 {service.name} is DOWN (HTTP {status_code}, error: {error})")
        self.circuit_breaker.record_failure(service.name)

        # Analyze failure pattern to determine best healing strategy
        failure_analysis = self._analyze_failure_pattern(service.name, error, status_code)

        success = False
        action_taken = HealingAction.NONE
        error_msg = error

        # Increment healing attempts metric
        self.metrics["healing_attempts"] += 1

        # Adaptive healing: prioritize actions based on failure analysis
        prioritized_actions = self._prioritize_healing_actions(service, failure_analysis)

        for action in prioritized_actions:
            if action == HealingAction.RESTART and service.provider == "render" and service.service_id:
                success, msg = self.render_healer.restart_service(service.service_id)
                action_taken = HealingAction.RESTART
                error_msg = msg
                if success:
                    break
            elif action == HealingAction.ROLLBACK and service.provider == "vercel" and service.service_id:
                success, msg = self.vercel_healer.rollback(service.service_id)
                action_taken = HealingAction.ROLLBACK
                error_msg = msg
                if success:
                    break
            elif action == HealingAction.SWITCH_PROVIDER and service.backup_url:
                success, msg = self.backup_switch.switch_to_backup(service.name, service.url)
                action_taken = HealingAction.SWITCH_PROVIDER
                error_msg = msg
                if success:
                    break

        # Verification step: check if healing was successful
        if success and action_taken != HealingAction.SWITCH_PROVIDER:
            # Update metrics based on healing outcome
            if success:
                self.metrics["successful_healings"] += 1
            else:
                self.metrics["failed_healings"] += 1

            # Wait for service to stabilize before verifying
            time.sleep(15)
            is_healthy, status_code, error = self.health_checker.check(service.url)
            if is_healthy:
                self.circuit_breaker.record_success(service.name)
                logger.info(f"✅ {service.name} recovered after {action_taken.value}")
            else:
                success = False
                error_msg = f"Healing action succeeded but service still down: {error}"
                self.circuit_breaker.record_failure(service.name)

        record = HealingRecord(
            timestamp=timestamp,
            service_name=service.name,
            action=action_taken.value,
            success=success,
            error_message=error_msg,
            duration_seconds=time.time() - start_time,
        )
        self.healing_history.append(record)
        self.alerter.send_healing_alert(record)
        return record

    def _analyze_failure_pattern(self, service_name: str, error: str | None, status_code: int | None) -> dict[str, Any]:
        """Analyze failure pattern to inform healing decisions."""
        failure_analysis = {
            "error_type": "connection" if error and "Connection" in error else "http" if status_code else "timeout",
            "severity": "critical" if status_code and status_code >= 500 else "moderate" if status_code and status_code >= 400 else "minor",
            "previous_failures": self.circuit_breaker.failures.get(service_name, 0),
        }
        return failure_analysis

    def _prioritize_healing_actions(self, service: ServiceConfig, analysis: dict[str, Any]) -> list[HealingAction]:
        """Prioritize healing actions based on failure analysis."""
        # Default priority is the configured order
        actions = service.healing_actions.copy()

        # Adjust priorities based on analysis
        if analysis["severity"] == "critical" and HealingAction.SWITCH_PROVIDER in actions:
            # For critical failures, try switching provider first
            actions.remove(HealingAction.SWITCH_PROVIDER)
            actions.insert(0, HealingAction.SWITCH_PROVIDER)
        elif analysis["previous_failures"] > 3 and HealingAction.ROLLBACK in actions:
            # For persistent issues, prioritize rollback
            actions.remove(HealingAction.ROLLBACK)
            actions.insert(0, HealingAction.ROLLBACK)

        return actions

    def get_metrics(self) -> dict:
        """Get current healing metrics."""
        return self.metrics

    def run(self, services: list[ServiceConfig]) -> list[HealingRecord]:
        logger.info(f"🚑 AutoHealer starting scan for {len(services)} services...")
        records: list[HealingRecord] = []

        with ThreadPoolExecutor(max_workers=min(len(services), 5)) as pool:
            futures = {pool.submit(self.heal_service, svc): svc.name for svc in services}
            for future in as_completed(futures):
                svc_name = futures[future]
                try:
                    record = future.result()
                    records.append(record)
                except Exception as e:
                    logger.error(f"❌ Exception healing {svc_name}: {e}")

        logger.info(f"🏁 AutoHealer scan complete. {len(records)} services checked.")
        return records


def main():
    services = [
        ServiceConfig(
            name="SupremeAI API",
            url=os.getenv("SUPREMEAI_API_URL", "https://supremeai.onrender.com/health"),
            provider="render",
            service_id=os.getenv("RENDER_SERVICE_ID", ""),
            healing_actions=[HealingAction.RESTART, HealingAction.SWITCH_PROVIDER],
            backup_url=BACKUP_PROVIDER_URL,
        ),
        ServiceConfig(
            name="SupremeAI Admin Panel",
            url=os.getenv("SUPREMEAI_ADMIN_URL", "https://supremeai-admin.vercel.app"),
            provider="vercel",
            service_id=os.getenv("VERCEL_PROJECT_ID", ""),
            healing_actions=[HealingAction.ROLLBACK, HealingAction.SWITCH_PROVIDER],
            backup_url=BACKUP_PROVIDER_URL,
        ),
    ]

    healer = AutoHealer()

    # Run healing process
    records = healer.run(services)

    # Log metrics
    logger.info("AutoHealer Metrics:")
    metrics = healer.get_metrics()
    for key, value in metrics.items():
        logger.info(f"  {key}: {value}")

    # Log health summaries
    logger.info("Health Summaries:")
    for service in services:
        summary = healer.health_checker.get_health_summary(service.url)
        logger.info(f"  {service.name}: {summary}")


if __name__ == "__main__":
    main()
