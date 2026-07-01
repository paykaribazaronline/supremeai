
import pytest

from agents.crew_departments import ArchitectureAgent, CodeGeneratorAgent, QAAgent
from models.shared_workspace import SharedWorkspace


@pytest.fixture
def workspace():
    return SharedWorkspace(
        task_id="task-1",
        original_prompt="Design a python app",
        architecture_design="files:\n  main.py",
        generated_code={"main.py": "print('hi')"},
        test_results={},
        execution_logs=[],
    )


def test_architecture_agent_design(workspace):
    agent = ArchitectureAgent()
    assert hasattr(agent, "design")


def test_code_generator_agent_generate_code(workspace):
    agent = CodeGeneratorAgent()
    assert hasattr(agent, "generate_code")


@pytest.mark.asyncio
async def test_qa_agent_verify_blocks_dangerous_code(workspace, monkeypatch):
    from agents import crew_departments as mod

    class FakeGateway:
        async def acompletion(self, *args, **kwargs):
            return {"choices": [{"message": {"content": "fake"}}]}

    monkeypatch.setattr(mod, "llm_gateway", FakeGateway())

    workspace.generated_code["main.py"] = "import os\nimport eval"
    qa = QAAgent()
    await qa.verify(workspace, user_id="u")
    assert workspace.test_results.get("safe") is False
    assert "Security Exception" in workspace.test_results.get("error", "")


@pytest.mark.asyncio
async def test_qa_agent_verify_passes_clean_code(workspace, monkeypatch):
    from agents import crew_departments as mod

    class FakeGateway:
        async def acompletion(self, *args, **kwargs):
            return {"choices": [{"message": {"content": "fake"}}]}

    monkeypatch.setattr(mod, "llm_gateway", FakeGateway())

    workspace.generated_code["main.py"] = "print('hello')"
    qa = QAAgent()
    await qa.verify(workspace, user_id="u")
    assert workspace.test_results.get("safe") is True
    assert workspace.test_results.get("passed") is True


def test_shared_workspace_log():
    ws = SharedWorkspace(task_id="t1", original_prompt="do stuff")
    ws.log("step 1")
    ws.log("step 2")
    assert ws.execution_logs == ["step 1", "step 2"]


@pytest.mark.asyncio
async def test_swarm_agent_base_call_gateway(monkeypatch):
    from agents import crew_departments as mod

    class FakeGateway:
        async def acompletion(self, *args, **kwargs):
            return {"choices": [{"message": {"content": "fake response"}}]}

    monkeypatch.setattr(mod, "llm_gateway", FakeGateway())

    agent = ArchitectureAgent()
    response = await agent.call_gateway("sys", "user", "u1")
    assert response == "fake response"
