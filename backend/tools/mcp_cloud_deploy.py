#!/usr/bin/env python3
"""
MCP Server for Cloud Deployment Integration in SupremeAI 2.0.

এই সার্ভারটি এজেন্টকে Render, Railway, Oracle Cloud-এ সরাসরে
কোড ডিপ্লয় ও লগ মনিটর করার ক্ষমতা দেয়।
"""

import os
import json
import re
from typing import Optional, List, Dict, Any
from enum import Enum

import httpx
from loguru import logger
from pydantic import BaseModel, Field, ConfigDict
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("cloud_deploy_mcp")

CHARACTER_LIMIT = 25000
RENDER_API_KEY = os.getenv("RENDER_API_KEY", "")
RAILWAY_TOKEN = os.getenv("RAILWAY_TOKEN", "")
ORACLE_API_KEY = os.getenv("ORACLE_CLOUD_API_KEY", "")

# বাংলা মন্তব্য: ক্লাউড প্রোভাইডারের এনভায়রনমেন্ট ভ্যারিয়েবল ভ্যালিডেশন ও সতর্কতা
if not RENDER_API_KEY:
    logger.warning("RENDER_API_KEY is not set in environment variables.")
if not RAILWAY_TOKEN:
    logger.warning("RAILWAY_TOKEN is not set in environment variables.")
if not ORACLE_API_KEY:
    logger.warning("ORACLE_CLOUD_API_KEY is not set in environment variables.")

ORACLE_REGION = os.getenv("ORACLE_REGION", "")
if not ORACLE_REGION:
    logger.warning("ORACLE_REGION is not set, defaulting to 'us-phoenix-1'.")
    ORACLE_REGION = "us-phoenix-1"
else:
    if not re.match(r"^[a-z0-9\-]+$", ORACLE_REGION):
        logger.error(f"Invalid ORACLE_REGION format: '{ORACLE_REGION}'. It should only contain lowercase letters, numbers, and hyphens.")


class CloudProvider(str, Enum):
    """সমর্থিত ক্লাউড প্রোভাইডার।"""
    RENDER = "render"
    RAILWAY = "railway"
    ORACLE = "oracle"


class ResponseFormat(str, Enum):
    """আউটপুট ফরম্যাট।"""
    MARKDOWN = "markdown"
    JSON = "json"


class DeployServiceInput(BaseModel):
    """সার্ভিস ডিপ্লয়ের জন্য ইনপুট।"""
    model_config = ConfigDict(str_strip_whitespace=True, validate_assignment=True)

    provider: CloudProvider = Field(..., description="ডিপ্লয় করার ক্লাউড প্রোভাইডার")
    service_name: str = Field(
        ..., 
        description="সার্ভিসের নাম", 
        min_length=1, 
        max_length=100, 
        pattern=r"^[a-zA-Z0-9\-_]+$"
    )
    branch: Optional[str] = Field(default="main", description="ডিপ্লয় ব্রাঞ্চ")


class GetLogsInput(BaseModel):
    """লগ রিট্রিভালের জন্য ইনপুট।"""
    model_config = ConfigDict(str_strip_whitespace=True, validate_assignment=True)

    provider: CloudProvider = Field(..., description="ক্লাউড প্রোভাইডার")
    service_name: str = Field(
        ..., 
        description="সার্ভিসের নাম", 
        min_length=1, 
        pattern=r"^[a-zA-Z0-9\-_]+$"
    )
    lines: int = Field(default=100, description="রিট্রিভ করার লাইন সংখ্যা", ge=1, le=1000)


def _check_admin_auth() -> bool:
    """অ্যাডমিন অথেন্টিকেশন চেক করে।"""
    return os.getenv("ADMIN_AUTHORIZED", "false").lower() == "true"


def _handle_api_error(e: Exception, status_code: int = None) -> str:
    """API এরর স্ট্যান্ডার্ডাইজ্ড হ্যান্ডলিং।"""
    if status_code == 401:
        return "Error: Invalid API key. Check cloud provider credentials."
    if status_code == 404:
        return "Error: Service not found. Verify service name and provider."
    if status_code == 429:
        return "Error: Rate limit exceeded. Please wait before retrying."
    return f"Error: API request failed - {type(e).__name__}"


