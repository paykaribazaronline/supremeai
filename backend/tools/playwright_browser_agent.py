from __future__ import annotations

import json
import asyncio
import random
import time
from pathlib import Path
from typing import Any, Callable, Dict, List, Optional

from loguru import logger
from playwright.sync_api import BrowserContext, Page, sync_playwright
from playwright_stealth import stealth_sync

from core.secure_credential_store import SecureCredentialStore
from database.supabase_client import db


class PlaywrightBrowserAgent:
    COOKIE_STORAGE_BASE = Path(__file__).resolve().parents[1] / ".cache" / "playwright_cookies"

    def __init__(self, headless: bool = True, timeout_ms: int = 30000) -> None:
        self.headless = headless
        self.timeout_ms = timeout_ms
        self.playwright = None
        self.browser = None
        self.secure_store = SecureCredentialStore()
        self.COOKIE_STORAGE_BASE.mkdir(parents=True, exist_ok=True)

    def is_available(self) -> bool:
        import importlib.util

        return importlib.util.find_spec("playwright") is not None

    def _cookie_file_path(self, session_name: str) -> Path:
        safe_name = "".join(ch if ch.isalnum() or ch in "-_" else "_" for ch in session_name)
        return self.COOKIE_STORAGE_BASE / f"{safe_name}_cookies.json"

    def _load_cookies(self, context: Any, session_name: str) -> None:
        cookie_path = self._cookie_file_path(session_name)
        if not cookie_path.exists():
            return

        try:
            raw = cookie_path.read_text()
            payload = json.loads(raw)
            cookies = self.secure_store.decrypt(payload) if isinstance(payload, dict) else payload
            if isinstance(cookies, dict) and cookies.get("__enc__"):
                cookies = self.secure_store.decrypt(cookies)

            if isinstance(cookies, list):
                context.add_cookies(cookies)
                logger.info("Loaded Playwright cookies for session '%s' from %s", session_name, cookie_path)
            else:
                raise ValueError("Cookie payload is not a list")
        except Exception as exc:
            logger.warning("Failed to load cookies from %s: %s. Removing stale cookie file.", cookie_path, exc)
            try:
                cookie_path.unlink()
            except OSError:
                pass

    def _save_cookies(self, context: Any, session_name: str) -> None:
        cookie_path = self._cookie_file_path(session_name)
        cookies = context.cookies()
        payload = self.secure_store.encrypt(cookies)
        cookie_path.write_text(json.dumps(payload, indent=2))
        logger.info("Saved Playwright cookies for session '%s' to %s", session_name, cookie_path)

    def _human_like_type(self, page: Page, selector: str, text: str):
        """Types text into a field character by character with random delays."""
        for char in text:
            page.type(selector, char, delay=random.uniform(30, 100))

    def _human_like_click(self, page: Page, selector: str):
        """Moves mouse over an element, waits, and then clicks."""
        page.hover(selector)
        time.sleep(random.uniform(0.2, 0.6))
        page.click(selector)

    def _new_context(self, session_name: Optional[str] = None) -> Any:
        if self.browser is None:
            raise RuntimeError("Browser is not started. Call start() before creating a context.")

        context = self.browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            # Use a common viewport size
            viewport={"width": 1920, "height": 1080},
            # Set common browser language
            locale="en-US",
            # Set timezone
            timezone_id="America/New_York",
            # Set geolocation (optional, can be randomized)
            geolocation={"latitude": 40.7128, "longitude": -74.0060},
            # Set permissions
            permissions=["geolocation"],
        )

        if session_name:
            self._load_cookies(context, session_name)

        return context

    def start(self) -> None:
        if not self.is_available():
            raise RuntimeError("playwright is not installed")

        from playwright.sync_api import sync_playwright  # type: ignore

        if self.playwright is None:
            self.playwright = sync_playwright().start()
        if self.browser is None:
            self.browser = self.playwright.chromium.launch(headless=self.headless)
        
        # Apply stealth patches to all new contexts
        stealth_sync(self.browser)

    def stop(self) -> None:
        if self.browser:
            self.browser.close()
            self.browser = None
        if self.playwright:
            self.playwright.stop()
            self.playwright = None

    def perform_task(
        self,
        url: str,
        task_function: Callable[[Any], Any],
        session_name: Optional[str] = None,
        login_check_selector: Optional[str] = None,
        login_flow: Optional[Callable[[Any, Dict[str, str]], None]] = None,
        credentials: Optional[Dict[str, str]] = None,
    ) -> Dict[str, Any]:
        self.start()
        context = self._new_context(session_name)
        page = context.new_page()
        stealth_sync(page) # Apply stealth to the page as well
        page.set_default_timeout(self.timeout_ms)

        try:
            page.goto(url)

            if login_check_selector and login_flow and credentials:
                try:
                    is_authenticated = page.is_visible(login_check_selector)
                except Exception:
                    is_authenticated = False

                if not is_authenticated:
                    logger.info("Session invalid or expired, running login flow for '%s'.", session_name)
                    # Add a small delay to mimic human reading time
                    time.sleep(random.uniform(1.0, 2.5))
                    login_flow(page, credentials)
                    page.wait_for_load_state("networkidle")
                    self._save_cookies(context, session_name)
                else:
                    logger.info("Session restored from cookies for '%s'.", session_name)

            result = task_function(page)
            return {"success": True, "result": result}
        except Exception as exc:
            logger.error("Playwright task failed: %s", exc)
            return {"success": False, "error": str(exc)}
        finally:
            page.close()
            context.close()

    def open(self, url: str, session_name: Optional[str] = None) -> Dict[str, Any]:
        self.start()
        context = self._new_context(session_name)
        page = context.new_page()
        stealth_sync(page)
        page.set_default_timeout(self.timeout_ms)

        try:
            page.goto(url)
            title = page.title()
            return {"success": True, "url": url, "title": title}
        finally:
            page.close()
            context.close()

    def screenshot(self, url: str, path: str = "browser_screenshot.png", session_name: Optional[str] = None) -> Dict[str, Any]:
        self.start()
        context = self._new_context(session_name)
        page = context.new_page()
        stealth_sync(page)
        page.set_default_timeout(self.timeout_ms)

        try:
            page.goto(url)
            page.screenshot(path=path, full_page=False)
            return {"success": True, "path": path}
        finally:
            page.close()
            context.close()

    def click(self, url: str, selector: str, session_name: Optional[str] = None) -> Dict[str, Any]:
        self.start()
        context = self._new_context(session_name)
        page = context.new_page()
        stealth_sync(page)
        page.set_default_timeout(self.timeout_ms)

        try:
            page.goto(url)
            self._human_like_click(page, selector)
            return {"success": True}
        finally:
            page.close()
            context.close()

    def text(self, url: str, selector: str, session_name: Optional[str] = None) -> Dict[str, Any]:
        self.start()
        context = self._new_context(session_name)
        page = context.new_page()
        stealth_sync(page)
        page.set_default_timeout(self.timeout_ms)

        try:
            page.goto(url)
            content = page.text_content(selector) or ""
            return {"success": True, "text": content}
        finally:
            page.close()
            context.close()

    def _update_model_behavior_in_background(self, model_name: str, latency_ms: float, success: bool):
        """Runs the DB update in a background thread to avoid blocking."""
        try:
            import threading
            thread = threading.Thread(target=self._update_model_behavior, args=(model_name, latency_ms, success))
            thread.start()
        except Exception as e:
            logger.warning(f"Failed to spawn background thread for model behavior update: {e}")

    def _update_model_behavior(self, model_name: str, latency_ms: float, success: bool):
        """The actual database update logic."""
        # This logic would be more sophisticated in a real scenario, calculating rolling averages.
        # For now, we just upsert the latest data.
        db.upsert_model_behavior({"model_name": model_name, "avg_latency_ms": latency_ms, "last_seen_success": success})

    def cross_verify_prompt(
        self,
        prompt: str,
        primary_site: Dict[str, str],
        verifier_site: Dict[str, str],
    ) -> Dict[str, Any]:
        """
        Asks a question to a primary AI, gets the result, and asks a second AI to verify it.
        """
        logger.info(f"Starting cross-verification for prompt: '{prompt[:50]}...'")
        self.start()
        context = self._new_context("cross-verification-session")
        page = context.new_page()
        stealth_sync(page)
        page.set_default_timeout(self.timeout_ms)

        try:
            # Step 0: Check if the primary AI needs verification
            primary_behavior = db.get_model_behavior(primary_site['name'])
            # Default to verifying if no data is found
            requires_verification = True
            if primary_behavior:
                # Trust score could be a mix of success rate, latency, etc.
                # For now, we use a simple flag.
                trust_score = primary_behavior.get('trust_score', 0)
                if primary_behavior.get('requires_verification') is False or trust_score > 0.95:
                    requires_verification = False

            logger.info(f"Checking primary AI '{primary_site['name']}'. Verification required: {requires_verification}")

            # Step 1: Get response from the primary AI site
            logger.info(f"Querying Primary AI: {primary_site['name']}")
            start_time = time.time()
            initial_response, primary_success = self._query_ai_site(page, primary_site, prompt)
            latency_ms = (time.time() - start_time) * 1000
            self._update_model_behavior_in_background(primary_site['name'], latency_ms, primary_success)

            if not primary_success:
                raise RuntimeError(f"Failed to get a response from {primary_site['name']}")
            logger.info(f"Got initial response from {primary_site['name']}: '{initial_response[:100]}...'")

            # If verification is not required, return early
            if not requires_verification:
                logger.info(f"Skipping verification for trusted AI: {primary_site['name']}")
                return {"success": True, "prompt": prompt, "initial_response": initial_response, "is_confirmed": True, "final_action": "implement", "verification_skipped": True}

            # Step 2: Ask the verifier AI to check the response
            logger.info(f"Querying Verifier AI: {verifier_site['name']}")
            verification_prompt = (
                f"Please verify the following statement and determine if it is correct. "
                f"Answer with only 'CORRECT' or 'INCORRECT'.\n\nStatement: '{initial_response}'"
            )

            start_time_verifier = time.time()
            verification_result, verifier_success = self._query_ai_site(page, verifier_site, verification_prompt)
            latency_ms_verifier = (time.time() - start_time_verifier) * 1000
            self._update_model_behavior_in_background(verifier_site['name'], latency_ms_verifier, verifier_success)

            if not verifier_success:
                raise RuntimeError(f"Failed to get a response from {verifier_site['name']}")
            logger.info(f"Got verification result from {verifier_site['name']}: '{verification_result}'")

            # Step 3: Analyze the verification and return the final result
            is_confirmed = "correct" in verification_result.lower()

            return {
                "success": True,
                "prompt": prompt,
                "initial_response": initial_response,
                "verification_response": verification_result,
                "is_confirmed": is_confirmed,
                "final_action": "implement" if is_confirmed else "reject",
            }

        except Exception as exc:
            logger.error(f"Cross-verification failed: {exc}")
            return {"success": False, "error": str(exc)}
        finally:
            page.close()
            context.close()

    def _query_ai_site(self, page: Page, site_config: Dict[str, str], prompt: str) -> tuple[str, bool]:
        """Helper function to interact with a single AI chat website."""
        try:
            page.goto(site_config["url"])
            page.wait_for_selector(site_config["input_selector"], state="visible", timeout=20000)

            # Use human-like typing
            self._human_like_type(page, site_config["input_selector"], prompt)
            time.sleep(random.uniform(0.5, 1.0))

            # Click submit
            self._human_like_click(page, site_config["submit_button"])

            # Wait for the response to be generated.
            # This waits until the text content of the last message stops changing.
            page.wait_for_function(
                """
                () => {
                    const outputElements = document.querySelectorAll(arguments[0]);
                    if (outputElements.length === 0) return false;
                    const lastElement = outputElements[outputElements.length - 1];
                    if (!lastElement) return false;
                    const initialText = lastElement.textContent;
                    return new Promise(resolve => {
                        setTimeout(() => {
                            const currentText = lastElement.textContent;
                            resolve(initialText === currentText && currentText.length > 0);
                        }, 2500); // Wait 2.5 seconds to see if text changes
                    });
                }
                """,
                site_config["output_selector"],
                timeout=60000
            )

            # Extract the text from the last message element
            response_text = page.evaluate(
                f"Array.from(document.querySelectorAll('{site_config['output_selector']}')).pop()?.textContent"
            )

            if response_text and response_text.strip():
                return response_text.strip(), True
            return "", False
        except Exception as e:
            logger.error(f"Querying AI site {site_config['name']} failed: {e}")
            return "", False

# Example Usage:
# agent = PlaywrightBrowserAgent(headless=False)
# GROQ_CONFIG = {"name": "Groq", "url": "https://chat.groq.com/", "input_selector": 'textarea[aria-label="Prompt"]', "output_selector": '.message-content', "submit_button": 'button[aria-label="Submit"]'}
# GEMINI_CONFIG = {"name": "Gemini", "url": "https://gemini.google.com/", "input_selector": '.query-input > .input-area > .ql-editor', "output_selector": '.model-response-text .markdown', "submit_button": '.send-button-container > button'}
# result = agent.cross_verify_prompt("What is the capital of Bangladesh?", GROQ_CONFIG, GEMINI_CONFIG)
# print(result)
# agent.stop()
