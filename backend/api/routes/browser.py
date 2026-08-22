from datetime import UTC, datetime
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Response
from loguru import logger
from pydantic import BaseModel
import hashlib
import ipaddress
import json
import socket
import urllib.error
import urllib.request
from urllib.parse import urlparse

from api.deps import get_current_user_token
from api.routes.admin_dashboard import require_admin_token
from core.cache.redis_manager import MultiLevelCache
from core.error_bus import with_error_bus
from core.observability.audit_logger import AuditLogger
from core.security.secure_credential_store import SecureCredentialStore


def get_audit() -> AuditLogger:
    return AuditLogger()


def get_credential_store() -> SecureCredentialStore:
    return SecureCredentialStore()


router = APIRouter(prefix="/api/browser", tags=["browser"], dependencies=[Depends(get_current_user_token)])
BROWSER_STATUS: dict[str, Any] = {"browsing": False, "currentUrl": "about:blank"}
RECENT_ACTIVITIES: list[dict[str, Any]] = []
CREDENTIALS: list[dict[str, Any]] = []
PAUSED_STATE: dict[str, Any] = {"paused": False}
URL_PERMISSIONS: list[dict[str, Any]] = []
PERMISSION_REQUESTS: list[dict[str, Any]] = []
SYSTEM_LEARNING: dict[str, Any] = {"enabled": True}
TASKS: dict[str, dict[str, Any]] = {}
FINDINGS: list[dict[str, Any]] = []

# বাংলা মন্তব্য: সার্কিট ব্রেকার থ্রেশোল্ড — টাস্ক এক্সিকিউশন ক্যাপ (৪৫ সেকেন্ড)
EXECUTION_CAP_MS = 45000


class GoalRequest(BaseModel):
    goal: str


class NavigateRequest(BaseModel):
    url: str


class ClickRequest(BaseModel):
    selector: str


class FillRequest(BaseModel):
    selector: str
    value: str


class ClickAtRequest(BaseModel):
    x: int
    y: int


class KeyRequest(BaseModel):
    key: str


class CredentialRequest(BaseModel):
    serviceName: str  # -- camelCase required to match frontend JSON API contract
    username: str
    password: str
    userId: str | None = "default"  # -- camelCase required to match frontend JSON API contract


class UrlPermissionRequest(BaseModel):
    urlPattern: str  # -- camelCase required to match frontend JSON API contract
    userId: str | None = "default"  # -- camelCase required to match frontend JSON API contract
    reason: str | None = "None"


class DecisionRequest(BaseModel):
    approved: bool


@router.get("/surf/status")
def get_status():
    return BROWSER_STATUS


@router.post("/surf/start")
def start_surf():
    BROWSER_STATUS["browsing"] = True
    return {"status": "started"}


@router.post("/surf/stop")
def stop_surf():
    BROWSER_STATUS["browsing"] = False
    return {"status": "stopped"}


@router.get("/activity/recent")
def get_recent_activity():
    return {"activities": RECENT_ACTIVITIES}


@router.get("/credentials", dependencies=[Depends(require_admin_token)])
@with_error_bus("get_credentials")
def get_credentials(userId: str = "default"):
    import json

    cred_store = get_credential_store()
    user_creds = []
    for c in CREDENTIALS:
        if c.get("userId") == userId:
            decrypted = cred_store.decrypt(c.get("ciphertext", ""), c.get("key_ref"))
            try:
                decrypted_dict = json.loads(decrypted)
            except Exception:
                logger.exception("Unhandled exception")
                decrypted_dict = {}

            masked_dict = {}
            for k, v in decrypted_dict.items():
                if k in (
                    "password",
                    "token",
                    "secret",
                    "api_key",
                    "username",
                ) and isinstance(v, str):
                    if k == "username":
                        masked_dict[k] = v
                    else:
                        masked_dict[k] = cred_store.mask(v)
                else:
                    masked_dict[k] = v
            masked_dict["serviceName"] = c.get("serviceName")
            user_creds.append(masked_dict)
    return {"credentials": user_creds}


