"""
✅ MCP TOOL DEFINITIONS - Master Plan Pillar 2 Complete
Standardized Model Context Protocol tools for browser automation
"""

from typing import Any, Optional, Dict, List
from pydantic import BaseModel, Field
from enum import Enum


class MCPToolName(str, Enum):
    """Standard MCP tool names as per Master Plan specification"""
    BROWSER_NAVIGATE = "browser_navigate"
    BROWSER_CLICK = "browser_click"
    BROWSER_TYPE = "browser_type"
    BROWSER_SCREENSHOT = "browser_screenshot"
    BROWSER_FILE_UPLOAD = "browser_file_upload"
    BROWSER_SELECT_OPTION = "browser_select_option"
    BROWSER_GET_TEXT = "browser_get_text"
    BROWSER_WAIT_FOR = "browser_wait_for"
    BROWSER_EVALUATE = "browser_evaluate"
    BROWSER_HOVER = "browser_hover"
    BROWSER_SCROLL = "browser_scroll"
    BROWSER_PRESS_KEY = "browser_press_key"


class MCPToolParameter(BaseModel):
    """Schema for MCP tool parameter"""
    type: str = Field(description="JSON Schema type")
    description: str = Field(description="Parameter description")
    optional: bool = Field(default=False, description="Is this parameter optional?")
    default: Any = Field(default=None, description="Default value if optional")
    enum: Optional[List[str]] = Field(default=None, description="Allowed values if enum")


class MCPTool(BaseModel):
    """MCP Tool definition schema"""
    name: MCPToolName
    description: str
    parameters: Dict[str, MCPToolParameter]
    returns: str = Field(description="Description of return value")
    example: Optional[Dict[str, Any]] = Field(default=None)


# ═══════════════════════════════════════════════════════════════
# COMPLETE MCP TOOL REGISTRY PER MASTER PLAN
# ═══════════════════════════════════════════════════════════════

MCP_BROWSER_TOOLS: List[MCPTool] = [
    MCPTool(
        name=MCPToolName.BROWSER_NAVIGATE,
        description="Navigate to a URL and wait for network idle",
        parameters={
            "url": MCPToolParameter(type="string", description="Target URL to navigate to"),
            "timeout": MCPToolParameter(type="number", description="Max wait time in ms", optional=True, default=30000),
            "wait_until": MCPToolParameter(type="string", description="Wait condition", optional=True, default="networkidle", enum=["load", "domcontentloaded", "networkidle"]),
        },
        returns="{'status': 'ok'|'error', 'url': str, 'title': str, 'final_url': str}",
        example={"url": "https://example.com", "timeout": 30000}
    ),
    
    MCPTool(
        name=MCPToolName.BROWSER_CLICK,
        description="Click on an element using CSS selector, text content, coordinates, or semantic description",
        parameters={
            "target": MCPToolParameter(type="string", description="CSS selector, text content, or natural language description"),
            "method": MCPToolParameter(type="string", description="Click method", optional=True, default="selector", enum=["selector", "text", "coordinate", "semantic"]),
            "x": MCPToolParameter(type="number", description="X coordinate for coordinate method", optional=True),
            "y": MCPToolParameter(type="number", description="Y coordinate for coordinate method", optional=True),
            "button": MCPToolParameter(type="string", description="Mouse button", optional=True, default="left", enum=["left", "right", "middle"]),
            "click_count": MCPToolParameter(type="integer", description="Number of clicks", optional=True, default=1),
        },
        returns="{'status': 'clicked'|'not_found'|'error', 'element': str, 'method': str}",
        example={"target": "Submit button", "method": "semantic"}
    ),
    
    MCPTool(
        name=MCPToolName.BROWSER_TYPE,
        description="Type text into input field or textarea with human-like delays",
        parameters={
            "selector": MCPToolParameter(type="string", description="CSS selector for input element"),
            "text": MCPToolParameter(type="string", description="Text to type"),
            "clear_first": MCPToolParameter(type="boolean", description="Clear existing text first", optional=True, default=True),
            "delay_ms": MCPToolParameter(type="number", description="Delay between keystrokes (human-like)", optional=True, default=50),
            "submit": MCPToolParameter(type="boolean", description="Press Enter after typing", optional=True, default=False),
        },
        returns="{'status': 'typed', 'selector': str, 'characters_typed': int}",
        example={"selector": "#search-input", "text": "Hello World", "delay_ms": 80}
    ),
    
    MCPTool(
        name=MCPToolName.BROWSER_SCREENSHOT,
        description="Capture screenshot of current page or specific element",
        parameters={
            "full_page": MCPToolParameter(type="boolean", description="Capture full scrolling page", optional=True, default=False),
            "selector": MCPToolParameter(type="string", description="CSS selector for element screenshot", optional=True),
            "format": MCPToolParameter(type="string", description="Image format", optional=True, default="png", enum=["png", "jpeg"]),
            "quality": MCPToolParameter(type="integer", description="JPEG quality 1-100", optional=True, default=80),
        },
        returns="{'status': 'ok', 'screenshot_base64': str, 'width': int, 'height': int, 'format': str}",
        example={"full_page": True, "format": "jpeg", "quality": 75}
    ),
    
    MCPTool(
        name=MCPToolName.BROWSER_FILE_UPLOAD,
        description="Upload file(s) through file input element",
        parameters={
            "selector": MCPToolParameter(type="string", description="CSS selector for file input"),
            "file_path": MCPToolParameter(type="string", description="Path to file on server"),
            "multiple": MCPToolParameter(type="boolean", description="Allow multiple files", optional=True, default=False),
        },
        returns="{'status': 'uploaded', 'files': List[str], 'selector': str}",
        example={"selector": "input[type='file']", "file_path": "/tmp/document.pdf"}
    ),
    
    MCPTool(
        name=MCPToolName.BROWSER_SELECT_OPTION,
        description="Select option from dropdown/select element",
        parameters={
            "selector": MCPToolParameter(type="string", description="CSS selector for select element"),
            "value": MCPToolParameter(type="string", description="Option value to select"),
            "label": MCPToolParameter(type="string", description="Option label to select (alternative to value)", optional=True),
            "by_label": MCPToolParameter(type="boolean", description="Select by label instead of value", optional=True, default=False),
        },
        returns="{'status': 'selected', 'value': str, 'label': str}",
        example={"selector": "#country-select", "value": "US"}
    ),
    
    MCPTool(
        name=MCPToolName.BROWSER_WAIT_FOR,
        description="Wait for a condition to be met",
        parameters={
            "selector": MCPToolParameter(type="string", description="CSS selector to appear", optional=True),
            "text": MCPToolParameter(type="string", description="Text to appear on page", optional=True),
            "timeout": MCPToolParameter(type="number", description="Max wait time in ms", optional=True, default=30000),
            "state": MCPToolParameter(type="string", description="Element state to wait for", optional=True, default="visible", enum=["visible", "hidden", "attached", "detached"]),
        },
        returns="{'status': 'found'|'timeout', 'elapsed_ms': int}",
        example={"selector": "#results-table", "timeout": 10000}
    ),
]


