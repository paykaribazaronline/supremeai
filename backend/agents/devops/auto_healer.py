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

import requests

# বাংলা মন্তব্য: উইন্ডোজ টার্মিনালে ইউনিকোড/ইমোজি আউটপুট সাপোর্ট করার জন্য এনকোডিং কনফিগার করা হলো।
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except AttributeError:
        pass

# --- Path Setup (consistent with existing codebase) ---
# বাংলা মন্তব্য: পাথ সেটআপ ও কোর কনফিগ ইম্পোর্ট
try:
    from backend.core.config import settings
except ImportError:
    sys.path.insert(0, str(Path(__file__).resolve().parent / "backend"))
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent))
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from core.config import settings

# --- Configuration ---
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("autohealer")

REQUEST_TIMEOUT = int(os.getenv("HTTP_TIMEOUT_SECONDS", "10"))
HEALTH_CHECK_INTERVAL = int(os.getenv("HEALTH_CHECK_INTERVAL", "30"))  # seconds
MAX_RETRIES = int(os.getenv("AUTOHEAL_MAX_RETRIES", "3"))
CIRCUIT_BREAKER_THRESHOLD = int(os.getenv("CIRCUIT_BREAKER_THRESHOLD", "5"))
CIRCUIT_BREAKER_TIMEOUT = int(os.getenv("CIRCUIT_BREAKER_TIMEOUT", "300"))  # 5 minutes
STATE_FILE = Path(__file__).parent / ".autohealer_state.json"

