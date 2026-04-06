# AirLLM Hybrid Deployment

## Target Layout

Use SupremeAI as the orchestration plane and AirLLM as a separate inference sidecar.

- Local development: Java backend + AirLLM sidecar + admin UI on the laptop
- Cloud deployment: SupremeAI on Cloud Run, AirLLM on a separate Cloud Run service, dashboard on Firebase Hosting

This keeps Python inference runtime concerns separate from the Spring Boot container.

## Local Development

Run AirLLM separately and point SupremeAI at it.

### Option A: Docker Compose

The root [docker-compose.yml](../../docker-compose.yml) now includes an optional `airllm` profile.

```bash
docker compose --profile airllm up --build
```

Default wiring:

- SupremeAI: `http://localhost:8080`
- AirLLM sidecar: `http://localhost:8081`
- AirLLM chat endpoint: `http://localhost:8081/v1/chat/completions`
- AirLLM health endpoint: `http://localhost:8081/health`

### Option B: Separate Processes

```bash
docker run -p 8081:8081 \
  -e MODEL=meta-llama/Llama-3.3-70B-Instruct \
  -e COMPRESSION=4bit \
  supremeai/airllm-sidecar:latest
```

Then start SupremeAI with either environment variables or JVM properties:

```bash
./gradlew bootRun -DAIRLLM_ENDPOINT=http://localhost:8081/v1/chat/completions -DAIRLLM_HEALTHCHECK_URL=http://localhost:8081/health
```

## Spring Configuration

Relevant keys now exist in [src/main/resources/application.properties](../../src/main/resources/application.properties):

- `spring.profiles.active`
- `ai.providers.airllm.endpoint`
- `ai.providers.airllm.health-check-url`
- `ai.providers.airllm.api-key`
- `ai.providers.airllm.model`
- `ai.providers.airllm.compression`
- `ai.providers.airllm.rate-limit-per-minute`

Runtime overrides:

- `SPRING_PROFILES_ACTIVE`
- `AIRLLM_ENDPOINT`
- `AIRLLM_HEALTHCHECK_URL`
- `AIRLLM_API_KEY`
- `AIRLLM_MODEL`
- `AIRLLM_COMPRESSION`
- `AIRLLM_RATE_LIMIT_PER_MINUTE`

## Cloud Run Pattern

Deploy AirLLM as its own service, then pass its URL into SupremeAI.

```bash
gcloud run deploy airllm-sidecar \
  --image gcr.io/supremeai-a/airllm-sidecar:latest \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --memory 4Gi \
  --cpu 2 \
  --port 8081 \
  --set-env-vars="MODEL=meta-llama/Llama-3.3-70B-Instruct,COMPRESSION=4bit"
```

Then wire SupremeAI to the deployed URL:

```bash
gcloud run deploy supremeai \
  --image gcr.io/supremeai-a/supremeai:latest \
  --set-env-vars="AIRLLM_ENDPOINT=https://YOUR-AIRLLM-URL/v1/chat/completions,AIRLLM_HEALTHCHECK_URL=https://YOUR-AIRLLM-URL/health" \
  --region us-central1
```

Validate GPU sizing and throughput before treating any cost or capacity number as final.

## Recommended Rollout

1. Start locally with `AIRLLM_RATE_LIMIT_PER_MINUTE=10`.
2. Probe the provider from SupremeAI.
3. Benchmark actual throughput and latency.
4. Raise the limit toward `20` only after real measurements.