@router.post("/credentials", dependencies=[Depends(require_admin_token)])
def save_credential(cred: CredentialRequest):
    import json

    cred_store = get_credential_store()
    ciphertext, key_ref = cred_store.encrypt(json.dumps(cred.model_dump()))
    new_cred = {
        "id": f"cred_{len(CREDENTIALS) + 1}",
        "userId": cred.userId,
        "serviceName": cred.serviceName,
        "ciphertext": ciphertext,
        "key_ref": key_ref,
    }
    CREDENTIALS.append(new_cred)
    get_audit().log_decision(
        action_type="browser_credential_saved",
        decision_details=f"Stored credential for service '{cred.serviceName}'",
        reasoning=f"User '{cred.userId}' saved browser credential.",
    )
    return {"id": new_cred["id"], "serviceName": cred.serviceName}


@router.delete("/credentials/{id}", dependencies=[Depends(require_admin_token)])
def delete_credential(credential_id: str):
    global CREDENTIALS
    CREDENTIALS = [c for c in CREDENTIALS if c.get("id") != credential_id]
    return {"success": True}


@router.post("/surf/resume")
def resume_surf(body: dict[str, str]):
    PAUSED_STATE["paused"] = False
    return {"status": "resumed"}


@router.post("/surf/skip-auth")
def skip_auth(body: dict[str, str]):
    PAUSED_STATE["paused"] = False
    return {"status": "auth_skipped"}


@router.post("/surf/pause-manual")
def pause_manual(body: dict[str, str]):
    PAUSED_STATE["paused"] = True
    return {"status": "paused_for_manual"}


@router.get("/surf/paused-state")
def get_paused_state():
    return PAUSED_STATE


@router.get("/urls/allowed")
def get_allowed_urls(userId: str = "default"):
    allowed = [u for u in URL_PERMISSIONS if u.get("type") == "allowed" and u.get("userId") == userId]
    return {"urls": allowed}


@router.get("/urls/denied")
def get_denied_urls(userId: str = "default"):
    denied = [u for u in URL_PERMISSIONS if u.get("type") == "denied" and u.get("userId") == userId]
    return {"urls": denied}


@router.post("/urls/allowed", dependencies=[Depends(require_admin_token)])
def add_allowed_url(req: UrlPermissionRequest):
    perm = req.model_dump()
    perm["id"] = f"perm_{len(URL_PERMISSIONS) + 1}"
    perm["type"] = "allowed"
    URL_PERMISSIONS.append(perm)
    return perm


@router.post("/urls/denied", dependencies=[Depends(require_admin_token)])
def add_denied_url(req: UrlPermissionRequest):
    perm = req.model_dump()
    perm["id"] = f"perm_{len(URL_PERMISSIONS) + 1}"
    perm["type"] = "denied"
    URL_PERMISSIONS.append(perm)
    return perm


@router.post("/urls/allowAll", dependencies=[Depends(require_admin_token)])
def allow_all_urls(userId: str = "default"):
    perm = {
        "id": f"perm_{len(URL_PERMISSIONS) + 1}",
        "urlPattern": "*",
        "userId": userId,
        "type": "allowAll",
        "reason": "Allow all URLs",
    }
    URL_PERMISSIONS.append(perm)
    return perm


@router.delete("/urls/{id}", dependencies=[Depends(require_admin_token)])
def delete_url(url_id: str):
    global URL_PERMISSIONS
    URL_PERMISSIONS = [u for u in URL_PERMISSIONS if u.get("id") != url_id]
    return {"success": True}


@router.get("/urls/requests")
def get_requests():
    return {"requests": PERMISSION_REQUESTS}


@router.post("/urls/requests/{id}/decision")
def decision(request_id: str, req: DecisionRequest):
    for r in PERMISSION_REQUESTS:
        if r["id"] == request_id:
            r["status"] = "APPROVED" if req.approved else "DENIED"
            return {"success": True}
    raise HTTPException(status_code=404, detail="Request not found")


@router.get("/system-learning")
def get_system_learning():
    return SYSTEM_LEARNING


@router.post("/system-learning/toggle")
def toggle_learning(body: dict[str, bool]):
    SYSTEM_LEARNING["enabled"] = body.get("enabled", True)
    return {"success": True}


@router.get("/tasks")
def get_tasks():
    return {"tasks": list(TASKS.values())}


@router.post("/tasks")
def create_task(req: GoalRequest):
    task_id = f"task_{len(TASKS) + 1}"
    task = {
        "id": task_id,
        "goal": req.goal,
        "status": "ACTIVE",
        "createdAt": datetime.now(UTC).isoformat(),
        "durationMs": 0,
    }
    TASKS[task_id] = task
    return task


