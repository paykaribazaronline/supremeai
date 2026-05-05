#!/usr/bin/env python3
"""
SupremeAI Full System Integration Test
Tests the complete multi-API rotation with VM models and MCP server
"""

import asyncio
import logging
import time
import sys
import os
import json
from datetime import datetime

# Add project root to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scripts.supremeai_mcp_server import SupremeAIMCP
from scripts.multi_account_rotator import MultiAccountRotator, TaskType

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SystemIntegrationTest:
    """Comprehensive system integration test"""

    def __init__(self):
        self.mcp = SupremeAIMCP()
        self.rotator = self.mcp.rotator
        self.test_results = []

    async def setup_test_accounts(self):
        """Setup test accounts for testing"""
        logger.info("Setting up test accounts...")

        # Add test accounts for different providers
        test_accounts = [
            ("groq", "test1@supremeai.com", "gsk_test_key_1"),
            ("groq", "test2@supremeai.com", "gsk_test_key_2"),
            ("deepseek", "test3@supremeai.com", "ds_test_key_1"),
            ("google_ai_studio", "test4@supremeai.com", "ga_test_key_1"),
        ]

        for provider, email, api_key in test_accounts:
            try:
                self.rotator.add_account(str(provider), str(email), str(api_key))
                logger.info(f"Added test account: {provider} - {email}")
            except Exception as e:
                logger.error(f"Failed to add account {provider} - {email}: {e}")

    async def test_api_rotation(self):
        """Test multi-API rotation functionality"""
        logger.info("Testing API rotation...")

        test_cases = [
            ("Write a Python function to reverse a string", TaskType.CODING, "low"),
            ("Explain quantum computing simply", TaskType.REASONING, "medium"),
            ("What is the capital of France?", TaskType.CHAT, "low"),
            ("Debug this code: def test(): return x", TaskType.DEBUGGING, "low"),
        ]

        for prompt, task_type, complexity in test_cases:
            task_type_str = task_type.value if hasattr(task_type, 'value') else str(task_type)
            logger.info(f"Testing: {task_type_str} - {prompt[:50]}...")

            start_time = time.time()
            result = await self.rotator.execute_task(task_type, prompt)
            end_time = time.time()

            test_result = {
                "test_type": "api_rotation",
                "task_type": task_type_str,
                "complexity": complexity,
                "prompt": prompt,
                "success": result is not None,
                "response_time": end_time - start_time,
                "provider": result.get("provider") if result else None,
                "account": result.get("account") if result else None,
                "timestamp": datetime.now().isoformat()
            }

            self.test_results.append(test_result)

            if result:
                logger.info(f"✅ Success: {result['provider']} in {test_result['response_time']:.2f}s")
            else:
                logger.error(f"❌ Failed: No provider available")

    async def test_mcp_pipeline(self):
        """Test full MCP server pipeline"""
        logger.info("Testing MCP pipeline...")

        test_inputs = [
            "Write a JavaScript function to validate email addresses",
            "Explain the difference between supervised and unsupervised learning",
            "Help me debug this Python error: NameError: name 'x' is not defined",
            "Create a simple REST API in Node.js",
        ]

        for user_input in test_inputs:
            logger.info(f"Testing MCP: {user_input[:50]}...")

            start_time = time.time()
            result = await self.mcp.process_request(user_input)
            end_time = time.time()

            test_result = {
                "test_type": "mcp_pipeline",
                "input": user_input,
                "success": result.get("success", False),
                "response_time": end_time - start_time,
                "provider": result.get("metadata", {}).get("provider"),
                "model": result.get("metadata", {}).get("model"),
                "confidence": result.get("metadata", {}).get("confidence"),
                "intent": result.get("metadata", {}).get("intent"),
                "strategy": result.get("metadata", {}).get("strategy"),
                "error": result.get("error") if not result.get("success") else None,
                "timestamp": datetime.now().isoformat()
            }

            self.test_results.append(test_result)

            if result.get("success"):
                logger.info(f"✅ MCP Success: {result['metadata']['provider']} ({test_result['response_time']:.2f}s)")
            else:
                logger.error(f"❌ MCP Failed: {result.get('error', 'Unknown error')}")

    async def test_failover_scenarios(self):
        """Test failover scenarios"""
        logger.info("Testing failover scenarios...")

        # Simulate account failures
        original_status = {}
        for provider in self.rotator.providers.values():
            for account in provider.accounts:
                original_status[account.id] = account.status
                # Temporarily disable first account of each provider
                if account == provider.accounts[0]:
                    account.status = account.status.FAILED

        logger.info("Disabled first account of each provider for failover test")

        # Test with "failed" accounts
        test_prompt = "Write a simple hello world function in Python"
        result = await self.rotator.execute_task(TaskType.CODING, test_prompt)

        test_result = {
            "test_type": "failover_test",
            "prompt": test_prompt,
            "success": result is not None,
            "provider": result.get("provider") if result else None,
            "account": result.get("account") if result else None,
            "failover_success": result is not None,  # Should use backup account
            "timestamp": datetime.now().isoformat()
        }

        self.test_results.append(test_result)

        # Restore original status
        for provider in self.rotator.providers.values():
            for account in provider.accounts:
                account.status = original_status[account.id]

        logger.info("Restored account statuses")

    async def test_system_health(self):
        """Test system health monitoring"""
        logger.info("Testing system health monitoring...")

        # Get system status
        mcp_status = self.mcp.get_system_status()
        rotator_status = self.rotator.get_system_status()

        # Handle potential errors in system status
        try:
            overall_health = mcp_status.get("overall_health", 0)
            api_health = rotator_status.get("system_health", 0)
        except Exception as e:
            logger.error(f"Error getting system health: {e}")
            overall_health = 0
            api_health = 0

        test_result = {
            "test_type": "system_health",
            "mcp_status": mcp_status,
            "rotator_status": rotator_status,
            "overall_health": overall_health,
            "api_health": api_health,
            "vm_status": mcp_status.get("vm_models", {}).get("status") if isinstance(mcp_status, dict) else "unknown",
            "timestamp": datetime.now().isoformat()
        }

        self.test_results.append(test_result)

        logger.info(f"System Health: {test_result['overall_health']:.1f}%")
        logger.info(f"API Health: {test_result['api_health']:.1f}%")
        logger.info(f"VM Status: {test_result['vm_status']}")

    async def test_learning_system(self):
        """Test the learning system"""
        logger.info("Testing learning system...")

        # Execute same task multiple times to test learning
        test_prompt = "Explain recursion in programming"
        initial_patterns = len(self.mcp.knowledge_base.get("patterns", {}))

        for i in range(3):
            result = await self.mcp.process_request(test_prompt)
            if result.get("success"):
                logger.info(f"Learning iteration {i+1}: {result['metadata']['provider']}")

        final_patterns = len(self.mcp.knowledge_base.get("patterns", {}))
        patterns_learned = final_patterns - initial_patterns

        test_result = {
            "test_type": "learning_system",
            "prompt": test_prompt,
            "iterations": 3,
            "patterns_learned": patterns_learned,
            "initial_patterns": initial_patterns,
            "final_patterns": final_patterns,
            "timestamp": datetime.now().isoformat()
        }

        self.test_results.append(test_result)

        logger.info(f"Patterns learned: {patterns_learned}")

    def generate_report(self):
        """Generate comprehensive test report"""
        report = {
            "test_summary": {
                "total_tests": len(self.test_results),
                "passed_tests": len([t for t in self.test_results if t.get("success", False)]),
                "failed_tests": len([t for t in self.test_results if not t.get("success", False)]),
                "success_rate": 0,
                "timestamp": datetime.now().isoformat()
            },
            "test_results": self.test_results,
            "system_status": self.mcp.get_system_status(),
            "recommendations": []
        }

        if report["test_summary"]["total_tests"] > 0:
            report["test_summary"]["success_rate"] = (
                report["test_summary"]["passed_tests"] / report["test_summary"]["total_tests"] * 100
            )

        # Generate recommendations
        system_health = report["system_status"].get("overall_health", 0)
        if system_health < 70:
            report["recommendations"].append("System health is below 70%. Check VM connectivity and API accounts.")

        api_health = report["system_status"].get("api_rotation", {}).get("system_health", 0)
        if api_health < 80:
            report["recommendations"].append("API rotation health is below 80%. Add more accounts or check account status.")

        vm_status = report["system_status"].get("vm_models", {}).get("status")
        if vm_status != "online":
            report["recommendations"].append("VM models are not online. Check GCloud VM connectivity.")

        # Save report
        os.makedirs("test_reports", exist_ok=True)
        report_file = f"test_reports/integration_test_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

        with open(report_file, 'w') as f:
            json.dump(report, f, indent=2, default=str)

        logger.info(f"Test report saved to: {report_file}")
        return report

    async def run_all_tests(self):
        """Run all integration tests"""
        logger.info("🚀 Starting SupremeAI Full System Integration Test")
        logger.info("=" * 60)

        try:
            # Setup
            await self.setup_test_accounts()

            # Core functionality tests
            await self.test_api_rotation()
            await self.test_mcp_pipeline()

            # Advanced tests
            await self.test_failover_scenarios()
            await self.test_system_health()
            await self.test_learning_system()

            # Generate report
            report = self.generate_report()

            logger.info("=" * 60)
            logger.info("🏁 Integration Test Complete")
            logger.info(f"Success Rate: {report['test_summary']['success_rate']:.1f}%")
            logger.info(f"Passed: {report['test_summary']['passed_tests']}")
            logger.info(f"Failed: {report['test_summary']['failed_tests']}")

            if report["recommendations"]:
                logger.info("📋 Recommendations:")
                for rec in report["recommendations"]:
                    logger.info(f"  • {rec}")

            return report

        except Exception as e:
            logger.error(f"Integration test failed: {e}")
            return None

async def main():
    """Run integration tests"""
    tester = SystemIntegrationTest()
    report = await tester.run_all_tests()

    if report and report["test_summary"]["success_rate"] >= 80:
        logger.info("🎉 SupremeAI Plugin is PERFECTLY WORKING!")
        return True
    else:
        logger.error("❌ SupremeAI Plugin needs fixes")
        return False

if __name__ == "__main__":
    success = asyncio.run(main())
    sys.exit(0 if success else 1)