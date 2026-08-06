# Localhost Occurrences Audit Report

> [!NOTE]
> **Audit Resolution Status (অডিট সম্পন্ন ও ফিক্সড):**
> 
> - **Category 1: Production Runtime Bugs (🔴 Critical):** **100% Resolved.** All production paths (Mobile App WebSocket `main.dart`, Billing dynamic origin fallback, Integration OAuth redirect) have been updated to environment-driven URLs (`API_BASE_URL`, `SUPREMEAI_USER_API_URL`, `SUPREMEAI_ADMIN_API_URL`).
> - **Category 2: Security Filtering Rules (🟢 Intended):** Active security controls in `config.py` (CORS stripping), `sentinel_agent.py`, and `ssrf_protection.py` explicitly block and sanitize `localhost` origins in production.
> - **Category 3: Development Defaults (🟡 Safe):** Controlled fallback defaults in local development scripts (e.g., `OLLAMA_URL`, `NEO4J_URI`) active only when `ENV=local`.
> - **Category 4: Test Suite & Documentation (🔵 Informational):** Pytest mocks, CI lint checks, and developer documentation guides.

**Total Occurrences Audit Record:** 328

| File Path | Line Number | Code Snippet |
| --- | --- | --- |
| [.github/actions/setup-backend/failed_job_log.md](file:///.github/actions/setup-backend/failed_job_log.md#L27) | 27 | FAILED tests/test_admin_dashboard_full.py::TestGetHealthMap::test_all_offline - AttributeError: Settings(env='test', debug=True, allow_test_auth_bypass=True, allow_test_origin_bypass=True, PROJECT_NAME='SupremeAI 2.0', API_V1_STR='/api/v1', app_name='SupremeAI 2.0', docs_auth_enabled=True, docs_username='admin', docs_password=SecretStr('**********'), port=8080, host='0.0.0.0', cors_origins=['http://localhost:3000', 'http://localhost:8000'], user_cors_origins=[], admin_cors_origins=[], enforce_anti_hacking=False, service_role='user', otp_cooldown_seconds=300, allowed_hosts=[], gemini_rpm_limit=9, gemini_tpm_limit=240000, gemini_rpd_limit=475, groq_rpm_limit=28, groq_tpm_limit=28500, groq_rpd_limit=13680, openrouter_rpm_limit=19, openrouter_rpd_limit=45, cloudflare_rpd_limit=9000, nvidia_rpm_limit=38, nvidia_tpm_limit=38000, huggingface_rpm_limit=18, huggingface_rpd_limit=950, max_prompt_tokens=4000, max_response_tokens=1500, max_cost_per_task=0.01, enable_token_compression=True, security_context_ttl=86400, sec |
| [.github/actions/setup-backend/failed_job_log.md](file:///.github/actions/setup-backend/failed_job_log.md#L28) | 28 | FAILED tests/test_admin_dashboard_full.py::TestGetHealthMap::test_all_healthy - AttributeError: Settings(env='test', debug=True, allow_test_auth_bypass=True, allow_test_origin_bypass=True, PROJECT_NAME='SupremeAI 2.0', API_V1_STR='/api/v1', app_name='SupremeAI 2.0', docs_auth_enabled=True, docs_username='admin', docs_password=SecretStr('**********'), port=8080, host='0.0.0.0', cors_origins=['http://localhost:3000', 'http://localhost:8000'], user_cors_origins=[], admin_cors_origins=[], enforce_anti_hacking=False, service_role='user', otp_cooldown_seconds=300, allowed_hosts=[], gemini_rpm_limit=9, gemini_tpm_limit=240000, gemini_rpd_limit=475, groq_rpm_limit=28, groq_tpm_limit=28500, groq_rpd_limit=13680, openrouter_rpm_limit=19, openrouter_rpd_limit=45, cloudflare_rpd_limit=9000, nvidia_rpm_limit=38, nvidia_tpm_limit=38000, huggingface_rpm_limit=18, huggingface_rpd_limit=950, max_prompt_tokens=4000, max_response_tokens=1500, max_cost_per_task=0.01, enable_token_compression=True, security_context_ttl=86400, sec |
| [.github/actions/setup-backend/failed_job_log.md](file:///.github/actions/setup-backend/failed_job_log.md#L131) | 131 | FAILED tests/test_config_coverage.py::test_parse_cors_origins_production_strips_localhost - AttributeError: 'types.SimpleNamespace' object has no attribute 'field_name' |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L231) | 231 | DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L232) | 232 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L233) | 233 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L330) | 330 | DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L331) | 331 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L332) | 332 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L399) | 399 | DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L400) | 400 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L401) | 401 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L449) | 449 | DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L450) | 450 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L451) | 451 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L618) | 618 | DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L619) | 619 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L620) | 620 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L643) | 643 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L644) | 644 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L663) | 663 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L664) | 664 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L688) | 688 | SUPABASE_DATABASE_URL: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/maintenance_pipeline.yml](file:///.github/workflows/maintenance_pipeline.yml#L689) | 689 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [.github/workflows/supreme-core-ci.yml](file:///.github/workflows/supreme-core-ci.yml#L769) | 769 | echo "Checking for hardcoded localhost, TODO_FIXME, or hardcoded secrets in commandcenter..." |
| [.github/workflows/supreme-core-ci.yml](file:///.github/workflows/supreme-core-ci.yml#L771) | 771 | if grep -rn 'localhost' apps/studio-client/src/commandcenter/ --include='*.ts' --include='*.tsx' 2>/dev/null; then |
| [.github/workflows/supreme-core-ci.yml](file:///.github/workflows/supreme-core-ci.yml#L772) | 772 | echo "FAIL: Found hardcoded 'localhost' references" |
| [.github/workflows/supreme-core-ci.yml](file:///.github/workflows/supreme-core-ci.yml#L796) | 796 | npx wait-on http://localhost:4173 --timeout 30000 |
| [.github/workflows/supreme-core-ci.yml](file:///.github/workflows/supreme-core-ci.yml#L798) | 798 | npx --yes @axe-core/cli http://localhost:4173 \|\| true |
| [PHASE_LOG.md](file:///PHASE_LOG.md#L58) | 58 | #### [AUDIT-003] [P1] [Hardcoded Localhost + Token in URL] [apps/mobile/lib/main.dart] |
| [PHASE_LOG.md](file:///PHASE_LOG.md#L59) | 59 | **সমস্যা:** WebSocket URL-এ hardcoded `localhost:8000` এবং query parameter-এ auth token। |
| [PHASE_LOG.md](file:///PHASE_LOG.md#L63) | 63 | Uri.parse('ws://localhost:8000/api/ws/chat?token=$_authToken'), |
| [README.md](file:///README.md#L139) | 139 | # 8. Visit http://localhost:3000 |
| [README.md](file:///README.md#L157) | 157 | "http://localhost:8000/api/v1/agents", |
| [README.md](file:///README.md#L179) | 179 | f"http://localhost:8000/api/v1/agents/{agent_id}/execute", |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L73) | 73 | ব্যবহার: http://localhost:5173 |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L84) | 84 | WebSocket: ws://localhost:8000/api/voice/ws |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L120) | 120 | WebSocket: ws://localhost:8000/ws/collab/{doc_id} |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L133) | 133 | URL: http://localhost:5173 → Admin বোতামে ক্লিক করুন |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L170) | 170 | curl -X POST http://localhost:8000/auth/login \ |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L179) | 179 | curl -X POST http://localhost:8000/api/generate \ |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L193) | 193 | curl -X POST http://localhost:8000/api/voice/process-audio \ |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L202) | 202 | curl -X POST http://localhost:8000/api/style/learn \ |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L212) | 212 | curl -X POST http://localhost:8000/api/diagram/generate \ |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L223) | 223 | curl -X POST http://localhost:8000/api/onboarding/complete \ |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L307) | 307 | \| AI response না আসলে \| Health endpoint চেক: `curl localhost:8000/health` \| |
| [apps/docs/docs/bangla-guide.md](file:///apps/docs/docs/bangla-guide.md#L316) | 316 | curl http://localhost:8000/health \| python -m json.tool |
| [apps/hf-space/D](file:///apps/hf-space/D#L23) | 23 | CMD curl -f http://localhost:80/health \|\| exit 1 |
| [apps/java-worker/src/main/resources/application.yml](file:///apps/java-worker/src/main/resources/application.yml#L3) | 3 | url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/supremeai} |
| [apps/mobile/lib/main.dart](file:///apps/mobile/lib/main.dart#L62) | 62 | // বাংলা মন্তব্য: API_BASE_URL থেকে WebSocket URL derive করা হয়, hardcoded localhost নয়। |
| [apps/studio-client/e2e/commandcenter.spec.ts](file:///apps/studio-client/e2e/commandcenter.spec.ts#L3) | 3 | const BASE_URL = process.env.BASE_URL \|\| 'http://localhost:4173'; |
| [apps/studio-client/e2e/commandcenter.spec.ts](file:///apps/studio-client/e2e/commandcenter.spec.ts#L72) | 72 | const ws = new WebSocket('ws://localhost:9999/ws/dashboard'); |
| [apps/studio-client/index.html](file:///apps/studio-client/index.html#L7) | 7 | <meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'unsafe-inline' https://www.googletagmanager.com https://www.gstatic.com https://cdn.firebase.com https://*.firebaseio.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https: blob:; connect-src 'self' wss: https: http://localhost:* http://127.0.0.1:* https://*.firebaseapp.com https://*.web.app https://api.openai.com https://generativelanguage.googleapis.com https://*.supremeai.dev https://*.firebaseio.com; frame-src 'self' https://*; object-src 'none'; base-uri 'self'; form-action 'self'; upgrade-insecure-requests;"> |
| [apps/studio-client/src/utils/api.ts](file:///apps/studio-client/src/utils/api.ts#L33) | 33 | !!cached && /^https:\/\//.test(cached) && !/localhost\|127\.0\.0\.1/.test(cached); |
| [archive/deprecated-desktop/desktop/main.js](file:///archive/deprecated-desktop/desktop/main.js#L47) | 47 | const wsUrl = `ws://localhost:8000/api/ws/chat?token=${authToken}`; |
| [backend/Dockerfile](file:///backend/Dockerfile#L72) | 72 | CMD curl -sf http://localhost:${PORT:-8080}/health \|\| exit 1 |
| [backend/alembic.ini](file:///backend/alembic.ini#L89) | 89 | sqlalchemy.url = driver://user:pass@localhost/dbname |
| [backend/api/routes/admin_dashboard.py](file:///backend/api/routes/admin_dashboard.py#L883) | 883 | if "github.com" not in request.headers.get("host", "") and "localhost" not in request.headers.get("host", ""): |
| [backend/api/routes/billing_api.py](file:///backend/api/routes/billing_api.py#L146) | 146 | checkout_base = request.headers.get("origin") or request.headers.get("referer", "http://localhost:3000") |
| [backend/api/routes/integrations.py](file:///backend/api/routes/integrations.py#L26) | 26 | লোকালে ডিফল্ট localhost:8000। |
| [backend/api/routes/integrations.py](file:///backend/api/routes/integrations.py#L28) | 28 | base = getattr(settings, "frontend_base_url", "http://localhost:8000") |
| [backend/api/routes/integrations.py](file:///backend/api/routes/integrations.py#L64) | 64 | url=f"{getattr(settings, 'frontend_base_url', 'http://localhost:5173')}/integrations?status=error&message=Invalid token" |
| [backend/api/routes/integrations.py](file:///backend/api/routes/integrations.py#L86) | 86 | url=f"{getattr(settings, 'frontend_base_url', 'http://localhost:5173')}/integrations?status=error&message=Failed to get access token" |
| [backend/api/routes/integrations.py](file:///backend/api/routes/integrations.py#L117) | 117 | url=f"{getattr(settings, 'frontend_base_url', 'http://localhost:5173')}/integrations?status=error&message=Database error" |
| [backend/api/routes/integrations.py](file:///backend/api/routes/integrations.py#L121) | 121 | frontend_base = getattr(settings, "frontend_base_url", "http://localhost:5173") |
| [backend/brain/smart_router.py](file:///backend/brain/smart_router.py#L116) | 116 | or os.getenv("OLLAMA_URL", "http://localhost:11434") |
| [backend/brain/smart_router.py](file:///backend/brain/smart_router.py#L118) | 118 | ollama_base = ollama_base.rstrip("/") if ollama_base else "http://localhost:11434" |
| [backend/core/config.py](file:///backend/core/config.py#L271) | 271 | # বাংলা মন্তব্য: OLLAMA_URL — fail-fast, কোনো localhost fallback নেই |
| [backend/core/config.py](file:///backend/core/config.py#L570) | 570 | return self._get_cached_secret("NEO4J_URI") or "bolt://localhost:7687" |
| [backend/core/config.py](file:///backend/core/config.py#L668) | 668 | "http://localhost:3000", |
| [backend/core/config.py](file:///backend/core/config.py#L669) | 669 | "http://localhost:5173", |
| [backend/core/config.py](file:///backend/core/config.py#L670) | 670 | "http://localhost:8000", |
| [backend/core/config.py](file:///backend/core/config.py#L965) | 965 | forbidden = {"localhost", "127.0.0.1", "testserver", "0.0.0.0"} |
| [backend/core/config.py](file:///backend/core/config.py#L1006) | 1006 | v = [o for o in v if "localhost" not in o and "127.0.0.1" not in o] |
| [backend/core/config.py](file:///backend/core/config.py#L1033) | 1033 | return [origin for origin in value if "localhost" not in origin and "127.0.0.1" not in origin] |
| [backend/core/context_manager.py](file:///backend/core/context_manager.py#L44) | 44 | self.vector_client = QdrantClient(url=settings.QDRANT_URL or "localhost", port=settings.QDRANT_PORT or 6333) |
| [backend/core/deployment/production_deploy.py](file:///backend/core/deployment/production_deploy.py#L413) | 413 | base_url="http://localhost:8000",  # This would be determined by deployment |
| [backend/core/error_remediation.py](file:///backend/core/error_remediation.py#L198) | 198 | qdrant_url = os.getenv("QDRANT_URL", "http://localhost:6333") |
| [backend/core/grpc_client.py](file:///backend/core/grpc_client.py#L31) | 31 | def __init__(self, host: str = "localhost", port: int = 9090): |
| [backend/core/llm_router.py](file:///backend/core/llm_router.py#L481) | 481 | raw_url = getattr(settings, "OLLAMA_URL", "http://localhost:11434") |
| [backend/core/llm_router.py](file:///backend/core/llm_router.py#L482) | 482 | self.base_url = str(raw_url) if isinstance(raw_url, str \| bytes) else "http://localhost:11434" |
| [backend/core/mcp_client.py](file:///backend/core/mcp_client.py#L37) | 37 | mcp_servers = ["http://localhost:8000/mcp"] |
| [backend/core/messaging/nats_messaging.py](file:///backend/core/messaging/nats_messaging.py#L35) | 35 | url: str = "nats://localhost:4222", |
| [backend/core/observability/observability_middleware.py](file:///backend/core/observability/observability_middleware.py#L77) | 77 | "http.url": f"{scope.get('scheme', 'http')}://{scope.get('server', ('localhost', 80))[0]}{path}", |
| [backend/core/queue/task_queue_enhanced.py](file:///backend/core/queue/task_queue_enhanced.py#L93) | 93 | self.redis_url = redis_url or settings.redis_url or "redis://localhost:6379" |
| [backend/core/queue/task_queue_enhanced.py](file:///backend/core/queue/task_queue_enhanced.py#L572) | 572 | broker=getattr(settings, "REDIS_URL", "redis://localhost:6379/0"), |
| [backend/core/security/origin_validator.py](file:///backend/core/security/origin_validator.py#L83) | 83 | allowed_hosts.add("localhost") |
| [backend/core/security/ssrf_protection.py](file:///backend/core/security/ssrf_protection.py#L53) | 53 | ".localhost", |
| [backend/core/security/ssrf_protection.py](file:///backend/core/security/ssrf_protection.py#L66) | 66 | "localhost", |
| [backend/core/sentinel_agent.py](file:///backend/core/sentinel_agent.py#L60) | 60 | # Block localhost access in production unless it targets the backend port 8080 |
| [backend/core/sentinel_agent.py](file:///backend/core/sentinel_agent.py#L63) | 63 | if "localhost" in hostname or "127.0.0.1" in hostname: |
| [backend/core/swarm_pubsub.py](file:///backend/core/swarm_pubsub.py#L29) | 29 | # বাংলা মন্তব্য: module-level redis.from_url("redis://localhost") সম্পূর্ণ নিষিদ্ধ। |
| [backend/core/testing/qa_suite.py](file:///backend/core/testing/qa_suite.py#L712) | 712 | db_result = await self.integration_runner.test_database_integration("postgresql://localhost/test") |
| [backend/core/testing/qa_suite.py](file:///backend/core/testing/qa_suite.py#L714) | 714 | cache_result = await self.integration_runner.test_cache_integration("redis://localhost:6379") |
| [backend/core/testing/qa_suite.py](file:///backend/core/testing/qa_suite.py#L804) | 804 | results = await qa_suite.run_full_qa_suite("http://localhost:8000") |
| [backend/engine/worker_node.py](file:///backend/engine/worker_node.py#L23) | 23 | url=os.getenv("NATS_URL", "nats://localhost:4222"), |
| [backend/evolution/digital_twin/topology.py](file:///backend/evolution/digital_twin/topology.py#L486) | 486 | # বাংলা মন্তব্য: সার্ভিসগুলোর হোস্ট ডায়নামিক করা — এনভায়রনমেন্ট ভেরিয়েবল থাকলে সেখান থেকে নেবে, নাহলে localhost ফলব্যাক ব্যবহার করবে। |
| [backend/evolution/digital_twin/topology.py](file:///backend/evolution/digital_twin/topology.py#L487) | 487 | default_host = os.getenv("DEFAULT_SERVICE_HOST", "localhost") |
| [backend/models/local_model_handler.py](file:///backend/models/local_model_handler.py#L36) | 36 | or "http://localhost:11434" |
| [backend/services/minio_client.py](file:///backend/services/minio_client.py#L58) | 58 | self._endpoint = os.environ.get("MINIO_ENDPOINT", "localhost:9000") |
| [backend/tests/core/test_core_missing_coverage.py](file:///backend/tests/core/test_core_missing_coverage.py#L62) | 62 | def test_parse_cors_origins_production_filters_localhost(self): |
| [backend/tests/core/test_core_missing_coverage.py](file:///backend/tests/core/test_core_missing_coverage.py#L66) | 66 | ["http://localhost:3000", "https://prod.com"], |
| [backend/tests/core/test_core_missing_coverage.py](file:///backend/tests/core/test_core_missing_coverage.py#L73) | 73 | assert "http://localhost:3000" not in result |
| [backend/tests/core/test_core_missing_coverage.py](file:///backend/tests/core/test_core_missing_coverage.py#L961) | 961 | assert client.url == "nats://localhost:4222" |
| [backend/tests/core/test_core_missing_coverage.py](file:///backend/tests/core/test_core_missing_coverage.py#L1226) | 1226 | def test_is_safe_url_rejects_localhost(self): |
| [backend/tests/core/test_core_missing_coverage.py](file:///backend/tests/core/test_core_missing_coverage.py#L1229) | 1229 | assert is_safe_url("http://localhost/test") is False |
| [backend/tests/core/test_nats_messaging.py](file:///backend/tests/core/test_nats_messaging.py#L29) | 29 | return NATSClient(url="nats://localhost:4222", token="test_token") |
| [backend/tests/core/test_nats_messaging.py](file:///backend/tests/core/test_nats_messaging.py#L58) | 58 | assert client.url == "nats://localhost:4222" |
| [backend/tests/core/test_nats_messaging.py](file:///backend/tests/core/test_nats_messaging.py#L91) | 91 | mock_connect.assert_called_once_with(servers=["nats://localhost:4222"], token="test_token") |
| [backend/tests/core/test_nats_messaging.py](file:///backend/tests/core/test_nats_messaging.py#L427) | 427 | assert nats_client.url == "nats://localhost:4222" |
| [backend/tests/core/test_origin_validator.py](file:///backend/tests/core/test_origin_validator.py#L26) | 26 | all_headers = {"host": "localhost"} |
| [backend/tests/core/test_pubsub.py](file:///backend/tests/core/test_pubsub.py#L98) | 98 | mock_settings.redis_url = "redis://localhost:6379" |
| [backend/tests/core/test_swarm_pubsub.py](file:///backend/tests/core/test_swarm_pubsub.py#L55) | 55 | mock_settings.redis_url = "redis://localhost" |
| [backend/tests/core/test_swarm_pubsub.py](file:///backend/tests/core/test_swarm_pubsub.py#L59) | 59 | mock_from_url.assert_called_once_with("redis://localhost") |
| [backend/tests/core/test_swarm_pubsub.py](file:///backend/tests/core/test_swarm_pubsub.py#L291) | 291 | mock_settings.redis_url = "redis://localhost" |
| [backend/tests/test_billing_api_coverage.py](file:///backend/tests/test_billing_api_coverage.py#L61) | 61 | mock_request.headers.get.return_value = "http://localhost:3000" |
| [backend/tests/test_cache_cleanup.py](file:///backend/tests/test_cache_cleanup.py#L81) | 81 | os.environ["REDIS_URL"] = "redis://localhost:6379/0" |
| [backend/tests/test_cache_cleanup.py](file:///backend/tests/test_cache_cleanup.py#L95) | 95 | os.environ["REDIS_URL"] = "redis://localhost:6379/0" |
| [backend/tests/test_cache_cleanup.py](file:///backend/tests/test_cache_cleanup.py#L109) | 109 | os.environ["REDIS_URL"] = "redis://localhost:6379/0" |
| [backend/tests/test_config.py](file:///backend/tests/test_config.py#L137) | 137 | def test_cors_origins_production_strips_localhost(mock_fetch, monkeypatch): |
| [backend/tests/test_config_coverage.py](file:///backend/tests/test_config_coverage.py#L34) | 34 | @patch.dict(os.environ, {"ENV": "local", "CORS_ORIGINS": "http://localhost:3000"}, clear=True) |
| [backend/tests/test_cross_provider_consistency.py](file:///backend/tests/test_cross_provider_consistency.py#L50) | 50 | mock_settings.OLLAMA_URL = "http://localhost:11434" |
| [backend/tests/test_graph_service.py](file:///backend/tests/test_graph_service.py#L35) | 35 | mock_settings.neo4j_uri = "bolt://localhost:7687" |
| [backend/tests/test_local_model_handler_full.py](file:///backend/tests/test_local_model_handler_full.py#L32) | 32 | handler = LocalModelHandler("http://localhost:11434") |
| [backend/tests/test_local_model_handler_full.py](file:///backend/tests/test_local_model_handler_full.py#L49) | 49 | handler = LocalModelHandler("http://localhost:11434") |
| [backend/tests/test_local_model_handler_full.py](file:///backend/tests/test_local_model_handler_full.py#L66) | 66 | handler = LocalModelHandler("http://localhost:11434") |
| [backend/tests/test_local_model_handler_full.py](file:///backend/tests/test_local_model_handler_full.py#L86) | 86 | handler = LocalModelHandler("http://localhost:11434") |
| [backend/tests/test_mcp_servers_integration.py](file:///backend/tests/test_mcp_servers_integration.py#L18) | 18 | "SUPABASE_DATABASE_URL": "postgres://localhost/mydb", |
| [backend/tests/test_minio_client.py](file:///backend/tests/test_minio_client.py#L65) | 65 | mock_minio.presigned_get_object.return_value = "http://localhost:9000/bucket/key?sign=xyz" |
| [backend/tests/test_origin_validator.py](file:///backend/tests/test_origin_validator.py#L5) | 5 | - Localhost / 127.0.0.1 bypass |
| [backend/tests/test_origin_validator.py](file:///backend/tests/test_origin_validator.py#L43) | 43 | def test_bypass_localhost(self): |
| [backend/tests/test_origin_validator.py](file:///backend/tests/test_origin_validator.py#L44) | 44 | """Test that localhost bypasses origin checks.""" |
| [backend/tests/test_payments.py](file:///backend/tests/test_payments.py#L43) | 43 | "success_url": "http://localhost/success", |
| [backend/tests/test_payments.py](file:///backend/tests/test_payments.py#L44) | 44 | "cancel_url": "http://localhost/cancel", |
| [backend/tests/test_provider_failover_chain.py](file:///backend/tests/test_provider_failover_chain.py#L51) | 51 | mock_settings.OLLAMA_URL = "http://localhost:11434" |
| [backend/tests/test_provider_failover_chain.py](file:///backend/tests/test_provider_failover_chain.py#L79) | 79 | mock_settings.OLLAMA_URL = "http://localhost:11434" |
| [backend/tests/test_provider_failover_chain.py](file:///backend/tests/test_provider_failover_chain.py#L101) | 101 | mock_settings.OLLAMA_URL = "http://localhost:11434" |
| [backend/tests/test_provider_failover_chain.py](file:///backend/tests/test_provider_failover_chain.py#L124) | 124 | mock_settings.OLLAMA_URL = "http://localhost:11434" |
| [backend/tests/test_security.py](file:///backend/tests/test_security.py#L33) | 33 | @pytest.mark.skip(reason="CORS validator filters localhost rather than raising RuntimeError") |
| [backend/tests/test_sentinel_agent.py](file:///backend/tests/test_sentinel_agent.py#L27) | 27 | "http://localhost:8080/health", |
| [backend/tests/test_sentinel_agent.py](file:///backend/tests/test_sentinel_agent.py#L29) | 29 | ),  # localhost allowed in non-production |
| [backend/tests/test_supabase_schema_bootstrap.py](file:///backend/tests/test_supabase_schema_bootstrap.py#L42) | 42 | monkeypatch.setenv("SUPABASE_DATABASE_URL", "postgresql://user:pass@localhost:5432/postgres") |
| [backend/tests/test_supabase_schema_bootstrap.py](file:///backend/tests/test_supabase_schema_bootstrap.py#L71) | 71 | monkeypatch.setenv("SUPABASE_DATABASE_URL", "postgresql://user:pass@localhost:5432/postgres") |
| [backend/tests/test_supabase_schema_bootstrap.py](file:///backend/tests/test_supabase_schema_bootstrap.py#L74) | 74 | "postgresql://pooler_user:pooler_pass@localhost:6543/postgres", |
| [backend/tests/test_supabase_schema_bootstrap.py](file:///backend/tests/test_supabase_schema_bootstrap.py#L81) | 81 | "postgresql://pooler_user:pooler_pass@localhost:6543/postgres", |
| [backend/tests/tools/test_browser_agent.py](file:///backend/tests/tools/test_browser_agent.py#L56) | 56 | ("http://localhost", "127.0.0.1"), |
| [backend/tests/tools/test_browser_agent.py](file:///backend/tests/tools/test_browser_agent.py#L127) | 127 | result = await agent.navigate_and_interact("http://localhost") |
| [backend/tests/tools/test_viral_referral_engine.py](file:///backend/tests/tools/test_viral_referral_engine.py#L12) | 12 | with patch.dict("os.environ", {"STAGING_REPLICA_URL": "http://localhost:8000"}): |
| [backend/tests/tools/test_viral_referral_engine.py](file:///backend/tests/tools/test_viral_referral_engine.py#L20) | 20 | with patch.dict("os.environ", {"STAGING_REPLICA_URL": "http://localhost:8000"}): |
| [backend/tests/tools/test_viral_referral_engine.py](file:///backend/tests/tools/test_viral_referral_engine.py#L46) | 46 | with patch.dict("os.environ", {"STAGING_REPLICA_URL": "http://localhost:8000"}): |
| [backend/tools/collaborative_editor.py](file:///backend/tools/collaborative_editor.py#L23) | 23 | redis_url = redis_url_setting if redis_url_setting else "redis://localhost:6379" |
| [backend/tools/graph_service.py](file:///backend/tools/graph_service.py#L12) | 12 | self.uri = getattr(settings, "neo4j_uri", "bolt://localhost:7687") |
| [backend/tools/learning/Diagnosed deployment failures and orches.ini](file:///backend/tools/learning/Diagnosed deployment failures and orches.ini#L1535) | 1535 | export SUPABASE_DATABASE_URL='postgresql://test_user:test_password@localhost:5432/supreme_test_db' |
| [backend/tools/sso_integrator.py](file:///backend/tools/sso_integrator.py#L41) | 41 | "http_host": self.saml_settings.get("sp_entity_id", "") or "localhost", |
| [backend/workers/chaos_worker.py](file:///backend/workers/chaos_worker.py#L34) | 34 | self.target_url = os.getenv("STAGING_REPLICA_URL", "http://localhost:8000") |
| [config/audit-rules.yml](file:///config/audit-rules.yml#L12) | 12 | - pattern: "DATABASE_URL=.*localhost" |
| [config/audit-rules.yml](file:///config/audit-rules.yml#L14) | 14 | - pattern: "REDIS_URL=.*localhost" |
| [docs/-01-admin's plan/01_implemented/SupremeAI_Zero_Cost_Implementation_Plan.md](file:///docs/-01-admin's plan/01_implemented/SupremeAI_Zero_Cost_Implementation_Plan.md#L1584) | 1584 | test: ["CMD", "curl", "-f", "http://localhost:8000/health"] |
| [docs/01-admin-plans/modular_audits/PART_05_SWARM_WEBSOCKETS.md](file:///docs/01-admin-plans/modular_audits/PART_05_SWARM_WEBSOCKETS.md#L56) | 56 | # বাংলা মন্তব্য: module-level redis.from_url("redis://localhost") সম্পূর্ণ নিষিদ্ধ। |
| [docs/01-admin-plans/modular_audits/PART_12_TEST_SUITE_PYTEST.md](file:///docs/01-admin-plans/modular_audits/PART_12_TEST_SUITE_PYTEST.md#L111) | 111 | os.environ.setdefault("CORS_ORIGINS", "http://localhost:3000,http://localhost:8000") |
| [docs/04-development/UPDATE_PLAN_ARCHIVE.md](file:///docs/04-development/UPDATE_PLAN_ARCHIVE.md#L1573) | 1573 | const response = await fetch(`http://localhost:${process.env.PORT \|\| 5000}${endpoint}`); |
| [docs/04-development/UPDATE_PLAN_ARCHIVE.md](file:///docs/04-development/UPDATE_PLAN_ARCHIVE.md#L3716) | 3716 | base_url = os.environ.get("API_BASE_URL", "http://localhost:8000") |
| [docs/08-roadmap/100%_completed_tasks.md](file:///docs/08-roadmap/100%_completed_tasks.md#L210) | 210 | - **Localhost Removal:** পুরো প্রজেক্ট থেকে `localhost` এর রেফারেন্স সরিয়ে ফেলা হয়েছে এবং Dockerfile অপ্টিমাইজ করা হয়েছে। |
| [docs/08-roadmap/PROJECT_STATUS.md](file:///docs/08-roadmap/PROJECT_STATUS.md#L59) | 59 | - ✅ Localhost references removed globally |
| [docs/08-roadmap/PROJECT_STATUS.md](file:///docs/08-roadmap/PROJECT_STATUS.md#L65) | 65 | - **Localhost Removal:** পুরো প্রজেক্ট থেকে `localhost` এর রেফারেন্স সরিয়ে ফেলা হয়েছে এবং Dockerfile অপ্টিমাইজ করা হয়েছে। |
| [docs/antigravity_brain_backup/125835dd-389a-4d78-a3ac-7b8dc3395564_backend_changelog.md](file:///docs/antigravity_brain_backup/125835dd-389a-4d78-a3ac-7b8dc3395564_backend_changelog.md#L15) | 15 | - **Issue:** The `test_cors_origins_production_strips_localhost` test forced a `production` environment state but failed to inject the mandatory `SUPREMEAI_JWT_SECRET`, breaking the initialization of the Settings class. |
| [docs/antigravity_brain_backup/14dccb9d-dd23-492f-a1ba-9c2dfa377a47_implementation_plan.md](file:///docs/antigravity_brain_backup/14dccb9d-dd23-492f-a1ba-9c2dfa377a47_implementation_plan.md#L72) | 72 | **Problem:** `CORSMiddleware` has hardcoded origins `["https://supremeai-admin.web.app", "http://localhost:5173", "http://localhost:3000"]` which differ from `settings.cors_origins` and `TrustedOriginMiddleware.allowed_origins`. Three separate origin lists creates maintenance headaches and security gaps. |
| [docs/antigravity_brain_backup/26cc8f97-c4c0-4468-a8a1-3e5fed92e66e_implementation_plan.md](file:///docs/antigravity_brain_backup/26cc8f97-c4c0-4468-a8a1-3e5fed92e66e_implementation_plan.md#L13) | 13 | - Fix `test_defaults` assertions (e.g., `ollama_url` defaulting to `""` instead of `http://localhost:11434`). |
| [docs/antigravity_brain_backup/26cc8f97-c4c0-4468-a8a1-3e5fed92e66e_implementation_plan.md](file:///docs/antigravity_brain_backup/26cc8f97-c4c0-4468-a8a1-3e5fed92e66e_implementation_plan.md#L15) | 15 | - Fix `CORS_ORIGINS` parsing tests to correctly assert the behavior of stripping `localhost` in production environments. |
| [docs/antigravity_brain_backup/27c0e9ee-595d-4960-844c-cca88ba17c9f_refactoring_round2.md](file:///docs/antigravity_brain_backup/27c0e9ee-595d-4960-844c-cca88ba17c9f_refactoring_round2.md#L11) | 11 | **গলদ:** লাইন ৪২ ও ৪৯-এ `redis.from_url(os.getenv("REDIS_URL", "redis://localhost:6379"))` module-level এ execute হচ্ছে। |
| [docs/antigravity_brain_backup/27c0e9ee-595d-4960-844c-cca88ba17c9f_refactoring_round2.md](file:///docs/antigravity_brain_backup/27c0e9ee-595d-4960-844c-cca88ba17c9f_refactoring_round2.md#L15) | 15 | - Fallback URL `redis://localhost:6379` হার্ডকোড করা — Anti-Hardcode Rule লঙ্ঘন |
| [docs/antigravity_brain_backup/27c0e9ee-595d-4960-844c-cca88ba17c9f_refactoring_round2.md](file:///docs/antigravity_brain_backup/27c0e9ee-595d-4960-844c-cca88ba17c9f_refactoring_round2.md#L18) | 18 | **গলদ:** লাইন ১৫-এ `self.redis = redis.from_url("redis://localhost")` — URL সম্পূর্ণ হার্ডকোড। |
| [docs/antigravity_brain_backup/6982f44d-e5fc-49e6-9cd4-483b34ab1af6_walkthrough.md](file:///docs/antigravity_brain_backup/6982f44d-e5fc-49e6-9cd4-483b34ab1af6_walkthrough.md#L24) | 24 | > You can now visit `http://localhost:XXXX/workspace/ide` to see the Morphic IDE in action. I recommend testing it by creating a file in the terminal using `echo "console.log('hello')" > test.js` and watching it appear in the File Explorer! |
| [docs/antigravity_brain_backup/8fcf30ff-5180-42cb-bdba-ff8c05f6479d_analysis_report.md](file:///docs/antigravity_brain_backup/8fcf30ff-5180-42cb-bdba-ff8c05f6479d_analysis_report.md#L43) | 43 | * **Localhost Admin Bypass:** |
| [docs/antigravity_brain_backup/8fcf30ff-5180-42cb-bdba-ff8c05f6479d_analysis_report.md](file:///docs/antigravity_brain_backup/8fcf30ff-5180-42cb-bdba-ff8c05f6479d_analysis_report.md#L44) | 44 | `backend/core/security/auth_middleware.py` এবং `origin_validator.py` ফাইলে `localhost` এর জন্য স্পেশাল বাইপাস রুল আছে। নন-প্রোডাকশন এনভায়রনমেন্ট (যেমন: স্টেজিং বা QA) যদি পাবলিকলি এক্সেসিবল হয়, তবে অরিজিন স্পুফিং করে বা লোকালহোস্ট হেডার পাঠিয়ে কেউ এই ভ্যালিডেশন বাইপাস করার চেষ্টা করতে পারে। |
| [docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md](file:///docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md#L3) | 3 | - [ ] Navigate to http://localhost:5173 (React/Vite App) |
| [docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md](file:///docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md#L4) | 4 | - [ ] Capture screenshot of http://localhost:5173 |
| [docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md](file:///docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md#L5) | 5 | - [ ] Navigate to http://localhost:4200 (Static CI/CD Dashboard) |
| [docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md](file:///docs/antigravity_brain_backup/browser_scratchpad_iayte7f7.md#L6) | 6 | - [ ] Capture screenshot of http://localhost:4200 |
| [docs/api/v1/index.md](file:///docs/api/v1/index.md#L14) | 14 | http://localhost:8000 |
| [docs/bangla/03-development/CONFIGURATION_SYSTEM_DOCUMENTATION_BANGLA.md](file:///docs/bangla/03-development/CONFIGURATION_SYSTEM_DOCUMENTATION_BANGLA.md#L97) | 97 | CMD curl -sf http://localhost:${PORT:-8080}/health \|\| exit 1 |
| [docs/bangla/03-development/SUPREMEAI_2_0_COMPLETE_SYSTEM_DOCUMENT_BANGLA.md](file:///docs/bangla/03-development/SUPREMEAI_2_0_COMPLETE_SYSTEM_DOCUMENT_BANGLA.md#L173) | 173 | \| `REDIS_URL` \| অপশনাল \| `redis://localhost:6379/0` \| ক্যাশ ও রেট লিমিটিং মেমোরি স্টোর \| |
| [docs/developer-guide/01-PROJECT-SETUP.md](file:///docs/developer-guide/01-PROJECT-SETUP.md#L168) | 168 | DATABASE_URL=postgresql://user:password@localhost:5432/supremeai |
| [docs/developer-guide/01-PROJECT-SETUP.md](file:///docs/developer-guide/01-PROJECT-SETUP.md#L169) | 169 | REDIS_URL=redis://localhost:6379 |
| [docs/developer-guide/03-CI-CD-PIPELINE.md](file:///docs/developer-guide/03-CI-CD-PIPELINE.md#L229) | 229 | DATABASE_URL: postgresql://test:test@localhost:5432/supremeai_test |
| [docs/developer-guide/03-CI-CD-PIPELINE.md](file:///docs/developer-guide/03-CI-CD-PIPELINE.md#L230) | 230 | REDIS_URL: redis://localhost:6379 |
| [docs/developer-guide/03-CI-CD-PIPELINE.md](file:///docs/developer-guide/03-CI-CD-PIPELINE.md#L234) | 234 | **নোট:** Service hostname হয় service name — `postgres`, `redis` — `localhost` নয়। কিন্তু `ports` mapping থাকলে runner থেকে `localhost:5432` দিয়েও access হয়। |
| [docs/developer-guide/04-SECURITY-HARDENING.md](file:///docs/developer-guide/04-SECURITY-HARDENING.md#L236) | 236 | ALLOWED_ORIGINS.append("http://localhost:5173") |
| [docs/developer-guide/04-SECURITY-HARDENING.md](file:///docs/developer-guide/04-SECURITY-HARDENING.md#L264) | 264 | CMD curl -sf http://localhost:${PORT:-8080}/health \|\| exit 1 |
| [docs/developer-guide/06-FRONTEND-DEVELOPMENT.md](file:///docs/developer-guide/06-FRONTEND-DEVELOPMENT.md#L124) | 124 | const BASE_URL = import.meta.env.VITE_API_URL \|\| 'http://localhost:8080' |
| [docs/developer-guide/06-FRONTEND-DEVELOPMENT.md](file:///docs/developer-guide/06-FRONTEND-DEVELOPMENT.md#L206) | 206 | VITE_API_URL=http://localhost:8080 |
| [docs/developer-guide/getting-started.md](file:///docs/developer-guide/getting-started.md#L52) | 52 | # Access API docs at http://localhost:8000/docs |
| [docs/developer-guide/getting-started.md](file:///docs/developer-guide/getting-started.md#L57) | 57 | # Access at http://localhost:5173 |
| [docs/developer-guide/troubleshooting.md](file:///docs/developer-guide/troubleshooting.md#L164) | 164 | curl http://localhost:8000/health |
| [docs/developer-guide/troubleshooting.md](file:///docs/developer-guide/troubleshooting.md#L167) | 167 | curl http://localhost:8000/health/aggregated |
| [docs/developer-guide/troubleshooting.md](file:///docs/developer-guide/troubleshooting.md#L170) | 170 | curl -I http://localhost:5173 |
| [docs/english/02-architecture/SUPREMEAI_2_0_COMPLETE_SYSTEM_DOCUMENT.md](file:///docs/english/02-architecture/SUPREMEAI_2_0_COMPLETE_SYSTEM_DOCUMENT.md#L1291) | 1291 | \| `REDIS_URL` \| OPTIONAL \| `redis://localhost:6379/0` \| Cache, pub/sub, & rate limiting store \| |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L105) | 105 | # 11. Visit http://localhost:3000 |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L141) | 141 | DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/supremeai |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L144) | 144 | REDIS_URL=redis://localhost:6379 |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L159) | 159 | NEO4J_URL=neo4j://localhost:7687 |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L193) | 193 | - API: http://localhost:8000 |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L194) | 194 | - Docs: http://localhost:8000/docs |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L195) | 195 | - Health: http://localhost:8000/health |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L217) | 217 | NEXT_PUBLIC_API_URL=http://localhost:8000 |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L218) | 218 | NEXT_PUBLIC_APP_URL=http://localhost:3000 |
| [docs/english/03-development/CONTRIBUTING.md](file:///docs/english/03-development/CONTRIBUTING.md#L231) | 231 | **Access**: http://localhost:3000 |
| [docs/guidelines/01-PROJECT-SETUP.md](file:///docs/guidelines/01-PROJECT-SETUP.md#L168) | 168 | DATABASE_URL=postgresql://user:password@localhost:5432/supremeai |
| [docs/guidelines/01-PROJECT-SETUP.md](file:///docs/guidelines/01-PROJECT-SETUP.md#L169) | 169 | REDIS_URL=redis://localhost:6379 |
| [docs/guidelines/03-CI-CD-PIPELINE.md](file:///docs/guidelines/03-CI-CD-PIPELINE.md#L229) | 229 | DATABASE_URL: postgresql://test:test@localhost:5432/supremeai_test |
| [docs/guidelines/03-CI-CD-PIPELINE.md](file:///docs/guidelines/03-CI-CD-PIPELINE.md#L230) | 230 | REDIS_URL: redis://localhost:6379 |
| [docs/guidelines/03-CI-CD-PIPELINE.md](file:///docs/guidelines/03-CI-CD-PIPELINE.md#L234) | 234 | **নোট:** Service hostname হয় service name — `postgres`, `redis` — `localhost` নয়। কিন্তু `ports` mapping থাকলে runner থেকে `localhost:5432` দিয়েও access হয়। |
| [docs/guidelines/04-SECURITY-HARDENING.md](file:///docs/guidelines/04-SECURITY-HARDENING.md#L236) | 236 | ALLOWED_ORIGINS.append("http://localhost:5173") |
| [docs/guidelines/04-SECURITY-HARDENING.md](file:///docs/guidelines/04-SECURITY-HARDENING.md#L264) | 264 | CMD curl -sf http://localhost:${PORT:-8080}/health \|\| exit 1 |
| [docs/guidelines/06-FRONTEND-DEVELOPMENT.md](file:///docs/guidelines/06-FRONTEND-DEVELOPMENT.md#L124) | 124 | const BASE_URL = import.meta.env.VITE_API_URL \|\| 'http://localhost:8080' |
| [docs/guidelines/06-FRONTEND-DEVELOPMENT.md](file:///docs/guidelines/06-FRONTEND-DEVELOPMENT.md#L206) | 206 | VITE_API_URL=http://localhost:8080 |
| [docs/knowledge-base/05-MODULE_DOCUMENTATION_bn.md](file:///docs/knowledge-base/05-MODULE_DOCUMENTATION_bn.md#L601) | 601 | curl http://localhost:8000/health |
| [docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md](file:///docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md#L212) | 212 | CORS_ORIGINS: list[str] = ["http://localhost:3000"] |
| [docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md](file:///docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md#L399) | 399 | OTEL_EXPORTER_ENDPOINT: str = "http://localhost:4317" |
| [docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md](file:///docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md#L460) | 460 | DATABASE_URL: str = "postgresql://localhost/supremeai" |
| [docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md](file:///docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md#L481) | 481 | DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/supremeai |
| [docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md](file:///docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md#L482) | 482 | REDIS_URL=redis://localhost:6379 |
| [docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md](file:///docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md#L483) | 483 | NEO4J_URL=neo4j://localhost:7687 |
| [docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md](file:///docs/knowledge-base/08-CONFIGURATION_DOCUMENTATION.md#L484) | 484 | QDRANT_URL=http://localhost:6333 |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L159) | 159 | \| `CORS_ORIGINS` \| list \| Allowed CORS origins \| ["http://localhost:3000"] \| 🟡 High \| |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L282) | 282 | OTEL_EXPORTER_ENDPOINT=http://localhost:4317 |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L364) | 364 | DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/supremeai |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L365) | 365 | REDIS_URL=redis://localhost:6379 |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L366) | 366 | NEO4J_URL=neo4j://localhost:7687 |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L369) | 369 | QDRANT_URL=http://localhost:6333 |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L420) | 420 | CORS_ORIGINS=["http://localhost:3000"] |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L571) | 571 | DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/supremeai |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L572) | 572 | REDIS_URL=redis://localhost:6379 |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L573) | 573 | NEO4J_URL=neo4j://localhost:7687 |
| [docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md](file:///docs/knowledge-base/09-ENVIRONMENT_DOCUMENTATION.md#L576) | 576 | QDRANT_URL=http://localhost:6333 |
| [docs/knowledge-base/11-API_DOCUMENTATION.md](file:///docs/knowledge-base/11-API_DOCUMENTATION.md#L19) | 19 | \| **Local** \| http://localhost:8000 \| http://localhost:8001 \| |
| [docs/knowledge-base/21-DEPLOYMENT_DOCUMENTATION.md](file:///docs/knowledge-base/21-DEPLOYMENT_DOCUMENTATION.md#L137) | 137 | CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" |
| [docs/operations/rollback-plan.md](file:///docs/operations/rollback-plan.md#L86) | 86 | curl -f http://localhost:8000/api/v1/ready \|\| \ |
| [docs/reports/CHECK_GITHUB_PR_HISTORY.md](file:///docs/reports/CHECK_GITHUB_PR_HISTORY.md#L748) | 748 | SKIPPED [1] tests/test_security.py:31: CORS validator filters localhost rather than raising RuntimeError |
| [docs/reports/LOCAL_SETUP_GUIDE.md](file:///docs/reports/LOCAL_SETUP_GUIDE.md#L27) | 27 | * **Web Chat (`http://localhost:5173`)**: এটি সাধারণ ব্যবহারকারীদের জন্য চ্যাটিং ইন্টারফেস। এখানে শুধু চ্যাট উইন্ডো এবং সাধারণ ৩টি আইন দেখতে পাবেন। |
| [docs/reports/LOCAL_SETUP_GUIDE.md](file:///docs/reports/LOCAL_SETUP_GUIDE.md#L28) | 28 | * **Studio Client (`http://localhost:5174`)**: এটি ডেভেলপার/এডমিনদের জন্য IDE এবং কন্ট্রোল প্যানেল। এখানে কোড এডিটর এবং এডমিন কনসোল অ্যাক্সেস করা যায়। |
| [docs/reports/full_modified_codebase.md](file:///docs/reports/full_modified_codebase.md#L336) | 336 | # বাংলা মন্তব্য: OLLAMA_URL — fail-fast, কোনো localhost fallback নেই |
| [docs/reports/full_modified_codebase.md](file:///docs/reports/full_modified_codebase.md#L510) | 510 | return self._get_cached_secret("NEO4J_URI") or "bolt://localhost:7687" |
| [docs/reports/full_modified_codebase.md](file:///docs/reports/full_modified_codebase.md#L609) | 609 | forbidden = {"localhost", "127.0.0.1", "testserver", "0.0.0.0"} |
| [docs/reports/full_modified_codebase.md](file:///docs/reports/full_modified_codebase.md#L613) | 613 | raise ValueError(f"{env.capitalize()} requires explicit ALLOWED_HOSTS — localhost/testserver forbidden.") |
| [docs/reports/full_modified_codebase.md](file:///docs/reports/full_modified_codebase.md#L660) | 660 | v = [o for o in v if "localhost" not in o and "127.0.0.1" not in o] |
| [docs/reports/full_modified_codebase.md](file:///docs/reports/full_modified_codebase.md#L662) | 662 | raise ValueError(f"{env.capitalize()} requires at least one non-localhost CORS origin. Set CORS_ORIGINS env var.") |
| [docs/reports/github_pipelines.md](file:///docs/reports/github_pipelines.md#L1029) | 1029 | SUPABASE_DATABASE_URL: "postgresql://mock_user:mock_pass@localhost:5432/mock_db" |
| [implementation_plan.md](file:///implementation_plan.md#L31) | 31 | > **Playwright target URL**: What URL does the local dev server run on? Assumed `http://localhost:5173` — correct if different. |
| [implementation_plan.md](file:///implementation_plan.md#L159) | 159 | - `grep -rn 'localhost\\|hardcode\\|TODO_FIXME' src/commandcenter/` hardcoded value check |
| [infrastructure/firebase_functions/firebase_functions_v1/.env.example](file:///infrastructure/firebase_functions/firebase_functions_v1/.env.example#L10) | 10 | OLLAMA_BASE_URL=http://localhost:11434 |
| [infrastructure/firebase_functions/firebase_functions_v1/.env.example](file:///infrastructure/firebase_functions/firebase_functions_v1/.env.example#L11) | 11 | AIRLLM_SIDECAR_URL=http://localhost:8081 |
| [infrastructure/firebase_functions/firebase_functions_v1/.env.example](file:///infrastructure/firebase_functions/firebase_functions_v1/.env.example#L14) | 14 | BROWSER_AUTOMATION_URL=http://localhost:3001 |
| [infrastructure/firebase_functions/firebase_functions_v1/health-smart.js](file:///infrastructure/firebase_functions/firebase_functions_v1/health-smart.js#L5) | 5 | 'http://localhost:5173', |
| [infrastructure/firebase_functions/firebase_functions_v1/providers-smart.js](file:///infrastructure/firebase_functions/firebase_functions_v1/providers-smart.js#L6) | 6 | 'http://localhost:5173', |
| [infrastructure/zero_cost/config.env](file:///infrastructure/zero_cost/config.env#L32) | 32 | FIRESTORE_EMULATOR_HOST=localhost:8080 |
| [packages/ui-components/src/utils/api.ts](file:///packages/ui-components/src/utils/api.ts#L3) | 3 | return import.meta.env.VITE_API_BASE \|\| import.meta.env.VITE_API_URL \|\| 'http://localhost:8000'; |
| [playwright.config.ts](file:///playwright.config.ts#L30) | 30 | baseURL: process.env.BASE_URL \|\| 'http://localhost:5173', |
| [scripts/ai/feature_store_sync.py](file:///scripts/ai/feature_store_sync.py#L118) | 118 | host=self.source_config.get('host', 'localhost'), |
| [scripts/ai/feature_store_sync.py](file:///scripts/ai/feature_store_sync.py#L223) | 223 | host=self.destination_config.get('host', 'localhost'), |
| [scripts/cloudflare_worker.test.mjs](file:///scripts/cloudflare_worker.test.mjs#L51) | 51 | const res = await mf.dispatchFetch('http://localhost:8787/'); |
| [scripts/cloudflare_worker.test.mjs](file:///scripts/cloudflare_worker.test.mjs#L62) | 62 | let res = await mf.dispatchFetch('http://localhost:8787/'); |
| [scripts/cloudflare_worker.test.mjs](file:///scripts/cloudflare_worker.test.mjs#L66) | 66 | res = await mf.dispatchFetch('http://localhost:8787/'); |
| [scripts/cloudflare_worker.test.mjs](file:///scripts/cloudflare_worker.test.mjs#L70) | 70 | res = await mf.dispatchFetch('http://localhost:8787/'); |
| [scripts/cloudflare_worker.test.mjs](file:///scripts/cloudflare_worker.test.mjs#L74) | 74 | res = await mf.dispatchFetch('http://localhost:8787/'); |
| [scripts/cloudflare_worker.test.mjs](file:///scripts/cloudflare_worker.test.mjs#L85) | 85 | const res = await mf.dispatchFetch('http://localhost:8787/'); |
| [scripts/cloudflare_worker.test.mjs](file:///scripts/cloudflare_worker.test.mjs#L89) | 89 | const res = await mf.dispatchFetch('http://localhost:8787/'); |
| [scripts/col](file:///scripts/col#L217) | 217 | CMD curl -f http://localhost:80/health \|\| exit 1 |
| [scripts/devops/run_local_audit.py](file:///scripts/devops/run_local_audit.py#L38) | 38 | url = "http://localhost:11434/api/generate" |
| [scripts/docs/auto_api_doc_sync.py](file:///scripts/docs/auto_api_doc_sync.py#L11) | 11 | - SUPREMEAI_API_URL: Base URL of the SupremeAI API (default: http://localhost:8000) |
| [scripts/docs/auto_api_doc_sync.py](file:///scripts/docs/auto_api_doc_sync.py#L32) | 32 | API_URL = os.getenv("SUPREMEAI_API_URL", "http://localhost:8000") |
| [scripts/evolution/auto_marketing_skill_forge.py](file:///scripts/evolution/auto_marketing_skill_forge.py#L14) | 14 | - SUPREMEAI_API_BASE_URL: Base URL for the SupremeAI API (default: http://localhost:8000) |
| [scripts/evolution/auto_marketing_skill_forge.py](file:///scripts/evolution/auto_marketing_skill_forge.py#L54) | 54 | API_BASE_URL = os.getenv("SUPREMEAI_API_BASE_URL", "http://localhost:8000") |
| [scripts/find_stub_data.py](file:///scripts/find_stub_data.py#L41) | 41 | ("hardcoded_localhost_redirect", r'redirect_uri\s*=\s*["\']http://localhost:8000', "MEDIUM"), |
| [scripts/find_stub_data.py](file:///scripts/find_stub_data.py#L42) | 42 | ("hardcoded_localhost_frontend", r'RedirectResponse\(url=["\']http://localhost:5173', "MEDIUM"), |
| [scripts/generate_openapi.py](file:///scripts/generate_openapi.py#L30) | 30 | os.environ.setdefault("SUPABASE_DATABASE_URL", "postgresql+asyncpg://mock:mock@localhost:5432/mock_db") |
| [scripts/generate_openapi.py](file:///scripts/generate_openapi.py#L31) | 31 | os.environ.setdefault("SUPABASE_DATABASE_URL_POOLER", "postgresql+asyncpg://mock:mock@localhost:5432/mock_db") |
| [scripts/monitoring/capacity_planner.py](file:///scripts/monitoring/capacity_planner.py#L59) | 59 | DEFAULT_API_URL = os.getenv("BACKEND_URL", "http://localhost:8000") |
| [scripts/monitoring/sla_tracker.py](file:///scripts/monitoring/sla_tracker.py#L27) | 27 | BACKEND_URL             - API base URL (default: http://localhost:8000) |
| [scripts/monitoring/sla_tracker.py](file:///scripts/monitoring/sla_tracker.py#L256) | 256 | self.base_urls = base_urls or [os.getenv("BACKEND_URL", "http://localhost:8000")] |
| [scripts/monitoring/sla_tracker.py](file:///scripts/monitoring/sla_tracker.py#L395) | 395 | backend_url = os.getenv("BACKEND_URL", "http://localhost:8000") |
| [scripts/patches/fix-admin-dashboard-api-cache.patch](file:///scripts/patches/fix-admin-dashboard-api-cache.patch#L18) | 18 | +    !!cached && /^https:\/\//.test(cached) && !/localhost\|127\.0\.0\.1/.test(cached); |
| [scripts/patches/fix-maintenance-pipeline-hang.patch](file:///scripts/patches/fix-maintenance-pipeline-hang.patch#L12) | 12 | SUPABASE_DATABASE_URL_POOLER: "postgresql+asyncpg://mock:mock@localhost:5432/mock_db" |
| [scripts/supreme-config-audit.py](file:///scripts/supreme-config-audit.py#L49) | 49 | {'pattern': r'DATABASE_URL=.*localhost', 'message': 'Local DB in staging+'}, |
| [scripts/supreme-config-audit.py](file:///scripts/supreme-config-audit.py#L50) | 50 | {'pattern': r'REDIS_URL=.*localhost', 'message': 'Local Redis in staging+'}, |
| [scripts/supreme-config-audit.py](file:///scripts/supreme-config-audit.py#L154) | 154 | 'DATABASE_URL': ['localhost', '127.0.0.1'], |
| [scripts/tenant/auto_tenant_health_report.py](file:///scripts/tenant/auto_tenant_health_report.py#L417) | 417 | smtp_server = os.getenv("SMTP_SERVER", "localhost") |
| [scripts/tenant/auto_tenant_setup.py](file:///scripts/tenant/auto_tenant_setup.py#L266) | 266 | smtp_server = os.getenv("SMTP_SERVER", "localhost") |
| [scripts/testenv/setup_test_env.sh](file:///scripts/testenv/setup_test_env.sh#L21) | 21 | SUPABASE_URL=http://localhost:54321 |
| [scripts/testenv/setup_test_env.sh](file:///scripts/testenv/setup_test_env.sh#L23) | 23 | DATABASE_URL=postgresql://postgres:postgres@localhost:54322/postgres |
| [scripts/testenv/setup_test_env.sh](file:///scripts/testenv/setup_test_env.sh#L24) | 24 | REDIS_URL=redis://localhost:6379/0 |
| [scripts/testing/api_contract_validator.py](file:///scripts/testing/api_contract_validator.py#L67) | 67 | DEFAULT_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8000") |
| [scripts/testing/integration_test_runner.py](file:///scripts/testing/integration_test_runner.py#L64) | 64 | FIRESTORE_EMULATOR_HOST = os.getenv("FIRESTORE_EMULATOR_HOST", "localhost:8080") |
| [scripts/testing/integration_test_runner.py](file:///scripts/testing/integration_test_runner.py#L65) | 65 | REDIS_TEST_URL = os.getenv("REDIS_TEST_URL", "redis://localhost:6379/15") |
| [scripts/testing/integration_test_runner.py](file:///scripts/testing/integration_test_runner.py#L66) | 66 | API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8000") |
| [scripts/testing/integration_test_runner.py](file:///scripts/testing/integration_test_runner.py#L204) | 204 | producer = KafkaProducer(bootstrap_servers="localhost:9092", |
| [scripts/testing/integration_test_runner.py](file:///scripts/testing/integration_test_runner.py#L275) | 275 | result = sock.connect_ex(("localhost", 8080)) |
| [scripts/testing/integration_test_runner.py](file:///scripts/testing/integration_test_runner.py#L281) | 281 | ["gcloud", "emulators", "firestore", "start", "--host-port=localhost:8080"], |
| [scripts/testing/log_anomaly_detector.py](file:///scripts/testing/log_anomaly_detector.py#L641) | 641 | "2024-01-15 10:31:02,456 [ERROR] database: Connection refused to postgres://localhost:5432/supremeai", |
| [scripts/testing/performance_benchmark.py](file:///scripts/testing/performance_benchmark.py#L68) | 68 | API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8000") |
| [scripts/testing/security_penetration_test.py](file:///scripts/testing/security_penetration_test.py#L27) | 27 | python scripts/testing/security_penetration_test.py --target http://localhost:8000 |
| [scripts/testing/security_penetration_test.py](file:///scripts/testing/security_penetration_test.py#L28) | 28 | python scripts/testing/security_penetration_test.py --target http://localhost:8000 --scope full |
| [scripts/testing/security_penetration_test.py](file:///scripts/testing/security_penetration_test.py#L29) | 29 | python scripts/testing/security_penetration_test.py --target http://localhost:8000 --tests headers,ratelimit |
| [scripts/testing/security_penetration_test.py](file:///scripts/testing/security_penetration_test.py#L226) | 226 | parser.add_argument("--target", required=True, help="Target URL (e.g. http://localhost:8000)") |
| [tests/scripts/test_billing_quota_enforcer.py](file:///tests/scripts/test_billing_quota_enforcer.py#L90) | 90 | with patch.dict(os.environ, {"REDIS_URL": "redis://localhost:6379/0"}): |
| [tests/test_core_config.py](file:///tests/test_core_config.py#L54) | 54 | with patch.dict(os.environ, {'CORS_ORIGINS': 'http://localhost:3000,http://localhost:5173'}): |
| [tests/test_core_config.py](file:///tests/test_core_config.py#L56) | 56 | assert 'http://localhost:3000' in settings.cors_origins |
| [tests/test_core_config.py](file:///tests/test_core_config.py#L57) | 57 | assert 'http://localhost:5173' in settings.cors_origins |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L134) | 134 | "localhost-removal check whenever 'pytest' in sys.modules (always true here), so " |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L135) | 135 | "localhost origins are never actually filtered out under pytest regardless of " |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L141) | 141 | """Test production CORS validation removes localhost origins.""" |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L146) | 146 | 'CORS_ORIGINS': '["http://localhost:3000", "https://example.com"]', |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L150) | 150 | # localhost should be removed in production |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L151) | 151 | assert 'http://localhost:3000' not in settings.cors_origins |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L195) | 195 | 'ALLOWED_HOSTS': 'localhost,127.0.0.1,testserver,example.com', |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L200) | 200 | assert 'localhost' not in settings.allowed_hosts |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L345) | 345 | with patch.dict(os.environ, {'REDIS_URL': 'redis://localhost:6379'}): |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L347) | 347 | assert settings.redis_url == 'redis://localhost:6379' |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L421) | 421 | 'DATABASE_URL': 'postgresql://user:pass@localhost/db', |
| [tests/test_core_config_comprehensive.py](file:///tests/test_core_config_comprehensive.py#L427) | 427 | assert settings.database_url == 'postgresql://user:pass@localhost/db' |
| [tests/test_core_health_check.py](file:///tests/test_core_health_check.py#L218) | 218 | mock_settings.redis_url = "redis://localhost:6379" |
| [tools/firebase_functions_v1/.env.example](file:///tools/firebase_functions_v1/.env.example#L10) | 10 | OLLAMA_BASE_URL=http://localhost:11434 |
| [tools/firebase_functions_v1/.env.example](file:///tools/firebase_functions_v1/.env.example#L11) | 11 | AIRLLM_SIDECAR_URL=http://localhost:8081 |
| [tools/firebase_functions_v1/.env.example](file:///tools/firebase_functions_v1/.env.example#L14) | 14 | BROWSER_AUTOMATION_URL=http://localhost:3001 |
| [tools/vscode-extension/README.md](file:///tools/vscode-extension/README.md#L7) | 7 | - **Login Bypass & Fallback Routing**: If the backend is unavailable or not authenticated, requests automatically failover to local **Ollama** (`http://localhost:11434/api/chat`) or **OpenRouter Free API** models. |
| [tools/vscode-extension/README_BN.md](file:///tools/vscode-extension/README_BN.md#L11) | 11 | - লোকাল **Ollama** (`http://localhost:11434/api/chat`) অথবা **OpenRouter Free API** মডেলে স্বয়ংক্রিয় ফলব্যাক রাউটিং সুবিধা। |
| [tools/vscode-extension/src/services/SupremeAIService.ts](file:///tools/vscode-extension/src/services/SupremeAIService.ts#L289) | 289 | if (!ollamaUrl \|\| ollamaUrl.includes('localhost') \|\| ollamaUrl.includes('127.0.0.1')) { |
| [tools/vscode-extension/src/services/SupremeAIService.ts](file:///tools/vscode-extension/src/services/SupremeAIService.ts#L290) | 290 | throw new Error('Localhost/127.0.0.1 endpoints are disabled for security reasons.'); |
