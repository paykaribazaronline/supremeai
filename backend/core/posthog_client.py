import os
from loguru import logger
import posthog

class PostHogClient:
    def __init__(self):
        self.api_key = os.getenv("POSTHOG_API_KEY", "")
        self.host = os.getenv("POSTHOG_HOST", "https://app.posthog.com")
        self.enabled = bool(self.api_key)
        
        if self.enabled:
            try:
                posthog.project_api_key = self.api_key
                posthog.host = self.host
                logger.info("Initialized PostHog Analytics Client")
            except Exception as e:
                logger.error(f"Failed to initialize PostHog: {e}")
                self.enabled = False
        else:
            logger.warning("POSTHOG_API_KEY not set. PostHog analytics running in mock/log mode.")

    def capture(self, distinct_id: str, event: str, properties: dict = None):
        if self.enabled:
            try:
                posthog.capture(distinct_id, event, properties or {})
            except Exception as e:
                logger.error(f"PostHog capture failed: {e}")
        else:
            logger.info(f"[Mock Analytics] User: {distinct_id} | Event: {event} | Props: {properties}")

posthog_client = PostHogClient()
