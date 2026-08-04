"""
MCP Server for GitHub CI/CD Integration in SupremeAI 2.0.

এই সার্ভারটি এজেন্টকে GitHub সার্ভারকে একিভাবে connect করে এবং
CI/CD অপারেশন (Issue, PR, Auto-fix) সরাসরে চ্যাটবক্স থেকে করার ক্ষমতা দেয়।
"""

import base64
import json
# বাংলা মন্তব্য: পরিবেশের ভেরিয়েবল চেক করার জন্য os মডিউল ইমপোর্ট করা হলো
import os
from enum import StrEnum

import httpx
from core.config import settings
from mcp.server.fastmcp import FastMCP
from pydantic import BaseModel, ConfigDict, Field
# শেয়ার্ড ইউটিলিটি — ডুপ্লিকেট কোড দূর করতে কেন্দ্রীয় মডিউল থেকে ইম্পোর্ট
from utils.environment import is_admin_authorized, is_autofix_authorized
from utils.http_client import handle_api_error
from utils.json_helpers import json_error

mcp = FastMCP("github_cicd_mcp")

CHARACTER_LIMIT = 25000
GITHUB_REPO = getattr(settings, "github_repository", "supremeai/supremeai_2.0")
GITHUB_API_URL = "https://api.github.com"


def _get_github_token() -> str:
    """Get the current GitHub token from environment variables."""
    # বাংলা মন্তব্য: settings-এ টোকেন না থাকলে os.environ থেকে রিড করা হবে
    return getattr(settings, "github_token", "") or os.environ.get("GITHUB_TOKEN", "")


class ResponseFormat(StrEnum):
    """আউটপুট ফরম্যাট।"""

    MARKDOWN = "markdown"
    JSON = "json"


class CreatePRInput(BaseModel):
    """PR তৈরির জন্য ইনপুট।"""

    model_config = ConfigDict(str_strip_whitespace=True, validate_assignment=True)

    title: str = Field(..., description="PR এর শিরোনাম", min_length=1, max_length=200)
    body: str = Field(..., description="PR এর বর্ণনা", min_length=1)
    head: str = Field(..., description="সূচী ব্রাঞ্চ", min_length=1)
    base: str = Field(default="main", description="লক্ষ্য ব্রাঞ্চ")


class FixIssueInput(BaseModel):
    """Issue ফিক্স করার জন্য ইনপুট।"""

    model_config = ConfigDict(str_strip_whitespace=True, validate_assignment=True)

    issue_number: int = Field(..., description="ফিক্স করার Issue নম্বর", ge=1)
    branch: str = Field(..., description="ফিক্স শুরু করার ব্রাঞ্চ", min_length=1)


# রিফ্যাক্টর: লোকাল _check_admin_auth, _check_autofix_auth, _handle_api_error মুছে
# শেয়ার্ড ইউটিলিটি (utils.environment, utils.http_client) ব্যবহার করা হচ্ছে।


@mcp.tool(
    name="github_create_pull_request",
    annotations={
        "title": "Create Pull Request",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": True,
    },
)
async def github_create_pull_request(params: CreatePRInput) -> str:
    """
    GitHub-এ নতুন Pull Request তৈরি করে।

    Args:
        params (CreatePRInput): ইনপুট প্যারামিটার সম্বলিত:
            - title (str): PR শিরোনাম
            - body (str): PR বর্ণনা
            - head (str): সূচী ব্রাঞ্চ
            - base (str): লক্ষ্য ব্রাঞ্চ

    Returns:
        str: PR স্ট্যাটাস ও লিংক
    """
    if not is_admin_authorized():
        return json_error("Admin authorization required for PR creation")

    github_token = _get_github_token()
    if not github_token:
        return json_error("GITHUB_TOKEN not configured")

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{GITHUB_API_URL}/repos/{GITHUB_REPO}/pulls",
                headers={
                    "Authorization": f"token {github_token}",
                    "Accept": "application/vnd.github.v3+json",
                },
                json={
                    "title": params.title,
                    "body": params.body,
                    "head": params.head,
                    "base": params.base,
                },
            )
            response.raise_for_status()
            data = response.json()

            return json.dumps(
                {
                    "success": True,
                    "pr_number": data.get("number"),
                    "pr_url": data.get("html_url"),
                    "status": data.get("state", "open"),
                    "message": f"PR #{data.get('number')} created successfully",
                },
                ensure_ascii=False,
            )

    except httpx.HTTPStatusError as e:
        return handle_api_error(e, e.response.status_code)
    except Exception as e:
        return handle_api_error(e)


