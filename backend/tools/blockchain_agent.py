from typing import Any

from loguru import logger


class BlockchainAgent:
    async def generate_contract(
        self, description: str, language: str = "solidity"
    ) -> dict[str, Any]:
        logger.info(f"Generating {language} contract for: {description}")
        contract = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract GeneratedContract {
    address public owner;
    bool public initialized;

    constructor() {
        owner = msg.sender;
        initialized = true;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized");
        _;
    }

    function updateOwner(address newOwner) external onlyOwner {
        require(newOwner != address(0), "Invalid address");
        owner = newOwner;
    }
}
"""
        return {
            "status": "success",
            "language": language,
            "contract": contract.strip(),
            "security_score": 85,
        }

    async def audit_contract(self, source_code: str) -> dict[str, Any]:
        logger.info("Auditing smart contract...")
        issues: list[dict[str, Any]] = []
        if "tx.origin" in source_code:
            issues.append(
                {
                    "severity": "critical",
                    "line": source_code.index("tx.origin"),
                    "message": "Avoid using tx.origin for authorization; use msg.sender instead.",
                }
            )
        if "selfdestruct" in source_code:
            issues.append(
                {
                    "severity": "medium",
                    "line": source_code.index("selfdestruct"),
                    "message": "selfdestruct is deprecated and potentially dangerous in newer Solidity versions.",
                }
            )
        if "unchecked" in source_code.lower():
            issues.append(
                {
                    "severity": "high",
                    "line": 0,
                    "message": "Use unchecked blocks carefully; ensure overflow protection.",
                }
            )
        return {
            "status": "success",
            "issues_found": len(issues),
            "details": issues,
            "gas_optimization_tips": [
                "Use event emission for state change notifications.",
                "Pack storage variables to minimize slots.",
            ],
        }
