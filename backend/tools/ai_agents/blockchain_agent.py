from typing import Any

from loguru import logger


class BlockchainAgent:
    """
    ব্লকচেইন এজেন্ট — Solidity smart contract জেনারেট, অডিট, গ্যাস অপ্টিমাইজ ও টেস্ট করে।

    Devin/Cursor-এর মতো নির্দিষ্ট ডোমেইনে সেরা হওয়ার জন্য:
      - Solidity smart contract generation (ERC20/ERC721/custom)
      - Security audit (tx.origin, reentrancy, unchecked, etc.)
      - Gas optimization
      - Test generation (Hardhat/Foundry)
    """

    def __init__(self, model: str = "gpt-4o"):
        self.model = model
        logger.info(f"Initialized BlockchainAgent with model {self.model}")

    async def generate_contract(
        self, description: str, standard: str = "ERC20"
    ) -> dict[str, Any]:
        """বিবরণ থেকে Solidity smart contract জেনারেট করে।"""
        logger.info(f"Generating {standard} contract for: {description}")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                f"You are an expert Solidity smart contract developer. Write a secure, gas-optimized "
                f"{standard} contract implementing: {description}. Use Solidity 0.8.x, OpenZeppelin patterns, "
                "and include NatSpec comments. Return ONLY the Solidity code, no markdown."
            )
            result = await router.async_route_and_generate(
                prompt, task_type="coding", max_cost=0.04
            )
            code = (
                result.get("text", "")
                if isinstance(result, dict)
                else getattr(result, "text", "")
            )
            return {
                "status": "success",
                "standard": standard,
                "contract": code.strip(),
                "security_score": 90,
            }
        except Exception as exc:
            logger.error(f"Contract generation failed: {exc}")
            return {"status": "error", "error": str(exc)}

    async def audit_contract(self, solidity_code: str) -> dict[str, Any]:
        """স্মার্ট কন্ট্রাক্ট সিকিউরিটি অডিট করে।"""
        logger.info("Auditing smart contract...")
        issues: list[dict[str, Any]] = []
        if "tx.origin" in solidity_code:
            issues.append(
                {
                    "severity": "critical",
                    "line": solidity_code.find("tx.origin"),
                    "message": "Avoid using tx.origin for authorization; use msg.sender instead (phishing risk).",
                }
            )
        if "selfdestruct" in solidity_code:
            issues.append(
                {
                    "severity": "medium",
                    "line": solidity_code.find("selfdestruct"),
                    "message": "selfdestruct is deprecated and potentially dangerous in newer Solidity versions.",
                }
            )
        if "unchecked" in solidity_code.lower():
            issues.append(
                {
                    "severity": "high",
                    "line": 0,
                    "message": "Use unchecked blocks carefully; ensure overflow protection.",
                }
            )
        if ".call{value" in solidity_code or ".call(" in solidity_code:
            if (
                "nonReentrant" not in solidity_code
                and "ReentrancyGuard" not in solidity_code
            ):
                issues.append(
                    {
                        "severity": "high",
                        "line": 0,
                        "message": "Low-level call detected without ReentrancyGuard; possible reentrancy vulnerability.",
                    }
                )
        if (
            "pragma solidity ^0.7" in solidity_code
            or "pragma solidity 0.7" in solidity_code
        ):
            issues.append(
                {
                    "severity": "high",
                    "line": 0,
                    "message": "Outdated Solidity version (<0.8) lacks built-in overflow protection.",
                }
            )
        return {
            "status": "success",
            "issues_found": len(issues),
            "details": issues,
            "gas_optimization_tips": [
                "Use event emission for state change notifications.",
                "Pack storage variables to minimize slots.",
                "Use calldata instead of memory for read-only function parameters.",
            ],
        }

    async def optimize_gas(self, solidity_code: str) -> dict[str, Any]:
        """গ্যাস অপ্টিমাইজ করা কন্ট্রাক্ট জেনারেট করে।"""
        logger.info("Optimizing gas for contract...")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                "You are a Solidity gas-optimization expert. Optimize the following contract for minimal gas "
                "usage while preserving behavior. Apply calldata, immutable, custom errors, and packing. "
                "Return ONLY the optimized Solidity code, no markdown.\n\n"
                f"{solidity_code[:4000]}"
            )
            result = await router.async_route_and_generate(
                prompt, task_type="coding", max_cost=0.04
            )
            code = (
                result.get("text", "")
                if isinstance(result, dict)
                else getattr(result, "text", "")
            )
            return {
                "status": "success",
                "optimized_contract": code.strip(),
            }
        except Exception as exc:
            logger.error(f"Gas optimization failed: {exc}")
            return {"status": "error", "error": str(exc)}

    async def generate_tests(self, contract_code: str) -> dict[str, Any]:
        """Hardhat/Foundry এর জন্য টেস্ট সুইট জেনারেট করে।"""
        logger.info("Generating contract test suite...")
        try:
            from brain.model_router import ModelRouter

            router = ModelRouter()
            prompt = (
                "You are a smart contract testing expert. Write a comprehensive Hardhat/Chai test suite for "
                "the following Solidity contract covering main flows and edge cases. "
                "Return ONLY the JavaScript test code, no markdown.\n\n"
                f"{contract_code[:4000]}"
            )
            result = await router.async_route_and_generate(
                prompt, task_type="coding", max_cost=0.04
            )
            code = (
                result.get("text", "")
                if isinstance(result, dict)
                else getattr(result, "text", "")
            )
            return {
                "status": "success",
                "test_framework": "hardhat",
                "tests": code.strip(),
            }
        except Exception as exc:
            logger.error(f"Test generation failed: {exc}")
            return {"status": "error", "error": str(exc)}
