# backend/tests/test_mcp_server.py
import pytest
from unittest.mock import AsyncMock, patch
import mcp.types as types
from tools.mcp_server import handle_list_tools, handle_call_tool

@pytest.mark.anyio
async def test_mcp_list_tools():
    # বাংলা মন্তব্য: MCP সার্ভার তার এভেইলেবল টুলসের স্কিমা ঠিকমতো লিস্ট করছে কিনা তা যাচাইয়ের টেস্ট।
    tools = await handle_list_tools()
    assert len(tools) == 2
    assert tools[0].name == "get_skill_dependencies"
    assert tools[1].name == "find_optimal_learning_path"

@pytest.mark.anyio
async def test_mcp_call_tool_dependencies():
    # বাংলা মন্তব্য: MCP টুল কল করার পর রেসপন্স ফরম্যাট টেক্সট কনটেন্ট আকারে আসছে কিনা তা যাচাই করা।
    res = await handle_call_tool("get_skill_dependencies", {})
    assert len(res) == 1
    assert isinstance(res[0], types.TextContent)
    assert "SupremeAI Skills Graph Context" in res[0].text

@pytest.mark.anyio
async def test_mcp_call_tool_path():
    # বাংলা মন্তব্য: পাথ ফাইন্ডিং MCP টুলের মক ডাটা রেসপন্স ভ্যালিডেশন।
    arguments = {"start_skill": "Python", "end_skill": "FastAPI"}
    res = await handle_call_tool("find_optimal_learning_path", arguments)
    assert len(res) == 1
    assert "Optimal execution path" in res[0].text
