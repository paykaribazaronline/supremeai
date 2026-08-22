"""Tests for model files that previously had 0% coverage.

Targets: agent_session, analytics, error_remediation, execution_log,
execution_policy, handoff_event, localization, selector_healing_event,
system_config, target_platform_credential, local_model_handler.
"""

import pytest


class TestAgentSessionState:
    def test_enum_values(self):
        from models.agent_session import AgentSessionState

        assert AgentSessionState.Idle == "Idle"
        assert AgentSessionState.Scanning_Target_DOM == "Scanning_Target_DOM"

    def test_enum_is_str_enum(self):
        from models.agent_session import AgentSessionState

        assert issubclass(AgentSessionState, str)


class TestControlMode:
    def test_enum_values(self):
        from models.agent_session import ControlMode

        assert ControlMode.agent == "agent"
        assert ControlMode.human == "human"


class TestAgentSessionModel:
    def test_model_attributes(self):
        from models.agent_session import AgentSession

        assert AgentSession.__tablename__ == "agent_sessions"

    def test_default_state(self):
        from models.agent_session import AgentSession, AgentSessionState

        col = AgentSession.__table__.c.current_state
        assert col.default.arg == AgentSessionState.Idle


class TestAnalyticsModels:
    def test_auto_report_attributes(self):
        from models.analytics import AutoReport

        assert AutoReport.__tablename__ == "auto_reports"

    def test_churn_prediction_attributes(self):
        from models.analytics import ChurnPrediction

        assert ChurnPrediction.__tablename__ == "churn_predictions"

    def test_retention_action_attributes(self):
        from models.analytics import RetentionAction

        assert RetentionAction.__tablename__ == "retention_actions"


class TestErrorRemediation:
    def test_external_service_init(self):
        from models.error_remediation import ExternalService

        svc = ExternalService()
        assert svc._fail_count == 0

    def test_external_service_success(self):
        from models.error_remediation import ExternalService

        svc = ExternalService()
        result = svc.unstable_operation(should_fail=False)
        assert "সফল" in result

    def test_external_service_failure(self):
        from models.error_remediation import ExternalService

        svc = ExternalService()
        with pytest.raises(ConnectionError):
            svc.unstable_operation(should_fail=True)

    def test_resilient_call_exists(self):
        from models.error_remediation import resilient_call

        assert callable(resilient_call)

    def test_db_breaker_exists(self):
        from models.error_remediation import db_breaker

        assert db_breaker is not None


class TestExecutionLog:
    def test_log_type_enum(self):
        from models.execution_log import LogType

        assert LogType.shell_cmd == "shell_cmd"
        assert LogType.dom_action == "dom_action"

    def test_execution_log_attributes(self):
        from models.execution_log import ExecutionLog

        assert ExecutionLog.__tablename__ == "execution_logs"


class TestExecutionPolicy:
    def test_execution_policy_tablename(self):
        from models.execution_policy import ExecutionPolicy

        assert ExecutionPolicy.__tablename__ == "execution_policies"


class TestHandoffEvent:
    def test_handoff_event_tablename(self):
        from models.handoff_event import HandoffEvent

        assert HandoffEvent.__tablename__ == "handoff_events"


class TestLocalizationModels:
    def test_translation_cache_tablename(self):
        from models.localization import TranslationCache

        assert TranslationCache.__tablename__ == "translation_cache"


class TestSelectorHealingEvent:
    def test_selector_healing_event_tablename(self):
        from models.selector_healing_event import SelectorHealingEvent

        assert SelectorHealingEvent.__tablename__ == "selector_healing_events"


class TestSystemConfig:
    def test_system_config_tablename(self):
        from models.system_config import SystemConfig

        assert SystemConfig.__tablename__ == "system_config"


class TestTargetPlatformCredential:
    def test_target_platform_credential_tablename(self):
        from models.target_platform_credential import TargetPlatformCredential

        assert TargetPlatformCredential.__tablename__ == "target_platform_credentials"


class TestLocalModelHandler:
    def test_handler_creation(self):
        from models.local_model_handler import LocalModelHandler

        handler = LocalModelHandler()
        assert handler is not None
