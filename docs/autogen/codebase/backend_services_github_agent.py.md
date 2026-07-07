# 📄 ফাইল: backend/services/github_agent.py

**প্রকার:** .py  
**সাইজ:** 3,566 বাইট  
**আপডেট:** 2026-07-07T17:46:01.311056

---

## কোড

```py
import base64
from datetime import datetime

import httpx


# from backend.models.integration import get_user_github_token # Will implement DB fetch later

async def create_autonomous_pr(user_id: str, repo_name: str, file_path: str, code_content: str, commit_msg: str):
    """
    এনক্রিপ্টেড টোকেন ডিক্রিপ্ট করে গিটহাবে নতুন ব্রাঞ্চ এবং PR তৈরি করবে।
    repo_name ফরম্যাট হতে হবে: "username/repo"
    """
    # ১. ডাটাবেস থেকে এনক্রিপ্টেড টোকেন নিয়ে ডিক্রিপ্ট করা (আপনার লজিক অনুযায়ী)
    # encrypted_token = get_user_github_token(user_id)
    # access_token = decrypt_token(encrypted_token)
    
    # TODO: Fetch from DB using user_id
    access_token = "YOUR_DECRYPTED_TOKEN_HERE" 
    
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    branch_name = f"supremeai-auto-fix-{datetime.now().strftime('%Y%m%d%H%M%S')}"
    base_url = f"https://api.github.com/repos/{repo_name}"

    async with httpx.AsyncClient() as client:
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