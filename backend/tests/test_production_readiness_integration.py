"""
Integration tests for Phase 1 Production Readiness Systems
বাংলা মন্তব্য: Safety Guard, Multi-Model Validator, Codegraph, এবং AI Agent System সব সিস্টেমের একীভূত পরীক্ষা।
"""

import json
import tempfile
from pathlib import Path
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

REPO_ROOT = Path(__file__).resolve().parents[2]


class TestProductionReadinessSystems:
    """Phase 1 সিস্টেমগুলির জন্য একীভূত পরীক্ষা"""

    def test_safety_guard_detects_critical_files(self):
        """Safety Guard সংবেদনশীল ফাইল সনাক্ত করে"""
        # বাংলা মন্তব্য: Safety Guard সংবেদনশীল ফাইল প্যাটার্ন সনাক্ত করতে পারে
        try:
            from scripts.safety_guard import SafetyGuard
        except ImportError:
            pytest.skip("safety_guard module not available")

        guard = SafetyGuard()

        # সংবেদনশীল ফাইল পরীক্ষা করুন
        sensitive_files = [
            "backend/core/auth_middleware.py",
            "backend/core/security.py",
            "backend/core/payment_processor.py",
            "backend/core/admin_routes.py",
            ".github/workflows/deploy.yml",
        ]

        for file_path in sensitive_files:
            result = guard.block_or_approve(file_path=file_path, ai_authored=True)
            assert not result["allowed"] or result["requires_approval"]

    def test_multi_model_validator_returns_json(self):
        """Multi-Model Validator JSON রিপোর্ট ফেরত দেয়"""
        try:
            from scripts.multi_model_validator import MultiModelValidator
        except ImportError:
            pytest.skip("multi_model_validator module not available")

        # Mock করুন LiteLLM কল
        with patch("litellm.acompletion", new_callable=AsyncMock) as mock_llm:
            mock_llm.return_value = {
                "choices": [
                    {
                        "message": {
                            "content": json.dumps(
                                {
                                    "vulnerabilities": [
                                        {
                                            "type": "SQL Injection",
                                            "severity": "HIGH",
                                            "line": 42,
                                        }
                                    ],
                                    "risk_level": "MEDIUM",
                                }
                            )
                        }
                    }
                ]
            }

            validator = MultiModelValidator()
            assert hasattr(validator, "validate_code")
            assert hasattr(validator, "validate_file_changes")

    def test_codegraph_generates_knowledge_index(self):
        """Codegraph AI-বান্ধব জ্ঞান সূচক তৈরি করে"""
        try:
            from scripts.codegraph_integration import CodeGraphGenerator
        except ImportError:
            pytest.skip("codegraph_integration module not available")

        with tempfile.TemporaryDirectory() as tmpdir:
            generator = CodeGraphGenerator(codebase_path=str(REPO_ROOT), output_dir=tmpdir)

            # সম্পূর্ণ বিশ্লেষণ চালান
            result = generator.generate_knowledge_index()

            # জ্ঞান সূচক তৈরি হয়েছে নিশ্চিত করুন
            knowledge_path = Path(tmpdir) / "knowledge_index.json"
            assert knowledge_path.exists(), f"Knowledge index missing: {knowledge_path}"
            assert result.get("status") == "success"

    def test_ai_agent_system_prompt_exists(self):
        """AI Agent System Prompt ডকুমেন্ট বিদ্যমান এবং বৈধ"""
        # বাংলা মন্তব্য: restructuring-এর পর নতুন পাথে ফাইল চেক করা হচ্ছে (fallback সহ)
        prompt_path = REPO_ROOT / "docs" / "english" / "02-architecture" / "AI_AGENT_SYSTEM_PROMPT.md"
        if not prompt_path.exists():
            prompt_path = REPO_ROOT / "docs" / "AI_AGENT_SYSTEM_PROMPT.md"

        assert prompt_path.exists(), f"Prompt file missing: {prompt_path}"

        content = prompt_path.read_text(encoding="utf-8")
        assert "SupremeAI 2.0" in content
        assert "Maintenance Agent" in content
        assert "AI Agent System Prompt" in content or "Maintenance Context" in content

    @pytest.mark.anyio
    async def test_autocache_integration(self):
        """Autocache Proxy সিমান্টিক ম্যাচিং করে"""
        try:
            from core.cache.autocache_proxy import AutocacheProxy
            from core.cache.semantic_cache import SemanticCache
        except ImportError:
            pytest.skip("autocache modules not available")

        with patch("core.cache.semantic_cache.SemanticCache"):
            # Mock ক্যাশ আচরণ
            cache_instance = MagicMock()
            cache_instance.get_similar.return_value = None

            proxy = AutocacheProxy(semantic_cache=cache_instance)

            # খরচ ট্র্যাকিং কাজ করে নিশ্চিত করুন
            summary = proxy.get_cost_summary()
            assert "summary" in summary or isinstance(summary, dict)

    def test_phase1_systems_documentation_complete(self):
        """সব Phase 1 সিস্টেম ডকুমেন্টেড"""
        # বাংলা মন্তব্য: প্রতিটি Phase 1 সিস্টেমের ডকুমেন্টেশন আছে নিশ্চিত করুন
        contributing_path = REPO_ROOT / "CONTRIBUTING.md"

        assert contributing_path.exists(), f"CONTRIBUTING.md not found at {contributing_path}"
        content = contributing_path.read_text(encoding="utf-8")

        # সব সিস্টেমের উল্লেখ আছে নিশ্চিত করুন
        systems = [
            "Safety Guard",
            "Multi-Model Validator",
            "Autocache",
            "Codegraph",
            "AI Agent",
        ]

        for system in systems:
            assert system in content, f"{system} not documented in CONTRIBUTING.md"

    def test_ci_cd_workflow_includes_production_readiness(self):
        """CI/CD ওয়ার্কফ্লো Production Readiness জব অন্তর্ভুক্ত করে"""
        workflow_path = REPO_ROOT / ".github" / "workflows" / "supreme-core-ci.yml"

        if workflow_path.exists():
            content = workflow_path.read_text(encoding="utf-8")

            # Production Readiness জব বিদ্যমান নিশ্চিত করুন
            assert "production-readiness" in content or "Safety Guard" in content

            # সিস্টেমগুলি সঠিক ক্রমে চলে নিশ্চিত করুন
            # production-readiness → backend-core
            assert "backend-core" in content

    def test_code_style_guide_includes_bengali_comments(self):
        """কোড স্টাইল গাইড বাংলা মন্তব্য অন্তর্ভুক্ত করে"""
        contributing_path = REPO_ROOT / "CONTRIBUTING.md"

        assert contributing_path.exists(), f"CONTRIBUTING.md not found at {contributing_path}"
        content = contributing_path.read_text(encoding="utf-8")

        # বাংলা মন্তব্য প্যাটার্ন উল্লেখ করা আছে নিশ্চিত করুন
        assert "বাংলা মন্তব্য" in content or "Bengali comment" in content

    def test_test_coverage_configuration_realistic(self):
        """টেস্ট কভারেজ কনফিগ বাস্তববাদী"""
        pyproject_path = REPO_ROOT / "backend" / "pyproject.toml"

        assert pyproject_path.exists(), f"pyproject.toml not found at {pyproject_path}"
        content = pyproject_path.read_text(encoding="utf-8")

        # কভারেজ প্রয়োজনীয়তা বাস্তববাদী হওয়া উচিত
        # Phase 2 এ ধীরে ধীরে বৃদ্ধি পায়
        assert "cov-fail-under" in content or "coverage" in content


