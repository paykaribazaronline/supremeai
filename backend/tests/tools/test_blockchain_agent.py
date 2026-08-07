# বাংলা মন্তব্য: Blockchain Agent-এর Solidity contract জেনারেশন ও audit ফাংজনালিটি টেস্ট।

from unittest.mock import AsyncMock, patch

import pytest

from tools.ai_agents.blockchain_agent import BlockchainAgent


@pytest.fixture
def mock_blockchain():
    yield
    return


@pytest.mark.anyio
async def test_generate_contract(mock_blockchain):
    # বাংলা মন্তব্য: Solidity smart contract জেনারেশন টেস্ট
    agent = BlockchainAgent()

    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_router:
        mock_router.return_value = {
            "text": """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract MyToken {
    string public name = "MyToken";
    string public symbol = "MTK";
    uint8 public decimals = 18;
    uint256 public totalSupply = 1000000 * 10 ** 18;

    mapping(address => uint256) public balanceOf;

    constructor() {
        balanceOf[msg.sender] = totalSupply;
    }

    function transfer(address to, uint256 amount) public returns (bool) {
        require(balanceOf[msg.sender] >= amount);
        balanceOf[msg.sender] -= amount;
        balanceOf[to] += amount;
        return true;
    }
}
"""
        }

        result = await agent.generate_contract(description="Create an ERC-20 token contract", standard="ERC20")

    assert result is not None
    assert "MyToken" in result.get("contract")
    # বাংলা মন্তব্য: result-টি ডিকশনারি হওয়ায় ডট নোটিফিকেশনের বদলে get() ব্যবহার করা হলো এবং 'erc20' অ্যাসার্ট করা হলো।
    assert "erc20" in result.get("standard").lower()


@pytest.mark.anyio
async def test_audit_contract(mock_blockchain):
    # বাংলা মন্তব্য: Smart contract security audit টেস্ট
    agent = BlockchainAgent()

    solidity_code = """
contract VulnerableToken {
    mapping(address => uint256) public balanceOf;

    function transfer(address to, uint256 amount) public {
        require(msg.sender == tx.origin);
        balanceOf[msg.sender] -= amount;
        balanceOf[to] += amount;
    }
}
"""

    with patch("brain.model_router.ModelRouter.async_route_and_generate") as mock_router:
        mock_router.return_value.async_route_and_generate = AsyncMock(
            return_value={
                "text": """
Security Issues Found:
1. Missing require statement for balance check - reentrancy risk
2. No overflow protection - use SafeMath
3. Missing events for transfer
"""
            }
        )

        result = await agent.audit_contract(solidity_code)

    assert result is not None
    assert "issues_found" in result
    assert len(result["details"]) > 0


@pytest.mark.anyio
async def test_optimize_gas(mock_blockchain):
    # বাংলা মন্তব্য: Gas optimization suggestions টেস্ট
    agent = BlockchainAgent()

    solidity_code = """
function expensiveLoop(uint256 n) public pure returns (uint256) {
    uint256 sum = 0;
    for (uint256 i = 0; i < n; i++) {
        sum += i;
    }
    return sum;
}
"""

    # বাংলা মন্তব্য: new_callable=AsyncMock ব্যবহার করে ডাইরেক্টলি ডিকশনারি রিটার্ন সেট করা হলো
    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_router:
        mock_router.return_value = {
            "text": """
// Optimized version
function optimizedLoop(uint256 n) public pure returns (uint256) {
    return n * (n - 1) / 2;
}
"""
        }

        result = await agent.optimize_gas(solidity_code)

    assert result is not None
    assert "optimized" in result.get("optimized_contract").lower()


@pytest.mark.anyio
async def test_generate_tests(mock_blockchain):
    # বাংলা মন্তব্য: Hardhat/Foundry test suite জেনারেশন টেস্ট
    agent = BlockchainAgent()

    contract_code = """
contract SimpleStorage {
    uint256 public value;

    function set(uint256 _value) public {
        value = _value;
    }
}
"""

    # বাংলা মন্তব্য: new_callable=AsyncMock ব্যবহার করে ডাইরেক্টলি ডিকশনারি রিটার্ন সেট করা হলো
    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_router:
        mock_router.return_value = {
            "text": """
const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("SimpleStorage", function () {
    it("Should set the value correctly", async function () {
        const SimpleStorage = await ethers.getContractFactory("SimpleStorage");
        const storage = await SimpleStorage.deploy();
        await storage.set(42);
        expect(await storage.value()).to.equal(42);
    });
});
"""
        }

        result = await agent.generate_tests(contract_code)

    assert result is not None
    assert "describe" in result.get("tests")
    assert "SimpleStorage" in result.get("tests")


@pytest.mark.anyio
async def test_erc721_nft_contract(mock_blockchain):
    # বাংলা মন্তব্য: ERC-721 NFT contract জেনারেশন টেস্ট
    agent = BlockchainAgent()

    # বাংলা মন্তব্য: new_callable=AsyncMock ব্যবহার করে ডাইরেক্টলি ডিকশনারি রিটার্ন সেট করা হলো
    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_router:
        mock_router.return_value = {
            "text": """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";

contract MyNFT is ERC721 {
    uint256 public tokenCounter;

    constructor() ERC721("MyNFT", "MNFT") {
        tokenCounter = 0;
    }

    function mintNFT(address recipient) public returns (uint256) {
        uint256 tokenId = tokenCounter;
        _safeMint(recipient, tokenId);
        tokenCounter += 1;
        return tokenId;
    }
}
"""
        }

        result = await agent.generate_contract(description="Create an ERC-721 NFT contract", standard="ERC721")

    assert result is not None
    assert "ERC721" in result.get("contract")
