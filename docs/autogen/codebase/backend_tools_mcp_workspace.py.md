# 📄 ফাইল: backend/tools/mcp_workspace.py

**প্রকার:** .py  
**সাইজ:** 13,912 বাইট  
**আপডেট:** 2026-07-04T22:28:39.300750

---

## কোড

```py
#!/usr/bin/env python3
"""
MCP Server for Dynamic Workspace Isolation in SupremeAI 2.0.

এই সার্ভারটি একাধিক প্রকল্পের জন্য ডাইনামিক ওয়ার্কস্পেস আইসোলেশন প্রদান করে,
যাতে একই সিস্টেমে একাধিক প্রজেক্ট চালালে ডেটা বা কোড মিক্স-আপ হয় না।
"""

import os
import json
import time
import tempfile
import contextlib
from pathlib import Path
from typing import Dict, Any
from enum import Enum

from pydantic import BaseModel, Field, ConfigDict
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("workspace_mcp")

CHARACTER_LIMIT = 25000
# বাংলা মন্তব্য: সেশন ফাইল পাথ কে প্রোজেক্ট রুট অনুসারে সেট করা হচ্ছে
_workspace_root = Path(__file__).parent.parent.parent
WORKSPACE_SESSION_FILE = _workspace_root / ".kilo" / "workspace" / "session.json"
WORKSPACE_CONFIG_FILE = _workspace_root / ".kilo" / "workspace" / "config.json"


class WorkspaceType(str, Enum):
    """ওয়ার্কস্পেসের ধরন।"""
    ECOMMERCE_BACKEND = "ecommerce_backend"
    ECOMMERCE_FRONTEND = "ecommerce_frontend"
    MOBILE_FLUTTER = "mobile_flutter"
    ANDROID_JAVA = "android_java"
    ADMIN_PANEL = "admin_panel"
    INFRASTRUCTURE = "infrastructure"


class WorkspaceContextInput(BaseModel):
    """ওয়ার্কস্পেস কনটেক্সট সেটআপের জন্য ইনপুট।"""
    model_config = ConfigDict(
        str_strip_whitespace=True,
        validate_assignment=True,
    )

    project_type: WorkspaceType = Field(..., description="কাজ করা বর্তমান প্রোজেক্টের ধরন")
    tenant_id: str | None = Field(default=None, description="টেন্যান্ট আইডি (যদি মাল্টি-টেন্যান্ট)")


class ScopedFilePathInput(BaseModel):
    """স্কোপযুক্ত ফাইল পাথ জার্জ্যাঙ্করনের জন্য।"""
    model_config = ConfigDict(str_strip_whitespace=True, validate_assignment=True)

    relative_path: str = Field(..., description="কাজ করা ফাইলের রিলেটিভ পাথ")
    project_type: WorkspaceType | None = Field(default=None, description="প্রোজেক্টের ধরন")


_workspace_config: Dict[str, Any] = {}


def _load_workspace_config() -> Dict[str, Any]:
    """ওয়ার্কস্পেস কনফিগারেশন লোড করে।"""
    config_path = Path(WORKSPACE_CONFIG_FILE)
    if config_path.exists():
        try:
            config = json.loads(config_path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            return {}
        # বাংলা মন্তব্য: কনফিগারেশনয় থাকা পাথগুলো সর্বদা প্রোজেক্ট রুটের সাপেক্ষে করে রূপান্তর করা হচ্ছে
        workspace_config = config.get("workspace", {})
        for key, value in workspace_config.items():
            if not Path(value).is_absolute():
                workspace_config[key] = str(_workspace_root / value)
        config["workspace"] = workspace_config
        return config
    return {}


def _get_workspace_path(project_type: WorkspaceType) -> Path:
    """প্রোজেক্টের ধরন থেকে ডাইনামিক ওয়ার্কস্পেস পাথ গণনা করে।"""
    config = _load_workspace_config()
    workspace_config = config.get("workspace", {})

    path_mapping = {
        WorkspaceType.ECOMMERCE_BACKEND: workspace_config.get("ecommerce_backend", "backend"),
        WorkspaceType.ECOMMERCE_FRONTEND: workspace_config.get("ecommerce_frontend", "apps/studio-client"),
        WorkspaceType.MOBILE_FLUTTER: workspace_config.get("mobile_flutter", "apps/mobile"),
        WorkspaceType.ANDROID_JAVA: workspace_config.get("android_java", "apps/android"),
        WorkspaceType.ADMIN_PANEL: workspace_config.get("admin_panel", "admin"),
        WorkspaceType.INFRASTRUCTURE: workspace_config.get("infrastructure", "infrastructure"),
    }

    path = path_mapping.get(project_type, "backend")
    # বাংলা মন্তব্য: কনফিগার্ড পাথ যদি অ্যাবসলুট হয়, সেটাই রিটার্ন করা হবে
    if Path(path).is_absolute():
        return Path(path)

    # অন্যথায় প্রোজেক্ট রুটের সাপেক্ষে পাথ তৈরি করা হবে
    return _workspace_root / path


def _ensure_session_dir():
    """সেশন ডিরেক্টরি তৈরি করে।"""
    Path(WORKSPACE_SESSION_FILE).parent.mkdir(parents=True, exist_ok=True)


# বাংলা মন্তব্য: কনকারেন্ট রাইট এড়াতে ডিরেক্টরি লক এবং ফাইল করাপশন এড়াতে অ্যাটমিক রাইট ব্যবহার করা হলো
@contextlib.contextmanager
def _session_file_lock(lock_path: Path):
    lock_dir = Path(str(lock_path) + ".lock")
    acquired = False
    for _ in range(50):  # 5 সেকেন্ড পর্যন্ত চেষ্টা করবে
        try:
            lock_dir.mkdir(parents=True, exist_ok=False)
            acquired = True
            break
        except FileExistsError:
            time.sleep(0.1)
    try:
        yield acquired
    finally:
        if acquired:
            try:
                lock_dir.rmdir()
            except OSError:
                pass

def _save_workspace_session(project_type: WorkspaceType, tenant_id: str | None = None):
    """ওয়ার্কস্পেস সেশন সংরক্ষণ করে।"""
    _ensure_session_dir()
    session = {
        "project_type": project_type.value,
        "tenant_id": tenant_id,
        "workspace_path": str(_get_workspace_path(project_type)),
    }
    session_path = Path(WORKSPACE_SESSION_FILE)
    
    with _session_file_lock(session_path):
        temp_fd, temp_path = tempfile.mkstemp(dir=str(session_path.parent), prefix=session_path.name + ".tmp")
        try:
            with os.fdopen(temp_fd, 'w', encoding='utf-8') as f:
                f.write(json.dumps(session, indent=2, ensure_ascii=False))
            os.replace(temp_path, str(session_path))
        except Exception as e:
            try:
                os.unlink(temp_path)
            except OSError:
                pass
            raise e


@mcp.tool(
    name="workspace_set_context",
    annotations={
        "title": "Set Active Workspace Context",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": False,
    }
)
async def workspace_set_context(params: WorkspaceContextInput) -> str:
    """
    বর্তমান সক্রিয় ওয়ার্কস্পেস কনটেক্সট সেট করে।

    এই টুলটি সেট করা ওয়ার্কস্পেসকে অনুসরণ করে পরবর্তী ফাইল অপারেশনগুলো স্বয়ংক্রিয়াভাবে
    সঠিক ডিরেক্টরিতে রিন্টার করে। এডমিন অথরাইজেশন প্রয়োজন হলে চেক করে।

    Args:
        params (WorkspaceContextInput): ইনপুট প্যারামিটার সম্বলিত:
            - project_type (WorkspaceType): কাজ করা প্রোজেক্টের ধরন
            - tenant_id (Optional[str]): টেন্যান্ট আইডি যদি মাল্টি-টেন্যান্ট অ্যাপ্লিকেশনের ক্ষেত্রে

    Returns:
        str: JSON-formatted সেশন তথ্য সহ সফলতা বার্তা
    """
    admin_authorized = os.getenv("ADMIN_AUTHORIZED", "false").lower() == "true"
    if not admin_authorized and params.project_type == WorkspaceType.ADMIN_PANEL:
        return json.dumps({
            "error": "Admin authorization required for admin panel workspace",
            "message": "Set ADMIN_AUTHORIZED=true in environment to access admin workspace"
        }, ensure_ascii=False)

    _save_workspace_session(params.project_type, params.tenant_id)

    return json.dumps({
        "success": True,
        "workspace_path": str(_get_workspace_path(params.project_type)),
        "project_type": params.project_type.value,
        "tenant_id": params.tenant_id,
        "message": f"Workspace context set to {params.project_type.value}"
    }, ensure_ascii=False)


@mcp.tool(
    name="workspace_get_scoped_path",
    annotations={
        "title": "Get Scoped File Path",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": False,
    }
)
async def workspace_get_scoped_path(params: ScopedFilePathInput) -> str:
    """
    ওয়ার্কস্পেস কনটেক্সটের ভিত্তিতে স্কোপযুক্ত ফাইল পাথ রিট্রাইস করে।

    এই টুলটি বর্তমান সক্রিয় ওয়ার্কস্পেসকে অনুসরণ করে একটি সুরক্ষিত পাথ
    তৈরি করে, যাতে এক প্রোজেক্টের ফাইল অ্যাক্সেস অন্য প্রোজেক্টে লিক করে না।

    Args:
        params (ScopedFilePathInput): ইনপুট প্যারামিটার সম্বলিত:
            - relative_path (str): কাজ করা ফাইলের রিলেটিভ পাথ
            - project_type (Optional[WorkspaceType]): স্পষ্ট করা প্রোজেক্টের ধরন (ঐচ্ছিক)

    Returns:
        str: JSON-formatted স্কোপযুক্ত পাথ তথ্য
    """
    workspace_path = Path("backend")
    session_file = Path(WORKSPACE_SESSION_FILE)
    
    if session_file.exists():
        try:
            session = json.loads(session_file.read_text(encoding="utf-8"))
            workspace_path = Path(session.get("workspace_path", "backend"))
        except (json.JSONDecodeError, OSError):
            workspace_path = Path("backend")

    if params.project_type:
        workspace_path = _get_workspace_path(params.project_type)

    # বাংলা মন্তব্য: পাথ ট্রাভার্সাল প্রতিরোধ এবং সিমলিংক আক্রমণ পরীক্ষা
    if "\\" in params.relative_path:
        return json.dumps({
            "error": "Invalid path",
            "message": "Path traversal not allowed - path must be a relative path within the workspace"
        }, ensure_ascii=False)

    ref_path = Path(params.relative_path)
    if ref_path.is_absolute() or ".." in ref_path.parts:
        return json.dumps({
            "error": "Invalid path",
            "message": "Path traversal not allowed - path must be a relative path within the workspace"
        }, ensure_ascii=False)

    scoped_path = workspace_path / ref_path

    try:
        resolved_scoped = scoped_path.resolve()
        resolved_workspace = workspace_path.resolve()
        
        # সিমলিংক যদি ওয়ার্কস্পেসের বাইরে ফাইল নির্দেশ করে তবে তা ব্লক করা হলো
        if scoped_path.is_symlink():
            real_target = Path(os.readlink(scoped_path)).resolve()
            real_target.relative_to(resolved_workspace)
            
        resolved_scoped.relative_to(resolved_workspace)
    except ValueError:
        return json.dumps({
            "error": "Invalid path",
            "message": "Path traversal not allowed - path must be within workspace"
        }, ensure_ascii=False)

    return json.dumps({
        "scoped_path": str(scoped_path),
        "exists": scoped_path.exists(),
        "workspace_root": str(workspace_path)
    }, ensure_ascii=False)


@mcp.tool(
    name="workspace_list_projects",
    annotations={
        "title": "List Available Workspace Projects",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": False,
    }
)
async def workspace_list_projects() -> str:
    """
    সক্রিয় ওয়ার্কস্পেসে উপলব্ধ প্রকল্পগুলোর তালিকা দেখায়।

    Returns:
        str: JSON-formatted প্রকল্প তালিকা
    """
    config = _load_workspace_config()

    projects = [
        {"type": ws_type.value, "path": config.get(ws_type.value, "default")}
        for ws_type in WorkspaceType
    ]

    session_file = Path(WORKSPACE_SESSION_FILE)
    current_session = None
    if session_file.exists():
        try:
            current_session = json.loads(session_file.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            current_session = None

    return json.dumps({
        "projects": projects,
        "current_session": current_session
    }, ensure_ascii=False)


if __name__ == "__main__":
    mcp.run()

```