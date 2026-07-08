# 📄 ফাইল: backend/services/github_agent.py

**প্রকার:** .py  
**সাইজ:** 5,536 বাইট  
**আপডেট:** 2026-07-08T18:50:08.165331

---

## কোড

```py
import base64
from datetime import datetime

import httpx
from loguru import logger
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from core.security_vault import decrypt_token
from models.integration import Integration


async def get_user_github_token(user_id: str, db: AsyncSession) -> str | None:
    """
    DB থেকে ইউজারের এনক্রিপ্টেড GitHub টোকেন রিট্রিভ করে ডিক্রিপ্ট করে।
    টোকেন না পেলে None রিটার্ন করে — কলারকে fail-fast করতে হবে।
    
    ⚠️ FIX: AsyncSession.get() শুধুমাত্র primary key নেয়, dict ফিল্টার নয়।
    আগে db.get(Integration, {"user_id": ..., "provider": ...}) দিয়ে ArgumentError 
    থ্রো করত। এখন select().where() ব্যবহার করা হচ্ছে।
    """  # noqa: W291, W293
    stmt = select(Integration).where(
        Integration.user_id == user_id,
        Integration.provider == "github",
    )
    result = await db.execute(stmt)
    integration = result.scalar_one_or_none()
    if not integration or not integration.encrypted_access_token:
        logger.warning(f"No GitHub token found for user '{user_id}'")
        return None

    try:
        access_token = decrypt_token(integration.encrypted_access_token)
        return access_token
    except Exception as exc:  # noqa: BLE001
        logger.error(f"Failed to decrypt GitHub token for user '{user_id}': {exc}")
        return None


async def create_autonomous_pr(
    user_id: str,
    repo_name: str,
    file_path: str,
    code_content: str,
    commit_msg: str,
    db: AsyncSession | None = None,
):
    """
    এনক্রিপ্টেড টোকেন ডিক্রিপ্ট করে গিটহাবে নতুন ব্রাঞ্চ এবং PR তৈরি করবে।
    repo_name ফরম্যাট হতে হবে: "username/repo"
    
    db_session বাধ্যতামূলক — না দিলে fail-fast করে, যাতে কেউ ভুলে placeholder দিয়ে ডিপ্লয় করতে না পারে।
    """  # noqa: W293
    # ১. ডাটাবেস থেকে এনক্রিপ্টেড টোকেন নিয়ে ডিক্রিপ্ট করা
    if db is None:
        raise RuntimeError(
            "create_autonomous_pr: db_session is required. "
            "Call with an active AsyncSession to fetch the GitHub token from DB."
        )

    access_token = await get_user_github_token(user_id, db)
    if access_token is None:
        raise RuntimeError(
            f"GitHub token not found or could not be decrypted for user '{user_id}'. "
            "Please connect GitHub via /integrations/github/link first."
        )

    headers = {
        "Authorization": f"Bearer {access_token}",
        "Accept": "application/vnd.github.v3+json"
    }

    branch_name = f"supremeai-auto-fix-{datetime.now().strftime('%Y%m%d%H%M%S')}"
    base_url = f"https://api.github.com/repos/{repo_name}"

    async with httpx.AsyncClient(timeout=15.0) as client:
        # Step A: Get Default Branch SHA (সাধারণত 'main' বা 'master')
        repo_info = await client.get(base_url, headers=headers)
        if repo_info.status_code != 200:
            raise Exception(f"Failed to fetch repo info: {repo_info.text}")

        default_branch = repo_info.json().get("default_branch", "main")

        ref_info = await client.get(f"{base_url}/git/refs/heads/{default_branch}", headers=headers)
        if ref_info.status_code != 200:
            raise Exception(f"Failed to fetch ref info: {ref_info.text}")

        base_sha = ref_info.json()["object"]["sha"]

        # Step B: Create New Branch
        branch_res = await client.post(
            f"{base_url}/git/refs",
            headers=headers,
            json={"ref": f"refs/heads/{branch_name}", "sha": base_sha}
        )
        if branch_res.status_code != 201:
            raise Exception(f"Failed to create branch: {branch_res.text}")

        # Step C: Commit the Code to New Branch
        encoded_code = base64.b64encode(code_content.encode("utf-8")).decode("utf-8")
        commit_res = await client.put(
            f"{base_url}/contents/{file_path}",
            headers=headers,
            json={
                "message": commit_msg,
                "content": encoded_code,
                "branch": branch_name
            }
        )
        if commit_res.status_code not in (200, 201):
            raise Exception(f"Failed to commit file: {commit_res.text}")

        # Step D: Create the Pull Request
        pr_response = await client.post(
            f"{base_url}/pulls",
            headers=headers,
            json={
                "title": f"🚀 SupremeAI Auto-Fix: {commit_msg}",
                "body": (
                    "This PR was autonomously generated and verified in the SupremeAI Zero-Cost Sandbox.\n\n"
                    "- ✅ Execution Verified\n"
                    "- 🧠 Saved to Memory Vault"
                ),
                "head": branch_name,
                "base": default_branch
            }
        )

        if pr_response.status_code != 201:
            raise Exception(f"Failed to create PR: {pr_response.text}")

        return pr_response.json().get("html_url")

```