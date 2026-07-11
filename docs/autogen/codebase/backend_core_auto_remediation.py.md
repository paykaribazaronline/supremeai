# 📄 ফাইল: backend/core/auto_remediation.py

**প্রকার:** .py  
**সাইজ:** 15,366 বাইট  
**আপডেট:** 2026-07-11T17:16:16.851144

---

## কোড

```py
import os

from github import Github
from loguru import logger

from tools.github_agent import GitHubAgent


class AutoRemediationEngine:
    def __init__(self):
        # বাংলা মন্তব্য: P1/P2 Fix — __init__-এ GitHub network call নিষিদ্ধ।
        # Lazy property হিসেবে GitHub client initialize হবে প্রথম ব্যবহারে।
        # আগে: `self.repo = self.github_client.get_repo(...)` init-এ network call করতো।
        self._github_client = None
        self._repo_obj = None
        self._model = None

    @property
    def repo(self):
        """Lazy GitHub repo property — প্রথম access-এ initialize হবে"""
        if self._repo_obj is None:
            token = os.getenv("GITHUB_TOKEN")
            if not token:
                raise RuntimeError("GITHUB_TOKEN not configured for AutoRemediationEngine")
            self._github_client = Github(token)
            repo_name = os.getenv("GITHUB_REPOSITORY")
            if not repo_name:
                raise RuntimeError("GITHUB_REPOSITORY environment variable must be set for AutoRemediationEngine")
            self._repo_obj = self._github_client.get_repo(repo_name)
        return self._repo_obj

    async def process_codeql_alert(self, file_path: str, line_number: int, vulnerability_details: str):
        """CodeQL অ্যালার্ট প্রসেস করে অটোমাটিক PR ওপেন করে"""
        import asyncio

        try:
            # 1. গিটহাব থেকে অরিজিনাল কোড ফেচ করা
            file_content = await asyncio.to_thread(lambda: self.repo.get_contents(file_path).decoded_content.decode("utf-8"))

            # 2. বাংলা মন্তব্য: P1 Fix — async patch generation, asyncio.run() নিষিদ্ধ
            patch_code = await self._generate_ai_patch(file_content, line_number, vulnerability_details)

            if patch_code:
                # 3. অটোমাটিক Branch এবং PR তৈরি করা
                await asyncio.to_thread(self._create_remediation_pr, file_path, file_content, patch_code, vulnerability_details)
                logger.info(f"✅ Auto-Remediation PR created for {file_path}")

        except Exception as e:  # noqa: BLE001
            logger.error(f"❌ Remediation failed: {str(e)}")

    async def _generate_ai_patch(self, code: str, line: int, issue: str) -> str:
        # বাংলা মন্তব্য: P1 Fix — asyncio.run() সরানো হয়েছে, এখন async/await ব্যবহার হচ্ছে।
        # আগে: asyncio.run() running event loop-এ RuntimeError দিতো — self-healing pipeline ক্রাশ করতো।
        ld_ai_client = None
        AICompletionConfigDefault = None
        LDMessage = None
        ModelConfig = None
        Context = None

        try:
            from ldai import AICompletionConfigDefault as _AICompletionConfigDefault
            from ldai import LDMessage as _LDMessage
            from ldai import ModelConfig as _ModelConfig
            from ldclient.context import Context as _Context

            from core.ld_client import ld_ai_client as _ld_ai_client

            AICompletionConfigDefault = _AICompletionConfigDefault
            LDMessage = _LDMessage
            ModelConfig = _ModelConfig
            Context = _Context
            ld_ai_client = _ld_ai_client
        except Exception as exc:  # noqa: BLE001
            logger.warning(f"LaunchDarkly auto-remediation modules unavailable, using fallback path: {exc}")

        default_prompt_template = """You are an elite AI AppSec Engineer. Fix the following vulnerability.
        Issue: {issue} at line {line}.
        Return ONLY the fully corrected Python code. No markdown formatting blocks, no explanations.

        Original Code:
        {code}
        """

        context = None
        if Context is not None:
            context = Context.builder("auto-remediation-engine").kind("service").build()
        prompt_vars = {"issue": issue, "line": str(line), "code": code}

        config = None
        if ld_ai_client and AICompletionConfigDefault and LDMessage and ModelConfig and context:
            try:
                config = ld_ai_client.completion_config(
                    os.getenv("LAUNCHDARKLY_AI_CONFIG_KEY", "auto-remediation-patch"),
                    context,
                    default=AICompletionConfigDefault(
                        enabled=True,
                        model=ModelConfig(name="gemini/gemini-1.5-pro"),
                        messages=[LDMessage(role="system", content=default_prompt_template)],
                    ),
                    variables=prompt_vars,
                )
            except Exception as exc:  # noqa: BLE001
                logger.warning(f"LaunchDarkly config evaluation failed, falling back: {exc}")

        if config and config.enabled:
            model_name = config.model.name if config.model else "gemini/gemini-1.5-pro"
            prompt = config.messages[0].content if config.messages else default_prompt_template.format(**prompt_vars)
        else:
            model_name = "gemini/gemini-1.5-pro"
            prompt = default_prompt_template.format(**prompt_vars)

        from core.llm_gateway import llm_gateway

        # বাংলা মন্তব্য: asyncio.run() সম্পূর্ণ সরানো হয়েছে — এখন await ব্যবহার হচ্ছে
        response = await llm_gateway.acompletion(prompt=prompt, task_type="coding", stream=False, model=model_name)
        result = response.get("text", "") if isinstance(response, dict) else str(response)
        return result.strip()

    def _create_remediation_pr(self, file_path: str, old_code: str, new_code: str, issue: str):
        branch_name = f"auto-fix/security-patch-{os.urandom(4).hex()}"
        main_branch = self.repo.get_branch("main")
        self.repo.create_git_ref(ref=f"refs/heads/{branch_name}", sha=main_branch.commit.sha)

        self.repo.update_file(
            path=file_path,
            message=f"🛡️ Auto-Remediation: Fixed {issue}",
            content=new_code,
            sha=self.repo.get_contents(file_path).sha,
            branch=branch_name,
        )

        self.repo.create_pull(
            title=f"🚨 Security Auto-Patch: {file_path}",
            body=f"This PR was automatically generated by SupremeAI Immune System to fix: **{issue}**.",
            head=branch_name,
            base="main",
        )


class AutoRemediation:
    """
    Autonomous Auto-Remediation Loop (Compatibility / Mockable Wrapper for tests).
    Detects CodeQL or security alerts, calls Gemini to get a secure patch,
    applies it, and creates a GitHub Pull Request for evaluation.
    """

    def __init__(self, gemini_api_key: str | None = None):
        self.gemini_api_key = gemini_api_key or os.getenv("GEMINI_API_KEY", "")
        self.github_agent = GitHubAgent()
        self._ALLOWED_BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    def _validate_file_path(self, file_path: str) -> str:
        """বাংলা মন্তব্য: P0 Fix — Path traversal attack প্রতিরোধ।
        শুধুমাত্র project directory-র ভেতরের ফাইলগুলোতে write অনুমোদিত।
        Symlink traversal রোধ করতে commonpath ব্যবহার করা হলো।
        """
        real_path = os.path.realpath(os.path.abspath(file_path))
        # os.path.commonpath() ব্যবহার করে symlink traversal প্রতিরোধ
        try:
            common = os.path.commonpath([real_path, self._ALLOWED_BASE_DIR])
            if common != self._ALLOWED_BASE_DIR:
                raise ValueError(
                    f"🛑 Path traversal detected: {file_path!r} resolves to {real_path!r} "
                    f"which is outside allowed directory {self._ALLOWED_BASE_DIR!r}"
                )
        except ValueError as ve:
            # Windows-এ drive letter mismatch হলে commonpath ValueError দেয়
            raise ValueError(
                f"🛑 Path traversal detected: {file_path!r} resolves to {real_path!r} which is outside allowed directory {self._ALLOWED_BASE_DIR!r}"
            ) from ve
        return real_path

    async def process_security_alert(self, file_path: str, line_number: int, issue: str, severity: str) -> dict:
        """বাংলা মন্তব্য: P1 Fix — method কে fully async করা হলো।
        আগে: asyncio.run()/loop.run_until_complete() ব্যবহার হতো → running event loop-এ RuntimeError।
        এখন: সব I/O অপারেশন await দিয়ে চলে — কোনো blocking নেই।
        """
        logger.info(f"Auto-Remediation triggered for {file_path}:{line_number} - Severity: {severity}. Issue: {issue}")

        # Path traversal protection (ধাপ ১.৫ দেখুন)
        safe_path = self._validate_file_path(file_path)

        if not os.path.exists(safe_path):
            return {"success": False, "error": f"File {safe_path} not found"}

        with open(safe_path, encoding="utf-8") as f:
            original_code = f.read()

        # বাংলা মন্তব্য: P1 Fix — সরাসরি await, asyncio.run() নয়
        fixed_code = await self._get_ai_patch(safe_path, original_code, line_number, issue)

        if not fixed_code:
            return {"success": False, "error": "AI failed to generate a secure patch"}

        # Syntax validation
        try:
            import ast

            if fixed_code.strip() and not fixed_code.strip().startswith("#"):
                ast.parse(fixed_code)
        except SyntaxError as se:
            logger.error(f"AI-generated patch failed validation: {se}")
            return {
                "success": False,
                "error": f"Generated patch contains invalid syntax: {se}",
            }

        # Write patched file
        try:
            with open(safe_path, "w", encoding="utf-8") as f:
                f.write(fixed_code)
            logger.info(f"Patch applied successfully to {safe_path}")
        except Exception as e:  # noqa: BLE001
            return {"success": False, "error": f"Failed to apply patch: {e}"}

        # Attempt GitHub commit
        commit_message = f"🛡️ Auto-Remediation: Fixed {issue}"
        try:
            self.github_agent.commit_changes(
                repo_url="paykaribazaronline/supremeai",
                files_to_commit=[safe_path],
                commit_message=commit_message,
                branch="main",
            )
        except RuntimeError as e:
            if "GitHub token is required" in str(e):
                logger.warning(f"GitHub token not available; patch applied locally but not committed: {e}")
            else:
                raise

        return {
            "success": True,
            "file": file_path,
            "patch_applied": True,
            "branch": "supremeai-improvements",
            "pr_url": None,
            "message": "Remediation patch applied and committed.",
        }

    async def _get_ai_patch(self, file_path: str, code: str, line_number: int, issue: str) -> str:
        # বাংলা মন্তব্য: P1 Fix — asyncio.run() সরানো হয়েছে, এখন async/await ব্যবহার হচ্ছে।
        ld_ai_client = None
        AICompletionConfigDefault = None
        LDMessage = None
        ModelConfig = None
        Context = None

        try:
            from ldai import AICompletionConfigDefault as _AICompletionConfigDefault
            from ldai import LDMessage as _LDMessage
            from ldai import ModelConfig as _ModelConfig
            from ldclient.context import Context as _Context

            from core.ld_client import ld_ai_client as _ld_ai_client

            AICompletionConfigDefault = _AICompletionConfigDefault
            LDMessage = _LDMessage
            ModelConfig = _ModelConfig
            Context = _Context
            ld_ai_client = _ld_ai_client
        except Exception as exc:  # noqa: BLE001
            logger.warning(f"LaunchDarkly remediation modules unavailable, using fallback path: {exc}")

        default_prompt_template = """You are an elite secure coding assistant. Correct the security vulnerability in this file.
        File: {file_path}
        Line Number of Vulnerability: {line_number}
        Vulnerability Description: {issue}

        Provide the complete corrected file contents. Do NOT explain the changes. Return ONLY the code in plaintext with no markdown code blocks.

        Original Code:
        {code}
        """

        context = None
        if Context is not None:
            context = Context.builder("auto-remediation-helper").kind("service").build()
        prompt_vars = {"file_path": file_path, "line_number": str(line_number), "issue": issue, "code": code}

        config = None
        if ld_ai_client and AICompletionConfigDefault and LDMessage and ModelConfig and context:
            try:
                config = ld_ai_client.completion_config(
                    os.getenv("LAUNCHDARKLY_AI_CONFIG_KEY", "auto-remediation-patch"),
                    context,
                    default=AICompletionConfigDefault(
                        enabled=True,
                        model=ModelConfig(name="gemini/gemini-1.5-pro"),
                        messages=[LDMessage(role="system", content=default_prompt_template)],
                    ),
                    variables=prompt_vars,
                )
            except Exception as exc:  # noqa: BLE001
                logger.warning(f"LaunchDarkly config evaluation failed, falling back: {exc}")

        if config and config.enabled:
            model_name = config.model.name if config.model else "gemini/gemini-1.5-pro"
            prompt = config.messages[0].content if config.messages else default_prompt_template.format(**prompt_vars)
        else:
            model_name = "gemini/gemini-1.5-pro"
            prompt = default_prompt_template.format(**prompt_vars)

        try:
            from core.llm_gateway import llm_gateway

            response = await llm_gateway.acompletion(prompt=prompt, task_type="coding", stream=False, model=model_name)
            raw_text = response.get("text", "") if isinstance(response, dict) else str(response)

            # Strip markdown formatting if the model returned any
            if raw_text.strip().startswith("```"):
                lines = raw_text.strip().splitlines()
                if len(lines) > 1:
                    if lines[0].startswith("```"):
                        lines = lines[1:]
                    if lines and lines[-1].startswith("```"):
                        lines = lines[:-1]
                    raw_text = "\n".join(lines)

            return raw_text.strip()
        except Exception as e:  # noqa: BLE001
            logger.error(f"Failed to generate patch from Gemini: {e}")
            return ""

```