@router.post("/tasks/{id}/circuit-open")
def set_task_circuit_open(task_id: str):
    """বাংলা মন্তব্য: টাস্কটি সার্কিট ব্রেকার স্টেটে সেট করে — UI তে লাল সতর্ক-আভা দেখানোর জন্য"""
    if task_id not in TASKS:
        raise HTTPException(status_code=404, detail="Task not found")
    TASKS[task_id]["status"] = "CIRCUIT_OPEN"
    TASKS[task_id]["durationMs"] = EXECUTION_CAP_MS
    return {"success": True, "status": "CIRCUIT_OPEN"}


@router.post("/tasks/{id}/complete")
def set_task_complete(task_id: str):
    """বাংলা মন্তব্য: টাস্ক সফলভাবে সম্পন্ন হলে কল করুন"""
    if task_id not in TASKS:
        raise HTTPException(status_code=404, detail="Task not found")
    TASKS[task_id]["status"] = "SUCCESS"
    return {"success": True, "status": "SUCCESS"}


@router.post("/tasks/{id}/fail")
def set_task_failed(task_id: str):
    """বাংলা মন্তব্য: টাস্ক ব্যর্থ হলে কল করুন"""
    if task_id not in TASKS:
        raise HTTPException(status_code=404, detail="Task not found")
    TASKS[task_id]["status"] = "FAILED"
    return {"success": True, "status": "FAILED"}


@router.delete("/tasks/{id}")
def delete_task(task_id: str):
    if task_id in TASKS:
        del TASKS[task_id]
        return {"success": True}
    raise HTTPException(status_code=404, detail="Task not found")


@router.get("/tasks/{id}/findings")
def get_findings(task_id: str):
    task_findings = [f for f in FINDINGS if f.get("taskId") == task_id]
    return {"findings": task_findings}


@router.post("/findings")
def add_finding(finding: dict[str, Any]):
    FINDINGS.append(finding)
    return finding


@router.get("/surf/screenshot")
def get_screenshot():
    # Return a mock transparent 1x1 PNG or read browser screenshot if initialized
    mock_png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
    return {"screenshot": mock_png}


@router.post("/surf/navigate")
def navigate(req: NavigateRequest):
    BROWSER_STATUS["currentUrl"] = req.url
    RECENT_ACTIVITIES.append(
        {
            "url": req.url,
            "action": "navigate",
            "timestamp": datetime.now(UTC).isoformat(),
        }
    )
    return {"success": True}


@router.post("/surf/click")
def click(req: ClickRequest):
    RECENT_ACTIVITIES.append(
        {
            "url": str(BROWSER_STATUS["currentUrl"]),
            "action": f"click {req.selector}",
            "timestamp": datetime.now(UTC).isoformat(),
        }
    )
    return {"success": True}


@router.post("/surf/fill")
def fill(req: FillRequest):
    RECENT_ACTIVITIES.append(
        {
            "url": str(BROWSER_STATUS["currentUrl"]),
            "action": f"fill {req.selector} with {req.value}",
            "timestamp": datetime.now(UTC).isoformat(),
        }
    )
    return {"success": True}


@router.post("/surf/click-at")
def click_at(req: ClickAtRequest):
    RECENT_ACTIVITIES.append(
        {
            "url": str(BROWSER_STATUS["currentUrl"]),
            "action": f"click at {req.x}, {req.y}",
            "timestamp": datetime.now(UTC).isoformat(),
        }
    )
    return {"success": True}


@router.post("/surf/type-key")
def type_key(req: KeyRequest):
    RECENT_ACTIVITIES.append(
        {
            "url": str(BROWSER_STATUS["currentUrl"]),
            "action": f"type key {req.key}",
            "timestamp": datetime.now(UTC).isoformat(),
        }
    )
    return {"success": True}


@router.get("/surf/accessibility")
def get_accessibility_tree():
    return {"role": "WebArea", "name": "SupremeAI Console", "children": []}


@router.post("/simulate-activity")
def simulate_activity(body: dict[str, str]):
    activity = {
        "url": body.get("url", "http://example.com"),
        "action": body.get("action", "surf"),
        "title": body.get("title", "Page Title"),
        "reasoning": body.get("reasoning", "Exploring content"),
        "timestamp": datetime.now(UTC).isoformat(),
    }
    RECENT_ACTIVITIES.append(activity)
    return activity


