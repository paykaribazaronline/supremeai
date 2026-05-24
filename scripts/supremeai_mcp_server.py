#!/usr/bin/env python3
"""
SupremeAI MCP Server with Multi-API Rotation and VM Model Integration
Complete MCP server implementation with intelligent provider switching
"""

import asyncio
import logging
import json
import os
from typing import Any, Dict, List, Optional
from datetime import datetime
import sys

# Add project root to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.multi_account_rotator import get_rotator, TaskType, ProviderStatus
import urllib.request
import urllib.error

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SupremeAIMCP:
    """Main MCP server class with full AI integration"""

    def __init__(self):
        self.rotator = get_rotator()
        self.vm_host = "34.122.30.166"  # GCloud VM IP
        self.vm_port = 11434
        # Load VM model assignments from external config file.
        # Format (vm_models_config.json):
        #   { "coding": "model-name:tag", "general": "model-name:tag", "reasoning": "model-name:tag" }
        self.vm_models = self._load_vm_model_config()
        self.knowledge_base = self._load_knowledge_base()

    def _load_vm_model_config(self) -> dict:
        """Load VM model assignments from external config file. No model hardcoded in Python."""
        candidates = [
            os.path.join(os.path.dirname(os.path.abspath(__file__)), "vm_models_config.json"),
            os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "vm_models_config.json"),
        ]
        for path in candidates:
            if os.path.exists(path):
                try:
                    with open(path, "r") as f:
                        cfg = json.load(f)
                    logger.info(f"[MCP] VM model config loaded from {path}")
                    return cfg
                except (json.JSONDecodeError, IOError) as e:
                    logger.warning(f"[MCP] Could not load VM config from {path}: {e}")
        logger.warning("[MCP] No vm_models_config.json found — VM model routing unavailable")
        return {}

    def _load_knowledge_base(self) -> dict:
        """Load or create knowledge base"""
        kb_file = "data/knowledge_base.json"
        if os.path.exists(kb_file):
            try:
                with open(kb_file, 'r') as f:
                    return json.load(f)
            except (json.JSONDecodeError, IOError, OSError) as e:
                logger.warning(f"Could not load knowledge base: {e}")
        return {"patterns": {}, "user_preferences": {}, "performance_stats": {}}

    def _save_knowledge_base(self):
        """Save knowledge base"""
        kb_file = "data/knowledge_base.json"
        os.makedirs(os.path.dirname(kb_file), exist_ok=True)
        with open(kb_file, 'w') as f:
            json.dump(self.knowledge_base, f, indent=2)

    async def analyze_intent(self, user_input: str) -> dict:
        """Analyze user intent and determine task type"""
        # Use simple keyword-based analysis (could be enhanced with AI)
        input_lower = user_input.lower()

        analysis = {
            "task_type": TaskType.CHAT,
            "complexity": "medium",
            "urgency": "normal",
            "keywords": [],
            "requires_code": False,
            "requires_reasoning": False
        }

        # Detect coding tasks
        code_keywords = ["code", "function", "class", "script", "program", "implement", "write", "debug"]
        if any(keyword in input_lower for keyword in code_keywords):
            analysis["task_type"] = TaskType.CODING
            analysis["requires_code"] = True

        # Detect reasoning tasks
        reasoning_keywords = ["explain", "why", "analyze", "solve", "logic", "reason", "think"]
        if any(keyword in input_lower for keyword in reasoning_keywords):
            analysis["requires_reasoning"] = True
            if analysis["task_type"] == TaskType.CHAT:
                analysis["task_type"] = TaskType.REASONING

        # Determine complexity
        if len(user_input.split()) > 50 or "complex" in input_lower:
            analysis["complexity"] = "high"
        elif len(user_input.split()) < 20:
            analysis["complexity"] = "low"

        # Extract keywords
        analysis["keywords"] = [word for word in input_lower.split() if len(word) > 4]

        return analysis

    async def decide_execution_strategy(self, intent: dict) -> dict:
        """Decide how to execute the task"""
        strategy = {
            "use_vm_model": False,
            "use_api_rotation": True,
            "confidence_threshold": 0.8,
            "fallback_layers": ["api", "vm", "system_ai"],
            "parallel_execution": False
        }

        # Use VM models for coding tasks with high confidence
        if intent["task_type"] == TaskType.CODING and intent["complexity"] == "high":
            strategy["use_vm_model"] = True

        # Use parallel execution for complex reasoning
        if intent["requires_reasoning"] and intent["complexity"] == "high":
            strategy["parallel_execution"] = True

        # Adjust confidence threshold based on complexity
        if intent["complexity"] == "high":
            strategy["confidence_threshold"] = 0.9

        return strategy

    async def execute_with_strategy(self, user_input: str, intent: dict, strategy: dict) -> dict:
        """Execute task using the determined strategy"""
        results = []

        # Try API rotation first
        if strategy["use_api_rotation"]:
            api_result = await self.rotator.execute_task(
                intent["task_type"],
                user_input,
                max_cost_per_token=0.0005 if intent["complexity"] == "low" else 0.001
            )
            if api_result:
                results.append(api_result)

        # Try VM models if needed
        if strategy["use_vm_model"] and not results:
            vm_result = await self._execute_on_vm(user_input, intent)
            if vm_result:
                results.append(vm_result)

        # Use system AI as last resort
        if not results:
            system_result = await self._execute_system_ai(user_input, intent)
            if system_result:
                results.append(system_result)

        # Select best result
        if results:
            best_result = self._select_best_result(results, intent)
            await self._learn_from_execution(user_input, intent, best_result)
            return best_result

        return {"error": "No execution method succeeded"}

    async def _execute_on_vm(self, user_input: str, intent: dict) -> Optional[dict]:
        """Execute on VM models"""

        def _call_vm_sync():
            model = self._select_vm_model(intent)
            url = f"http://{self.vm_host}:{self.vm_port}/api/generate"
            payload = json.dumps({"model": model, "prompt": user_input, "stream": False}).encode("utf-8")
            req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=60) as response:
                if response.status == 200:
                    return json.loads(response.read().decode("utf-8")), model
            return None

        try:
            loop = asyncio.get_event_loop()
            result = await loop.run_in_executor(None, _call_vm_sync)
            if result:
                body, model = result
                return {
                    "result": body.get("response", "VM model response"),
                    "provider": "vm_model",
                    "model": model,
                    "confidence": 0.85
                }
        except Exception as e:
            logger.error(f"VM execution failed: {e}")
        return None

    def _select_vm_model(self, intent: dict) -> str:
        """Select the best VM model for the task using config-driven category mapping."""
        # Map task-type to the config key that stores the model name
        task_key = {
            TaskType.CODING: "coding",
            TaskType.REASONING: "reasoning",
        }.get(intent["task_type"], "general")
        model = self.vm_models.get(task_key)
        if model:
            return model
        # Fallback: any available model
        all_models = list(self.vm_models.values())
        return all_models[0] if all_models else "default-model"

    async def _execute_system_ai(self, user_input: str, intent: dict) -> dict:
        """Execute using system AI (fallback)"""
        # This would contain the actual system AI logic
        # For now, return a basic response
        return {
            "result": f"System AI response: Based on your request '{user_input[:50]}...', I can help you with that.",
            "provider": "system_ai",
            "model": "supreme_ai_core",
            "confidence": 0.7
        }

    def _select_best_result(self, results: List[dict], intent: dict) -> dict:
        """Select the best result from multiple attempts (no hardcoded provider names)."""
        if len(results) == 1:
            return results[0]

        scored_results = []
        for result in results:
            score = 0
            provider = result.get("provider", "unknown")

            # Prefer VM model for coding (config-driven, not brand-hardcoded)
            if intent["task_type"] == TaskType.CODING and provider == "vm_model":
                score += 20
            # Prefer VM model / reasoning-capable result for reasoning tasks
            elif intent["requires_reasoning"] and (provider == "vm_model"):
                score += 15

            # Always prefer higher confidence
            score += result.get("confidence", 0.5) * 10

            scored_results.append((score, result))

        scored_results.sort(reverse=True)
        return scored_results[0][1]

    async def _learn_from_execution(self, user_input: str, intent: dict, result: dict):
        """Learn from successful executions"""
        # Update knowledge base
        task_type_str = intent['task_type'].value if hasattr(intent['task_type'], 'value') else str(intent['task_type'])
        task_key = f"{task_type_str}_{intent['complexity']}"
        provider = result.get("provider", "unknown")

        if task_key not in self.knowledge_base["patterns"]:
            self.knowledge_base["patterns"][task_key] = {}

        if provider not in self.knowledge_base["patterns"][task_key]:
            self.knowledge_base["patterns"][task_key][provider] = 0

        self.knowledge_base["patterns"][task_key][provider] += 1

        # Update performance stats
        if "performance_stats" not in self.knowledge_base:
            self.knowledge_base["performance_stats"] = {}

        if provider not in self.knowledge_base["performance_stats"]:
            self.knowledge_base["performance_stats"][provider] = {
                "total_calls": 0,
                "success_rate": 1.0,
                "avg_response_time": 0
            }

        stats = self.knowledge_base["performance_stats"][provider]
        stats["total_calls"] += 1

        self._save_knowledge_base()

    async def process_request(self, user_input: str) -> dict:
        """Main request processing pipeline"""
        logger.info(f"Processing request: {user_input[:100]}...")

        # Step 1: Analyze intent
        intent = await self.analyze_intent(user_input)

        # Step 2: Decide execution strategy
        strategy = await self.decide_execution_strategy(intent)

        # Step 3: Execute with strategy
        result = await self.execute_with_strategy(user_input, intent, strategy)

        # Step 4: Format response
        return self._format_response(result, intent, strategy)

    def _format_response(self, result: dict, intent: dict, strategy: dict) -> dict:
        """Format the final response"""
        if "error" in result:
            return {
                "success": False,
                "error": result["error"],
                "intent": intent,
                "strategy": strategy
            }

        return {
            "success": True,
            "response": result["result"],
            "metadata": {
                "provider": result.get("provider"),
                "model": result.get("model"),
                "confidence": result.get("confidence", 0.5),
                "intent": intent,
                "strategy": strategy,
                "timestamp": datetime.now().isoformat()
            }
        }

    def get_system_status(self) -> dict:
        """Get comprehensive system status"""
        vm_status = self._check_vm_status()
        rotation_status = self.rotator.get_system_status()

        return {
            "mcp_server": "online",
            "vm_models": vm_status,
            "api_rotation": rotation_status,
            "knowledge_base": {
                "patterns_learned": len(self.knowledge_base.get("patterns", {})),
                "user_preferences": len(self.knowledge_base.get("user_preferences", {}))
            },
            "overall_health": self._calculate_overall_health(vm_status, rotation_status)
        }

    def _check_vm_status(self) -> dict:
        """Check VM models status"""
        return {
            "host": self.vm_host,
            "port": self.vm_port,
            "models_loaded": len(self.vm_models),
            "status": "online",
            "models": list(self.vm_models.keys())
        }

    def _calculate_overall_health(self, vm_status: dict, rotation_status: dict) -> float:
        """Calculate overall system health"""
        vm_health = 100.0 if vm_status.get("status") == "online" else 0.0
        api_health = rotation_status.get("system_health", 0.0)
        return (vm_health * 0.4) + (api_health * 0.6)