RENDER_API_KEY = os.getenv("RENDER_API_KEY", "")
RENDER_API_KEY_BACKUP = os.getenv("RENDER_API_KEY_BACKUP", "")
VERCEL_TOKEN = os.getenv("VERCEL_OIDC_TOKEN", "")
DISCORD_WEBHOOK = getattr(
    settings, "discord_webhook_url", os.getenv("DISCORD_WEBHOOK_URL", "")
)
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
    """Circuit breaker pattern implementation."""

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
        self._load_state()

    def _load_state(self):
        # বাংলা মন্তব্য: সার্কিট ব্রেকার স্টেট ক্যাশ ফাইল থেকে লোড করা।
        if STATE_FILE.exists():
            try:
                data = json.loads(STATE_FILE.read_text(encoding="utf-8"))
                for svc, st in data.get("states", {}).items():
                    self.state[svc] = CircuitState[st]
                self.failures = data.get("failures", {})
                self.last_failure_time = {
                    k: datetime.datetime.fromisoformat(v)
                    for k, v in data.get("last_failure", {}).items()
                }
            except Exception as e:
                logger.warning(f"⚠️ Failed to load circuit breaker state: {e}")

    def _save_state(self):
        # বাংলা মন্তব্য: সার্কিট ব্রেকার স্টেট ক্যাশ ফাইলে রাইট করা।
        try:
            data = {
                "states": {k: v.name for k, v in self.state.items()},
                "failures": self.failures,
                "last_failure": {
                    k: v.isoformat() for k, v in self.last_failure_time.items()
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
        self.state[service_name] = CircuitState.CLOSED
        self._save_state()

    def record_failure(self, service_name: str) -> CircuitState:
        now = datetime.datetime.now(datetime.UTC)
        self.failures[service_name] = self.failures.get(service_name, 0) + 1
        self.last_failure_time[service_name] = now

        if self.state.get(service_name) == CircuitState.OPEN:
            last_fail = self.last_failure_time.get(service_name)
            if last_fail and (now - last_fail) > datetime.timedelta(
                seconds=self.timeout
            ):
                self.state[service_name] = CircuitState.HALF_OPEN
                logger.info(f"🔓 Circuit breaker HALF_OPEN for {service_name}")
            return self.state[service_name]

        if self.failures[service_name] >= self.threshold:
            self.state[service_name] = CircuitState.OPEN
            logger.warning(
                f"🔒 Circuit breaker OPEN for {service_name} ({self.failures[service_name]} failures)"
            )
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
            if last_fail and (
                datetime.datetime.now(datetime.UTC) - last_fail
            ) > datetime.timedelta(seconds=self.timeout):
                self.state[service_name] = CircuitState.HALF_OPEN
                self._save_state()
                return True
            return False
        return True


class HealthChecker:
    def __init__(self, timeout: int = REQUEST_TIMEOUT):
        self.timeout = timeout

    def check(self, url: str, retries: int = 2) -> tuple[bool, int | None, str | None]:
        # বাংলা মন্তব্য: রিট্রাই সহ সার্ভিস হেলথ চেক মেকানিজম।
        for attempt in range(retries + 1):
            try:
                resp = requests.get(url, timeout=self.timeout, allow_redirects=True)
                if resp.status_code < 500:
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
            logger.info(
                f"🔄 Render service {service_id} restart triggered. Deploy ID: {deploy_id}"
            )
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
            logger.info(
                f"⏪ Vercel project {project_id} rolled back to {target_id}. New deploy: {new_deploy_id}"
            )
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
                self.active_provider = json.loads(
                    state_path.read_text(encoding="utf-8")
                )
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
                    logger.warning(
                        f"⚠️ Corrupted state file moved to {corrupt_backup} for inspection."
                    )
                except Exception as move_err:
                    logger.error(
                        f"❌ Could not preserve corrupted state file: {move_err}"
                    )
                self.active_provider = {}

    def _save_state(self):
        state_path = Path(__file__).parent / ".provider_state.json"
        try:
            state_path.write_text(
                json.dumps(self.active_provider, indent=2), encoding="utf-8"
            )
        except Exception as e:
            logger.warning(f"⚠️ Failed to save provider state: {e}")

    def switch_to_backup(
        self, service_name: str, original_url: str
    ) -> tuple[bool, str]:
        if not self.backup_url:
            return False, "BACKUP_PROVIDER_URL not configured"
        self.active_provider[service_name] = self.backup_url
        self._save_state()
        logger.warning(
            f"🔄 Traffic switched for {service_name}: {original_url} → {self.backup_url}"
        )
        return True, f"Switched to backup provider: {self.backup_url}"

    def switch_to_primary(
        self, service_name: str, primary_url: str
    ) -> tuple[bool, str]:
        if service_name in self.active_provider:
            del self.active_provider[service_name]
            self._save_state()
        logger.info(f"🔄 Traffic restored for {service_name}: → {primary_url}")
        return True, f"Restored to primary: {primary_url}"


class AlertManager:
    def __init__(self, webhook_url: str):
        self.webhook_url = webhook_url

    def send_healing_alert(self, record: HealingRecord):
        # বাংলা মন্তব্য: ডিসকর্ড ওয়েবহুকে অটো-হিলিং প্রসেসের স্ট্যাটাস এলার্ট পাঠানো।
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
                json={"embeds": [embed]},
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

    def heal_service(self, service: ServiceConfig) -> HealingRecord:
        # বাংলা মন্তব্য: একটি ডেক্লারেটিভ সার্ভিসকে সেলফ-হিল করার মূল প্রসেস লুপ।
        start_time = time.time()
        timestamp = datetime.datetime.now(datetime.UTC).isoformat()

        if not self.circuit_breaker.can_heal(service.name):
            return HealingRecord(
                timestamp=timestamp,
                service_name=service.name,
                action="circuit_breaker_blocked",
                success=False,
                error_message="Circuit breaker is OPEN — too many recent failures",
                duration_seconds=time.time() - start_time,
            )

        is_healthy, status_code, error = self.health_checker.check(service.url)
        if is_healthy:
            self.circuit_breaker.record_success(service.name)
            return HealingRecord(
                timestamp=timestamp,
                service_name=service.name,
                action="health_check_pass",
                success=True,
                error_message=f"Healthy (HTTP {status_code})",
                duration_seconds=time.time() - start_time,
            )

        logger.warning(
            f"💔 {service.name} is DOWN (HTTP {status_code}, error: {error})"
        )
        self.circuit_breaker.record_failure(service.name)

        success = False
        action_taken = HealingAction.NONE
        error_msg = error

        for action in service.healing_actions:
            if (
                action == HealingAction.RESTART
                and service.provider == "render"
                and service.service_id
            ):
                success, msg = self.render_healer.restart_service(service.service_id)
                action_taken = HealingAction.RESTART
                error_msg = msg
                if success:
                    break
            elif (
                action == HealingAction.ROLLBACK
                and service.provider == "vercel"
                and service.service_id
            ):
                success, msg = self.vercel_healer.rollback(service.service_id)
                action_taken = HealingAction.ROLLBACK
                error_msg = msg
                if success:
                    break
            elif action == HealingAction.SWITCH_PROVIDER and service.backup_url:
                success, msg = self.backup_switch.switch_to_backup(
                    service.name, service.url
                )
                action_taken = HealingAction.SWITCH_PROVIDER
                error_msg = msg
                if success:
                    break

        if success and action_taken != HealingAction.SWITCH_PROVIDER:
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

    def run(self, services: list[ServiceConfig]) -> list[HealingRecord]:
        logger.info(f"🚑 AutoHealer starting scan for {len(services)} services...")
        records: list[HealingRecord] = []

        with ThreadPoolExecutor(max_workers=min(len(services), 5)) as pool:
            futures = {
                pool.submit(self.heal_service, svc): svc.name for svc in services
            }
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
    healer.run(services)


if __name__ == "__main__":
    main()
