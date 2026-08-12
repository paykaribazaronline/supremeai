import sys
from unittest.mock import MagicMock, patch

import pytest

# Conditional mock for opentelemetry exporter when running in environments
# without ml dependencies (e.g. CI)
try:
    import opentelemetry.exporter.otlp.proto.grpc.trace_exporter as _
except ImportError:
    import opentelemetry

    mock_exporter = MagicMock()
    sys.modules["opentelemetry.exporter"] = mock_exporter
    sys.modules["opentelemetry.exporter.otlp"] = mock_exporter
    sys.modules["opentelemetry.exporter.otlp.proto"] = mock_exporter
    sys.modules["opentelemetry.exporter.otlp.proto.grpc"] = mock_exporter
    sys.modules["opentelemetry.exporter.otlp.proto.grpc.trace_exporter"] = mock_exporter
    opentelemetry.exporter = mock_exporter

from core.observability.telemetry import (
    _NoOpSpan,
    _RealSpan,
    get_tracer,
    setup_tracing,
    trace_span,
)

# বাংলা মন্তব্য: সব patch target core.observability.telemetry-তে করা হচ্ছে
# কারণ setup_tracing, trace_span, get_tracer এই module-এ define করা আছে।
_TEL = "core.observability.telemetry"


def test_setup_tracing_noop():
    with patch(f"{_TEL}.otel_trace") as mock_trace:
        setup_tracing("test-service")
        assert mock_trace.set_tracer_provider.called
        assert mock_trace.get_tracer.called
        assert get_tracer() is not None


def test_setup_tracing_with_endpoint():
    with (
        patch("opentelemetry.exporter.otlp.proto.grpc.trace_exporter.OTLPSpanExporter") as mock_exporter,
        patch(f"{_TEL}.BatchSpanProcessor") as mock_processor,
        patch(f"{_TEL}.TracerProvider") as mock_provider_class,
    ):
        mock_provider = MagicMock()
        mock_provider_class.return_value = mock_provider

        setup_tracing("test-service", "http://127.0.0.1:4317")

        mock_exporter.assert_called_once_with(endpoint="http://127.0.0.1:4317", insecure=True)
        mock_processor.assert_called_once_with(mock_exporter.return_value)
        mock_provider.add_span_processor.assert_called_once_with(mock_processor.return_value)


def test_setup_tracing_without_endpoint_no_exporter():
    with (
        patch("opentelemetry.exporter.otlp.proto.grpc.trace_exporter.OTLPSpanExporter") as mock_exporter,
        patch(f"{_TEL}.TracerProvider") as mock_provider_class,
    ):
        mock_provider = MagicMock()
        mock_provider_class.return_value = mock_provider

        setup_tracing("test-service")

        mock_exporter.assert_not_called()
        mock_provider.add_span_processor.assert_not_called()


def test_trace_span_no_tracer():
    with (
        patch(f"{_TEL}.get_tracer", return_value=None),
        trace_span("test-span") as span,
    ):
        assert isinstance(span, _NoOpSpan)


def test_trace_span_with_tracer():
    mock_tracer = MagicMock()
    mock_span = MagicMock()

    mock_tracer.start_as_current_span.return_value.__enter__.return_value = mock_span

    with patch(f"{_TEL}.get_tracer", return_value=mock_tracer):
        with trace_span("test-span", attributes={"key": "val"}) as span:
            assert isinstance(span, _RealSpan)
            mock_span.set_attribute.assert_called_with("key", "val")

        mock_tracer.start_as_current_span.assert_called_once()


def test_trace_span_sets_ok_status_on_success():
    mock_tracer = MagicMock()
    mock_span = MagicMock()
    mock_tracer.start_as_current_span.return_value.__enter__.return_value = mock_span

    with patch(f"{_TEL}.get_tracer", return_value=mock_tracer):
        with trace_span("ok-span"):
            pass

        mock_span.set_status.assert_called_once()


def test_trace_span_records_exception_on_error():
    mock_tracer = MagicMock()
    mock_span = MagicMock()
    mock_tracer.start_as_current_span.return_value.__enter__.return_value = mock_span

    with patch(f"{_TEL}.get_tracer", return_value=mock_tracer):
        with pytest.raises(RuntimeError):
            with trace_span("error-span"):
                raise RuntimeError("boom")

        mock_span.set_status.assert_called()
        mock_span.record_exception.assert_called()


def test_trace_span_unknown_kind_defaults_to_internal():
    mock_tracer = MagicMock()
    mock_span = MagicMock()
    mock_tracer.start_as_current_span.return_value.__enter__.return_value = mock_span

    with patch(f"{_TEL}.get_tracer", return_value=mock_tracer):
        with trace_span("span", kind="unknown"):
            pass
        from opentelemetry import trace as otel_trace

        call_kwargs = mock_tracer.start_as_current_span.call_args
        assert call_kwargs.kwargs["kind"] == otel_trace.SpanKind.INTERNAL


def test_trace_span_sets_attributes_multiple():
    mock_tracer = MagicMock()
    mock_span = MagicMock()
    mock_tracer.start_as_current_span.return_value.__enter__.return_value = mock_span

    with patch(f"{_TEL}.get_tracer", return_value=mock_tracer):
        with trace_span("span", attributes={"a": 1, "b": "two"}):
            pass
        assert mock_span.set_attribute.call_count == 2


def test_noop_span_methods_are_silent():
    span = _NoOpSpan()
    span.set_attribute("key", "val")
    span.record_exception(Exception("x"))


def test_real_span_delegates_set_attribute():
    mock_span = MagicMock()
    real = _RealSpan(mock_span)
    real.set_attribute("k", "v")
    mock_span.set_attribute.assert_called_once_with("k", "v")


def test_real_span_delegates_record_exception():
    mock_span = MagicMock()
    real = _RealSpan(mock_span)
    exc = ValueError("v")
    real.record_exception(exc)
    mock_span.record_exception.assert_called_once_with(exc)


def test_tracer_shared_globally_after_setup():
    with (
        patch("opentelemetry.exporter.otlp.proto.grpc.trace_exporter.OTLPSpanExporter"),
        patch(f"{_TEL}.BatchSpanProcessor"),
        patch(f"{_TEL}.TracerProvider"),
    ):
        setup_tracing("svc-a")
        t1 = get_tracer()
        assert t1 is not None
        setup_tracing("svc-b")
        t2 = get_tracer()
        assert get_tracer() is t2
