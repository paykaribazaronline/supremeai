import os
from contextlib import contextmanager
from typing import Any

from opentelemetry import trace as otel_trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.trace import Span
from opentelemetry.trace import Status
from opentelemetry.trace import StatusCode
from opentelemetry.trace import Tracer


_tracer: Tracer | None = None
tracer: Tracer | None = None


def setup_tracing(
    service_name: str = "supremeai", otlp_endpoint: str | None = None
) -> None:
    endpoint = otlp_endpoint or os.getenv("OTLP_ENDPOINT", "")
    provider = TracerProvider()
    if endpoint:
        try:
            from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter

            exporter = OTLPSpanExporter(endpoint=endpoint, insecure=True)
            provider.add_span_processor(BatchSpanProcessor(exporter))
        except ImportError as exc:
            from loguru import logger

            logger.warning(f"OTLP exporter not available: {exc}")
    otel_trace.set_tracer_provider(provider)
    globals()["_tracer"] = otel_trace.get_tracer(service_name)


globals()["tracer"] = globals()["_tracer"]


def get_tracer() -> Tracer | None:
    return _tracer


@contextmanager
def trace_span(
    name: str, attributes: dict[str, Any] | None = None, kind: str = "internal"
):
    tracer = get_tracer()
    if tracer is None:
        yield _NoOpSpan()
        return
    span_kind = {
        "internal": otel_trace.SpanKind.INTERNAL,
        "server": otel_trace.SpanKind.SERVER,
        "client": otel_trace.SpanKind.CLIENT,
        "producer": otel_trace.SpanKind.PRODUCER,
        "consumer": otel_trace.SpanKind.CONSUMER,
    }.get(kind, otel_trace.SpanKind.INTERNAL)
    with tracer.start_as_current_span(name, kind=span_kind) as span:
        if attributes:
            for k, v in attributes.items():
                span.set_attribute(k, v)
        try:
            yield _RealSpan(span)
            span.set_status(Status(StatusCode.OK))
        except Exception as exc:
            span.set_status(Status(StatusCode.ERROR, str(exc)))
            span.record_exception(exc)
            raise


class _NoOpSpan:
    def set_attribute(self, *args, **kwargs):
        pass

    def record_exception(self, *args, **kwargs):
        pass


class _RealSpan:
    def __init__(self, span: Span):
        self._span = span

    def set_attribute(self, key: str, value: Any):
        self._span.set_attribute(key, value)

    def record_exception(self, exc: BaseException):
        self._span.record_exception(exc)
