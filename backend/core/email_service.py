import os

import httpx
from loguru import logger

from core.event_bus import ErrorEvent
from core.event_bus import error_event_bus


class EmailService:
    """বাংলা মন্তব্য: ইমেইল সার্ভিস যা Pydantic Settings থেকে URL এবং API Key রিড করে।"""

    def __init__(self):
        # Lazy initialization for settings
        self._settings = None

    def _get_settings(self):
        if self._settings is None:
            from core.config import settings

            self._settings = settings
        return self._settings

    async def _send_email(self, to_email: str, subject: str, html_body: str) -> bool:
        settings = self._get_settings()
        api_key = settings.resend_api_key.get_secret_value() if settings.resend_api_key else ""
        from_email = os.getenv("RESEND_FROM_EMAIL", "onboarding@supremeai.dev")
        # Hardcoded URL removed, use settings
        api_url = getattr(settings, "resend_api_url", "https://api.resend.com/emails")

        if not api_key:
            logger.warning(f"[Mock Email] To: {to_email} | Subject: {subject}")
            logger.debug(f"Body: {html_body[:100]}...")
            return False

        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                response = await client.post(
                    api_url,
                    headers={
                        "Authorization": f"Bearer {api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "from": from_email,
                        "to": [to_email],
                        "subject": subject,
                        "html": html_body,
                    },
                )
                if response.status_code in (200, 201):
                    logger.info(f"Email sent successfully to {to_email}")
                    return True
                else:
                    logger.error(f"Failed to send email to {to_email}: {response.text}")
                    error_event_bus.emit(
                        ErrorEvent(
                            module="email_service",
                            error_type="RESEND_API_ERROR",
                            message=response.text[:200],
                            severity="ERROR",
                            context={"status_code": response.status_code, "to_email": to_email},
                        )
                    )
                    return False
        except Exception as e:
            # বাংলা মন্তব্য: Silent Exception রিমুভ করা হলো এবং ErrorEventBus-এ এমিট করা হলো।
            logger.error(f"Exception while sending email: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="email_service", error_type="HTTP_REQUEST_FAILED", message=str(e)[:200], severity="ERROR", context={"to_email": to_email}
                )
            )
            # Fail-fast: Re-raise exception
            raise RuntimeError(f"Failed to send email to {to_email}") from e

    async def send_welcome_email(self, user_email: str, user_name: str = "Developer") -> bool:
        subject = "Welcome to SupremeAI 2.0 🚀"
        frontend_url = getattr(self._get_settings(), "frontend_url", "https://supremeai.dev")
        html = f"""
        <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2>Welcome, {user_name}!</h2>
                <p>We're thrilled to have you onboard SupremeAI 2.0.</p>
                <p>You can now orchestrate multiple cloud providers, run local agents, and build faster than ever.</p>
                <a href="{frontend_url}/studio" style="padding: 10px 20px; background-color: #2563eb; color: white; text-decoration: none; border-radius: 5px;">Go to Studio</a>
            </body>
        </html>
        """
        return await self._send_email(user_email, subject, html)

    async def send_password_reset(self, user_email: str, reset_link: str) -> bool:
        subject = "Reset Your SupremeAI Password"
        html = f"""
        <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2>Password Reset Request</h2>
                <p>Click the link below to reset your password. This link expires in 1 hour.</p>
                <a href="{reset_link}">{reset_link}</a>
            </body>
        </html>
        """
        return await self._send_email(user_email, subject, html)

    async def send_billing_notification(self, user_email: str, amount: float, usage: str) -> bool:
        subject = "SupremeAI - Upcoming Invoice Notification"
        html = f"""
        <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2>Billing Update</h2>
                <p>Your upcoming invoice for this month is <strong>${amount:.2f}</strong>.</p>
                <p>Top usage category: {usage}</p>
                <p>To keep zero-cost operations, consider connecting your own API keys in the Studio.</p>
            </body>
        </html>
        """
        return await self._send_email(user_email, subject, html)


email_service = EmailService()
