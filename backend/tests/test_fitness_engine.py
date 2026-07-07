import pytest
from skills.registry import SkillRegistry

from evolution.fitness_engine import FitnessEngine


@pytest.fixture
def temp_fitness_env(tmp_path):
    metrics_path = tmp_path / "metrics.json"
    registry_path = tmp_path / "registry.json"
    skills_dir = tmp_path / "dynamic"
    deprecated_dir = tmp_path / "deprecated"

    skills_dir.mkdir()
    deprecated_dir.mkdir()

    # Create a dummy skill in skills_dir and register it
    skill_name = "DummySkill"
    dummy_skill_dir = skills_dir / skill_name
    dummy_skill_dir.mkdir()
    with open(dummy_skill_dir / "main.py", "w") as f:
        f.write("class DummySkill:\n    pass\n")
    with open(dummy_skill_dir / "schema.json", "w") as f:
        f.write('{"metadata": {"name": "DummySkill", "version": "1.0.0", "description": "test"}}')

    registry = SkillRegistry(registry_path=str(registry_path))
    registry.register_skill(
        name=skill_name,
        version="1.0.0",
        description="test",
        entry_point=str(dummy_skill_dir / "main.py"),
        uss={
            "metadata": {
                "name": "DummySkill",
                "version": "1.0.0",
                "description": "test",
                "author": "test",
                "tags": [],
            },
            "interface": {
                "input_schema": {"type": "object"},
                "output_schema": {"type": "object"},
            },
            "execution": {
                "runtime": "python3.11",
                "entry_point": "main.execute",
                "dependencies": [],
                "timeout_seconds": 30,
            },
            "validation": {"tests": [], "security_level": "sandboxed"},
        },
    )

    # Create a Firestore db mock to verify updates
    class FirestoreDocMock:
        def __init__(self):
            self.updates = {}

        def update(self, payload):
            self.updates.update(payload)

    class FirestoreCollectionMock:
        def __init__(self):
            self.docs = {}

        def document(self, name):
            if name not in self.docs:
                self.docs[name] = FirestoreDocMock()
            return self.docs[name]

    class FirestoreClientMock:
        def __init__(self):
            self.collections = {}

        def collection(self, name):
            if name not in self.collections:
                self.collections[name] = FirestoreCollectionMock()
            return self.collections[name]

    db_mock = FirestoreClientMock()

    engine = FitnessEngine(
        metrics_path=str(metrics_path),
        registry_path=str(registry_path),
        skills_dir=str(skills_dir),
        deprecated_dir=str(deprecated_dir),
        db=db_mock,
    )

    return engine, skill_name, skills_dir, deprecated_dir, registry_path, db_mock


def test_track_execution(temp_fitness_env):
    engine, skill_name, _, _, _, _ = temp_fitness_env

    # Track 3 successes and 1 failure
    engine.track_execution(skill_name, success=True, latency=1.5, token_cost=0.01)
    engine.track_execution(skill_name, success=True, latency=2.0, token_cost=0.02)
    engine.track_execution(skill_name, success=True, latency=1.0, token_cost=0.01)
    engine.track_execution(skill_name, success=False, latency=4.0, token_cost=0.05)

    metrics = engine.metrics[skill_name]
    assert metrics["success_count"] == 3
    assert metrics["failure_count"] == 1
    assert metrics["total_latency"] == 8.5
    assert metrics["token_cost"] == pytest.approx(0.09)
    assert metrics["reuse_count"] == 4


def test_calculate_fitness(temp_fitness_env):
    engine, skill_name, _, _, _, _ = temp_fitness_env

    # Initially defaults to 1.0
    assert engine.calculate_fitness("NonExistent") == 1.0

    # 5 runs, all successful, average latency = 2.0s
    for _ in range(5):
        engine.track_execution(skill_name, success=True, latency=2.0)

    score = engine.calculate_fitness(skill_name)
    # Success Rate = 1.0 (70% weight -> 0.7)
    # Avg Latency = 2.0s (Latency Penalty = 2.0/10.0 = 0.2, weight -> (1 - 0.2) * 0.3 = 0.24)
    # Total Score = 0.7 + 0.24 = 0.94
    assert score == pytest.approx(0.94)


def test_evaluate_and_prune_success_threshold(temp_fitness_env):
    engine, skill_name, _, _, _, _ = temp_fitness_env

    # If not enough runs (min_runs=5), prune should be skipped
    engine.track_execution(skill_name, success=False, latency=9.0)
    assert engine.evaluate_and_prune(skill_name, threshold=0.5, min_runs=5) is False

    # Track 4 more failures (total 5 runs, success rate = 0.0)
    for _ in range(4):
        engine.track_execution(skill_name, success=False, latency=9.0)

    score = engine.calculate_fitness(skill_name)
    # Success Rate = 0.0 -> 0.0
    # Avg Latency = 9.0s -> Latency penalty = 0.9 -> (1 - 0.9) * 0.3 = 0.03
    # Total Score = 0.03 (< 0.5 threshold)
    assert score < 0.5

    # Now pruning should trigger
    is_pruned = engine.evaluate_and_prune(skill_name, threshold=0.5, min_runs=5)
    assert is_pruned is True

    # Verify file is moved to deprecated directory
    assert not (temp_fitness_env[2] / skill_name).exists()
    assert (temp_fitness_env[3] / skill_name).exists()
    assert (temp_fitness_env[3] / skill_name / "main.py").exists()

    # Verify status updated to DEPRECATED in registry
    updated_skill = engine.registry.get_skill(skill_name)
    assert updated_skill["status"] == "DEPRECATED"

    # Verify firestore document status updated
    db_mock = temp_fitness_env[5]
    doc = db_mock.collection("supreme_dynamic_skills").document(skill_name)
    assert doc.updates["status"] == "DEPRECATED"
