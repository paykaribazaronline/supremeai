from fastapi import APIRouter
from fastapi import HTTPException
from pydantic import BaseModel

from tools.email_agent import EmailAgent


router = APIRouter(prefix="/integrations/email", tags=["email"])
email_agent = EmailAgent()


class GmailAuthRequest(BaseModel):
    provider: str
    scopes: list[str]


class ImapAuthRequest(BaseModel):
    host: str
    port: int
    username: str
    app_password: str


@router.post("/gmail")
async def gmail_auth(payload: GmailAuthRequest):
    try:
        success = email_agent.connect_gmail_oauth(payload.provider, payload.scopes)
        if success:
            return {"status": "success", "message": "Connected Gmail via OAuth"}
        raise HTTPException(status_code=400, detail="Failed to connect Gmail OAuth")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) from e


@router.post("/imap")
async def imap_auth(payload: ImapAuthRequest):
    try:
        success = email_agent.connect_imap(
            payload.host, payload.port, payload.username, payload.app_password
        )
        if success:
            return {"status": "success", "message": "Connected generic IMAP"}
        raise HTTPException(status_code=400, detail="Failed to connect generic IMAP")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) from e
