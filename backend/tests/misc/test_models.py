"""
Tests for models — SQLAlchemy model definitions
"""

from __future__ import annotations

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from models.base import Base
from models.dynamic_agent import DynamicAgent


class TestBase:
    def test_base_is_declarative(self):
        assert hasattr(Base, "metadata")
        assert hasattr(Base, "registry")

    def test_base_can_create_tables(self):
        engine = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(engine)
        # Should not raise
        assert True


class TestDynamicAgent:
    def test_model_has_expected_columns(self):
        assert hasattr(DynamicAgent, "id")
        assert hasattr(DynamicAgent, "name")
        assert hasattr(DynamicAgent, "description")
        assert hasattr(DynamicAgent, "execution_steps")
        assert hasattr(DynamicAgent, "is_active")
        assert hasattr(DynamicAgent, "created_at")

    def test_tablename(self):
        assert DynamicAgent.__tablename__ == "dynamic_agents"

    def test_create_instance(self):
        agent = DynamicAgent(
            name="test-agent",
            description="A test agent",
            execution_steps={"script": "print('hello')"},
        )
        assert agent.name == "test-agent"
        assert agent.description == "A test agent"
        assert agent.execution_steps == {"script": "print('hello')"}
        assert agent.is_active is None or agent.is_active is True

    def test_create_instance_with_defaults(self):
        agent = DynamicAgent(name="defaults", execution_steps={})
        assert agent.name == "defaults"
        assert agent.description is None
        assert agent.is_active is None or agent.is_active is True

    def test_persist_and_retrieve(self):
        engine = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(engine)

        with Session(engine) as session:
            agent = DynamicAgent(
                name="persisted-agent",
                description="Will be saved",
                execution_steps={"action": "test"},
            )
            session.add(agent)
            session.commit()

        with Session(engine) as session:
            retrieved = session.query(DynamicAgent).filter_by(name="persisted-agent").first()
            assert retrieved is not None
            assert retrieved.description == "Will be saved"
            assert retrieved.execution_steps == {"action": "test"}

    def test_unique_name_constraint(self):
        engine = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(engine)

        with Session(engine) as session:
            session.add(DynamicAgent(name="unique-agent", execution_steps={}))
            session.commit()

        with Session(engine) as session:
            session.add(DynamicAgent(name="unique-agent", execution_steps={}))
            with pytest.raises(
                Exception
            ):  # -- intentionally broad: asserts *some* error propagates (mocked/validation failure), exact type varies
                session.commit()

    def test_is_active_default_true(self):
        engine = create_engine("sqlite:///:memory:")
        Base.metadata.create_all(engine)
        with Session(engine) as session:
            agent = DynamicAgent(name="active-agent", execution_steps={})
            session.add(agent)
            session.commit()
            assert agent.is_active is True

    def test_deactivate_agent(self):
        agent = DynamicAgent(name="deactivate-me", execution_steps={}, is_active=False)
        assert agent.is_active is False