@mcp.tool(
    name="cloud_deploy_service",
    annotations={
        "title": "Deploy Service to Cloud",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": True,
    }
)
async def cloud_deploy_service(params: DeployServiceInput) -> str:
    """
    ক্লাউড প্রোভাইডারে নতুন সার্ভিস ডিপ্লয় করে।

    এই টুলটি Render, Railway, Oracle Cloud-এ ডিপ্লয় সমর্থন করে।
    প্রতিটি প্রোভাইডারের জন্য নির্দিষ্ট API ইন্টিগ্রেশন।

    Args:
        params (DeployServiceInput): ইনপুট প্যারামিটার সম্বলিত:
            - provider (CloudProvider): ক্লাউড প্রোভাইডার
            - service_name (str): সার্ভিসের নাম
            - branch (Optional[str]): ডিপ্লয় ব্রাঞ্চ

    Returns:
        str: ডিপ্লয় স্ট্যাটাস ও ইনফরমেশন
    """
    if not _check_admin_auth():
        return json.dumps({
            "error": "Admin authorization required for deployments",
            "message": "Set ADMIN_AUTHORIZED=true in environment"
        }, ensure_ascii=False)

    headers = {}
    api_url = ""

    if params.provider == CloudProvider.RENDER:
        if not RENDER_API_KEY:
            return json.dumps({"error": "RENDER_API_KEY not configured"}, ensure_ascii=False)
        api_url = "https://api.render.com/v1/services"
        headers = {"Authorization": f"Bearer {RENDER_API_KEY}"}

    elif params.provider == CloudProvider.RAILWAY:
        if not RAILWAY_TOKEN:
            return json.dumps({"error": "RAILWAY_TOKEN not configured"}, ensure_ascii=False)
        api_url = "https://back-end.railway.app/v2/services"
        headers = {"Authorization": f"Bearer {RAILWAY_TOKEN}"}

    elif params.provider == CloudProvider.ORACLE:
        if not ORACLE_API_KEY:
            return json.dumps({"error": "ORACLE_CLOUD_API_KEY not configured"}, ensure_ascii=False)
        api_url = f"https://containerengine.{ORACLE_REGION}.oraclecloud.com/api/v1/deploy"
        headers = {"Authorization": f"Bearer {ORACLE_API_KEY}"}

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                api_url,
                headers=headers,
                json={"serviceName": params.service_name, "branch": params.branch}
            )
            response.raise_for_status()
            data = response.json()

            return json.dumps({
                "success": True,
                "provider": params.provider.value,
                "service": params.service_name,
                "status": data.get("status", "deploying"),
                "url": data.get("url", ""),
                "message": f"Deployment initiated for '{params.service_name}' on {params.provider.value}"
            }, ensure_ascii=False)

    except httpx.HTTPStatusError as e:
        return _handle_api_error(e, e.response.status_code)
    except Exception as e:
        return _handle_api_error(e)


@mcp.tool(
    name="cloud_get_deployment_logs",
    annotations={
        "title": "Get Deployment Logs",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    }
)
async def cloud_get_deployment_logs(params: GetLogsInput) -> str:
    """
    ক্লাউড সার্ভিসের ডিপ্লয়মেন্ট লগ রিট্রিভ করে।

    Args:
        params (GetLogsInput): ইনপুট প্যারামিটার সম্বলিত:
            - provider (CloudProvider): ক্লাউড প্রোভাইডার
            - service_name (str): সার্ভিসের নাম
            - lines (int): রিট্রিভ করার লাইন সংখ্যা

    Returns:
        str: সার্ভিসের লগ
    """
    api_url = ""
    headers = {}

    if params.provider == CloudProvider.RENDER:
        if not RENDER_API_KEY:
            return json.dumps({"error": "RENDER_API_KEY not configured"}, ensure_ascii=False)
        api_url = f"https://api.render.com/v1/services/{params.service_name}/logs"
        headers = {"Authorization": f"Bearer {RENDER_API_KEY}"}

    elif params.provider == CloudProvider.RAILWAY:
        if not RAILWAY_TOKEN:
            return json.dumps({"error": "RAILWAY_TOKEN not configured"}, ensure_ascii=False)
        api_url = f"https://back-end.railway.app/v2/services/{params.service_name}/logs"
        headers = {"Authorization": f"Bearer {RAILWAY_TOKEN}"}

    elif params.provider == CloudProvider.ORACLE:
        if not ORACLE_API_KEY:
            return json.dumps({"error": "ORACLE_CLOUD_API_KEY not configured"}, ensure_ascii=False)
        api_url = f"https://logging.{ORACLE_REGION}.oraclecloud.com/api/v1/logs"
        headers = {"Authorization": f"Bearer {ORACLE_API_KEY}"}

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                api_url,
                headers=headers,
                params={"lines": params.lines}
            )
            response.raise_for_status()
            data = response.json()

            logs = data.get("logs", []) if isinstance(data, dict) else data

            return json.dumps({
                "provider": params.provider.value,
                "service": params.service_name,
                "logs": logs[:params.lines],
                "total_lines": len(logs)
            }, ensure_ascii=False)

    except httpx.HTTPStatusError as e:
        return _handle_api_error(e, e.response.status_code)
    except Exception as e:
        return _handle_api_error(e)


@mcp.tool(
    name="cloud_list_services",
    annotations={
        "title": "List Cloud Services",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    }
)
async def cloud_list_services() -> str:
    """
    সব ক্লাউড প্রোভাইডারে ডিপ্লট করা সার্ভিসের তালিকা দেখায়।

    Returns:
        str: সার্ভিস তালিকা
    """
    services = []

    if RENDER_API_KEY:
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    "https://api.render.com/v1/services",
                    headers={"Authorization": f"Bearer {RENDER_API_KEY}"}
                )
                if response.status_code == 200:
                    for svc in response.json():
                        services.append({
                            "provider": "render",
                            "name": svc.get("serviceName"),
                            "status": svc.get("status"),
                            "url": svc.get("url", "")
                        })
        except Exception as e:
            logger.error(f"Failed to list services from Render: {e}")

    if RAILWAY_TOKEN:
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    "https://back-end.railway.app/v2/services",
                    headers={"Authorization": f"Bearer {RAILWAY_TOKEN}"}
                )
                if response.status_code == 200:
                    for svc in response.json():
                        services.append({
                            "provider": "railway",
                            "name": svc.get("name"),
                            "status": svc.get("status"),
                            "url": svc.get("url", "")
                        })
        except Exception as e:
            logger.error(f"Failed to list services from Railway: {e}")

    return json.dumps({
        "services": services,
        "count": len(services)
    }, ensure_ascii=False)


if __name__ == "__main__":
    mcp.run()