from core.messaging.event_bus import ErrorContext

"""This module provides a robust and asynchronous email service for the SupremeAI project, centralizing the functionality for sending various transactional emails such as welcome messages, password reset links, and billing notifications. It integrates with an external email API (e.g., Resend) for delivery, leverages application settings for configuration, and reports errors via the internal event bus, ensuring reliable communication with users within the highly scalable AI ecosystem.

Key Components:
- `EmailService`: Manages the configuration and dispatch of different types of emails through an external API, handling API key retrieval, settings, and error reporting.
- `email_service`: A module-level instance of `EmailService` for convenient, singleton access throughout the application.
- `_get_settings()`: Lazily loads application settings from `core.config`, providing a simple fallback for environments where settings might not be available (e.g., certain test setups).
- `api_key`: Property to retrieve the Resend API key from environment variables, essential for authenticating with the email service.
- `from_email`: Property to retrieve the sender's email address from environment variables, with a default fallback.
- `_send_email()`: An internal asynchronous method responsible for making the actual HTTP POST request to the configured email API, handling success/failure logging and error event emission.
- `send_welcome_email()`: Sends a personalized welcome email to new users, including a link to the SupremeAI studio.
- `send_password_reset()`: Dispatches a password reset email containing a unique, time-limited reset link.
- `send_billing_notification()`: Sends a notification regarding upcoming invoices, usage details, and suggestions for cost optimization.

Dependencies:
- `os`: For accessing environment variables (e.g., `RESEND_API_KEY`, `RESEND_FROM_EMAIL`).
- `httpx`: For making asynchronous HTTP requests to the external email API.
- `loguru`: For structured logging of email sending operations, warnings, and errors.
- `core.messaging.event_bus`: For emitting `ErrorEvent`s when email sending encounters API or network failures.
- `core.config`: For accessing application-wide settings such as the Resend API URL and the SupremeAI frontend URL."""

import os  # noqa: E402

import httpx  # noqa: E402
from core.messaging.event_bus import ErrorEvent  # noqa: E402
from core.messaging.event_bus import error_event_bus  # noqa: E402
from loguru import logger  # noqa: E402


class EmailService:
    """বাংলা মন্তব্য: ইমেইল সার্ভিস যা Pydantic Settings থেকে URL এবং API Key রিড করে।"""

    def __init__(self):
        # Lazy initialization for settings
        self._settings = None

    def _get_settings(self):
        if self._settings is None:
            try:
                from core.config import settings

                self._settings = settings
            except Exception:  # noqa: BLE001
                # বাংলা মন্তব্য: Settings not available in test env — use a simple fallback
                import types

                self._settings = types.SimpleNamespace(
                    resend_api_url="https://api.resend.com/emails",
                    frontend_url="https://supremeai.dev",
                )
        return self._settings

    @property
    def api_key(self) -> str:
        """বাংলা মন্তব্য: Resend API key from env var."""
        return os.getenv("RESEND_API_KEY", "")

    @property
    def from_email(self) -> str:
        """বাংলা মন্তব্য: From email address."""
        return os.getenv("RESEND_FROM_EMAIL", "onboarding@supremeai.dev")

    async def _send_email(self, to_email: str, subject: str, html_body: str) -> bool:
        settings = self._get_settings()
        api_key = self.api_key
        from_email = self.from_email
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
                            structured_context=ErrorContext(module="auto_fixed"),
                            context={
                                "status_code": response.status_code,
                                "to_email": to_email,
                            },
                        )
                    )
                    return False
        except Exception as e:  # noqa: BLE001
            # বাংলা মন্তব্য: Silent Exception রিমুভ করা হলো এবং ErrorEventBus-এ এমিট করা হলো।
            logger.error(f"Exception while sending email: {e}")
            error_event_bus.emit(
                ErrorEvent(
                    module="email_service",
                    error_type="HTTP_REQUEST_FAILED",
                    message=str(e)[:200],
                    severity="ERROR",
                    structured_context=ErrorContext(module="auto_fixed"),
                    context={"to_email": to_email},
                )
            )
            return False

    async def send_welcome_email(
        self, user_email: str, user_name: str = "Developer"
    ) -> bool:
        subject = "Welcome to SupremeAI 2.0 🚀"
        frontend_url = getattr(
            self._get_settings(), "frontend_url", "https://supremeai.dev"
        )
        html = f"""
        <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2>Welcome, {user_name}!</h2>
                <p>We're thrilled to have you onboard SupremeAI 2.0.</p>
                <p>You can now orchestrate multiple cloud providers, run local agents, and build faster than ever.</p>
                <a href="{frontend_url}/studio" style="padding: 10px 20px; background-color: #2563eb; color: white; text-decoration: none; border-radius: 5px;">Go to Studio</a>
            </body>
        </html>
        """  # noqa: E501
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
