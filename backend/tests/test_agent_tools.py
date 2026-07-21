"""
Tests for tools/agent_tools.py — SupremeAI Tools
"""

from __future__ import annotations

import pytest

from tools.agent_tools import (SUPREME_TOOLS, check_system_health,
                               execute_python_code, search_database)


class TestSearchDatabase:
    @pytest.mark.asyncio
    async def test_search_database_returns_result(self):
        result = await search_database("test query")
        assert isinstance(result, str)
        assert "matching records" in result

    @pytest.mark.asyncio
    async def test_search_database_includes_query(self):
        result = await search_database("deployment status")
        assert "deployment" in result


class TestCheckSystemHealth:
    def test_returns_status_string(self):
        result = check_system_health()
        assert isinstance(result, str)
        assert "ONLINE" in result

    def test_includes_resource_metrics(self):
        result = check_system_health()
        assert "CPU" in result
        assert "RAM" in result
        assert "Redis" in result


class TestExecutePythonCode:
    def test_returns_execution_result(self):
        result = execute_python_code("print('hello')")
        assert isinstance(result, str)
        assert "Execution successful" in result

    def test_includes_sandbox_reference(self):
        result = execute_python_code("print('test')")
        assert "SupremeAI Sandbox" in result


class TestSupremeTools:
    def test_tools_list_contains_all_tools(self):
        assert len(SUPREME_TOOLS) == 3
        assert search_database in SUPREME_TOOLS
        assert check_system_health in SUPREME_TOOLS
        assert execute_python_code in SUPREME_TOOLS

    def test_tools_are_callable(self):
        for tool in SUPREME_TOOLS:
            assert callable(tool)
