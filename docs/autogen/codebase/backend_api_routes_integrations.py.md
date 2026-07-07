# 📄 ফাইল: backend/api/routes/integrations.py

**প্রকার:** .py  
**সাইজ:** 2,369 বাইট  
**আপডেট:** 2026-07-07T18:09:12.344549

---

## কোড

```py
from urllib.parse import urlencode

import httpx
from fastapi import APIRouter
from fastapi import Request
from fastapi.responses import RedirectResponse

from core.config import settings
from core.security_vault import encrypt_token


# Assuming we will use a database session/dependency to save the token. 
# For now, we stub the DB save and print it.

router = APIRouter()

@router.get("/integrations/github/link")
async def link_github():
    """
    Redirects the user to GitHub's OAuth login page.
    """
    params = {
        "client_id": settings.github_client_id,
        "scope": "repo user",
        "redirect_uri": "http://localhost:8000/api/v1/integrations/github/callback"
    }
    github_auth_url = f"https://github.com/login/oauth/authorize?{urlencode(params)}"
    return RedirectResponse(url=github_auth_url)

@router.get("/integrations/github/callback")
async def github_callback(code: str, request: Request):
    """
    Handles the callback from GitHub, exchanges code for token, and saves it.
    """
    token_url = "https://github.com/login/oauth/access_token"
    payload = {
        "client_id": settings.github_client_id,
        "client_secret": settings.github_client_secret,
        "code": code,
        "redirect_uri": "http://localhost:8000/api/v1/integrations/github/callback"
    }
    headers = {"Accept": "application/json"}
    
    async with httpx.AsyncClient() as client:
        response = await client.post(token_url, json=payload, headers=headers)
        data = response.json()
        
    access_token = data.get("access_token")
    if not access_token:
        return {"status": "error", "message": "Failed to get access token from GitHub."}
    
    # Encrypt the token using our AES-256 (Fernet) vault
    _encrypted_token = encrypt_token(access_token)
    
    # TODO: In a real app, extract user_id from the session/JWT
    user_id = "test_user_id" 
    
    # Simulate saving to database
    # new_integration = Integration(user_id=user_id, provider="github", encrypted_access_token=encrypted_token)
    # db.add(new_integration)
    # db.commit()
    
    print(f"🔗 [Universal Integration Hub] GitHub connected for user '{user_id}'. Token encrypted successfully.")
    
    # Redirect back to the frontend Integrations page
    return RedirectResponse(url="http://localhost:5173/integrations?status=success")

```