@router.post("/tasks/{id}/step")
def execute_step(task_id: str):
    if task_id not in TASKS:
        raise HTTPException(status_code=404, detail="Task not found")
    # Simulate a step execution
    return {
        "success": True,
        "action": "navigated to dashboard",
        "details": "Autonomous step succeeded",
    }


# ──────────────────────────────────────────────
# বাংলা মন্তব্য: সেশন স্টোর — Firestore-ভিত্তিক সেশন সিঙ্ক (VaultPage-এর মত ব্যাকএন্ড API কল)
# ──────────────────────────────────────────────
SESSIONS: dict[str, dict[str, Any]] = {}


class SessionMessageIn(BaseModel):
    id: int
    sender: str
    text: str
    timestamp: str


class SessionIn(BaseModel):
    id: str
    title: str
    status: str = "running"
    created_at: str = ""
    updated_at: str = ""
    messages: list[SessionMessageIn] = []


@router.get("/sessions")
def list_sessions():
    """বাংলা মন্তব্য: সব সেশন তালিকা রিটার্ন করে"""
    return {"sessions": list(SESSIONS.values())}


@router.get("/sessions/{session_id}")
def get_session(session_id: str):
    """বাংলা মন্তব্য: নির্দিষ্ট সেশন রিটার্ন করে"""
    if session_id not in SESSIONS:
        raise HTTPException(status_code=404, detail="Session not found")
    return SESSIONS[session_id]


@router.post("/sessions")
def create_session(session: SessionIn):
    """বাংলা মন্তব্য: নতুন সেশন তৈরি করে"""
    now = datetime.now(UTC).isoformat()
    data = session.model_dump()
    if not data.get("created_at"):
        data["created_at"] = now
    if not data.get("updated_at"):
        data["updated_at"] = now
    SESSIONS[session.id] = data
    return {"success": True, "session": data}


@router.put("/sessions/{session_id}")
def update_session(session_id: str, session: SessionIn):
    """বাংলা মন্তব্য: বিদ্যমান সেশন আপডেট করে"""
    if session_id not in SESSIONS:
        raise HTTPException(status_code=404, detail="Session not found")
    data = session.model_dump()
    data["updated_at"] = datetime.now(UTC).isoformat()
    SESSIONS[session_id] = data
    return {"success": True, "session": data}


@router.delete("/sessions/{session_id}")
def delete_session(session_id: str):
    """বাংলা মন্তব্য: সেশন মুছে ফেলে"""
    SESSIONS.pop(session_id, None)
    return {"success": True}


from tools.ai_agents.browser_agent import BrowseRequest

from pydantic import BaseModel


class ScrapeRequest(BaseModel):
    url: str

# বাংলা মন্তব্য: আগের BrowserAgent গ্লোবাল সিঙ্গলটন সরিয়ে দিয়েছি।
# এখন ব্রাউজার অটোমেশন স্ক্র্যাপার মাইক্রোসার্ভিসে HTTP প্রক্সি করে (zero-cost,
# decoupled)। AGENTS.md §2: "Never treat tasks in isolation" — এই পরিবর্তনের পাশাপাশি
# Cloudflare Worker (worker.js) এবং render.yaml-এ scraper route যোগ করতে হবে।

import httpx
from core.config import settings


_SCRAPER_URL = settings.scraper_service_url.rstrip("/") if settings.scraper_service_url else None

# Hybrid-plan cache: keep scraped results in Upstash Redis (L2) + in-memory (L1)
# so the (off-Render, scale-to-zero) scraper microservice is invoked as rarely as
# possible — directly cutting its compute/quota consumption.
_SCRAPE_CACHE_TTL = 3600  # 1h
_scrape_cache = MultiLevelCache(l2_ttl=_SCRAPE_CACHE_TTL)


def _scrape_cache_key(url: str) -> str:
    return "scrape_cache:" + hashlib.sha256(url.encode("utf-8")).hexdigest()


