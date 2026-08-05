import email
import imaplib
import re
from email.header import decode_header
from typing import Any

from loguru import logger

from core.error_bus import with_error_bus
from core.security.secure_credential_store import SecureCredentialStore


class EmailAgent:
    """
    গ্যাপ ফিক্স (Anti-Silent-Failure): আগে এই ক্লাসের connect_gmail_oauth()/connect_imap()
    সবসময় True রিটার্ন করত এবং কোনো ক্রেডেনশিয়াল আসলে যাচাই বা সংরক্ষণ করত না। receive_otp()
    সবসময় একটি হার্ডকোড করা fake OTP ("849301") রিটার্ন করত — /integrations/email/gmail এবং
    /integrations/email/imap পাবলিক রাউট (api/routes/email.py) থেকে সরাসরি এক্সপোজড থাকায়
    ইউজার মনে করতেন তাদের ইমেইল সত্যিই কানেক্টেড, অথচ backend কিছুই করত না।

    এখন: IMAP-এর জন্য প্রকৃত imaplib কানেকশন যাচাই + এনক্রিপ্টেড ক্রেডেনশিয়াল সংরক্ষণ
    (SecureCredentialStore, browser.py-তে ইতিমধ্যে ব্যবহৃত একই প্যাটার্ন), এবং receive_otp()
    সত্যিকারের ইনবক্স পোল করে OTP খোঁজে — কোনো fake ডেটা রিটার্ন করে না।
    Gmail OAuth-এর জন্য প্রকৃত consent/redirect ফ্লো এখনো তৈরি হয়নি, তাই সেটি এখন সততার
    সাথে ব্যর্থ (fail-closed) হয়, silent-success দেখায় না।
    """

    def __init__(self, auth_method: str = "oauth"):
        self.auth_method = auth_method
        self.connected = False
        self._credential_store = SecureCredentialStore()
        self._imap_config: dict[str, Any] | None = None
        logger.info(f"EmailAgent initialized with auth_method={auth_method}")

    def connect_gmail_oauth(self, provider: str, scopes: list) -> bool:
        """Gmail OAuth এখনো real consent/token-exchange ফ্লো-র সাথে ওয়্যার করা হয়নি।
        আগে এখানে চুপচাপ True রিটার্ন হতো — এখন স্পষ্টভাবে ব্যর্থ হয়, যাতে caller
        ভুল করে ধরে না নেয় যে ইমেইল অ্যাকাউন্ট আসলে কানেক্টেড।"""
        logger.warning(
            f"Gmail OAuth connect requested (provider={provider}, scopes={scopes}) — OAuth flow not implemented yet."
        )
        raise NotImplementedError(
            "Gmail OAuth is not implemented yet (no real consent/redirect flow wired up). "
            "Use IMAP with an app password via connect_imap() instead."
        )

    def connect_imap(self, host: str, port: int, username: str, app_password: str) -> bool:
        """একটি রিয়েল IMAP লগইন করে ক্রেডেনশিয়াল যাচাই করে, তারপর এনক্রিপ্ট করে সংরক্ষণ করে।"""
        try:
            with imaplib.IMAP4_SSL(host, port, timeout=10) as imap:
                imap.login(username, app_password)
        except (imaplib.IMAP4.error, OSError) as exc:
            logger.error(f"IMAP login failed for {username}@{host}:{port}: {exc}")
            self.connected = False
            return False

        ciphertext, key_ref = self._credential_store.encrypt(app_password)
        self._imap_config = {
            "host": host,
            "port": port,
            "username": username,
            "ciphertext": ciphertext,
            "key_ref": key_ref,
        }
        self.auth_method = "imap"
        self.connected = True
        logger.info(f"IMAP connection verified and credentials stored (encrypted) for {username}@{host}:{port}")
        return True

    def receive_otp(self, website: str, lookback: int = 10) -> str:
        """সংযুক্ত ইনবক্স থেকে সাম্প্রতিক unread মেইল পোল করে `website`-সম্পর্কিত একটি রিয়েল OTP খোঁজে।
        কোনো লাইভ কানেকশন না থাকলে বা OTP না পাওয়া গেলে খালি স্ট্রিং রিটার্ন করে — কখনো fabricate করে না।
        """
        if self.auth_method != "imap" or not self._imap_config or not self.connected:
            logger.warning("receive_otp() called without a live IMAP connection — cannot fetch a real OTP.")
            return ""

        cfg = self._imap_config
        try:
            app_password = self._credential_store.decrypt(cfg["ciphertext"], cfg["key_ref"])
        except Exception as exc:
            logger.error(f"Failed to decrypt stored IMAP credentials: {exc}")
            return ""

        try:
            with imaplib.IMAP4_SSL(cfg["host"], cfg["port"], timeout=10) as imap:
                imap.login(cfg["username"], app_password)
                imap.select("INBOX")
                status, data = imap.search(None, "UNSEEN")
                if status != "OK" or not data or not data[0]:
                    return ""
                msg_ids = data[0].split()[-lookback:]
                for msg_id in reversed(msg_ids):
                    status, msg_data = imap.fetch(msg_id, "(RFC822)")
                    if status != "OK" or not msg_data or not msg_data[0]:
                        continue
                    msg = email.message_from_bytes(msg_data[0][1])
                    subject = self._decode_mime(msg.get("Subject", ""))
                    sender = msg.get("From", "")
                    body = self._extract_body(msg)
                    haystack = f"{subject} {sender} {body}".lower()
                    if website.lower() not in haystack:
                        continue
                    otp = self.extract_otp(body) or self.extract_otp(subject)
                    if otp:
                        return otp
        except (imaplib.IMAP4.error, OSError) as exc:
            logger.error(f"IMAP poll failed while looking for OTP from {website}: {exc}")
        return ""

    @staticmethod
    def _decode_mime(value: str) -> str:
        parts = decode_header(value or "")
        decoded = []
        for part, enc in parts:
            if isinstance(part, bytes):
                decoded.append(part.decode(enc or "utf-8", errors="ignore"))
            else:
                decoded.append(part)
        return "".join(decoded)

    @staticmethod
    @with_error_bus("_extract_body")
    def _extract_body(msg) -> str:
        if msg.is_multipart():
            for part in msg.walk():
                if part.get_content_type() == "text/plain" and "attachment" not in str(part.get("Content-Disposition")):
                    try:
                        payload = part.get_payload(decode=True)
                        if payload:
                            return payload.decode(part.get_content_charset() or "utf-8", errors="ignore")
                    except Exception:  # noqa: S112
                        continue
            return ""
        try:
            payload = msg.get_payload(decode=True)
            return payload.decode(msg.get_content_charset() or "utf-8", errors="ignore") if payload else ""
        except Exception:
            return ""

    def extract_otp(self, email_body: str) -> str:
        """Extract a 4 to 8 digit numeric OTP from email body text."""
        match = re.search(r"\b\d{4,8}\b", email_body or "")
        if match:
            return match.group(0)
        return ""

    def signup_flow(self, website_url: str) -> dict:
        """সংযুক্ত ইনবক্স থেকে রিয়েল OTP পোল করে — কখনো fabricated OTP বা fake 'stored_in_vault' দাবি করে না।"""
        logger.info(f"Starting automated signup flow for {website_url}")
        if not self.connected:
            return {
                "status": "failed",
                "reason": "No email account connected. Call connect_imap() first.",
            }
        otp = self.receive_otp(website_url)
        if otp:
            return {"status": "success", "credentials_encrypted": True, "otp": otp}
        return {"status": "failed", "reason": "OTP not found in recent inbox messages"}