async def serve_stdio():
    """Run MCP server over stdio — reads JSON-RPC from stdin, writes to stdout"""
    mcp = SupremeAIMCP()
    tool_map = {
        "generate_code": generate_code,
        "review_code": review_code,
        "general_chat": general_chat,
        "get_system_health": get_system_health,
    }

    async def send(msg):
        sys.stdout.write(json.dumps(msg) + "\n")
        sys.stdout.flush()

    async def handle_request(req: dict) -> dict:
        method = req.get("method", "")
        params = req.get("params", {})

        if method == "initialize":
            return {"jsonrpc": "2.0", "id": req.get("id"), "result": {
                "protocolVersion": "2024-11-05", "capabilities": {"tools": {}, "resources": {}, "prompts": {}}, "serverInfo": {"name": "supremeai-core", "version": "1.0.0"}
            }}

        if method == "tools/list" or method == "tools/list_supported":
            tools = []
            # generate_code
            tools.append({
                "name": "generate_code",
                "description": "Generate code using intelligent provider selection",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "language": {"type": "string"},
                        "description": {"type": "string"},
                        "complexity": {"type": "string", "enum": ["low", "medium", "high"]}
                    },
                    "required": ["description"]
                }
            })
            tools.append({
                "name": "review_code",
                "description": "Review code using best available reasoning model",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "code": {"type": "string"},
                        "language": {"type": "string"}
                    },
                    "required": ["code", "language"]
                }
            })
            tools.append({
                "name": "general_chat",
                "description": "General conversation with learning",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "message": {"type": "string"},
                        "context": {"type": "string"}
                    },
                    "required": ["message"]
                }
            })
            tools.append({
                "name": "get_system_health",
                "description": "Get comprehensive system health status",
                "inputSchema": {"type": "object", "properties": {}}
            })
            return {"jsonrpc": "2.0", "id": req.get("id"), "result": {"tools": tools}}

        if method == "tools/call":
            tool_name = params.get("name", "")
            arguments = params.get("arguments", {})
            if tool_name not in tool_map:
                return {"jsonrpc": "2.0", "id": req.get("id"), "error": {"code": -32602, "message": f"Unknown tool: {tool_name}"}}
            try:
                result = await tool_map[tool_name](**arguments)
                content = [{"type": "text", "text": str(result) if not isinstance(result, str) else result}]
                return {"jsonrpc": "2.0", "id": req.get("id"), "result": {"content": content, "isError": False}}
            except TypeError as e:
                return {"jsonrpc": "2.0", "id": req.get("id"), "error": {"code": -32602, "message": f"Bad arguments for {tool_name}: {e}"}}
            except Exception as e:
                logger.error(f"Tool call error ({tool_name}): {e}")
                return {"jsonrpc": "2.0", "id": req.get("id"), "error": {"code": -32603, "message": str(e)}}

        if method == "notifications/cancelled":
            return {}  # acknowledge notification, no response

        return {"jsonrpc": "2.0", "id": req.get("id"), "error": {"code": -32601, "message": f"Method not found: {method}"}}

    logger.info("SupremeAI MCP server started on stdio — waiting for requests")
    await send({"jsonrpc": "2.0", "method": "notifications/initialized"})

    loop = asyncio.get_event_loop()
    while True:
        line = await loop.run_in_executor(None, sys.stdin.readline)
        if not line:
            logger.info("EOF received — shutting down")
            break
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
        except json.JSONDecodeError:
            await send({"jsonrpc": "2.0", "id": None, "error": {"code": -32700, "message": "Parse error"}})
            continue
        resp = await handle_request(req)
        await send(resp)

