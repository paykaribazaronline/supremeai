import sys
from unittest.mock import patch


sys.path.append("..")
from agents.legal_agent import LegalAgent


class TestLegalAgent:
    def test_init(self):
        agent = LegalAgent()
        assert agent is not None

    def test_analyze_contract(self):
        agent = LegalAgent()
        doc = "This agreement has unlimited liability for all parties involved."
        result = agent.analyze(doc, doc_type="contract")
        assert "doc_type" in result
        assert result["doc_type"] == "contract"
        assert "risks" in result
        assert "disclaimer" in result
        assert "legal advice" in result["disclaimer"].lower()

    def test_analyze_nda(self):
        agent = LegalAgent()
        doc = "The parties agree to indemnify each other for any breaches."
        result = agent.analyze(doc, doc_type="nda")
        assert result["doc_type"] == "nda"
        assert len(result["risks"]) > 0

    def test_review_nda(self):
        agent = LegalAgent()
        with patch.object(agent, "analyze", return_value={"doc_type": "nda", "risks": [], "disclaimer": "test"}):
            result = agent.review_nda("NDA text here")
            assert result["doc_type"] == "nda"

    def test_review_contract(self):
        agent = LegalAgent()
        with patch.object(agent, "analyze", return_value={"doc_type": "contract", "risks": [], "disclaimer": "test"}):
            result = agent.review_contract("Contract text here")
            assert result["doc_type"] == "contract"

    def test_risk_score(self):
        agent = LegalAgent()
        risks = [{"severity": "high"}, {"severity": "medium"}]
        score = agent._risk_score(risks)
        assert 0.0 <= score <= 1.0

    def test_risk_score_empty(self):
        agent = LegalAgent()
        score = agent._risk_score([])
        assert score == 1.0

    def test_rule_based_scan_unlimited_liability(self):
        agent = LegalAgent()
        text = "This contract contains unlimited liability clauses."
        findings = agent._rule_based_scan(text)
        assert len(findings) > 0
        assert any(f["category"] == "unlimited_liability" for f in findings)

    def test_rule_based_scan_auto_renewal(self):
        agent = LegalAgent()
        text = "This agreement will auto-renew automatically each year."
        findings = agent._rule_based_scan(text)
        assert any(f["category"] == "auto_renewal" for f in findings)

    def test_rule_based_scan_ambiguous(self):
        agent = LegalAgent()
        text = "The party shall use reasonable efforts to perform."
        findings = agent._rule_based_scan(text)
        assert any(f["category"] == "ambiguous" for f in findings)
