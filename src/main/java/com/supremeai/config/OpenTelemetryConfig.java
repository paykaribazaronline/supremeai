package com.supremeai.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry configuration for distributed tracing. Enables request flow analysis across
 * microservices.
 */
@Configuration
public class OpenTelemetryConfig {

  @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
  private String otlpEndpoint;

  @Value("${otel.service.name:supremeai-backend}")
  private String serviceName;

  @Value("${otel.traces.exporter:otlp}")
  private String tracesExporter;

  /** Configure OpenTelemetry SDK with OTLP exporter. */
  @Bean
  public OpenTelemetry openTelemetry() {
    // Validate endpoint
    String endpoint =
        (otlpEndpoint == null || otlpEndpoint.isBlank()) ? "http://localhost:4317" : otlpEndpoint;

    // Create OTLP exporter
    SpanExporter spanExporter =
        OtlpGrpcSpanExporter.builder()
            .setEndpoint(endpoint)
            .setTimeout(Duration.ofSeconds(10))
            .build();

    // Configure tracer provider
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                BatchSpanProcessor.builder(spanExporter)
                    .setMaxQueueSize(2048)
                    .setScheduleDelay(Duration.ofMillis(5000))
                    .build())
            .setResource(
                Resource.getDefault().toBuilder()
                    .put("service.name", serviceName)
                    .put("service.version", "1.0.0")
                    .build())
            .build();

    try {
      return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();
    } catch (IllegalStateException e) {
      return io.opentelemetry.api.GlobalOpenTelemetry.get();
    }
  }

  /** Create tracer for manual instrumentation. */
  @Bean
  public Tracer tracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(serviceName);
  }
}
