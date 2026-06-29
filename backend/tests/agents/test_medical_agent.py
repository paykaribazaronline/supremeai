import sys
from unittest.mock import patch


sys.path.append("..")
from agents.medical_agent import MedicalAgent


class TestMedicalAgent:
    def test_init(self):
        agent = MedicalAgent()
        assert agent is not None

    def test_symptom_analysis(self):
        agent = MedicalAgent()
        result = agent.symptom_analysis("fever and cough", age=30)
        assert "action" in result
        assert "disclaimer" in result

    def test_symptom_analysis_with_history(self):
        agent = MedicalAgent()
        result = agent.symptom_analysis("headache", age=25, medical_history="migraine")
        assert "action" in result

    def test_drug_interaction_found(self):
        agent = MedicalAgent()
        result = agent.drug_interaction(["warfarin", "aspirin"])
        assert "interactions" in result
        assert "disclaimer" in result
        assert len(result["interactions"]) > 0

    def test_drug_interaction_none(self):
        agent = MedicalAgent()
        result = agent.drug_interaction(["vitamin_c", "zinc"])
        assert "interactions" in result
        assert len(result["interactions"]) == 0

    def test_drug_interaction_maoi_ssri(self):
        agent = MedicalAgent()
        result = agent.drug_interaction(["maoi", "ssri"])
        assert len(result["interactions"]) > 0
        assert any("Serotonin syndrome" in i["risk"] for i in result["interactions"])

    def test_generate_with_domain_adapter(self):
        agent = MedicalAgent()
        with patch.object(agent.domain_adapter, "adapt_request", return_value={"response": "test", "model": "m", "provider": "p"}):
            result = agent._generate("test", context="ctx", action="test_action")
            assert result["action"] == "test_action"
            assert result["response"] == "test"

    def test_generate_without_domain_adapter(self):
        agent = MedicalAgent()
        agent.domain_adapter = None
        result = agent._generate("test", context="ctx", action="test_action")
        assert result["action"] == "test_action"
        assert "fallback" in result["response"].lower()