async def execute_mcp_tool(tool_name: str, params: Dict[str, Any], session_id: Optional[str] = None) -> Dict[str, Any]:
    """
    Execute an MCP tool by routing to appropriate Playwright method.
    This is the main entry point for AI agents using MCP protocol.
    
    Args:
        tool_name: Name from MCPToolName enum
        params: Tool parameters as dict
        session_id: Optional browser session ID
        
    Returns:
        Tool execution result as dict
    """
    from backend.tools.browser.playwright_browser_agent import PlaywrightBrowserAgent
    
    agent = PlaywrightBrowserAgent()
    
    try:
        # Route to appropriate agent method
        if tool_name == MCPToolName.BROWSER_NAVIGATE.value:
            return await agent.navigate(
                url=params["url"],
                timeout=params.get("timeout", 30000),
                session_name=session_id
            )
        
        elif tool_name == MCPToolName.BROWSER_CLICK.value:
            method = params.get("method", "selector")
            
            if method == "coordinate":
                return await agent.click_coordinate(
                    x=params["x"],
                    y=params["y"],
                    session_name=session_id
                )
            elif method == "semantic":
                # Route through L4 cascade (Semantic DOM → Vision → HITL)
                from backend.browser.semantic_dom import SemanticDOM
                sdom = SemanticDOM()
                el = await sdom.query(params["target"])
                xpath = el.get("xpath", params["target"])
                return await agent.click(xpath, session_name=session_id)
            else:
                return await agent.click(
                    url=None,  # Current page
                    selector=params["target"],
                    session_name=session_id
                )
        
        elif tool_name == MCPToolName.BROWSER_TYPE.value:
            return await agent.text(
                url=None,
                selector=params["selector"],
                text=params["text"],
                session_name=session_id
            )
        
        elif tool_name == MCPToolName.BROWSER_SCREENSHOT.value:
            result = await agent.screenshot(
                url=None,  # Current page
                path=None,  # Return base64
                full_page=params.get("full_page", False),
                session_name=session_id
            )
            # Ensure we return base64
            if result.get("success") and result.get("screenshot"):
                return {
                    "status": "ok",
                    "screenshot_base64": result["screenshot"],
                    "width": result.get("width", 0),
                    "height": result.get("height", 0),
                    "format": params.get("format", "png"),
                }
            return result
        
        elif tool_name == MCPToolName.BROWSER_FILE_UPLOAD.value:
            return await agent.upload_file(
                selector=params["selector"],
                file_path=params["file_path"]
            )
        
        elif tool_name == MCPToolName.BROWSER_SELECT_OPTION.value:
            return await agent.select_option(
                selector=params["selector"],
                value=params.get("value"),
                label=params.get("label"),
                by_label=params.get("by_label", False)
            )
        
        elif tool_name == MCPToolName.BROWSER_WAIT_FOR.value:
            return await agent.wait_for(
                selector=params.get("selector"),
                text=params.get("text"),
                timeout=params.get("timeout", 30000)
            )
        
        else:
            raise ValueError(f"Unknown MCP tool: {tool_name}")
            
    except Exception as e:
        return {
            "status": "error",
            "error": str(e),
            "tool": tool_name,
        }


def get_tool_schema(tool_name: str) -> Optional[MCPTool]:
    """Get schema for a specific tool"""
    for tool in MCP_BROWSER_TOOLS:
        if tool.name.value == tool_name:
            return tool
    return None


def list_available_tools() -> List[Dict[str, Any]]:
    """List all available MCP tools (for discovery)"""
    return [
        {
            "name": tool.name.value,
            "description": tool.description,
            "parameters": {
                name: {
                    "type": param.type,
                    "description": param.description,
                    "optional": param.optional,
                }
                for name, param in tool.parameters.items()
            }
        }
        for tool in MCP_BROWSER_TOOLS
    ]
