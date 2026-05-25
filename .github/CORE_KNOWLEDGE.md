# SupremeAI Core Knowledge & Standards

## 1. Deployment Standards (Cloud Run)
- **Zero-Downtime:** All deployments must use the `canary` (traffic shifting) strategy.
- **Traffic Shifting:** New revisions must be tagged `new-revision`, health-checked, shifted to 10% for observation, then promoted to 100%.
- **Security:** Never use Service Account Key JSONs. Always use **Workload Identity Federation (WIF)**.
- **Networking:** Internal health checks must target the specific tagged Revision URL, not the global service URL.

## 2. Java / Spring Boot Standards
- **Java Version:** Always use JDK 21 (Temurin distribution).
- **Build Tool:** Gradle with `--no-daemon` in CI.
- **Optimization:** 
    - Use **Layered Jars** in Docker to optimize cache hits.
    - Use **AppCDS (Class Data Sharing)** to reduce startup time (Cold Starts).
    - Set `MaxRAMPercentage=75.0` to respect container memory limits.

## 3. Dockerfile Requirements
- **Security:** Always run as a non-root user (`spring`).
- **Base Image:** Use `eclipse-temurin:21-jre-jammy`.
- **Cleaning:** Always `rm -rf /var/lib/apt/lists/*` after installing packages to keep images small.

## 4. GitHub Actions CI/CD Rules
- **Caching:** Gradle caches must be restored *before* the build starts.
- **Error Handling:** Use `set -euo pipefail` in bash scripts to ensure failures stop the pipeline immediately.
- **Cleanup:** Post-deployment should leave the environment clean (e.g., untagging old revisions if necessary).

## 5. Error Detection Checklist
1. Does the change skip tests? (Warning)
2. Are there hardcoded Project IDs or Regions? (Error)
3. Does the Dockerfile run as root? (Security Error)
4. Is the health check robust with retries? (Reliability Warning)
5. Are dependencies cached? (Performance Warning)
6. Is the image built once and pushed, or rebuilt multiple times? (Efficiency Error)

## 6. AI Performance & Observability
- **Latency Monitoring:** Track TTFT (Time To First Token) and total response time.
- **Model Versioning:** Always tag AI models separately from infrastructure changes.
- **Feedback Loop:** Implement a basic user feedback (thumbs up/down) tracking mechanism to evaluate AI quality post-deployment.

## 7. AI Intelligence & Model Strategy
- **RAG First:** Prioritize Retrieval-Augmented Generation over generic prompting for factual accuracy. Use high-quality vector embeddings.
- **Agentic Design:** Break complex logic into smaller, verifiable tasks handled by specialized agents (Chain-of-Thought).
- **Continuous Evaluation:** Use automated "Evals" (LLM-as-a-judge) to score model responses against a ground-truth dataset.
- **Hybrid Architecture:** Use smaller, faster models for simple tasks and "expensive" models (Gemini 1.5 Pro/Claude 3.5) only for complex reasoning to balance intelligence and cost.
- **Prompt Versioning:** Treat prompts as code. Version them and test them against regression before production rollout.

## 9. Frameworks & Observability Standards
- **Framework:** Use **LangChain4j** for Java/Spring Boot integration to manage LLM interactions.
- **Observability:** Enable **LangSmith** for tracing every request. This is mandatory for debugging hallucinations.
- **Evaluation:** Implement "Evaluators" in LangSmith to check for relevance, toxicity, and factual accuracy.

## 8. Development Principles
- **Fail Fast:** If an AI confidence score is low, fallback to a human-in-the-loop or a safe default instead of hallucinating.
- **Data Privacy:** Scrub PII (Personally Identifiable Information) before sending data to external LLM APIs.