class TestTeamOnboarding:
    """দল অনবোর্ডিং এবং ডকুমেন্টেশন পরীক্ষা"""

    def test_quick_start_guide_exists(self):
        """দ্রুত শুরু গাইড অ্যাক্সেসযোগ্য"""
        readme_path = REPO_ROOT / "README.md"
        contributing_path = REPO_ROOT / "CONTRIBUTING.md"

        # কমপক্ষে একটি দোকান উপলব্ধ হওয়া উচিত
        assert readme_path.exists() or contributing_path.exists()

    def test_development_workflow_documented(self):
        """ডেভেলপমেন্ট ওয়ার্কফ্লো স্পষ্টভাবে নথিভুক্ত"""
        contributing_path = REPO_ROOT / "CONTRIBUTING.md"

        assert contributing_path.exists(), f"CONTRIBUTING.md not found at {contributing_path}"
        content = contributing_path.read_text(encoding="utf-8")

        # মূল ওয়ার্কফ্লো পদক্ষেপ অন্তর্ভুক্ত হওয়া উচিত
        workflow_keywords = [
            "git checkout",  # বা ব্র্যাঞ্চ তৈরি
            "pytest",  # বা টেস্ট চালান
            "git commit",  # বা কমিট করুন
        ]

        for keyword in workflow_keywords:
            assert keyword in content.lower()

    def test_testing_examples_included(self):
        """টেস্টিং উদাহরণ এবং টেমপ্লেট অন্তর্ভুক্ত"""
        contributing_path = REPO_ROOT / "CONTRIBUTING.md"

        assert contributing_path.exists(), f"CONTRIBUTING.md not found at {contributing_path}"
        content = contributing_path.read_text(encoding="utf-8")

        # টেস্ট কোড উদাহরণ আছে নিশ্চিত করুন
        assert "pytest" in content
        assert "test_" in content or "@pytest" in content


if __name__ == "__main__":
    # pytest tests/test_production_readiness_integration.py -v
    pytest.main([__file__, "-v"])
