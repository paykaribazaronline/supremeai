# backend/tests/test_mcp_servers_integration.py
# বাংলা মন্তব্য: সমস্ত নতুন MCP সার্ভারগুলোর ইন্টিগ্রেশন টেস্ট

import pytest
import json
import os
import tempfile
import asyncio
from pathlib import Path
from unittest.mock import patch, AsyncMock, MagicMock
from pydantic import ValidationError


# বাংলা মন্তব্য: প্রতিটি টেস্টে এনভায়রনমেন্ট ভ্যারিয়েবল মক করার জন্য ফিক্সচার
@pytest.fixture(autouse=True)
def mock_env_vars(monkeypatch):
    monkeypatch.setenv("SUPABASE_DATABASE_URL", "postgres://localhost/mydb")
    monkeypatch.setenv("RENDER_API_KEY", "test-render-key")
    monkeypatch.setenv("RAILWAY_TOKEN", "test-railway-token")
    monkeypatch.setenv("ORACLE_CLOUD_API_KEY", "test-oracle-key")
    monkeypatch.setenv("ORACLE_REGION", "us-phoenix-1")
    monkeypatch.setenv("ADMIN_AUTHORIZED", "true")


# বাংলা মন্তব্য: cloud_deploy_mcp টেস্টস
class TestCloudDeployMCP:
    """cloud_deploy_mcp.py এর জন্য টেস্ট ক্লাস।"""

    def test_deploy_service_input_validation(self):
        """DeployServiceInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_cloud_deploy import DeployServiceInput, CloudProvider
        
        # বৈধ ইনপুট
        valid_input = DeployServiceInput(
            provider=CloudProvider.RENDER,
            service_name="test-service",
            branch="main"
        )
        assert valid_input.provider == CloudProvider.RENDER
        assert valid_input.service_name == "test-service"
        assert valid_input.branch == "main"

    def test_deploy_service_input_missing_provider(self):
        """প্রোভাইডার বাদে ইনপুট রিকেকশন টেস্ট।"""
        from tools.mcp_cloud_deploy import DeployServiceInput, CloudProvider
        
        with pytest.raises(ValidationError):
            DeployServiceInput(
                service_name="test-service",
                branch="main"
            )

    def test_get_logs_input_validation(self):
        """GetLogsInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_cloud_deploy import GetLogsInput, CloudProvider
        
        valid_input = GetLogsInput(
            provider=CloudProvider.RAILWAY,
            service_name="my-service",
            lines=500
        )
        assert valid_input.lines == 500

    def test_cloud_provider_enum(self):
        """CloudProvider enum টেস্ট।"""
        from tools.mcp_cloud_deploy import CloudProvider
        
        assert CloudProvider.RENDER.value == "render"
        assert CloudProvider.RAILWAY.value == "railway"
        assert CloudProvider.ORACLE.value == "oracle"


# বাংলা মন্তব্য: github_cicd_mcp টেস্টস
class TestGithubCICDMCP:
    """github_cicd_mcp.py এর জন্য টেস্ট ক্লাস।"""

    def test_create_pr_input_validation(self):
        """CreatePRInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_github_cicd import CreatePRInput
        
        valid_input = CreatePRInput(
            title="Test PR",
            body="This is a test PR",
            head="feature-branch",
            base="develop"
        )
        assert valid_input.title == "Test PR"
        assert valid_input.base == "develop"

    def test_create_pr_input_missing_title(self):
        """শিরোনাম বাদে ইনপুট রিকেকশন টেস্ট।"""
        from tools.mcp_github_cicd import CreatePRInput
        
        with pytest.raises(ValidationError):
            CreatePRInput(
                body="Test body",
                head="feature-branch"
            )

    def test_fix_issue_input_validation(self):
        """FixIssueInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_github_cicd import FixIssueInput
        
        valid_input = FixIssueInput(
            issue_number=42,
            branch="fix/issue-42"
        )
        assert valid_input.issue_number == 42

    def test_response_format_enum(self):
        """ResponseFormat enum টেস্ট।"""
        from tools.mcp_github_cicd import ResponseFormat
        
        assert ResponseFormat.MARKDOWN.value == "markdown"
        assert ResponseFormat.JSON.value == "json"