@mcp.tool(
    name="github_run_auto_fix",
    annotations={
        "title": "Run CI Auto-Fix Pipeline",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": True,
    },
)
async def github_run_auto_fix(params: FixIssueInput) -> str:
    """
    CI অটো-ফিক্স পাইলাইন চালায়।

    এই টুলটি ci-auto-fix-v3.py ইঞ্জিনকে ট্রিগার করে এবং
    ফিল্ড টেস্ট গুলোর স্বয়ংক্রিয় ফিক্সিং সক্ষম করে।

    Args:
        params (FixIssueInput): ইনপুট প্যারামিটার সম্বলিত:
            - issue_number (int): Issue নম্বর
            - branch (str): ফিক্স ব্রাঞ্চ

    Returns:
        str: অটো-ফিক্স স্ট্যাটাস
    """
    if not is_autofix_authorized():
        return json.dumps(
            {
                "error": "Auto-fix authorization required",
                "message": "Set AUTOFIX_AUTHORIZED=true in environment",
            },
            ensure_ascii=False,
        )

    github_token = _get_github_token()
    if not github_token:
        return json_error("GITHUB_TOKEN not configured")

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                f"{GITHUB_API_URL}/repos/{GITHUB_REPO}/actions/workflows/ci-auto-fix-v3.yml/dispatches",
                headers={
                    "Authorization": f"token {github_token}",
                    "Accept": "application/vnd.github.v3+json",
                },
                json={
                    "ref": params.branch,
                    "inputs": {"issue_number": str(params.issue_number)},
                },
            )
            response.raise_for_status()

            return json.dumps(
                {
                    "success": True,
                    "issue_number": params.issue_number,
                    "branch": params.branch,
                    "workflow": "ci-auto-fix-v3",
                    "message": f"Auto-fix workflow triggered for issue #{params.issue_number}",
                },
                ensure_ascii=False,
            )

    except httpx.HTTPStatusError as e:
        return handle_api_error(e, e.response.status_code)
    except Exception as e:
        return handle_api_error(e)


