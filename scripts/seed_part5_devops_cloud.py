#!/usr/bin/env python3
"""
Part 5 — DevOps & Cloud
Seeds SupremeAI Firebase with deep knowledge about:
  • Docker (Dockerfile best practices, multi-stage builds, compose)
  • Kubernetes (Pods, Deployments, Services, ConfigMaps, HPA, health checks)
  • Google Cloud Run (configuration, scaling, secrets, cold starts)
  • GitHub Actions CI/CD (workflow design, caching, matrix builds)
  • Infrastructure as Code (Terraform basics)
  • Observability (logging, metrics, tracing — the three pillars)
  • Site Reliability Engineering (SLIs, SLOs, error budgets)

Collections written:
  • system_learning  (SystemLearning model records)
  • devops_knowledge (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part5_devops_cloud.py [--dry-run]
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {

    "docker_multistage_build": _learning(
        type_="PATTERN",
        category="DOCKER",
        content=(
            "Multi-stage Docker build for Spring Boot: "
            "Stage 1 (builder): FROM eclipse-temurin:21-jdk-alpine AS builder — compile and package. "
            "Stage 2 (runtime): FROM eclipse-temurin:21-jre-alpine — copy only the JAR, not JDK. "
            "Benefits: final image 3-5x smaller (no Maven/Gradle, no JDK, no test code). "
            "Spring Boot layered JAR: use 'java -Djarmode=layertools -jar app.jar extract' in builder "
            "to split JAR into layers (dependencies, spring-boot-loader, snapshot-dependencies, application). "
            "Each layer becomes a Docker layer — dependency layer cached between builds unless changed."
        ),
        solutions=[
            "Use eclipse-temurin:21-jre-alpine as final base (150MB vs 350MB for JDK image)",
            "Copy layered JAR output in dependency order: dependencies first (most stable layer)",
            "Add .dockerignore: .git, .gradle, node_modules, build/, target/ — speeds up build context",
            "Set ENTRYPOINT ['java', '-XX:+UseContainerSupport', '-jar', '/app/app.jar']",
            "Use --mount=type=cache,target=/root/.gradle in build stage to cache Gradle downloads",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=89,
        context={
            "final_image_size": "eclipse-temurin:21-jre-alpine + Spring Boot JAR ≈ 200MB",
            "tip": "-XX:+UseContainerSupport makes JVM respect container memory limits (not host RAM)",
        },
    ),

    "kubernetes_deployment": _learning(
        type_="PATTERN",
        category="KUBERNETES",
        content=(
            "Kubernetes Deployment best practices: "
            "Always specify resource requests AND limits: CPU request=0.25, limit=1; Memory request=256Mi, limit=512Mi. "
            "Liveness probe: restarts container if it becomes unresponsive. "
            "Readiness probe: removes pod from service endpoints until ready to serve traffic. "
            "Startup probe: for slow-starting apps (Spring Boot) — disable liveness until started. "
            "Use RollingUpdate strategy: maxUnavailable=0, maxSurge=1 for zero-downtime deploys. "
            "Anti-affinity rules: spread replicas across nodes for high availability."
        ),
        solutions=[
            "Set resources.requests AND resources.limits — without limits, OOMKilled risk",
            "Add readinessProbe to every deployment: httpGet /actuator/health path",
            "Set startupProbe for Spring Boot: failureThreshold=30, periodSeconds=10 = 5 min startup time",
            "Use HorizontalPodAutoscaler (HPA) to scale on CPU/memory metrics",
            "Use PodDisruptionBudget: minAvailable=1 to prevent all replicas being evicted at once",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=67,
        context={
            "cloud_run_equivalent": "Cloud Run handles health checks, scaling, rolling updates automatically",
            "yaml_tip": "Use 'kubectl diff -f deployment.yaml' to preview changes before applying",
        },
    ),

    "cloud_run_configuration": _learning(
        type_="PATTERN",
        category="CLOUD_RUN",
        content=(
            "Google Cloud Run configuration for SupremeAI: "
            "Memory: start with 512Mi; increase to 1Gi for Spring Boot with AI workloads. "
            "CPU: 1 vCPU default; enable 'CPU always allocated' to avoid cold starts for latency-sensitive workloads. "
            "Concurrency: 80 requests per instance (default) — reduce for CPU-intensive tasks. "
            "Min instances: 1 to eliminate cold starts in production. "
            "Max instances: 100 (set hard limit to prevent runaway cost). "
            "Service account: assign dedicated SA with minimum Firestore + Secret Manager permissions."
        ),
        solutions=[
            "Set --min-instances=1 in production to keep at least one warm instance",
            "Set --cpu-boost for faster startup (extra CPU during container startup)",
            "Enable HTTP/2 end-to-end: --use-http2 flag for gRPC or streaming",
            "Deploy with --no-traffic first to test, then traffic split to migrate gradually",
            "Use Cloud Run Jobs for batch processing (not HTTP triggers)",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=112,
        context={
            "deploy_command": "gcloud run deploy supremeai-a --image=gcr.io/supremeai-a/api:latest --region=asia-southeast1",
            "cold_start_tip": "Spring Boot: enable lazy initialisation + spring-context-indexer to cut startup time",
        },
    ),

    "github_actions_cicd": _learning(
        type_="PATTERN",
        category="CICD",
        content=(
            "GitHub Actions CI/CD pipeline for Spring Boot + Docker + Cloud Run: "
            "Trigger: on push to main and PRs. "
            "Jobs: (1) test — checkout, setup-java, gradle test + report. "
            "(2) build — gradle bootJar, docker build, push to GCR. "
            "(3) deploy — gcloud run deploy with latest image tag. "
            "Caching: use actions/cache for Gradle wrapper and dependencies. "
            "Secrets: GCP_SA_KEY in GitHub Secrets for authentication. "
            "Parallelism: run unit tests in matrix strategy (Java 21 × OS) for speed."
        ),
        solutions=[
            "Use actions/setup-java@v4 with distribution: temurin, java-version: 21",
            "Cache Gradle: key: gradle-${{ hashFiles('**/*.gradle.kts') }} to invalidate on build file change",
            "Use google-github-actions/auth for Workload Identity Federation (no JSON key needed)",
            "Add gradle test --tests to job output; upload test reports as artifacts",
            "Use environment protection rules for production deployment approval",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=134,
        context={
            "supremeai_workflows": ["firebase-hosting-merge.yml", "deploy-cloudrun.yml", "java-ci.yml", "code-quality.yml", "knowledge-reseed.yml"],
            "tip": "Use concurrency: group: ${{ github.ref }} to cancel in-flight runs on new push",
        },
    ),

    "docker_security": _learning(
        type_="PATTERN",
        category="DOCKER",
        content=(
            "Docker container security hardening: "
            "(1) Use non-root user: adduser -D appuser; USER appuser in Dockerfile. "
            "(2) Read-only filesystem: --read-only with tmpfs for /tmp. "
            "(3) Drop Linux capabilities: --cap-drop=ALL --cap-add=NET_BIND_SERVICE. "
            "(4) No new privileges: --security-opt=no-new-privileges. "
            "(5) Scan images: trivy image myapp:latest before pushing to registry. "
            "(6) Use distroless or Alpine base images — smaller attack surface. "
            "(7) Never run as root in production containers."
        ),
        solutions=[
            "Add to Dockerfile: RUN addgroup -S appgroup && adduser -S appuser -G appgroup",
            "Add USER appuser as second-to-last line before ENTRYPOINT/CMD",
            "Scan in CI: trivy image --exit-code 1 --severity HIGH,CRITICAL myapp:latest",
            "Use docker scout or Snyk Container for continuous vulnerability monitoring",
            "Sign images with Sigstore/cosign for supply chain security",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=78,
        context={
            "tool": "Trivy, Snyk, Docker Scout for image scanning",
            "distroless": "gcr.io/distroless/java21-debian12 — no shell, no package manager, minimal attack surface",
        },
    ),

    "observability_three_pillars": _learning(
        type_="PATTERN",
        category="OBSERVABILITY",
        content=(
            "Observability = Metrics + Logs + Traces (the three pillars). "
            "Metrics: time-series data — request rate, error rate, latency, saturation (USE method). "
            "Logs: timestamped events — structured JSON with request_id, user_id, operation, duration_ms. "
            "Traces: end-to-end request tracking across services — visualise bottlenecks. "
            "Golden Signals (Google SRE): Latency, Traffic, Errors, Saturation. "
            "Spring Boot + Micrometer: expose /actuator/prometheus endpoint; scrape with Prometheus; "
            "visualise in Grafana."
        ),
        solutions=[
            "Add spring-boot-starter-actuator + micrometer-registry-prometheus to expose metrics",
            "Use SLF4J MDC to add request_id and user_id to all log lines in the request context",
            "Add Micrometer Tracing (OpenTelemetry) + zipkin-reporter for distributed tracing",
            "Set up Grafana dashboards: latency P50/P95/P99, error rate, JVM heap, DB pool usage",
            "Use Cloud Logging log-based metrics for GCP-native alerting on error patterns",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=89,
        context={
            "spring_boot": "spring-boot-actuator + micrometer + OpenTelemetry = full observability stack",
            "gcp_tools": "Cloud Logging + Cloud Monitoring + Cloud Trace (OpenTelemetry compatible)",
        },
    ),

    "sre_sli_slo": _learning(
        type_="PATTERN",
        category="SRE",
        content=(
            "Site Reliability Engineering (SRE) — SLIs, SLOs, Error Budgets: "
            "SLI (Service Level Indicator): measurable metric — 'successful requests / total requests'. "
            "SLO (Service Level Objective): target for the SLI — 'availability >= 99.9%'. "
            "Error Budget: allowed downtime = 1 - SLO = 0.1% = 43.8 min/month. "
            "When error budget is exhausted: freeze new feature deployments until replenished. "
            "Common SLOs: availability 99.9%, latency P99 < 500ms, error rate < 0.1%."
        ),
        solutions=[
            "Define SLOs for every user-facing service before deploying to production",
            "Set SLO alerts at 50% and 90% burn rate to catch issues before budget exhaustion",
            "Calculate error budget: (1 - availability_target) × seconds_in_period",
            "Review SLOs quarterly — tighten as the service matures",
            "Implement SLO dashboards in Grafana with burn rate panels",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=44,
        context={
            "reference": "Google SRE Book: sre.google/sre-book",
            "tool": "Google Cloud Monitoring SLO tracking, Prometheus recording rules for burn rate",
        },
    ),

    "cloud_run_cold_start_optimization": _learning(
        type_="IMPROVEMENT",
        category="CLOUD_RUN",
        content=(
            "Spring Boot cold start optimisation for Cloud Run: "
            "(1) spring.main.lazy-initialization=true — only initialise beans on first use. "
            "(2) spring-context-indexer — pre-computes component scan result at build time. "
            "(3) Remove unused Spring Boot starters (each adds auto-configuration). "
            "(4) Use Spring Native (GraalVM) — compile to native binary, starts in < 100ms "
            "(but adds 10-20 min to build time). "
            "(5) Cloud Run --cpu-boost: extra CPU at startup. "
            "(6) Min instances=1: keeps one instance warm, zero cold starts."
        ),
        solutions=[
            "Add spring.main.lazy-initialization=true to application.properties",
            "Exclude unused auto-configurations: @SpringBootApplication(exclude={DataSourceAutoConfiguration.class})",
            "Enable context indexer: add spring-context-indexer to dependencies",
            "Use JVM flags: -XX:TieredStopAtLevel=1 for faster startup (less JIT optimisation)",
            "Set --min-instances=1 in Cloud Run — ~$10/month per always-on instance",
        ],
        severity="HIGH",
        confidence=0.92,
        times_applied=56,
        context={
            "typical_startup": "Spring Boot on Cloud Run: 3-8s cold start; <100ms warm request",
            "native": "Spring Native: <200ms cold start; requires GraalVM AOT compilation",
        },
    ),

    "kubernetes_hpa_scaling": _learning(
        type_="PATTERN",
        category="KUBERNETES",
        content=(
            "Horizontal Pod Autoscaler (HPA) for automatic scaling: "
            "Scale based on CPU: target 70% CPU utilisation → add/remove replicas. "
            "Scale based on custom metrics (KEDA): Kafka lag, queue depth, HTTP requests/second. "
            "Behaviour tuning: scale-up stabilisation 0s (fast), scale-down 300s (slow, prevent flapping). "
            "VPA (Vertical Pod Autoscaler): automatically adjust resource requests/limits — "
            "use in Recommend mode first to tune limits before enabling auto-apply."
        ),
        solutions=[
            "Set HPA minReplicas=2 for HA; maxReplicas based on cost budget",
            "Use KEDA for event-driven scaling: scale to 0 when queue is empty (Kafka, SQS, Pub/Sub)",
            "Set scale-down stabilizationWindowSeconds=300 to prevent excessive scale-down events",
            "Monitor HPA events: kubectl describe hpa <name> to see scaling decisions",
            "Target 60-70% CPU utilisation — headroom for traffic spikes before scale-out",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=47,
        context={
            "cloud_run_equivalent": "Cloud Run auto-scales based on concurrent requests — HPA-free",
            "keda": "Kubernetes Event-Driven Autoscaling — scale to zero for batch/event-driven workloads",
        },
    ),

    "cicd_gitops_strategy": _learning(
        type_="PATTERN",
        category="CICD",
        content=(
            "GitOps deployment strategy: the Git repository is the single source of truth for infrastructure. "
            "Principle: all changes are made via Git commits; reconciler (ArgoCD, Flux) syncs cluster to Git state. "
            "Workflow: developer PRs code → CI builds image + updates manifest repo → "
            "ArgoCD detects manifest change → syncs cluster → sends deployment notification. "
            "Benefits: full audit trail in Git, easy rollback (revert commit), "
            "environment parity, no manual kubectl apply."
        ),
        solutions=[
            "Use ArgoCD or Flux for Kubernetes GitOps; Cloud Deploy for GCP/Cloud Run GitOps",
            "Separate application repo (code) from manifest repo (Kubernetes YAML) for cleaner history",
            "Use Kustomize overlays: base + dev/staging/production overlays for environment differences",
            "Tag images with Git SHA: :main-abc1234 for immutable, traceable deployments",
            "Set up progressive delivery: Argo Rollouts or Flagger for canary/blue-green with metrics",
        ],
        severity="HIGH",
        confidence=0.91,
        times_applied=38,
        context={
            "tools": ["ArgoCD", "Flux CD", "Google Cloud Deploy", "Spinnaker"],
            "supremeai": "Cloud Run + Cloud Build + Cloud Deploy for GCP-native GitOps",
        },
    ),

    "infrastructure_as_code_terraform": _learning(
        type_="PATTERN",
        category="IAC",
        content=(
            "Terraform for Infrastructure as Code: "
            "Define Cloud Run, Firestore, IAM, networking in HCL (.tf files). "
            "State management: store terraform.tfstate in GCS bucket with locking. "
            "Modules: reusable infrastructure components (e.g., cloud-run-service module). "
            "Workspaces or directory separation for dev/staging/prod environments. "
            "Plan before apply: 'terraform plan' shows changes without applying. "
            "Drift detection: 'terraform plan' on CI to detect manual console changes."
        ),
        solutions=[
            "Backend config: store state in GCS bucket (gcs { bucket = 'supremeai-terraform-state' })",
            "Use google provider: provider 'google' { project = 'supremeai-a', region = 'asia-southeast1' }",
            "Resource google_cloud_run_v2_service for Cloud Run deployments",
            "Resource google_firestore_database for Firestore configuration",
            "Add terraform fmt + terraform validate + terraform plan to CI pipeline",
        ],
        severity="MEDIUM",
        confidence=0.90,
        times_applied=31,
        context={
            "alternatives": "Pulumi (Python/TypeScript), Google Cloud Deployment Manager, CDK for Terraform",
            "state_locking": "GCS backend supports state locking natively — prevents concurrent applies",
        },
    ),

    "improvement_deployment_checklist": _learning(
        type_="IMPROVEMENT",
        category="DEPLOYMENT",
        content=(
            "Pre-deployment checklist for production releases: "
            "(1) All tests pass in CI (unit, integration, e2e). "
            "(2) Security scan: no HIGH/CRITICAL vulnerabilities in image (Trivy). "
            "(3) Performance test: no regression in P95 latency. "
            "(4) Database migration tested in staging with production data copy. "
            "(5) Rollback plan documented: previous image tag, migration undo steps. "
            "(6) Stakeholder notified of deployment window. "
            "(7) Monitoring dashboards open during deployment. "
            "(8) Post-deploy smoke tests run automatically."
        ),
        solutions=[
            "Add deployment gate in GitHub Actions: require approval before production deploy",
            "Run automated smoke tests after deployment: health check + 3 key user flows",
            "Set up deployment tracking in PagerDuty/Opsgenie to correlate deploys with incidents",
            "Use feature flags to decouple deployment from feature release",
            "Keep deployment window < 30 minutes; rollback if error rate increases > 1%",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=89,
        context={"rollback": "Cloud Run: 'gcloud run services update-traffic supremeai-a --to-revisions=<prev>=100'"},
    ),
}

# ============================================================================
# DEVOPS_KNOWLEDGE rich topic documents
# ============================================================================

DEVOPS_KNOWLEDGE_DOCS = {

    "docker_guide": {
        "topic": "Docker — Complete Production Guide",
        "category": "DOCKER",
        "description": "Containerisation with Docker for production Spring Boot applications.",
        "dockerfile_template": (
            "# Stage 1: Build\n"
            "FROM eclipse-temurin:21-jdk-alpine AS builder\n"
            "WORKDIR /workspace\n"
            "COPY gradlew* ./\n"
            "COPY gradle gradle\n"
            "COPY build.gradle.kts settings.gradle.kts ./\n"
            "RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon\n"
            "COPY src src\n"
            "RUN ./gradlew bootJar -x test --no-daemon\n"
            "RUN java -Djarmode=layertools -jar build/libs/*.jar extract\n\n"
            "# Stage 2: Runtime\n"
            "FROM eclipse-temurin:21-jre-alpine\n"
            "RUN addgroup -S appgroup && adduser -S appuser -G appgroup\n"
            "WORKDIR /app\n"
            "COPY --from=builder /workspace/dependencies/ ./\n"
            "COPY --from=builder /workspace/spring-boot-loader/ ./\n"
            "COPY --from=builder /workspace/snapshot-dependencies/ ./\n"
            "COPY --from=builder /workspace/application/ ./\n"
            "USER appuser\n"
            "EXPOSE 8080\n"
            "ENTRYPOINT [\"java\",\"-XX:+UseContainerSupport\",\"-XX:MaxRAMPercentage=75.0\",\"org.springframework.boot.loader.launch.JarLauncher\"]\n"
        ),
        "docker_compose_dev": (
            "services:\n"
            "  api:\n"
            "    build: .\n"
            "    ports: ['8080:8080']\n"
            "    environment:\n"
            "      SPRING_PROFILES_ACTIVE: dev\n"
            "      DATABASE_URL: jdbc:postgresql://postgres:5432/supremeai\n"
            "    depends_on:\n"
            "      postgres:\n"
            "        condition: service_healthy\n"
            "  postgres:\n"
            "    image: postgres:16-alpine\n"
            "    environment: {POSTGRES_DB: supremeai, POSTGRES_USER: admin, POSTGRES_PASSWORD: secret}\n"
            "    healthcheck:\n"
            "      test: ['CMD-SHELL', 'pg_isready -U admin']\n"
            "      interval: 5s\n"
            "      retries: 5\n"
        ),
        "best_practices": [
            "Use multi-stage builds to minimise final image size",
            "Pin base image to digest: FROM eclipse-temurin@sha256:abc123 for reproducibility",
            "Never store secrets in ENV or ARG in Dockerfile — use runtime env vars",
            "Add HEALTHCHECK instruction for orchestrator health checking",
            "Use .dockerignore to exclude .git, build/, node_modules/ from build context",
        ],
        "confidence": 0.95,
    },

    "kubernetes_guide": {
        "topic": "Kubernetes — Production Deployment Guide",
        "category": "KUBERNETES",
        "description": "Deploying and operating Spring Boot microservices on Kubernetes.",
        "deployment_template": (
            "apiVersion: apps/v1\n"
            "kind: Deployment\n"
            "metadata:\n"
            "  name: supremeai-api\n"
            "spec:\n"
            "  replicas: 2\n"
            "  strategy:\n"
            "    type: RollingUpdate\n"
            "    rollingUpdate: {maxUnavailable: 0, maxSurge: 1}\n"
            "  template:\n"
            "    spec:\n"
            "      containers:\n"
            "      - name: api\n"
            "        image: gcr.io/supremeai-a/api:latest\n"
            "        resources:\n"
            "          requests: {cpu: '250m', memory: '256Mi'}\n"
            "          limits: {cpu: '1', memory: '512Mi'}\n"
            "        livenessProbe:\n"
            "          httpGet: {path: /actuator/health/liveness, port: 8080}\n"
            "          initialDelaySeconds: 60\n"
            "          periodSeconds: 10\n"
            "        readinessProbe:\n"
            "          httpGet: {path: /actuator/health/readiness, port: 8080}\n"
            "          initialDelaySeconds: 30\n"
            "          periodSeconds: 5\n"
            "        startupProbe:\n"
            "          httpGet: {path: /actuator/health, port: 8080}\n"
            "          failureThreshold: 30\n"
            "          periodSeconds: 10\n"
        ),
        "key_objects": {
            "Pod": "Smallest deployable unit; one or more containers sharing network/storage",
            "Deployment": "Manages ReplicaSet; handles rolling updates and rollbacks",
            "Service": "Stable network endpoint for a set of pods (ClusterIP/NodePort/LoadBalancer)",
            "Ingress": "HTTP routing rules; TLS termination; path-based routing",
            "ConfigMap": "Non-secret configuration (app settings, feature flags)",
            "Secret": "Sensitive data (passwords, tokens) — base64 encoded + encrypted at rest",
            "HPA": "Auto-scales replicas based on CPU/memory/custom metrics",
            "PDB": "PodDisruptionBudget — prevents too many pods being evicted at once",
        },
        "confidence": 0.94,
    },

    "github_actions_guide": {
        "topic": "GitHub Actions — CI/CD Complete Guide",
        "category": "CICD",
        "description": "Building production-grade CI/CD pipelines with GitHub Actions.",
        "workflow_template": (
            "name: CI/CD Pipeline\n"
            "on:\n"
            "  push:\n"
            "    branches: [main]\n"
            "  pull_request:\n"
            "    branches: [main]\n"
            "concurrency:\n"
            "  group: ${{ github.ref }}\n"
            "  cancel-in-progress: true\n"
            "jobs:\n"
            "  test:\n"
            "    runs-on: ubuntu-latest\n"
            "    steps:\n"
            "      - uses: actions/checkout@v4\n"
            "      - uses: actions/setup-java@v4\n"
            "        with: {distribution: temurin, java-version: '21'}\n"
            "      - uses: actions/cache@v4\n"
            "        with:\n"
            "          path: ~/.gradle/caches\n"
            "          key: gradle-${{ hashFiles('**/*.gradle.kts') }}\n"
            "      - run: ./gradlew test --no-daemon\n"
            "      - uses: actions/upload-artifact@v4\n"
            "        if: always()\n"
            "        with: {name: test-results, path: build/reports/tests/}\n"
            "  deploy:\n"
            "    needs: test\n"
            "    if: github.ref == 'refs/heads/main'\n"
            "    runs-on: ubuntu-latest\n"
            "    permissions:\n"
            "      id-token: write\n"
            "      contents: read\n"
            "    steps:\n"
            "      - uses: actions/checkout@v4\n"
            "      - uses: google-github-actions/auth@v2\n"
            "        with: {workload_identity_provider: ${{ secrets.WIF_PROVIDER }}}\n"
            "      - run: gcloud run deploy supremeai-a --image=gcr.io/... --region=asia-southeast1\n"
        ),
        "best_practices": [
            "Use concurrency groups to cancel stale in-progress runs on new push",
            "Use Workload Identity Federation instead of JSON service account keys",
            "Cache Gradle/Maven dependencies with hashFiles-based cache keys",
            "Separate test, build, and deploy jobs with proper dependencies (needs:)",
            "Use environment secrets and protection rules for production deployments",
            "Upload test reports and coverage as artifacts for PR inspection",
            "Use matrix builds to test across multiple Java versions or OS targets",
        ],
        "confidence": 0.95,
    },

    "observability_guide": {
        "topic": "Observability — Metrics, Logs, Traces",
        "category": "OBSERVABILITY",
        "description": "Implementing production observability for Spring Boot on GCP.",
        "metrics": {
            "setup": "spring-boot-starter-actuator + micrometer-registry-prometheus",
            "endpoint": "/actuator/prometheus scraped by Prometheus every 15s",
            "key_metrics": {
                "http_server_requests": "Request rate, latency, error rate by endpoint",
                "hikaricp_connections": "DB connection pool utilisation",
                "jvm_memory_used": "JVM heap and non-heap usage",
                "process_cpu_usage": "CPU utilisation",
                "system_load_average": "System load",
            },
            "custom_metric": "@Timed('order.processing') on service methods; Counter/Gauge/Timer beans",
        },
        "logging": {
            "format": "JSON structured logging with logstash-logback-encoder",
            "required_fields": ["timestamp", "level", "requestId", "userId", "operation", "durationMs", "message"],
            "mdc_setup": "MDCFilter adds requestId to MDC on every request; cleared after response",
            "levels": {
                "DEBUG": "Dev noise — SQL queries, method entry/exit",
                "INFO": "Business events — order placed, user registered",
                "WARN": "Recoverable issues — retry attempt, slow query",
                "ERROR": "Failures requiring investigation — exception with stack trace",
            },
            "gcp": "Cloud Logging auto-parses JSON logs; use severity field not level",
        },
        "tracing": {
            "library": "Micrometer Tracing + OpenTelemetry SDK",
            "backends": ["Jaeger", "Zipkin", "Google Cloud Trace", "Tempo"],
            "propagation": "W3C TraceContext (traceparent header) across service boundaries",
            "spring_boot": "micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp",
        },
        "alerting": {
            "error_rate": "Alert if error rate > 1% for 5 minutes",
            "latency": "Alert if P95 latency > 500ms for 5 minutes",
            "availability": "Alert if health check fails for 2 consecutive checks",
            "tool": "Cloud Monitoring alerting policies / Grafana alerts / PagerDuty",
        },
        "confidence": 0.94,
    },

    "cloud_run_guide": {
        "topic": "Google Cloud Run — Complete Configuration Guide",
        "category": "CLOUD_RUN",
        "description": "Running SupremeAI API on Google Cloud Run — the primary deployment platform.",
        "configuration": {
            "memory": "512Mi (Spring Boot light); 1Gi (Spring Boot with AI); 2Gi (AI inference)",
            "cpu": "1 vCPU (default); 2 vCPU for CPU-intensive; set CPU always allocated for latency",
            "concurrency": "80 requests per instance (default); reduce for CPU-heavy tasks",
            "timeout": "300s max per request",
            "min_instances": "0 (auto-scales to 0 for cost); 1 (always warm for latency)",
            "max_instances": "100 (set limit to prevent runaway cost)",
        },
        "deploy_command": (
            "gcloud run deploy supremeai-a \\\n"
            "  --image=gcr.io/supremeai-a/api:${GITHUB_SHA} \\\n"
            "  --region=asia-southeast1 \\\n"
            "  --platform=managed \\\n"
            "  --allow-unauthenticated \\\n"
            "  --memory=1Gi \\\n"
            "  --cpu=1 \\\n"
            "  --min-instances=1 \\\n"
            "  --max-instances=100 \\\n"
            "  --concurrency=80 \\\n"
            "  --set-env-vars=SPRING_PROFILES_ACTIVE=prod \\\n"
            "  --set-secrets=JWT_SECRET=jwt-secret:latest \\\n"
            "  --service-account=supremeai-sa@supremeai-a.iam.gserviceaccount.com"
        ),
        "traffic_splitting": {
            "canary": "gcloud run services update-traffic supremeai-a --to-revisions=NEW=10,OLD=90",
            "full_cutover": "gcloud run services update-traffic supremeai-a --to-latest",
            "rollback": "gcloud run services update-traffic supremeai-a --to-revisions=PREV=100",
        },
        "iam": {
            "unauthenticated": "roles/run.invoker on allUsers for public APIs",
            "service_account": "Assign SA with: roles/datastore.user, roles/secretmanager.secretAccessor",
            "workload_identity": "No JSON key needed — SA bound to Cloud Run service identity",
        },
        "confidence": 0.96,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 5 — DevOps & Cloud",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "devops_knowledge": DEVOPS_KNOWLEDGE_DOCS,
        },
    )