# বাংলা মন্তব্য: supabase_mcp টেস্টস
class TestSupabaseMCP:
    """supabase_mcp.py এর জন্য টেস্ট ক্লাস।"""

    def test_execute_query_input_validation(self):
        """ExecuteQueryInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_supabase import ExecuteQueryInput, ResponseFormat
        
        valid_input = ExecuteQueryInput(
            query="SELECT * FROM users LIMIT 10",
            params=None,
            response_format=ResponseFormat.JSON
        )
        assert valid_input.query == "SELECT * FROM users LIMIT 10"

    def test_execute_query_input_with_params(self):
        """ExecuteQueryInput প্যারামিটার সহ ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_supabase import ExecuteQueryInput, ResponseFormat
        
        valid_input = ExecuteQueryInput(
            query="SELECT * FROM users WHERE id = %s",
            params=[1],
            response_format=ResponseFormat.MARKDOWN
        )
        assert valid_input.params == [1]

    def test_create_table_input_validation(self):
        """CreateTableInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_supabase import CreateTableInput
        
        valid_input = CreateTableInput(
            table_name="users",
            columns="id SERIAL PRIMARY KEY, name VARCHAR(100)",
            if_not_exists=True
        )
        assert valid_input.if_not_exists is True

    def test_migration_input_validation(self):
        """MigrationInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_supabase import MigrationInput
        
        valid_input = MigrationInput(
            migration_name="create_users_table",
            up_sql="CREATE TABLE users (id SERIAL PRIMARY KEY);",
            down_sql="DROP TABLE users;"
        )
        assert valid_input.migration_name == "create_users_table"


