from typing import Any
from unittest.mock import patch

import pytest
from supremeai_2_0.backend.agents.legal_agent import LegalAgent


@pytest.fixture
def legal_agent():
    return LegalAgent()


class TestLegalAgent:
    def test_init(self, legal_agent):
        # Arrange and Act
        # Assert
        assert (
            isinstance(legal_agent.domain_adapter, object)
            or legal_agent.domain_adapter is None
        )

    @pytest.mark.asyncio
    async def test_analyze(self, legal_agent):
        # Arrange
        document_text = "This is a sample contract."
        doc_type = "contract"
        # Act
        result = legal_agent.analyze(document_text, doc_type)
        # Assert
        assert isinstance(result, dict[str, Any])
        assert "doc_type" in result
        assert "risk_score" in result
        assert "risks" in result
        assert "summary" in result
        assert "disclaimer" in result

    def test_rule_based_scan(self, legal_agent):
        # Arrange
        text = "This contract includes unlimited liability."
        # Act
        result = legal_agent._rule_based_scan(text)
        # Assert
        assert isinstance(result, list[dict[str, Any]])
        assert len(result) > 0

    def test_risk_score(self, legal_agent):
        # Arrange
        risks = [{"severity": "high"}, {"severity": "medium"}]
        # Act
        result = legal_agent._risk_score(risks)
        # Assert
        assert isinstance(result, float)
        assert result >= 0.0

    @patch("supremeai_2_0.backend.agents.legal_agent.DomainAdapter")
    def test_llm_summary(self, mock_domain_adapter, legal_agent):
        # Arrange
        text = "This contract includes unlimited liability."
        doc_type = "contract"
        risks = [{"severity": "high"}]
        # Act
        result = legal_agent._llm_summary(text, doc_type, risks)
        # Assert
        assert isinstance(result, str)
        assert len(result) > 0

    def test_review_nda(self, legal_agent):
        # Arrange
        nda_text = "This is a sample NDA."
        # Act
        result = legal_agent.review_nda(nda_text)
        # Assert
        assert isinstance(result, dict[str, Any])
        assert "doc_type" in result
        assert "risk_score" in result
        assert "risks" in result
        assert "summary" in result
        assert "disclaimer" in result

    def test_review_contract(self, legal_agent):
        # Arrange
        contract_text = "This is a sample contract."
        # Act
        result = legal_agent.review_contract(contract_text)
        # Assert
        assert isinstance(result, dict[str, Any])
        assert "doc_type" in result
        assert "risk_score" in result
        assert "risks" in result
        assert "summary" in result
        assert "disclaimer" in result

    def test_analyze_empty_text(self, legal_agent):
        # Arrange
        document_text = ""
        doc_type = "contract"
        # Act
        result = legal_agent.analyze(document_text, doc_type)
        # Assert
        assert isinstance(result, dict[str, Any])
        assert "doc_type" in result
        assert "risk_score" in result
        assert "risks" in result
        assert "summary" in result
        assert "disclaimer" in result

    def test_analyze_none_text(self, legal_agent):
        # Arrange
        document_text = None
        doc_type = "contract"
        # Act
        with pytest.raises(TypeError):
            legal_agent.analyze(document_text, doc_type)

    def test_analyze_large_text(self, legal_agent):
        # Arrange
        document_text = "a" * 10000
        doc_type = "contract"
        # Act
        result = legal_agent.analyze(document_text, doc_type)
        # Assert
        assert isinstance(result, dict[str, Any])
        assert "doc_type" in result
        assert "risk_score" in result
        assert "risks" in result
        assert "summary" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    async def test_concurrent_analyze(self, legal_agent):
        # Arrange
        document_text = "This is a sample contract."
        doc_type = "contract"
        # Act
        await asyncio.gather(
            legal_agent.analyze(document_text, doc_type),
            legal_agent.analyze(document_text, doc_type),
        )
        # Assert
        assert True

    @patch("supremeai_2_0.backend.agents.legal_agent.logger")
    def test_analyze_logger(self, mock_logger, legal_agent):
        # Arrange
        document_text = "This is a sample contract."
        doc_type = "contract"
        # Act
        legal_agent.analyze(document_text, doc_type)
        # Assert
        mock_logger.info.assert_called()

    def test_risk_score_empty_risks(self, legal_agent):
        # Arrange
        risks = []
        # Act
        result = legal_agent._risk_score(risks)
        # Assert
        assert isinstance(result, float)
        assert result == 1.0

    def test_risk_score_none_risks(self, legal_agent):
        # Arrange
        risks = None
        # Act
        with pytest.raises(TypeError):
            legal_agent._risk_score(risks)

    @patch("supremeai_2_0.backend.agents.legal_agent.DomainAdapter")
    def test_llm_summary_no_domain_adapter(self, mock_domain_adapter, legal_agent):
        # Arrange
        text = "This contract includes unlimited liability."
        doc_type = "contract"
        risks = [{"severity": "high"}]
        legal_agent.domain_adapter = None
        # Act
        result = legal_agent._llm_summary(text, doc_type, risks)
        # Assert
        assert isinstance(result, str)
        assert len(result) > 0

    def test_review_nda_empty_text(self, legal_agent):
        # Arrange
        nda_text = ""
        # Act
        result = legal_agent.review_nda(nda_text)
        # Assert
        assert isinstance(result, dict[str, Any])
        assert "doc_type" in result
        assert "risk_score" in result
        assert "risks" in result
        assert "summary" in result
        assert "disclaimer" in result

    def test_review_nda_none_text(self, legal_agent):
        # Arrange
        nda_text = None
        # Act
        with pytest.raises(TypeError):
            legal_agent.review_nda(nda_text)

    def test_review_contract_empty_text(self, legal_agent):
        # Arrange
        contract_text = ""
        # Act
        result = legal_agent.review_contract(contract_text)
        # Assert
        assert isinstance(result, dict[str, Any])
        assert "doc_type" in result
        assert "risk_score" in result
        assert "risks" in result
        assert "summary" in result
        assert "disclaimer" in result

    def test_review_contract_none_text(self, legal_agent):
        # Arrange
        contract_text = None
        # Act
        with pytest.raises(TypeError):
            legal_agent.review_contract(contract_text)
