import os
from unittest.mock import MagicMock, patch

import pytest

from core.admin_god import AdminGodLayer
from core.constants import COMMON_STRINGS_TO_IGNORE, DEFAULT_CODE_SMELL_THRESHOLDS
from core.logging_config import setup_logging
from core.mcp_allowlist import MCPAllowlist, get_mcp_servers
from core.task_queue import process_requirement_async


class TestConstants:
    def test_default_code_smell_thresholds(self):
        assert "complexity" in DEFAULT_CODE_SMELL_THRESHOLDS
        assert DEFAULT_CODE_SMELL_THRESHOLDS["complexity"] == 10
        assert DEFAULT_CODE_SMELL_THRESHOLDS["lines"] == 75
        assert DEFAULT_CODE_SMELL_THRESHOLDS["args"] == 5
        assert DEFAULT_CODE_SMELL_THRESHOLDS["class_methods"] == 15

    def test_common_strings_to_ignore(self):
        assert "" in COMMON_STRINGS_TO_IGNORE
        assert "utf-8" in COMMON_STRINGS_TO_IGNORE
        assert "rb" in COMMON_STRINGS_TO_IGNORE


class TestLoggingConfig:
    @patch("core.logging_config.logger")
    def test_setup_logging(self, mock_logger):
        setup_logging()
        assert mock_logger.remove.called
        assert mock_logger.add.call_count == 2


class TestMcpAllowlist:
    def test_get_mcp_servers(self):
        servers = get_mcp_servers()
        assert "github" in servers
        assert "slack" in servers
        assert "filesystem" in servers
        assert "allowed_tools" in servers["github"]

    def test_validate_server_allowed(self):
        result = MCPAllowlist.validate_server("github")
        assert result["allowed"] is True
        assert result["server"] == "github"
        assert len(result["tools"]) > 0

    def test_validate_server_unknown(self):
        result = MCPAllowlist.validate_server("nonexistent")
        assert result["allowed"] is False
        assert "unknown" in result["reason"]

    def test_allowed_tools_approved(self):
        result = MCPAllowlist.allowed_tools("github", ["search_repositories", "get_file_contents"])
        assert result["allowed"] is True
        assert "search_repositories" in result["allowed_tools"]
        assert "get_file_contents" in result["allowed_tools"]

    def test_allowed_tools_denied(self):
        result = MCPAllowlist.allowed_tools("github", ["evil_tool"])
        assert result["allowed"] is False
        assert "evil_tool" in result["denied"]


class TestAdminGodLayer:
    def test_verify_admin_success(self):
        layer = AdminGodLayer(rules_engine=MagicMock())
        os.environ["SUPREMEAI_ADMIN_PASSWORD_HASH"] = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"
        assert layer.verify_admin("admin123") is True

    def test_verify_admin_failure(self):
        layer = AdminGodLayer(rules_engine=MagicMock())
        assert layer.verify_admin("wrong_password") is False

    def test_verify_admin_empty(self):
        layer = AdminGodLayer(rules_engine=MagicMock())
        assert layer.verify_admin("") is False

    def test_enforce_with_user_context(self):
        mock_ctx = MagicMock()
        mock_ctx.role = "admin"
        mock_rbac = MagicMock()
        mock_rbac.require.return_value = {"allowed": True, "reason": "ok"}
        layer = AdminGodLayer(rules_engine=MagicMock())
        layer.rbac = mock_rbac
        result = layer.enforce("test_action", mock_ctx)
        assert result["allowed"] is True

    def test_enforce_with_string_role(self):
        mock_rbac = MagicMock()
        mock_rbac.require.return_value = {"allowed": True, "reason": "ok"}
        layer = AdminGodLayer(rules_engine=MagicMock())
        layer.rbac = mock_rbac
        result = layer.enforce("test_action", "viewer")
        assert result["allowed"] is True

    def test_enforce_denied(self):
        mock_ctx = MagicMock()
        mock_ctx.role = "viewer"
        mock_rbac = MagicMock()
        mock_rbac.require.return_value = {"allowed": False, "reason": "denied"}
        layer = AdminGodLayer(rules_engine=MagicMock())
        layer.rbac = mock_rbac
        with pytest.raises(PermissionError):
            layer.enforce("admin_action", mock_ctx)

    def test_enforce_rules(self):
        mock_rules = MagicMock()
        mock_rules.apply.return_value = {"passed": True}
        layer = AdminGodLayer(rules_engine=mock_rules)
        result = layer.enforce_rules({"decision": "test"})
        assert result["passed"] is True

    def test_inject_prompt_constraints(self):
        mock_rules = MagicMock()
        mock_rules.rules = {
            "no_harm": "Do no harm",
            "be_helpful": "Be helpful",
        }
        layer = AdminGodLayer(rules_engine=mock_rules)
        result = layer.inject_prompt_constraints("You are a helpful assistant.")
        assert "CONSTITUTIONAL RULES" in result
        assert "You are a helpful assistant." in result
        assert "No Harm" in result
        assert "Be Helpful" in result


class TestTaskQueue:
    def test_process_requirement_async_fallback(self):
        result = process_requirement_async("proj_123", "Build a cool feature")
        assert result["status"] == "completed"
        assert "Build a cool feature" in result["result"]

    @patch("core.task_queue.celery_app", None)
    def test_process_requirement_async_no_celery(self):
        result = process_requirement_async("proj_456", "Another feature")
        assert result["status"] == "completed"
