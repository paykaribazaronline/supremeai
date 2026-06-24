import pytest
from skills.schema import UniversalSkillSchema
from skills.installer import SkillInstaller
from skills.registry import SkillRegistry
from skill_loader import SkillLoader


@pytest.fixture
def temp_skills_dir(tmp_path):
    # Set up temp dir for registry and dynamic skills
    reg_path = tmp_path / "skills_registry.json"
    registry = SkillRegistry(registry_path=str(reg_path))
    installer = SkillInstaller(registry=registry, skills_dir=str(tmp_path / "dynamic"))
    loader = SkillLoader(registry=registry, installer=installer)
    loader.skills_dir = tmp_path / "dynamic"
    loader.skills_dir.mkdir(parents=True, exist_ok=True)
    return loader, registry, installer


VALID_USS_DATA = {
    "metadata": {
        "name": "sentiment_analyzer",
        "version": "1.2.3",
        "description": "Analyzes sentiment of reviews.",
        "author": "supremeai_agent_id",
        "tags": ["nlp", "sentiment"]
    },
    "interface": {
        "input_schema": {
            "type": "object",
            "properties": {
                "text": {"type": "string"}
            },
            "required": ["text"]
        },
        "output_schema": {
            "type": "object",
            "properties": {
                "sentiment": {"type": "string"}
            }
        }
    },
    "execution": {
        "runtime": "python3.11",
        "entry_point": "main.run",
        "dependencies": [],
        "timeout_seconds": 30
    },
    "validation": {
        "tests": [
            {
                "input": {"text": "I love this!"},
                "expected_output": {"sentiment": "positive"}
            }
        ],
        "security_level": "sandboxed"
    }
}


def test_uss_valid_pydantic():
    schema = UniversalSkillSchema(**VALID_USS_DATA)
    assert schema.metadata.name == "sentiment_analyzer"
    assert schema.metadata.version == "1.2.3"
    assert schema.validation.security_level == "sandboxed"


def test_uss_invalid_semver():
    bad_data = VALID_USS_DATA.copy()
    bad_data["metadata"] = bad_data["metadata"].copy()
    bad_data["metadata"]["version"] = "1.2"  # invalid semver
    with pytest.raises(ValueError):
        UniversalSkillSchema(**bad_data)


def test_uss_invalid_security():
    bad_data = VALID_USS_DATA.copy()
    bad_data["validation"] = bad_data["validation"].copy()
    bad_data["validation"]["security_level"] = "unrestricted"  # invalid security level
    with pytest.raises(ValueError):
        UniversalSkillSchema(**bad_data)


def test_installer_and_loader_with_uss(temp_skills_dir):
    loader, registry, installer = temp_skills_dir
    
    code = "def run(text):\n    return {'sentiment': 'positive'}\n"
    
    success = installer.install_skill_from_source(
        name="sentiment_analyzer",
        code=code,
        version="1.2.3",
        description="Analyzes sentiment",
        dependencies=[],
        uss=VALID_USS_DATA
    )
    assert success is True
    
    # Check registration
    skill_meta = registry.get_skill("sentiment_analyzer")
    assert skill_meta is not None
    assert skill_meta["uss"]["metadata"]["name"] == "sentiment_analyzer"
    
    # Load and verify
    mod = loader.load("sentiment_analyzer")
    assert mod is not None
    assert mod.run("test") == {"sentiment": "positive"}