async def _proxy_to_scraper(endpoint: str, payload: dict) -> dict:
    """Forward browser/scrape requests to the standalone scraper microservice."""
    if not _SCRAPER_URL:
        # Fallback: use local BrowserAgent (for local dev / when scraper service is not deployed)
        from tools.ai_agents.browser_agent import BrowserAgent
        agent = BrowserAgent()
        return await agent.navigate_and_interact(**payload)
    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            resp = await client.post(f"{_SCRAPER_URL}/{endpoint}", json=payload)
            return resp.json()
    except (httpx.RequestError, httpx.HTTPStatusError) as e:
        logger.error(f"Scraper service proxy failed: {e}")
        return {"success": False, "error": str(e)}


async def _cached_scrape(payload: dict) -> dict:
    """Scrape with a Redis-backed cache (idempotent fetch only)."""
    url = payload.get("url", "")
    if not url:
        return await _proxy_to_scraper("scrape", payload)
    key = _scrape_cache_key(url)
    cached = await _scrape_cache.get(key)
    if cached is not None:
        try:
            return json.loads(cached)
        except (json.JSONDecodeError, TypeError):
            pass
    result = await _proxy_to_scraper("scrape", payload)
    if isinstance(result, dict) and result.get("success"):
        await _scrape_cache.set(key, json.dumps(result), ttl=_SCRAPE_CACHE_TTL)
    return result


@router.post("/scrape", dependencies=[Depends(require_admin_token)])
async def scrape(request: ScrapeRequest):
    """Fetch URL and return cleaned content via the Scraper Microservice."""
    result = await _cached_scrape({"url": request.url})
    return result


@router.post("/browse", dependencies=[Depends(require_admin_token)])
async def browse(request: BrowseRequest):
    """Navigate to a URL and perform browser actions via the Scraper Microservice (Admin Only)."""
    if request.action in ("click", "type", "scroll", "screenshot"):
        result = await _proxy_to_scraper(
            "browse",
            {"url": request.url, "action": request.action, "selector": request.selector,
             "text": request.text, "wait_for": request.wait_for},
        )
        return result

    # Default action (fetch) — delegate to scraper service (cache-backed)
    result = await _cached_scrape({"url": request.url})
    return result


@router.post("/extract", dependencies=[Depends(require_admin_token)])
async def extract(url: str, extraction_prompt: str):
    """Fetch page and extract structured data with AI (Admin Only).

    Now proxies to the standalone scraper microservice for browser automation,
    then performs AI extraction on the returned content.
    """
    from tools.browser.ai_web_extractor import AIWebExtractor

    extractor = AIWebExtractor()
    return await extractor.extract_data(url, extraction_prompt)


# বাংলা মন্তব্য: ইন-অ্যাপ ব্রাউজার proxy (public) — বাহিরের সাইট X-Frame-Options/frame-ancestors দিয়ে
# iframe ব্লক করে, তাই সার্ভার-সাইড ফেচ করে iframe-এ রেন্ডার করা হয়। SSRF প্রতিরোধ জরুরি।
_BLOCKED_NETS = [
    ipaddress.ip_network("0.0.0.0/8"),
    ipaddress.ip_network("10.0.0.0/8"),
    ipaddress.ip_network("127.0.0.0/8"),
    ipaddress.ip_network("169.254.0.0/16"),
    ipaddress.ip_network("172.16.0.0/12"),
    ipaddress.ip_network("192.168.0.0/16"),
    ipaddress.ip_network("::1/128"),
    ipaddress.ip_network("fc00::/7"),
    ipaddress.ip_network("fe80::/10"),
]


def _host_is_blocked(hostname: str) -> bool:
    try:
        infos = socket.getaddrinfo(hostname, 80)
    except (socket.gaierror, socket.herror, OSError):
        # DNS resolution failure means we cannot verify the host — treat as
        # blocked to fail-closed (deny-by-default) rather than masking the error.
        logger.warning("Host resolution failed for %s", hostname, exc_info=True)
        return True
    for info in infos:
        raw_ip = info[4][0].split("%")[0]
        try:
            addr = ipaddress.ip_address(raw_ip)
        except ValueError:
            return True
        if addr.is_loopback or addr.is_private or addr.is_reserved or addr.is_link_local:
            return True
        for net in _BLOCKED_NETS:
            if addr in net:
                return True
    return False


