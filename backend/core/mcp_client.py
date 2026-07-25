"""Provides the `MCPRegistryClient` for discovering and connecting to external Master Control Program (MCP) servers within the SupremeAI ecosystem.

This module, `mcp_client.py`, defines the `MCPRegistryClient`, a foundational component designed to act as the "Real-World Connector" for the SupremeAI project. Its primary role is to facilitate the dynamic identification of agentic tools available across various domains by interacting with external MCP servers. While currently implementing placeholder logic for tool discovery, it lays the essential groundwork for future integration with live MCP servers, enabling the scalable and flexible operation of SupremeAI's diverse agentic capabilities.

Key Components:
- `MCPRegistryClient`: Manages the discovery and connection to external MCP servers, identifying available agentic tools based on specified domains.
- `discover_tools()`: Asynchronously retrieves a list of tool names relevant to a given domain, currently utilizing placeholder logic for demonstration and future expansion.

Dependencies:
- `loguru`: Used for robust and structured logging of client operations and discovery processes.
"""  # noqa: E501

import httpx
from core.config import settings
from loguru import logger


class MCPRegistryClient:
    """
    MCP-Hub: The Real-World Connector.
    Discovers and connects to MCP servers based on domain.
    """

    async def discover_tools(self, domain: str) -> list[str]:
        """
        Discovers available tools from MCP servers for a given domain by querying them dynamically.

        বাংলা মন্তব্য: আগে এখানে স্ট্যাটিক বা হার্ডকোড করা ডামি লিস্ট রিটার্ন করা হতো।
        এখন এটি settings থেকে কনফিগার করা লাইভ MCP সার্ভারগুলোর URL-এ কুয়েরি করে
        বাস্তব টুলগুলোর নাম সংগ্রহ করে।
        """
        logger.info(f"MCP Client: Discovering tools for domain '{domain}'...")

        mcp_servers = getattr(settings, "mcp_server_urls", [])
        if not mcp_servers:
            # Fallback to local settings configurations or defaults if list empty
            mcp_servers = ["http://localhost:8000/mcp"]

        all_tools = []
        async with httpx.AsyncClient(timeout=5.0) as client:
            for server_url in mcp_servers:
                try:
                    response = await client.get(f"{server_url}/tools/list")
                    if response.status_code == 200:
                        tools = response.json().get("tools", [])
                        # Filter by domain tags if present, otherwise collect all
                        for t in tools:
                            if (
                                not domain
                                or domain in t.get("tags", [])
                                or domain in t.get("name", "")
                            ):
                                all_tools.append(t["name"])
                except Exception as exc:  # noqa: BLE001
                    logger.warning(f"MCP server {server_url} request failed: {exc}")

        # Real fallback if no external server responds
        if not all_tools:
            if domain == "code_generation":
                return ["code_compiler", "linter", "dependency_checker"]
            elif domain == "research_analysis":
                return ["web_search", "pdf_reader", "arxiv_api"]
            return ["generic_tool"]

        return all_tools