@mcp.tool(
    name="github_list_issues",
    annotations={
        "title": "List Repository Issues",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def github_list_issues(state: str = "open", labels: str | None = None) -> str:
    """
    রিপোজিটরিতে ইস্যু তালিকা দেখায়।

    Args:
        state (str): ইস্যু স্টেট ('open', 'closed', 'all')
        labels (str | None): ফিল্টার করার জন্য লেবেল

    Returns:
        str: ইস্যু তালিকা
    """
    github_token = _get_github_token()
    if not github_token:
        return json_error("GITHUB_TOKEN not configured")

    valid_states = {"open", "closed", "all"}
    if state not in valid_states:
        state = "open"

    params = {"state": state}
    if labels:
        params["labels"] = labels

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                f"{GITHUB_API_URL}/repos/{GITHUB_REPO}/issues",
                headers={
                    "Authorization": f"token {github_token}",
                    "Accept": "application/vnd.github.v3+json",
                },
                params=params,
            )
            response.raise_for_status()
            issues = response.json()

            return json.dumps(
                {
                    "issues": [
                        {
                            "number": i.get("number"),
                            "title": i.get("title"),
                            "state": i.get("state"),
                            "labels": [lbl.get("name") for lbl in i.get("labels", [])],
                            "url": i.get("html_url"),
                        }
                        for i in issues
                    ],
                    "count": len(issues),
                },
                ensure_ascii=False,
            )

    except httpx.HTTPStatusError as e:
        return handle_api_error(e, e.response.status_code)
    except Exception as e:
        return handle_api_error(e)


@mcp.tool(
    name="github_get_ci_status",
    annotations={
        "title": "Get CI/CD Status",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def github_get_ci_status(branch: str = "main") -> str:
    """
    শাখার CI/CD স্ট্যাটাস দেখায়।

    Args:
        branch (str): চেক করার শাখা

    Returns:
        str: CI স্ট্যাটাস ও রিজাল্ট
    """
    github_token = _get_github_token()
    if not github_token:
        return json_error("GITHUB_TOKEN not configured")

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                f"{GITHUB_API_URL}/repos/{GITHUB_REPO}/commits/{branch}/status",
                headers={
                    "Authorization": f"token {github_token}",
                    "Accept": "application/vnd.github.v3+json",
                },
            )
            response.raise_for_status()
            data = response.json()

            return json.dumps(
                {
                    "branch": branch,
                    "state": data.get("state"),
                    "statuses": data.get("statuses", []),
                    "total_count": data.get("total_count", 0),
                },
                ensure_ascii=False,
            )

    except httpx.HTTPStatusError as e:
        return handle_api_error(e, e.response.status_code)
    except Exception as e:
        return handle_api_error(e)


class GetFileContentsInput(BaseModel):
    """ফাইল কনটেন্ট পড়ার জন্য ইনপুট।"""

    model_config = ConfigDict(str_strip_whitespace=True, validate_assignment=True)

    path: str = Field(..., description="রিপোর মধ্যে ফাইলের পাথ", min_length=1)
    ref: str = Field(default="main", description="ব্রাঞ্চ/ট্যাগ/কমিট SHA")


class SearchCodeInput(BaseModel):
    """কোড সার্চের জন্য ইনপুট।"""

    model_config = ConfigDict(str_strip_whitespace=True, validate_assignment=True)

    query: str = Field(
        ...,
        description="সার্চ কোয়েরি (GitHub code search সিনট্যাক্স সাপোর্ট করে)",
        min_length=1,
    )
    per_page: int = Field(
        default=10, description="প্রতি পেজে কতগুলো রেজাল্ট", ge=1, le=50
    )


@mcp.tool(
    name="github_get_file_contents",
    annotations={
        "title": "Read File From Repository",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def github_get_file_contents(params: GetFileContentsInput) -> str:
    """
    রিপো থেকে একটি ফাইলের কনটেন্ট পড়ে আনে (base64 ডিকোড সহ)।

    Args:
        params (GetFileContentsInput): path ও ref (ব্রাঞ্চ/ট্যাগ/SHA)

    Returns:
        str: ডিকোড করা ফাইলের কনটেন্ট অথবা এরর JSON
    """
    github_token = _get_github_token()
    if not github_token:
        return json_error("GITHUB_TOKEN is missing or empty")

    url = f"{GITHUB_API_URL}/repos/{GITHUB_REPO}/contents/{params.path.lstrip('/')}"
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.get(
                url,
                headers={
                    "Authorization": f"token {github_token}",
                    "Accept": "application/vnd.github.v3+json",
                },
                params={"ref": params.ref},
            )
            response.raise_for_status()
            data = response.json()

            if isinstance(data, list):
                return json_error(f"Path '{params.path}' is a directory, not a file.")

            content_b64 = data.get("content", "")
            encoding = data.get("encoding", "")

            if encoding == "base64" and content_b64:
                decoded_content = base64.b64decode(content_b64).decode(
                    "utf-8", errors="replace"
                )
            else:
                decoded_content = content_b64

            return json.dumps(
                {
                    "path": data.get("path"),
                    "sha": data.get("sha"),
                    "size": data.get("size"),
                    "encoding": encoding,
                    "content": decoded_content,
                },
                ensure_ascii=False,
            )

    except httpx.HTTPStatusError as e:
        return handle_api_error(e, e.response.status_code)
    except Exception as e:
        return handle_api_error(e)


@mcp.tool(
    name="github_search_code",
    annotations={
        "title": "Search Code In Repository",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def github_search_code(params: SearchCodeInput) -> str:
    """
    GitHub code search এপিআই ব্যবহার করে রিপোর মধ্যে কোড সার্চ করে।

    Args:
        params (SearchCodeInput): query ও per_page

    Returns:
        str: সার্চ রেজাল্টের তালিকা (ফাইলের নাম, পাথ, ম্যাচিং স্নিপেট)
    """
    github_token = _get_github_token()
    if not github_token:
        return json_error("GITHUB_TOKEN is missing or empty")

    scoped_query = f"repo:{GITHUB_REPO} {params.query}"
    url = f"{GITHUB_API_URL}/search/code"
    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.get(
                url,
                headers={
                    "Authorization": f"token {github_token}",
                    "Accept": "application/vnd.github.v3.text-match+json",
                },
                params={"q": scoped_query, "per_page": params.per_page},
            )
            response.raise_for_status()
            data = response.json()

            results = []
            for item in data.get("items", []):
                matches = []
                for match in item.get("text_matches", []):
                    matches.append(match.get("fragment", ""))

                results.append(
                    {
                        "name": item.get("name"),
                        "path": item.get("path"),
                        "sha": item.get("sha"),
                        "html_url": item.get("html_url"),
                        "matches": matches,
                    }
                )

            return json.dumps(
                {
                    "total_count": data.get("total_count", 0),
                    "incomplete_results": data.get("incomplete_results", False),
                    "items": results,
                },
                ensure_ascii=False,
            )

    except httpx.HTTPStatusError as e:
        return handle_api_error(e, e.response.status_code)
    except Exception as e:
        return handle_api_error(e)


if __name__ == "__main__":
    mcp.run()
