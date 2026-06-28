from unittest.mock import patch

from core.posthog_client import posthog_client


@patch("core.posthog_client.logger")
def test_posthog_client_capture_mock(mock_logger):
    # Verify capture log output in mock mode when POSTHOG_API_KEY is not set
    posthog_client.enabled = False
    posthog_client.capture(
        distinct_id="test-user-123", event="test_event", properties={"foo": "bar"}
    )
    # Check that it logged the event mock capture
    mock_logger.info.assert_called_once()
    args, _ = mock_logger.info.call_args
    assert "test_event" in args[0]