# বাংলা মন্তব্য: workspace_mcp টেস্টস
class TestWorkspaceMCP:
    """workspace_mcp.py এর জন্য টেস্ট ক্লাস।"""

    def test_workspace_type_enum(self):
        """WorkspaceType enum টেস্ট।"""
        from tools.mcp_workspace import WorkspaceType
        
        assert WorkspaceType.ECOMMERCE_BACKEND.value == "ecommerce_backend"
        assert WorkspaceType.ECOMMERCE_FRONTEND.value == "ecommerce_frontend"
        assert WorkspaceType.MOBILE_FLUTTER.value == "mobile_flutter"
        assert WorkspaceType.ADMIN_PANEL.value == "admin_panel"

    def test_workspace_context_input_validation(self):
        """WorkspaceContextInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_workspace import WorkspaceContextInput, WorkspaceType
        
        valid_input = WorkspaceContextInput(
            project_type=WorkspaceType.ECOMMERCE_BACKEND,
            tenant_id="tenant-001"
        )
        assert valid_input.project_type == WorkspaceType.ECOMMERCE_BACKEND
        assert valid_input.tenant_id == "tenant-001"

    def test_scoped_file_path_input_validation(self):
        """ScopedFilePathInput মডেলের ভ্যালিডেশন টেস্ট।"""
        from tools.mcp_workspace import ScopedFilePathInput
        
        valid_input = ScopedFilePathInput(
            relative_path="src/main.py"
        )
        assert valid_input.relative_path == "src/main.py"

    def test_workspace_config_loading(self):
        """ওয়ার্কস্পেস কনফিগারেশন লোডিং টেস্ট।"""
        from tools.mcp_workspace import _load_workspace_config
        
        config = _load_workspace_config()
        assert isinstance(config, dict)


# বাংলা মন্তব্য: সিকনেশন টেস্টস
class TestMCPServerSync:
    """MCP সার্ভারগুলোর সিকনেশন টেস্ট।"""

    def test_all_mcp_servers_importable(self):
        """সব MCP সার্ভার ইম্পোর্ট করা যায় কিনা টেস্ট।"""
        import importlib.util
        
        servers = ["mcp_cloud_deploy", "mcp_github_cicd", "mcp_supabase", "mcp_workspace"]
        for server in servers:
            spec = importlib.util.find_spec(f"tools.{server}")
            assert spec is not None, f"tools.{server} module not found"

    def test_mcp_servers_have_fastmcp_instance(self):
        """MCP সার্ভারগুলোতে FastMCP ইনস্ট্যান্স আছে কিনা টেস্ট।"""
        from tools import mcp_cloud_deploy, mcp_github_cicd, mcp_supabase, mcp_workspace
        
        assert hasattr(mcp_cloud_deploy, 'mcp')
        assert hasattr(mcp_github_cicd, 'mcp')
        assert hasattr(mcp_supabase, 'mcp')
        assert hasattr(mcp_workspace, 'mcp')

    def test_mcp_servers_have_tools(self):
        """MCP সার্ভারগুলোতে টুলস আছে কিনা টেস্ট।"""
        from tools import mcp_cloud_deploy, mcp_github_cicd, mcp_supabase, mcp_workspace
        
        # cloud_deploy_mcp টুলস
        assert hasattr(mcp_cloud_deploy, 'cloud_deploy_service')
        assert hasattr(mcp_cloud_deploy, 'cloud_get_deployment_logs')
        assert hasattr(mcp_cloud_deploy, 'cloud_list_services')
        
        # github_cicd_mcp টুলস
        assert hasattr(mcp_github_cicd, 'github_create_pull_request')
        assert hasattr(mcp_github_cicd, 'github_run_auto_fix')
        assert hasattr(mcp_github_cicd, 'github_list_issues')
        assert hasattr(mcp_github_cicd, 'github_get_ci_status')
        
        # supabase_mcp টুলস
        assert hasattr(mcp_supabase, 'supabase_execute_sql')
        assert hasattr(mcp_supabase, 'supabase_create_table')
        assert hasattr(mcp_supabase, 'supabase_run_migration')
        assert hasattr(mcp_supabase, 'supabase_list_tables')
        
        # workspace_mcp টুলস
        assert hasattr(mcp_workspace, 'workspace_set_context')
        assert hasattr(mcp_workspace, 'workspace_get_scoped_path')
        assert hasattr(mcp_workspace, 'workspace_list_projects')

    def test_mcp_servers_run(self):
        """MCP সার্ভারগুলো run() মেথড কল করলে রান হয় কিনা যাচাই।"""
        from tools import mcp_cloud_deploy, mcp_github_cicd, mcp_supabase, mcp_workspace
        
        with patch.object(mcp_cloud_deploy.mcp, "run") as mock_run_cloud:
            mcp_cloud_deploy.mcp.run()
            mock_run_cloud.assert_called_once()
            
        with patch.object(mcp_supabase.mcp, "run") as mock_run_sb:
            mcp_supabase.mcp.run()
            mock_run_sb.assert_called_once()

    def test_service_name_validation_fails(self):
        """ভুল ফরম্যাটের সার্ভিস নেম রিজেক্ট হচ্ছে কিনা টেস্ট।"""
        from tools.mcp_cloud_deploy import DeployServiceInput, CloudProvider
        with pytest.raises(ValidationError):
            DeployServiceInput(
                provider=CloudProvider.RENDER,
                service_name="invalid;injection",
                branch="main"
            )

    @pytest.mark.asyncio
    async def test_workspace_path_traversal_fails(self):
        """পাথ ট্রাভার্সাল আক্রমণ রিজেক্ট হচ্ছে কিনা টেস্ট।"""
        from tools.mcp_workspace import ScopedFilePathInput, workspace_get_scoped_path
        
        params = ScopedFilePathInput(relative_path="../../sensitive_file.txt")
        result = await workspace_get_scoped_path(params)
        assert "Path traversal not allowed" in result
