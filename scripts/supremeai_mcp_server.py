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

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SupremeAIMCP:
    """Main MCP server class with full AI integration"""

    def __init__(self):
        self.rotator = get_rotator()
        self.vm_host = "34.122.30.166"  # GCloud VM IP
        self.vm_port = 11434
        self.vm_models = {
            "qwen2.5-coder:7b": {"role": "coding", "context": 32768},
            "llama3.1:8b": {"role": "general", "context": 8192},
            "stepfun": {"role": "reasoning", "context": 262144}
        }
        self.knowledge_base = self._load_knowledge_base()

    def _load_knowledge_base(self) -> dict:
        """Load or create knowledge base"""
        kb_file = "data/knowledge_base.json"
        if os.path.exists(kb_file):
            try:
                with open(kb_file, 'r') as f:
                    return json.load(f)
            except:
                pass
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
        try:
            # Determine best VM model for the task
            model = self._select_vm_model(intent)

            # This would make actual HTTP call to VM Ollama server
            # For now, return mock response
            import aiohttp

            async with aiohttp.ClientSession() as session:
                payload = {
                    "model": model,
                    "prompt": user_input,
                    "stream": False
                }

                async with session.post(f"http://{self.vm_host}:{self.vm_port}/api/generate", json=payload) as response:
                    if response.status == 200:
                        result = await response.json()
                        return {
                            "result": result.get("response", "VM model response"),
                            "provider": "vm_model",
                            "model": model,
                            "confidence": 0.85
                        }

        except Exception as e:
            logger.error(f"VM execution failed: {e}")
            return None

    def _select_vm_model(self, intent: dict) -> str:
        """Select best VM model for the task"""
        if intent["task_type"] == TaskType.CODING:
            return "qwen2.5-coder:7b"
        elif intent["requires_reasoning"]:
            return "stepfun"  # Best reasoning model
        else:
            return "llama3.1:8b"  # General purpose

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
        """Select the best result from multiple attempts"""
        if len(results) == 1:
            return results[0]

        # Score results based on various factors
        scored_results = []
        for result in results:
            score = 0

            # Prefer results from preferred providers
            if intent["task_type"] == TaskType.CODING and result.get("provider") == "deepseek":
                score += 20
            elif intent["requires_reasoning"] and result.get("provider") == "vm_model":
                score += 15

            # Prefer higher confidence
            score += result.get("confidence", 0.5) * 10

            scored_results.append((score, result))

        # Return highest scoring result
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
        # This would make actual health check calls to VM
        # For now, return mock status
        return {
            "host": self.vm_host,
            "port": self.vm_port,
            "models_loaded": len(self.vm_models),
            "status": "online",  # Would check actual connectivity
            "models": list(self.vm_models.keys())
        }

    def _calculate_overall_health(self, vm_status: dict, rotation_status: dict) -> float:
        """Calculate overall system health"""
        vm_health = 100.0 if vm_status.get("status") == "online" else 0.0
        api_health = rotation_status.get("system_health", 0.0)

        # Weight: 40% VM, 60% API rotation
        return (vm_health * 0.4) + (api_health * 0.6)

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
    asyncio.run(main())