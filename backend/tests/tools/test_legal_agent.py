# বাংলা মন্তব্য: Legal Agent-এর contract/terms generation ফাংজনালিটি টেস্ট।

from unittest.mock import AsyncMock, patch

import pytest

from tools.ai_agents.legal_agent import LegalAgent


@pytest.fixture
def mock_legal():
    yield


@pytest.mark.anyio
@pytest.mark.anyio
async def test_generate_contract(mock_legal):
    # বাংলা মন্তব্য: চুক্তি (Contract) তৈরি টেস্ট
    agent = LegalAgent()

    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_acompletion:
        mock_acompletion.return_value = {
            "success": True,
            "text": """
# NON-DISCLOSURE AGREEMENT

This Non-Disclosure Agreement ("Agreement") is made between:
- Party A: [Company Name]
- Party B: [Recipient Name]

## Terms:
1. Confidential Information shall not be disclosed
2. Term: 2 years
3. Governed by laws of Bangladesh

IN WITNESS WHEREOF, the parties have executed this Agreement.
""",
        }

        result = await agent.generate_contract(
            contract_type="NDA",
            parties=["Company A", "Company B"],
            terms={"duration": "2 years", "jurisdiction": "Bangladesh"},
        )

    assert result is not None
    assert "NON-DISCLOSURE" in result["document"]
    assert "Non-Disclosure" in result["document"]


@pytest.mark.anyio
@pytest.mark.anyio
async def test_analyze_clause(mock_legal):
    # বাংলা মন্তব্য: ক্লজ (Clause) বিশ্লেষণ টেস্ট
    agent = LegalAgent()

    clause_text = """
The party shall not disclose any confidential information for a period of 5 years.
This includes trade secrets, business plans, and technical data.
"""

    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_acompletion:
        mock_acompletion.return_value = {
            "success": True,
            "text": """
Clause Analysis:
- Type: Confidentiality Clause
- Duration: 5 years (standard)
- Risk Level: Low
- Recommendations: Consider adding specific definitions for "confidential information"
""",
        }

        result = await agent.analyze_clause(clause_text, jurisdiction="BD")

    assert result is not None
    assert "risk_count" in result
    assert "risks" in result


@pytest.mark.anyio
@pytest.mark.anyio
async def test_check_compliance(mock_legal):
    # বাংলা মন্তব্য: Compliance check টেস্ট
    agent = LegalAgent()

    document = """
Privacy Policy:
- We collect personal data
- Data is used for service improvement
- No third-party sharing without consent
"""

    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_acompletion:
        mock_acompletion.return_value = {
            "success": True,
            "text": """
Compliance Report:
- GDPR: Compliant (data collection disclosed)
- CCPA: Compliant (opt-out rights mentioned)
- Issues: None found
""",
        }

        result = await agent.check_compliance(document, regulation="GDPR")

    assert result is not None
    assert "compliant" in result
    assert result["compliant"] is True


@pytest.mark.anyio
@pytest.mark.anyio
async def test_generate_tos(mock_legal):
    # বাংলা মন্তব্য: Terms of Service জেনারেশন টেস্ট
    agent = LegalAgent()

    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_acompletion:
        mock_acompletion.return_value = {
            "success": True,
            "text": """
# TERMS OF SERVICE

Last Updated: [Date]

1. ACCEPTANCE OF TERMS
By accessing our service, you agree to these terms.

2. USER RESPONSIBILITIES
Users must provide accurate information and comply with applicable laws.

3. INTELLECTUAL PROPERTY
All content is owned by the company.

4. GOVERNING LAW
These terms are governed by the laws of Bangladesh.
""",
        }

        result = await agent.generate_tos(
            product_description="AI-powered code generation platform", jurisdiction="BD"
        )

    assert result is not None
    assert "TERMS OF SERVICE" in result["document"]


@pytest.mark.anyio
@pytest.mark.anyio
async def test_generate_privacy_policy(mock_legal):
    # বাংলা মন্তব্য: Privacy Policy জেনারেশন টেস্ট
    agent = LegalAgent()

    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_acompletion:
        mock_acompletion.return_value = {
            "success": True,
            "text": """
# PRIVACY POLICY

We respect your privacy. This policy explains:
- What data we collect
- How we use it
- Your rights
- Data retention period

Contact: privacy@company.com
""",
        }

        result = await agent.generate_tos(
            product_description="Web application that collects user data",
            jurisdiction="BD",
        )

    assert result is not None
    assert "PRIVACY" in result["document"] or "privacy" in result["document"].lower()


@pytest.mark.anyio
@pytest.mark.anyio
async def test_legal_document_with_bangladesh_law(mock_legal):
    # বাংলা মন্তব্য: বাংলাদেশের আইন অনুযায়ী ডকুমেন্ট জেনারেশন টেস্ট
    agent = LegalAgent()

    with patch(
        "brain.model_router.ModelRouter.async_route_and_generate",
        new_callable=AsyncMock,
    ) as mock_acompletion:
        mock_acompletion.return_value = {
            "success": True,
            "text": """
# EMPLOYMENT AGREEMENT

This agreement is governed by Bangladesh Labour Act, 2006.

Terms:
- Salary: As per company policy
- Working hours: 8 hours per day
- Leave: As per Bangladesh law
- Termination: 30 days notice
""",
        }

        result = await agent.generate_contract(
            contract_type="Employment",
            parties=["Employer", "Employee"],
            terms={"jurisdiction": "Bangladesh"},
        )

    assert result is not None
    assert "Bangladesh" in result["document"]
