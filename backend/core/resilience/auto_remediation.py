import os

from loguru import logger


class AutoRemediation:
    """Autonomous Auto-Remediation Loop.

    Detects CodeQL/security alerts, generates a secure patch, and delegates the
    application to RemediationPipeline.

    Note: This module is designed to be mockable for tests.
    """

    def __init__(self, gemini_api_key: str | None = None):
        self.gemini_api_key = gemini_api_key or os.getenv("GEMINI_API_KEY", "")
        # Allowed base dir is repo/backend (backend/core/resilience -> ../..)
        self._ALLOWED_BASE_DIR = os.path.abspath(
            os.path.join(os.path.dirname(__file__), "../..")
        )

    def _validate_file_path(self, file_path: str) -> str:
        """Path traversal attack প্রতিরোধ.

        Only allow reads/writes inside the allowed directory.
        """
        real_path = os.path.realpath(os.path.abspath(file_path))

        common = os.path.commonpath([real_path, self._ALLOWED_BASE_DIR])
        if common != self._ALLOWED_BASE_DIR:
            raise ValueError(
                f"🛑 Path traversal detected: {file_path!r} resolves to {real_path!r} "
                f"which is outside allowed directory {self._ALLOWED_BASE_DIR!r}"
            )

        return real_path

    async def process_security_alert(
        self,
        file_path: str,
        line_number: int,
        issue: str,
        severity: str,
        tenant_id: str = "default_tenant",
    ) -> dict:
        logger.info(
            f"Auto-Remediation triggered for {file_path}:{line_number} - Severity: {severity}. Issue: {issue}"
        )

        safe_path = self._validate_file_path(file_path)

        if not os.path.exists(safe_path):
            return {"success": False, "error": f"File {safe_path} not found"}

        with open(safe_path, encoding="utf-8") as f:
            original_code = f.read()

        fixed_code = await self._get_ai_patch(
            safe_path, original_code, line_number, issue
        )

        if not fixed_code:
            return {"success": False, "error": "AI failed to generate a secure patch"}

        # Import inside function keeps tests isolated but also allows patching via module path.
        from core.health.self_healer import RemediationPipeline

        pipeline = RemediationPipeline()

        impact_score = 0.8 if severity.lower() in {"high", "critical"} else 0.3

        result = await pipeline.submit(tenant_id, issue, fixed_code, impact_score, [])

        if str(result).startswith("reject"):
            return {"success": False, "error": f"Patch rejected by pipeline: {result}"}

        return {
            "success": True,
            "file": file_path,
            "patch_applied": True,
            "branch": "supremeai-improvements",
            "pr_url": None,
            "message": f"Remediation patch processed by pipeline. ID: {result}",
        }

    async def _get_ai_patch(
        self, file_path: str, code: str, line_number: int, issue: str
    ) -> str:
        # The actual LLM integration is intentionally dynamic to keep this module testable.
        from core.ld_client import get_ld_ai_components

        ld_ai_client, AICompletionConfigDefault, LDMessage, ModelConfig, Context = (
            get_ld_ai_components()
        )

        default_prompt_template = (
            "You are an elite secure coding assistant. Correct the security vulnerability in this file.\n"
            "File: {file_path}\n"
            "Line Number of Vulnerability: {line_number}\n"
            "Vulnerability Description: {issue}\n\n"
            "Provide the complete corrected file contents. Do NOT explain the changes. "
            "Return ONLY the code in plaintext with no markdown code blocks.\n\n"
            "Original Code:\n{code}\n"
        )

        context = None
        if Context is not None:
            context = Context.builder("auto-remediation-helper").kind("service").build()

        prompt_vars = {
            "file_path": file_path,
            "line_number": str(line_number),
            "issue": issue,
            "code": code,
        }

        config = None
        if (
            ld_ai_client
            and AICompletionConfigDefault
            and LDMessage
            and ModelConfig
            and context
        ):
            try:
                config = ld_ai_client.completion_config(
                    os.getenv("LAUNCHDARKLY_AI_CONFIG_KEY", "auto-remediation-patch"),
                    context,
                    default=AICompletionConfigDefault(
                        enabled=True,
                        model=ModelConfig(name="gemini/gemini-2.5-pro"),
                        messages=[
                            LDMessage(role="system", content=default_prompt_template)
                        ],
                    ),
                    variables=prompt_vars,
                )
            except Exception as exc:  # noqa: BLE001
                logger.warning(
                    f"LaunchDarkly config evaluation failed, falling back: {exc}"
                )

        if config and getattr(config, "enabled", False):
            model_name = (
                config.model.name
                if getattr(config, "model", None)
                else "gemini/gemini-2.5-pro"
            )
            prompt = (
                config.messages[0].content
                if getattr(config, "messages", None)
                else default_prompt_template.format(**prompt_vars)
            )
        else:
            model_name = "gemini/gemini-2.5-pro"
            prompt = default_prompt_template.format(**prompt_vars)

        try:
            from core.llm.llm_gateway import llm_gateway

            response = await llm_gateway.acompletion(
                prompt=prompt,
                task_type="coding",
                stream=False,
                model=model_name,
            )
            raw_text = (
                response.get("text", "")
                if isinstance(response, dict)
                else str(response)
            )

            from utils.text_helpers import strip_markdown_code_block

            return strip_markdown_code_block(raw_text)
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Failed to generate patch from Gemini: {exc}")
            return ""
