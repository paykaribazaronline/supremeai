# বাংলা মন্তব্য: core.telemetry মডিউল — observability.telemetry থেকে সব symbol re-export করা হয়েছে
# যাতে tests `core.telemetry.BatchSpanProcessor`, `core.telemetry.otel_trace` ইত্যাদি patch করতে পারে।
from core.observability.telemetry import (_NoOpSpan, _RealSpan, get_tracer,
                                          setup_tracing, trace_span)
from opentelemetry import trace as otel_trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

__all__ = [
    "BatchSpanProcessor",
    "TracerProvider",
    "_NoOpSpan",
    "_RealSpan",
    "get_tracer",
    "otel_trace",
    "setup_tracing",
    "trace_span",
]
