# বাংলা মন্তব্য: টেস্টে ব্যবহৃত logger ইম্পোর্টটি ফিরিয়ে আনা হলো।
from unittest.mock import patch

import pytest
from loguru import logger

from backend.agents.medical_agent import MedicalAgent


@pytest.mark.asyncio
class TestMedicalAgent:
    @pytest.fixture
    def medical_agent(self):
        return MedicalAgent()

    @pytest.mark.asyncio
    async def test_init(self, medical_agent):
        assert medical_agent.domain_adapter is not None or logger.info.called

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_symptom_analysis(self, mock_logger, medical_agent):
        symptoms = "headache"
        age = 30
        medical_history = "none"
        result = medical_agent.symptom_analysis(symptoms, age, medical_history)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_symptom_analysis_empty_input(self, mock_logger, medical_agent):
        symptoms = ""
        age = None
        medical_history = None
        result = medical_agent.symptom_analysis(symptoms, age, medical_history)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_symptom_analysis_large_input(self, mock_logger, medical_agent):
        symptoms = "a" * 1000
        age = 30
        medical_history = "none"
        result = medical_agent.symptom_analysis(symptoms, age, medical_history)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_drug_interaction(self, mock_logger, medical_agent):
        medications = ["aspirin", "ibuprofen"]
        result = medical_agent.drug_interaction(medications)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result
        assert "interactions" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_drug_interaction_empty_input(self, mock_logger, medical_agent):
        medications = []
        result = medical_agent.drug_interaction(medications)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result
        assert "interactions" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_drug_interaction_large_input(self, mock_logger, medical_agent):
        medications = ["a" * 1000] * 10
        result = medical_agent.drug_interaction(medications)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result
        assert "interactions" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_generate(self, mock_logger, medical_agent):
        prompt = "test prompt"
        context = "test context"
        action = "test action"
        result = medical_agent._generate(prompt, context, action)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.logger")
    async def test_generate_empty_input(self, mock_logger, medical_agent):
        prompt = ""
        context = None
        action = ""
        result = medical_agent._generate(prompt, context, action)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.DomainAdapter")
    async def test_domain_adapter(self, mock_domain_adapter):
        mock_domain_adapter.return_value.adapt_request.return_value = {
            "response": "test response"
        }
        medical_agent = MedicalAgent()
        medical_agent.domain_adapter = mock_domain_adapter.return_value
        prompt = "test prompt"
        context = "test context"
        action = "test action"
        result = medical_agent._generate(prompt, context, action)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.DomainAdapter")
    async def test_domain_adapter_exception(self, mock_domain_adapter):
        mock_domain_adapter.return_value.adapt_request.side_effect = Exception(
            "test exception"
        )
        medical_agent = MedicalAgent()
        medical_agent.domain_adapter = mock_domain_adapter.return_value
        prompt = "test prompt"
        context = "test context"
        action = "test action"
        result = medical_agent._generate(prompt, context, action)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result

    @pytest.mark.asyncio
    @patch("backend.agents.medical_agent.DomainAdapter")
    async def test_domain_adapter_none(self, mock_domain_adapter):
        medical_agent = MedicalAgent()
        medical_agent.domain_adapter = None
        prompt = "test prompt"
        context = "test context"
        action = "test action"
        result = medical_agent._generate(prompt, context, action)
        assert isinstance(result, dict)
        assert "action" in result
        assert "response" in result
        assert "model" in result
        assert "provider" in result
        assert "disclaimer" in result