@router.get("/render")
def render_proxy(url: str):
    """Server-side web proxy so the in-app browser can render sites that block iframes.

    Uses stdlib urllib only (no third-party http client) so the route cannot be dropped
    because of a missing optional dependency at import time.
    """
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise HTTPException(status_code=400, detail="Only absolute http(s) URLs are supported.")
    if _host_is_blocked(parsed.hostname):
        raise HTTPException(status_code=400, detail="Blocked or unresolvable host.")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "SupremeAI-Browser/1.0"})
        with urllib.request.urlopen(req, timeout=20) as resp:
            ctype = resp.headers.get("Content-Type", "") or ""
            data = resp.read()
        if len(data) > 5 * 1024 * 1024:
            raise HTTPException(status_code=502, detail="Response too large to proxy.")
        proxy_headers = {
            "Cache-Control": "no-store",
            "X-Frame-Options": "ALLOWALL",
            "Content-Security-Policy": "frame-ancestors *",
        }
        if "text/html" in ctype:
            text = data.decode("utf-8", errors="replace")
            base_tag = f'<base href="{url}">'
            if "<head" in text:
                text = text.replace("<head", f"<head>{base_tag}", 1)
            elif "<HEAD" in text:
                text = text.replace("<HEAD", f"<HEAD>{base_tag}", 1)
            else:
                text = base_tag + text
            return Response(
                content=text,
                media_type="text/html; charset=utf-8",
                headers=proxy_headers,
            )
        return Response(content=data, media_type=ctype or "application/octet-stream", headers=proxy_headers)
    except HTTPException:
        raise
    except urllib.error.HTTPError as e:
        logger.error(f"Render proxy upstream error: {e.code} {e.reason}")
        raise HTTPException(status_code=502, detail=f"Upstream returned {e.code}.")
    except Exception as e:
        logger.error(f"Render proxy error: {e!s}")
        raise HTTPException(status_code=502, detail="Failed to fetch the requested URL.")


# ═════════════════════════════════════════════════════════════════════════════
# 🌐 SUPREMEBROWSER ADVANCED COGNITIVE SUITE (L1 — L5)
# ═════════════════════════════════════════════════════════════════════════════

class SemanticClickRequest(BaseModel):
    target: str
    context: str = ""


class SwarmExploreRequest(BaseModel):
    site: str
    sub_goals: list[str]


@router.post("/semantic-click")
async def semantic_click(req: SemanticClickRequest):
    """L4: Click by meaning — matches natural language intent to dynamic DOM embeddings."""
    from browser.semantic_dom import SemanticDOM
    sdom = SemanticDOM(page=None)
    await sdom.build_index()
    el = await sdom.query(req.target)
    return {"status": "clicked", "matched_text": el.get("text"), "xpath": el.get("xpath"), "confidence": el.get("semantic_confidence")}


@router.post("/smart-click")
async def smart_click(req: SemanticClickRequest):
    """L4 Cascade: Semantic DOM → Vision Grounding Fallback → HITL Takeover."""
    from browser.semantic_dom import ElementNotFoundSemantically, SemanticDOM
    from browser.vision_grounding import LowConfidenceGrounding, VisionGrounding

    # 1. Semantic DOM
    try:
        sdom = SemanticDOM(page=None)
        await sdom.build_index()
        el = await sdom.query(req.target)
        return {"status": "clicked", "method": "semantic_dom", "element": el}
    except ElementNotFoundSemantically:
        pass

    # 2. Vision Grounding
    try:
        vg = VisionGrounding(page=None)
        click_res = await vg.click(req.target)
        return {"status": "clicked", "method": "vision_grounding", "coordinates": click_res}
    except LowConfidenceGrounding:
        pass

    # 3. HITL Takeover Escalation
    return {"status": "escalated_to_hitl", "method": "hitl", "target": req.target}


@router.post("/autonomous/run")
async def run_autonomous_goal(req: GoalRequest):
    """L5: Execute natural language browsing goal with reasoning, replanning, and memory."""
    from browser.autonomous_browser import AutonomousBrowserAgent
    agent = AutonomousBrowserAgent(session=None)
    result = await agent.achieve(req.goal)
    return result


@router.post("/swarm/explore")
async def explore_swarm(req: SwarmExploreRequest):
    """L5+: Deploy parallel agent swarm across web sub-goals and synthesize multi-agent findings."""
    from browser.swarm_browser import SwarmBrowser
    swarm = SwarmBrowser()
    result = await swarm.explore(req.site, req.sub_goals)
    return result

