# backend/tools/mcp_server.py
import json
import asyncio
from mcp.server import Server
from mcp import types
from tools.graph_service import GraphService
from loguru import logger

# বাংলা মন্তব্য: নলেজ গ্রাফের জন্য একটি অফিসিয়াল MCP সার্ভার ইনিশিয়ালাইজ করা হচ্ছে
app = Server("supremeai-knowledge-graph")
graph_service = GraphService()

@app.list_tools()
async def handle_list_tools() -> list[types.Tool]:
    """বাংলা মন্তব্য: এআই এজেন্টের কাছে এভেইলেবল গ্রাফ টুলসগুলোর তালিকা প্রকাশ করবে।"""
    return [
        types.Tool(
            name="get_skill_dependencies",
            description="Exposes the entire dependency and connection graph of SupremeAI skills.",
            inputSchema={
                "type": "object",
                "properties": {},
            },
        ),
        types.Tool(
            name="find_optimal_learning_path",
            description="Finds the shortest, optimized chain between two complex skills.",
            inputSchema={
                "type": "object",
                "properties": {
                    "start_skill": {"type": "string", "description": "The starting skill name"},
                    "end_skill": {"type": "string", "description": "The target skill name"},
                },
                "required": ["start_skill", "end_skill"],
            },
        ),
    ]

@app.call_tool()
async def handle_call_tool(name: str, arguments: dict | None) -> list[types.TextContent]:
    """বাংলা মন্তব্য: এআই এজেন্টের রিকোয়েস্ট অনুযায়ী নির্দিষ্ট গ্রাফ কোয়েরি এক্সিকিউট করে কনটেক্সট রিটার্ন করবে।"""
    if not arguments:
        arguments = {}

    try:
        if name == "get_skill_dependencies":
            # ডাটাবেস সেশন বা মক ডেটা থেকে কনটেক্সট গ্যাদারিং
            if graph_service.dry_run:
                graph_data = {"status": "dry-run", "nodes": ["Python", "FastAPI", "Redis"]}
            else:
                async with graph_service.driver.session() as session:
                    result = await session.run("MATCH (n:Skill) RETURN n.name AS name LIMIT 50")
                    records = await result.data()
                    graph_data = {"nodes": [r["name"] for r in records]}

            return [
                types.TextContent(
                    type="text",
                    text=f"SupremeAI Skills Graph Context:\n{json.dumps(graph_data, indent=2)}"
                )
            ]

        elif name == "find_optimal_learning_path":
            start = arguments.get("start_skill")
            end = arguments.get("end_skill")
            
            path = await graph_service.get_skill_path(start, end)
            return [
                types.TextContent(
                    type="text",
                    text=f"Optimal execution path from {start} to {end}:\n{ ' -> '.join(path) if path else 'No path found.' }"
                )
            ]

        else:
            raise ValueError(f"Unknown MCP tool: {name}")

    except Exception as e:
        logger.error(f"MCP Server execution error: {e}")
        return [types.TextContent(type="text", text=f"Error gathering graph context: {str(e)}")]

async def main():
    # Stdio ট্রান্সপোর্টের মাধ্যমে সার্ভারটি রান করানো (Standard Input/Output)
    from mcp.server.stdio import stdio_server
    logger.info("Starting SupremeAI MCP Graph Server over Stdio...")
    async with stdio_server() as (read_stream, write_server):
        await app.run(
            read_stream,
            write_server,
            app.create_initialization_options()
        )

if __name__ == "__main__":
    asyncio.run(main())