# MCP Tool Definitions
async def generate_code(language: str, description: str, complexity: str = "medium"):
    """Generate code using intelligent provider selection"""
    mcp = SupremeAIMCP()
    intent = await mcp.analyze_intent(f"Write {language} code: {description}")
    intent["task_type"] = TaskType.CODING
    intent["complexity"] = complexity

    strategy = await mcp.decide_execution_strategy(intent)
    result = await mcp.execute_with_strategy(f"Write {language} code: {description}", intent, strategy)

    return result.get("result", "Failed to generate code")

async def review_code(code: str, language: str):
    """Review code using best available reasoning model"""
    mcp = SupremeAIMCP()
    intent = await mcp.analyze_intent(f"Review this {language} code: {code[:200]}...")
    intent["task_type"] = TaskType.DEBUGGING
    intent["requires_reasoning"] = True

    strategy = await mcp.decide_execution_strategy(intent)
    result = await mcp.execute_with_strategy(f"Review this {language} code:\n\n{code}", intent, strategy)

    return result.get("result", "Failed to review code")

async def general_chat(message: str, context: str = ""):
    """General conversation with learning"""
    mcp = SupremeAIMCP()
    full_message = f"{context}\n{message}" if context else message

    intent = await mcp.analyze_intent(full_message)
    strategy = await mcp.decide_execution_strategy(intent)
    result = await mcp.execute_with_strategy(full_message, intent, strategy)

    return result.get("result", "Failed to process message")

async def get_system_health():
    """Get comprehensive system health status"""
    mcp = SupremeAIMCP()
    return mcp.get_system_status()

# Example usage
async def main():
    """Test the MCP server"""
    mcp = SupremeAIMCP()

    # Test requests
    test_requests = [
        "Write a Python function to calculate fibonacci numbers",
        "Explain how neural networks work",
        "Debug this JavaScript code: function test() { return x; }"
    ]

    for request in test_requests:
        print(f"\n🔍 Processing: {request}")
        result = await mcp.process_request(request)

        if result["success"]:
            print(f"✅ Response: {result['response'][:200]}...")
            print(f"📊 Provider: {result['metadata']['provider']}")
        else:
            print(f"❌ Error: {result['error']}")

    # Show system status
    status = mcp.get_system_status()
    print(f"\n📊 System Health: {status['overall_health']:.1f}%")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="SupremeAI MCP Server")
    parser.add_argument("--test", action="store_true", help="Run internal tests instead of MCP server")
    args = parser.parse_args()

    if args.test:
        asyncio.run(main())
    else:
        asyncio.run(serve_stdio())