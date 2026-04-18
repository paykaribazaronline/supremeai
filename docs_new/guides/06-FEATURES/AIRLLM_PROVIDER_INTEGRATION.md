# AirLLM Provider Integration for SupremeAI

## What Was Added

SupremeAI now supports AirLLM as a first-class provider in the existing provider registry and routing stack.

- Canonical provider id: `airllm-local`
- Display name: `AirLLM Local`
- Routing alias support: `airllm`, `airllm-local`, `local-airllm`
- Probe support with endpoint-only configuration
- Normal inference now honors registry-saved provider endpoints instead of ignoring them

This is implemented as an HTTP inference connector, not as embedded Python inside the Spring Boot process.

## Why This Shape

The current SupremeAI deployment is a Java Spring Boot service with a single-container Cloud Run workflow. AirLLM is a Python-based inference runtime, so the safe integration pattern is:

1. Keep SupremeAI as the orchestration and admin plane.
2. Run AirLLM behind a separate HTTP endpoint.
3. Register that endpoint through the existing provider registry.
4. Route complex tasks to the registered AirLLM provider when appropriate.

This avoids mixing Java application lifecycle, Python runtime dependencies, GPU driver requirements, and model cache management into the same container.

## Current Deployment Reality

Do not assume the current Cloud Run service can directly host large AirLLM models.

- Current deploy workflow is CPU-only and configured for `1Gi` memory in [.github/workflows/deploy-cloudrun.yml](../../.github/workflows/deploy-cloudrun.yml).
- Current runtime image is a Java container in [Dockerfile](../../Dockerfile).
- Large-model feasibility depends on the exact model, quantization, context length, concurrent requests, and whether AirLLM is exposed through an optimized inference gateway.

Treat any GPU sizing or cost numbers as benchmark targets until validated with load tests.

## Recommended Architecture

Use a hybrid layout:

- Fast layer: existing hosted providers or smaller models for low-latency tasks
- Intelligence layer: AirLLM endpoint for deep reasoning, background analysis, report generation, and higher-cost decisions

Suggested control flow:

1. Admin registers AirLLM as `airllm-local`.
2. SupremeAI probes the endpoint from `/api/providers/test/{id}`.
3. Capability routing or consensus includes `airllm-local` for selected tasks.
4. If AirLLM fails, fallback continues to existing providers.

## Example Provider Registration

POST `/api/providers/add`

```json
{
  "id": "airllm-local",
  "name": "AirLLM Local",
  "type": "LLM",
  "baseModel": "AIRLLM",
  "rateLimitPerMinute": 10,
  "endpoint": "http://localhost:8081/v1/chat/completions",
  "healthCheckUrl": "http://localhost:8081/health",
  "capabilities": [
    "complex-reasoning",
    "long-context",
    "analysis"
  ],
  "complexityTier": "high",
  "models": [
    "meta-llama/Llama-3.3-70B-Instruct"
  ],
  "status": "active",
  "notes": "Local or sidecar AirLLM gateway for complex reasoning"
}
```

If your gateway requires bearer auth, also provide `apiKey`.

Recommended rollout:

- Start with `rateLimitPerMinute: 10`
- Increase to `20` only after throughput benchmarking

## Environment Options

You can preconfigure the connector with:

- `AIRLLM_ENDPOINT`
- `AIRLLM_API_KEY` (optional)

If no endpoint is provided through environment or provider registry, the built-in default expects a local gateway at `http://localhost:8081/v1/chat/completions`.

## Operational Guidance

- Start with background or batch reasoning workloads, not latency-critical chat.
- Benchmark token throughput, warm-up time, and memory pressure before routing production traffic.
- Keep a fallback chain enabled so AirLLM failures do not block generation.
- If deploying on Cloud Run with GPU, validate region support, cold-start profile, and persistent model cache strategy first.

## Immediate Next Step

Register an AirLLM endpoint and validate it through `/api/providers/test/airllm-local` before adding it to wider consensus flows.

Example local test:

```bash
curl -X POST http://localhost:8081/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "meta-llama/Llama-3.3-70B-Instruct",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 100
  }'
```
