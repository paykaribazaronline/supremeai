import os

import httpx
from loguru import logger


class EmailService:
    def __init__(self):
        self.api_key = os.getenv("RESEND_API_KEY", "")
        self.from_email = os.getenv("RESEND_FROM_EMAIL", "onboarding@supremeai.dev")
        self.api_url = "https://api.resend.com/emails"

        if not self.api_key:
            logger.warning(
                "RESEND_API_KEY is not set. Email service will run in mock mode."
            )

    async def _send_email(self, to_email: str, subject: str, html_body: str) -> bool:
        if not self.api_key:
            logger.info(f"[Mock Email] To: {to_email} | Subject: {subject}")
            logger.debug(f"Body: {html_body[:100]}...")
            return True

        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    self.api_url,
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "from": self.from_email,
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
                    return False
        except Exception as e:
            logger.error(f"Exception while sending email: {e}")
            return False

    async def send_welcome_email(
        self, user_email: str, user_name: str = "Developer"
    ) -> bool:
        subject = "Welcome to SupremeAI 2.0 🚀"
        html = f"""
        <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2>Welcome, {user_name}!</h2>
                <p>We're thrilled to have you onboard SupremeAI 2.0.</p>
                <p>You can now orchestrate multiple cloud providers, run local agents, and build faster than ever.</p>
                <a href="https://supremeai.dev/studio" style="padding: 10px 20px; background-color: #2563eb; color: white; text-decoration: none; border-radius: 5px;">Go to Studio</a>
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

    async def send_billing_notification(
        self, user_email: str, amount: float, usage: str
    ) -> bool:
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
