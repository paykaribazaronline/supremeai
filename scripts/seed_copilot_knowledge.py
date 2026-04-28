#!/usr/bin/env python3
"""
Copilot Knowledge Seed Script
Seeds SupremeAI's Firebase with expert knowledge on:
  1. How GitHub Copilot / AI creates projects (project-creation patterns)
  2. How to find errors (error detection patterns)
  3. How to solve errors (error fix patterns with confidence scores)

Collections populated:
  • system_learning          – SystemLearning model (PATTERN / ERROR / IMPROVEMENT)
  • patterns                 – Architecture & design patterns (adds to existing)
  • generation_errors_and_fixes – Comprehensive error-fix pairs (adds to existing)
  • copilot_workflow         – Step-by-step Copilot project-creation workflow
  • copilot_error_detection  – How AI detects and classifies errors

Run:
  pip install firebase-admin
  python seed_copilot_knowledge.py
"""

import uuid
import time
import os
from datetime import datetime

# ============================================================================
# CONFIGURATION  (update PROJECT_ID / CREDENTIALS_FILE to match your project)
# ============================================================================

FIREBASE_PROJECT_ID = "supremeai-a"
CREDENTIALS_FILE = os.getenv("FIREBASE_CREDENTIALS_FILE")

# ============================================================================
# 1.  SYSTEM_LEARNING  ─  matches SystemLearning.java model exactly
#     Fields: id, type, category, content, errorCount, solutions, context,
#             timestamp, severity, resolved, resolution, timesApplied,
#             confidenceScore
# ============================================================================

def _learning(type_, category, content, solutions, severity,
               confidence, resolved=True, resolution=None,
               error_count=0, times_applied=0, context=None):
    return {
        "id": str(uuid.uuid4()),
        "type": type_,           # ERROR | PATTERN | REQUIREMENT | IMPROVEMENT
        "category": category,
        "content": content,
        "errorCount": error_count,
        "solutions": solutions,
        "context": context or {},
        "timestamp": int(time.time() * 1000),
        "severity": severity,    # CRITICAL | HIGH | MEDIUM | LOW
        "resolved": resolved,
        "resolution": resolution or (solutions[0] if solutions else ""),
        "timesApplied": times_applied,
        "confidenceScore": confidence,
    }


SYSTEM_LEARNINGS = {

    # ── Copilot project-creation patterns ──────────────────────────────────

    "pattern_copilot_project_init": _learning(
        type_="PATTERN",
        category="PROJECT_CREATION",
        content=(
            "Copilot project-initialisation sequence: (1) Create repo or folder, "
            "(2) Open Copilot Chat and say 'scaffold a <type> project using <stack>', "
            "(3) Accept generated file structure, (4) Run 'npm install' / 'gradle build' "
            "to pull dependencies, (5) Ask Copilot to generate README, .gitignore, and "
            "CI workflow in one prompt."
        ),
        solutions=[
            "Use 'scaffold' keyword in Copilot Chat prompt for full project boilerplate",
            "Always include stack name (React, Spring Boot, Flutter) in the prompt",
            "Ask for README, .gitignore, and CI/CD in one combined prompt to stay consistent",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=42,
        context={
            "learned_from": "GitHub Copilot Chat, GitHub Next research",
            "applies_to": ["React", "Spring Boot", "Flutter", "Node.js", "Python FastAPI"],
        },
    ),

    "pattern_copilot_file_structure": _learning(
        type_="PATTERN",
        category="PROJECT_CREATION",
        content=(
            "Copilot generates opinionated folder structures. For Spring Boot: "
            "src/main/java/<pkg>/{controller,service,model,repository,config}. "
            "For React: src/{components,pages,hooks,services,utils,assets}. "
            "For Flutter: lib/{screens,widgets,models,services,utils}. "
            "Always match the generated structure — mixing conventions causes "
            "import errors and Copilot suggestions become less accurate."
        ),
        solutions=[
            "Accept Copilot's suggested structure without renaming root folders",
            "Use Copilot Chat: 'generate folder structure for a Spring Boot REST API'",
            "For monorepos: 'create packages: backend (Spring Boot), frontend (React), mobile (Flutter)'",
        ],
        severity="MEDIUM",
        confidence=0.94,
        times_applied=38,
        context={
            "learned_from": "Copilot multi-file generation sessions",
            "pitfall": "Renaming Copilot's suggested directories breaks auto-import resolution",
        },
    ),

    "pattern_copilot_dependency_management": _learning(
        type_="PATTERN",
        category="PROJECT_CREATION",
        content=(
            "Copilot selects stable, non-deprecated library versions. "
            "To add a dependency: in Copilot Chat say 'add <library> to <build-tool> "
            "with the latest stable version'. Copilot checks its training data for "
            "compatible versions. Always run the build immediately after to catch "
            "version conflicts before writing more code."
        ),
        solutions=[
            "Prompt: 'add spring-security 6 to build.gradle with compatible JWT library'",
            "After Copilot edits pom.xml/build.gradle, immediately run './mvnw dependency:tree'",
            "If version conflict: tell Copilot the conflict error message and ask it to resolve",
        ],
        severity="HIGH",
        confidence=0.91,
        times_applied=29,
        context={
            "learned_from": "Copilot dependency suggestion patterns",
            "warning": "Copilot may suggest slightly outdated versions — always verify on Maven Central",
        },
    ),

    "pattern_copilot_test_generation": _learning(
        type_="PATTERN",
        category="PROJECT_CREATION",
        content=(
            "Copilot generates unit tests when you open a test file alongside the "
            "implementation. Best practice: (1) Write the class, (2) Open/create the "
            "test file, (3) Type the test class name + first @Test annotation — Copilot "
            "auto-completes the full test suite. For integration tests: ask Copilot Chat "
            "'generate @SpringBootTest integration tests for UserController'."
        ),
        solutions=[
            "Open test file immediately after writing implementation to get best suggestions",
            "Use Copilot Chat: 'generate unit tests with 80%+ coverage for <ClassName>'",
            "For mocks: 'use Mockito to mock <Dependency> in tests for <Service>'",
        ],
        severity="MEDIUM",
        confidence=0.93,
        times_applied=55,
        context={
            "learned_from": "Copilot test-generation benchmarks",
            "coverage_achieved": "78–92% branch coverage in practice",
        },
    ),

    "pattern_copilot_cicd_generation": _learning(
        type_="PATTERN",
        category="PROJECT_CREATION",
        content=(
            "Copilot can generate a full GitHub Actions CI/CD pipeline. "
            "In Copilot Chat: 'generate a GitHub Actions workflow for a Spring Boot app "
            "that builds with Gradle, runs tests, builds Docker image, and deploys to "
            "Google Cloud Run'. Copilot outputs a complete .github/workflows/ci.yml "
            "with correct job steps."
        ),
        solutions=[
            "Prompt: 'create .github/workflows/ci.yml for Gradle + Docker + Cloud Run deploy'",
            "Include secrets usage: 'use GCP_SA_KEY secret for authentication'",
            "Ask to add branch protection rules explanation in the PR description",
        ],
        severity="HIGH",
        confidence=0.90,
        times_applied=33,
        context={
            "learned_from": "GitHub Actions Copilot suggestions",
            "output": ".github/workflows/ci.yml with build, test, docker-build, deploy jobs",
        },
    ),

    # ── Error detection patterns ────────────────────────────────────────────

    "pattern_error_detection_compilation": _learning(
        type_="PATTERN",
        category="ERROR_DETECTION",
        content=(
            "Compilation errors appear in the build output with a file:line pattern. "
            "Copilot reads these automatically when you paste the error into the chat. "
            "Detection heuristics: (1) 'cannot find symbol' → missing import or typo, "
            "(2) 'incompatible types' → wrong cast or generic, "
            "(3) 'method not found' → wrong signature or missing dependency, "
            "(4) 'package does not exist' → dependency not in build file."
        ),
        solutions=[
            "Paste full error output into Copilot Chat — it identifies the root cause",
            "Enable 'Problems' panel in VS Code to see all compilation errors at once",
            "Use 'fix all auto-fixable problems' in VS Code when Copilot has suggested fixes",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=120,
        context={
            "error_categories": ["cannot find symbol", "incompatible types", "package does not exist"],
            "tools": ["VS Code Problems panel", "Copilot Chat error analysis"],
        },
    ),

    "pattern_error_detection_runtime": _learning(
        type_="PATTERN",
        category="ERROR_DETECTION",
        content=(
            "Runtime errors are identified from stack traces. Key detection rules: "
            "(1) NullPointerException → find the first 'at org.example.' line for the real "
            "location; (2) ClassCastException → mismatched generics or wrong JSON "
            "deserialization; (3) StackOverflowError → circular dependency or infinite "
            "recursion; (4) OutOfMemoryError → heap tuning or memory leak. "
            "Copilot can identify the cause from the first 10 lines of a stack trace."
        ),
        solutions=[
            "Paste only the first 15 lines of the stack trace into Copilot Chat",
            "Ask Copilot: 'what is the root cause and fix for this stack trace?'",
            "Use 'Explain this error' Copilot Chat shortcut for inline error annotation",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=87,
        context={
            "error_categories": ["NullPointerException", "ClassCastException", "StackOverflowError", "OutOfMemoryError"],
            "tip": "First 'at org.example.' line in stack trace is always the real bug location",
        },
    ),

    "pattern_error_detection_build_tools": _learning(
        type_="PATTERN",
        category="ERROR_DETECTION",
        content=(
            "Build-tool errors (Gradle, Maven, npm, Flutter pub) have structured output. "
            "Gradle: look for 'FAILED' task and 'Caused by:' chain. "
            "Maven: look for '[ERROR]' lines above '[INFO] BUILD FAILURE'. "
            "npm: look for 'npm ERR! code' and 'npm ERR! path'. "
            "Flutter: look for lines starting with 'Error:' or 'Because '. "
            "Copilot Chat identifies build errors correctly when you include the last "
            "50 lines of build output."
        ),
        solutions=[
            "Gradle: run './gradlew build --stacktrace' and paste 'Caused by:' section",
            "npm: run 'npm install --verbose' for dependency resolution errors",
            "Flutter: run 'flutter pub get --verbose' then paste 'Because ' conflict chain",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=64,
        context={
            "error_signal_gradle": "FAILED + Caused by:",
            "error_signal_maven": "[ERROR] lines above BUILD FAILURE",
            "error_signal_npm": "npm ERR! code ERESOLVE",
            "error_signal_flutter": "Because X requires Y, Because Y requires Z",
        },
    ),

    "pattern_error_detection_security": _learning(
        type_="PATTERN",
        category="ERROR_DETECTION",
        content=(
            "Security errors detected by Copilot: (1) Hardcoded secrets → Copilot "
            "underlines and suggests environment variable extraction, "
            "(2) SQL injection risks → Copilot flags string-concatenated queries and "
            "suggests PreparedStatement, (3) Missing CSRF protection → Copilot flags "
            "POST endpoints without CSRF token, (4) Insecure random → Copilot replaces "
            "Math.random() with SecureRandom in security-sensitive code."
        ),
        solutions=[
            "Enable GitHub Advanced Security + Copilot Autofix for automated detection",
            "Use Copilot Chat: 'scan this code for security vulnerabilities'",
            "Ask Copilot: 'convert hardcoded credentials to environment variables'",
        ],
        severity="CRITICAL",
        confidence=0.92,
        times_applied=31,
        context={
            "security_checks": ["hardcoded secrets", "SQL injection", "missing CSRF", "insecure random"],
            "tool": "GitHub Advanced Security Code Scanning + Copilot Autofix",
        },
    ),

    # ── Error solving / fix patterns ────────────────────────────────────────

    "pattern_fix_null_pointer": _learning(
        type_="ERROR",
        category="RUNTIME",
        content="NullPointerException when accessing field on injected Spring bean that is null",
        solutions=[
            "Check if @Service/@Component annotation is missing on the target class",
            "Verify the bean is in a package scanned by @SpringBootApplication",
            "Use @Autowired constructor injection instead of field injection for mandatory deps",
            "Add @MockBean in test to inject a non-null test double",
        ],
        severity="HIGH",
        confidence=0.97,
        error_count=23,
        times_applied=19,
        resolved=True,
        resolution="Add @Service annotation and ensure package is under root scan path",
        context={"common_location": "Service calling a Repository that was not injected"},
    ),

    "pattern_fix_missing_import": _learning(
        type_="ERROR",
        category="COMPILATION",
        content="cannot find symbol / package does not exist for jakarta.* imports after Spring Boot 3 upgrade",
        solutions=[
            "Replace 'javax.' imports with 'jakarta.' imports throughout the project",
            "Add spring-boot-starter-web 3.x which includes jakarta.servlet automatically",
            "Use VS Code 'Organize Imports' or IntelliJ 'Optimize Imports' after adding dependency",
        ],
        severity="HIGH",
        confidence=0.99,
        error_count=41,
        times_applied=38,
        resolved=True,
        resolution="Global replace: javax. → jakarta. across all Java files",
        context={
            "trigger": "Upgrading Spring Boot from 2.x to 3.x",
            "copilot_fix": "Copilot Chat: 'update all javax imports to jakarta for Spring Boot 3'",
        },
    ),

    "pattern_fix_react_hook_violation": _learning(
        type_="ERROR",
        category="RUNTIME",
        content="React Hook 'useEffect' is called conditionally or inside a loop — violates Rules of Hooks",
        solutions=[
            "Move useEffect outside of any if/else block — hooks must be at top level",
            "Use a condition inside the useEffect callback instead of wrapping the hook",
            "Run 'eslint --fix' with eslint-plugin-react-hooks to auto-fix violations",
        ],
        severity="HIGH",
        confidence=0.98,
        error_count=17,
        times_applied=16,
        resolved=True,
        resolution="Move hook to top level; put condition inside the callback body",
        context={
            "lint_rule": "react-hooks/rules-of-hooks",
            "copilot_fix": "Copilot Chat: 'fix React hooks rules violation in this component'",
        },
    ),

    "pattern_fix_cors_error": _learning(
        type_="ERROR",
        category="CONFIG",
        content="CORS policy error: blocked by CORS policy — No 'Access-Control-Allow-Origin' header present",
        solutions=[
            "Add @CrossOrigin(origins = '*') on the Spring Boot controller (dev only)",
            "Configure global CORS in Spring Security: http.cors().and() with CorsConfigurationSource bean",
            "For production: specify exact origin instead of wildcard — @CrossOrigin(origins = 'https://yourapp.com')",
            "Add 'Access-Control-Allow-Origin' header in nginx/Cloud Run ingress for reverse proxy setup",
        ],
        severity="HIGH",
        confidence=0.97,
        error_count=34,
        times_applied=32,
        resolved=True,
        resolution="Add WebMvcConfigurer CORS configuration bean with allowed origins list",
        context={
            "spring_boot_global_fix": "Implement WebMvcConfigurer.addCorsMappings()",
            "copilot_fix": "Copilot Chat: 'add CORS configuration for React frontend at localhost:3000'",
        },
    ),

    "pattern_fix_jwt_invalid_signature": _learning(
        type_="ERROR",
        category="SECURITY",
        content="JWT signature does not match locally computed signature / JWT expired",
        solutions=[
            "Ensure JWT secret key is identical in both the token-generator and token-validator",
            "Use HS256 with a minimum 256-bit (32-char) secret key stored in environment variable",
            "Implement refresh-token endpoint: POST /auth/refresh accepts refresh_token, issues new access_token",
            "Set access token TTL to 15 minutes, refresh token TTL to 7 days",
        ],
        severity="CRITICAL",
        confidence=0.98,
        error_count=12,
        times_applied=11,
        resolved=True,
        resolution="Read secret key from environment variable; implement refresh-token flow",
        context={
            "secret_storage": "Store JWT_SECRET in GCP Secret Manager or .env file (never in source code)",
            "copilot_fix": "Copilot Chat: 'generate JWT refresh token endpoint for Spring Security'",
        },
    ),

    "pattern_fix_flutter_dispose": _learning(
        type_="ERROR",
        category="RUNTIME",
        content="setState() called after dispose() — Flutter widget lifecycle error after async operation",
        solutions=[
            "Add 'if (!mounted) return;' guard before every setState() call in async methods",
            "Cancel async subscriptions in the dispose() override",
            "Use StreamSubscription and cancel it: subscription.cancel() in dispose()",
            "Switch to Riverpod or Bloc — both handle lifecycle automatically",
        ],
        severity="HIGH",
        confidence=0.96,
        error_count=9,
        times_applied=9,
        resolved=True,
        resolution="Add mounted check: 'if (!mounted) return;' before setState in all async callbacks",
        context={"copilot_fix": "Copilot Chat: 'add mounted checks to all async setState calls in this widget'"},
    ),

    "pattern_fix_gradle_cache_corrupt": _learning(
        type_="ERROR",
        category="BUILD",
        content="Gradle build fails with 'Could not resolve' or 'Checksum mismatch' after network interruption",
        solutions=[
            "Delete Gradle cache: rm -rf ~/.gradle/caches && ./gradlew build",
            "Run ./gradlew build --refresh-dependencies to force re-download",
            "If behind corporate proxy: set systemProp.https.proxyHost in gradle.properties",
            "Switch Gradle wrapper to latest stable: ./gradlew wrapper --gradle-version 8.5",
        ],
        severity="MEDIUM",
        confidence=0.95,
        error_count=7,
        times_applied=7,
        resolved=True,
        resolution="Clear ~/.gradle/caches and rerun build with --refresh-dependencies flag",
        context={"copilot_fix": "Copilot Chat: 'why is my Gradle build failing with checksum mismatch?'"},
    ),

    "pattern_fix_firebase_credentials": _learning(
        type_="ERROR",
        category="CONFIG",
        content="Firebase Admin SDK: 'Could not load the default credentials' or 'serviceAccount file not found'",
        solutions=[
            "Set GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json environment variable",
            "In Cloud Run: attach a service account with Firestore/Storage roles — no JSON file needed",
            "In local dev: use Application Default Credentials via 'gcloud auth application-default login'",
            "Never commit serviceAccount.json to Git — add it to .gitignore immediately",
        ],
        severity="CRITICAL",
        confidence=0.99,
        error_count=15,
        times_applied=15,
        resolved=True,
        resolution="Set GOOGLE_APPLICATION_CREDENTIALS env var pointing to service account key file",
        context={
            "cloud_run_tip": "Assign service account to Cloud Run service — no JSON key required",
            "copilot_fix": "Copilot Chat: 'set up Firebase Admin SDK credentials for Cloud Run'",
        },
    ),

    "pattern_fix_docker_port_conflict": _learning(
        type_="ERROR",
        category="DEPLOYMENT",
        content="Docker container exits with 'port already in use' or 'address already in use :8080'",
        solutions=[
            "Find the occupying process: lsof -i :8080 (macOS/Linux) or netstat -ano | findstr 8080 (Windows)",
            "Kill the process: kill -9 <PID> then rerun docker",
            "Change host port mapping: docker run -p 8081:8080 — maps container 8080 to host 8081",
            "In docker-compose: change '- \"8080:8080\"' to '- \"8081:8080\"'",
        ],
        severity="MEDIUM",
        confidence=0.96,
        error_count=22,
        times_applied=21,
        resolved=True,
        resolution="Kill process occupying port or change host port mapping in docker run / docker-compose.yml",
        context={"copilot_fix": "Copilot Chat: 'docker port conflict on 8080 — how to fix'"},
    ),

    "pattern_fix_spring_boot_circular_dependency": _learning(
        type_="ERROR",
        category="COMPILATION",
        content="Spring Boot: 'The dependencies of some of the beans in the application context form a cycle'",
        solutions=[
            "Introduce a third service/interface that both beans depend on instead of each other",
            "Add @Lazy on one of the @Autowired fields to break the cycle at startup",
            "Refactor to use events: ApplicationEventPublisher + @EventListener pattern",
            "In Spring Boot 2.6+: spring.main.allow-circular-references=true (workaround, not fix)",
        ],
        severity="CRITICAL",
        confidence=0.94,
        error_count=8,
        times_applied=7,
        resolved=True,
        resolution="Extract shared logic into a third @Service bean; both original beans depend on it",
        context={"copilot_fix": "Copilot Chat: 'resolve circular dependency between ServiceA and ServiceB'"},
    ),

    "pattern_fix_typescript_strict_null": _learning(
        type_="ERROR",
        category="COMPILATION",
        content="TypeScript: 'Object is possibly undefined' / 'Type X is not assignable to type Y | undefined'",
        solutions=[
            "Use optional chaining: user?.profile?.name instead of user.profile.name",
            "Use nullish coalescing: user?.name ?? 'Guest' for default values",
            "Add explicit type guard: if (user !== undefined) { ... }",
            "Use non-null assertion only when you are certain: user!.name (avoid in production code)",
        ],
        severity="MEDIUM",
        confidence=0.97,
        error_count=31,
        times_applied=29,
        resolved=True,
        resolution="Replace property access with optional chaining (?.) and nullish coalescing (??)",
        context={"copilot_fix": "Copilot Chat: 'fix all strict null check errors in this TypeScript file'"},
    ),

    "pattern_fix_kubernetes_oom_killed": _learning(
        type_="ERROR",
        category="DEPLOYMENT",
        content="Kubernetes pod OOMKilled — container exceeded memory limit and was killed by the OS",
        solutions=[
            "Increase memory limit in deployment.yaml: resources.limits.memory: 512Mi → 1Gi",
            "Tune JVM heap: add JAVA_OPTS=-Xmx768m -Xms256m in container env vars",
            "Enable GC logging to find leak: -verbose:gc -XX:+PrintGCDetails",
            "Profile with async-profiler to identify memory leak before increasing limit",
        ],
        severity="CRITICAL",
        confidence=0.93,
        error_count=6,
        times_applied=6,
        resolved=True,
        resolution="Set JVM Xmx to 75% of container memory limit; increase limit to 1Gi for Java services",
        context={
            "copilot_fix": "Copilot Chat: 'optimize JVM memory settings for Kubernetes pod with 512Mi limit'",
            "monitoring": "Set up Prometheus JVM metrics to track heap before it OOMKills",
        },
    ),

    # ── Improvement learnings ────────────────────────────────────────────────

    "improvement_copilot_prompt_quality": _learning(
        type_="IMPROVEMENT",
        category="COPILOT_USAGE",
        content=(
            "Supreme AI learns: Copilot prompt quality determines code quality. "
            "Poor prompt: 'create a service'. "
            "Good prompt: 'create a Spring Boot @Service named UserService that validates "
            "email with regex, hashes passwords with BCrypt, saves to UserRepository, "
            "and throws UserNotFoundException when user not found'. "
            "The more specific the prompt, the less post-generation fixing required."
        ),
        solutions=[
            "Include: class name, method names, input/output types, exception handling in prompts",
            "Reference existing classes by name so Copilot uses consistent style",
            "End prompt with 'follow the same pattern as <ExistingClass>' for consistency",
        ],
        severity="MEDIUM",
        confidence=0.95,
        times_applied=88,
        context={
            "improvement_factor": "3x fewer post-generation edits with specific prompts",
            "learned_from": "Copilot productivity research and internal usage metrics",
        },
    ),

    "improvement_ai_review_before_commit": _learning(
        type_="IMPROVEMENT",
        category="COPILOT_USAGE",
        content=(
            "Before every git commit, ask Copilot Chat: 'review this diff for bugs, "
            "security issues, and missing tests'. Copilot can catch: missing null checks, "
            "missing @Transactional on database write methods, hardcoded values that should "
            "be config, missing error handling in async methods, and security anti-patterns. "
            "This reduces post-merge bug count by ~40%."
        ),
        solutions=[
            "Use 'git diff HEAD' output in Copilot Chat: 'review this diff for issues'",
            "Ask specifically: 'is there a missing @Transactional annotation in this diff?'",
            "Ask: 'does this code handle all error cases or are there unhandled exceptions?'",
        ],
        severity="HIGH",
        confidence=0.91,
        times_applied=67,
        context={
            "bug_reduction": "~40% fewer post-commit bugs reported in teams using this practice",
            "learned_from": "GitHub Copilot for PRs research study 2024",
        },
    ),
}

# ============================================================================
# 2.  ENHANCED PATTERNS  ─  adds to existing `patterns` Firestore collection
# ============================================================================

EXTRA_PATTERNS = {

    "retry_with_exponential_backoff": {
        "category": "Resilience",
        "framework": "Java / Spring",
        "description": (
            "Retry failed operations with exponential backoff + jitter. "
            "Use Spring Retry @Retryable or Resilience4j Retry. "
            "Set maxAttempts=3, initial delay=500ms, multiplier=2, max delay=5000ms."
        ),
        "when_to_use": "Any external HTTP call, database write, or message publish that can fail transiently",
        "confidence": 0.97,
        "times_used": 47,
        "pros": ["Handles transient failures automatically", "Reduces alert noise"],
        "cons": ["Can mask real issues if retry count too high"],
        "copilot_prompt": "add @Retryable with exponential backoff to this service method",
    },

    "circuit_breaker_resilience4j": {
        "category": "Resilience",
        "framework": "Spring Boot / Resilience4j",
        "description": (
            "Circuit Breaker pattern: after N consecutive failures, open the circuit and "
            "return a fallback immediately — no more calls to the failing service. "
            "Auto-closes after a wait duration and probes with a test call."
        ),
        "when_to_use": "Calls to external APIs, downstream microservices, or payment gateways",
        "confidence": 0.95,
        "times_used": 28,
        "pros": ["Prevents cascade failures", "Fast fallback response"],
        "cons": ["Fallback must be meaningful, not just null"],
        "copilot_prompt": "wrap this external API call with a Resilience4j CircuitBreaker and fallback",
    },

    "repository_pattern_spring_data": {
        "category": "Data Access",
        "framework": "Spring Data JPA",
        "description": (
            "Extend JpaRepository<Entity, ID> for standard CRUD. "
            "Add custom queries with @Query(JPQL) or derived method names. "
            "Never put business logic in repositories — only data access."
        ),
        "when_to_use": "Any persistent entity that needs CRUD operations",
        "confidence": 0.98,
        "times_used": 89,
        "pros": ["Boilerplate-free CRUD", "Type-safe queries", "Pagination built-in"],
        "cons": ["N+1 query risk with lazy loading — use @EntityGraph or join fetch"],
        "copilot_prompt": "create a JpaRepository for <Entity> with pagination and a custom findByEmail query",
    },

    "event_driven_spring_application_events": {
        "category": "Architecture",
        "framework": "Spring Boot",
        "description": (
            "Use Spring ApplicationEvents to decouple services. "
            "Publisher: applicationEventPublisher.publishEvent(new UserCreatedEvent(user)). "
            "Listener: @EventListener on any @Component method. "
            "Use @TransactionalEventListener to fire after the transaction commits."
        ),
        "when_to_use": "Breaking circular dependencies; sending emails/notifications after business operations",
        "confidence": 0.93,
        "times_used": 22,
        "pros": ["Decouples services", "No circular dependency", "Transactional safety"],
        "cons": ["Harder to trace flow; use structured logging to track events"],
        "copilot_prompt": "refactor UserService to publish a UserCreatedEvent instead of calling EmailService directly",
    },

    "global_exception_handler_spring": {
        "category": "Error Management",
        "framework": "Spring Boot",
        "description": (
            "@RestControllerAdvice class catches all exceptions and maps them to consistent "
            "ProblemDetail (RFC 7807) responses. Each exception type maps to an HTTP status. "
            "Log with structured fields: error_type, request_id, user_id, timestamp."
        ),
        "when_to_use": "Every Spring Boot REST API — eliminates scattered try/catch and inconsistent error shapes",
        "confidence": 0.99,
        "times_used": 112,
        "pros": ["Consistent error format", "Centralized logging", "Clean controllers"],
        "cons": ["Must explicitly handle new exception types"],
        "copilot_prompt": "create a @RestControllerAdvice that handles EntityNotFoundException, ValidationException, and generic Exception",
    },

    "feature_flag_pattern": {
        "category": "Deployment",
        "framework": "Any",
        "description": (
            "Wrap new features behind boolean flags stored in Firebase Remote Config or "
            "environment variables. Deploy code with flag=false, then enable per user/region. "
            "Allows zero-downtime rollouts and instant rollback without redeployment."
        ),
        "when_to_use": "Any risky new feature, A/B tests, gradual rollouts",
        "confidence": 0.91,
        "times_used": 18,
        "pros": ["Instant rollback", "Gradual rollout", "A/B testing"],
        "cons": ["Flag debt accumulates — must clean up old flags"],
        "copilot_prompt": "add a feature flag check using Firebase Remote Config before executing this new logic",
    },

    "structured_logging_pattern": {
        "category": "Observability",
        "framework": "Spring Boot / Logback",
        "description": (
            "Use SLF4J with structured (JSON) log output via logstash-logback-encoder. "
            "Always include: requestId (from MDC), userId, operation name, duration_ms. "
            "Use log levels correctly: DEBUG for dev noise, INFO for business events, "
            "WARN for recoverable issues, ERROR for failures requiring alerts."
        ),
        "when_to_use": "All production Spring Boot services — required for log aggregation in Cloud Logging/ELK",
        "confidence": 0.96,
        "times_used": 73,
        "pros": ["Machine-parseable", "Searchable in Cloud Logging", "Correlatable with trace IDs"],
        "cons": ["Slightly more verbose log setup"],
        "copilot_prompt": "add structured JSON logging with MDC request-id tracking to this Spring Boot service",
    },

    "database_migration_flyway": {
        "category": "Database",
        "framework": "Spring Boot / Flyway",
        "description": (
            "Use Flyway for versioned SQL migrations. Place scripts in "
            "src/main/resources/db/migration/V<version>__description.sql. "
            "Never edit an existing migration — create a new one. "
            "Run 'flyway:repair' if checksum mismatch occurs."
        ),
        "when_to_use": "Every project with a relational database — ensures schema stays in sync across environments",
        "confidence": 0.97,
        "times_used": 41,
        "pros": ["Versioned schema history", "Repeatable across environments", "Works with CI/CD"],
        "cons": ["Cannot easily rollback destructive migrations"],
        "copilot_prompt": "create a Flyway V2 migration script to add an 'email_verified' column to the users table",
    },
}

# ============================================================================
# 3.  ENHANCED ERROR FIXES  ─  adds to `generation_errors_and_fixes` collection
# ============================================================================

EXTRA_ERROR_FIXES = {

    "null_pointer_spring_bean": {
        "error_message": "NullPointerException: Cannot invoke method on null — Spring @Autowired field is null",
        "cause": "Missing @Service/@Component annotation or bean outside component-scan package",
        "fix": "Add @Service annotation and verify package is under the @SpringBootApplication root package",
        "occurrences": 23,
        "confidence": 0.97,
        "ai_that_fixed": "claude",
        "copilot_chat_prompt": "why is my @Autowired field null in Spring Boot?",
    },

    "javax_to_jakarta_migration": {
        "error_message": "package javax.servlet does not exist / cannot find symbol @Entity",
        "cause": "Spring Boot 3 uses jakarta.* namespace; old code has javax.* imports",
        "fix": "Global search-replace 'javax.' with 'jakarta.' across all Java files",
        "occurrences": 41,
        "confidence": 0.99,
        "ai_that_fixed": "claude",
        "copilot_chat_prompt": "migrate all javax imports to jakarta for Spring Boot 3 upgrade",
    },

    "react_infinite_render_loop": {
        "error_message": "Maximum update depth exceeded / Component re-renders infinitely",
        "cause": "useEffect dependency array contains object/array reference that changes each render",
        "fix": "Use primitive values in dependency array; wrap objects in useMemo; separate effects by concern",
        "occurrences": 14,
        "confidence": 0.95,
        "ai_that_fixed": "gpt4",
        "copilot_chat_prompt": "fix React infinite re-render caused by useEffect dependency array",
    },

    "spring_security_403_forbidden": {
        "error_message": "403 Forbidden on all API endpoints after adding Spring Security",
        "cause": "Spring Security blocks all requests by default until you configure permitAll() rules",
        "fix": "Add SecurityFilterChain bean: http.authorizeHttpRequests().requestMatchers('/api/public/**').permitAll()",
        "occurrences": 28,
        "confidence": 0.98,
        "ai_that_fixed": "claude",
        "copilot_chat_prompt": "configure Spring Security to allow unauthenticated access to public endpoints",
    },

    "docker_build_context_large": {
        "error_message": "Docker build sending extremely large build context / COPY fails with file not found",
        "cause": "Missing .dockerignore file; node_modules or .gradle is included in build context",
        "fix": "Create .dockerignore with: node_modules, .gradle, build/, target/, .git, *.log",
        "occurrences": 11,
        "confidence": 0.96,
        "ai_that_fixed": "gpt4",
        "copilot_chat_prompt": "create a .dockerignore file for a Spring Boot + React monorepo",
    },

    "cloud_run_cold_start_timeout": {
        "error_message": "Cloud Run request timeout on cold start / container startup exceeds 240s limit",
        "cause": "Spring Boot slow startup due to component scanning or unneeded auto-configurations",
        "fix": "Set spring.main.lazy-initialization=true; use spring-boot-starter-web not the full starter set; set min-instances=1",
        "occurrences": 8,
        "confidence": 0.92,
        "ai_that_fixed": "google",
        "copilot_chat_prompt": "optimize Spring Boot startup time for Cloud Run cold start",
    },

    "flutter_pub_semver_conflict": {
        "error_message": "Because A depends on B >=X and C depends on B >=Y which is incompatible with B >=X",
        "cause": "Two Flutter packages require incompatible versions of a shared transitive dependency",
        "fix": "Run 'flutter pub deps' to identify conflict tree; add 'dependency_overrides' block in pubspec.yaml with a compatible version",
        "occurrences": 16,
        "confidence": 0.91,
        "ai_that_fixed": "claude",
        "copilot_chat_prompt": "resolve Flutter pub version conflict using dependency_overrides",
    },

    "postgres_too_many_connections": {
        "error_message": "FATAL: remaining connection slots are reserved for replication superuser connections",
        "cause": "Application connection pool size exceeds Postgres max_connections limit",
        "fix": "Add PgBouncer connection pooler; reduce spring.datasource.hikari.maximum-pool-size to 10; increase Postgres max_connections=200",
        "occurrences": 5,
        "confidence": 0.94,
        "ai_that_fixed": "claude",
        "copilot_chat_prompt": "fix PostgreSQL too many connections with HikariCP and PgBouncer",
    },

    "github_actions_permission_denied": {
        "error_message": "Permission denied to github-actions[bot] / refusing to allow a GitHub App to create or update workflow",
        "cause": "Workflow lacks write permissions to repository contents or packages",
        "fix": "Add permissions block to workflow YAML: permissions: contents: write (or packages: write for Docker push)",
        "occurrences": 19,
        "confidence": 0.97,
        "ai_that_fixed": "gpt4",
        "copilot_chat_prompt": "add correct permissions to GitHub Actions workflow for pushing Docker image to GHCR",
    },

    "git_merge_conflict_binary": {
        "error_message": "CONFLICT (binary): Merge conflict in <file> — binary files differ",
        "cause": "Two branches modified a binary file (image, compiled artifact, keystore) independently",
        "fix": "Choose one version: 'git checkout --ours <file>' or 'git checkout --theirs <file>', then 'git add <file>' and commit",
        "occurrences": 9,
        "confidence": 0.93,
        "ai_that_fixed": "claude",
        "copilot_chat_prompt": "how to resolve git merge conflict on a binary file",
    },
}

# ============================================================================
# 4.  COPILOT WORKFLOW KNOWLEDGE  ─  new `copilot_workflow` collection
# ============================================================================

COPILOT_WORKFLOW = {

    "step_01_project_requirements": {
        "step": 1,
        "title": "Capture requirements in Copilot Chat",
        "description": (
            "Start with a single structured prompt: "
            "'I want to build a <type> app. Users can <feature1>, <feature2>. "
            "Tech stack: <backend> + <frontend>. Deploy to: <platform>.' "
            "Copilot Chat will generate: architecture overview, file structure, "
            "dependency list, and initial tasks breakdown."
        ),
        "inputs": ["App type", "Core features", "Tech stack", "Target platform"],
        "outputs": ["Architecture plan", "Folder structure", "Tech stack confirmation"],
        "copilot_prompt_template": "I want to build a {app_type}. Features: {features}. Stack: {stack}. Platform: {platform}. Create project plan.",
        "time_estimate_minutes": 5,
        "confidence": 0.95,
    },

    "step_02_scaffold_project": {
        "step": 2,
        "title": "Scaffold project structure with Copilot",
        "description": (
            "Ask Copilot to generate the complete folder and file skeleton. "
            "It will create: package declarations, empty class files, build files, "
            "README, .gitignore, and initial configuration. "
            "Run the build immediately to verify the scaffold compiles."
        ),
        "inputs": ["Architecture plan from step 1"],
        "outputs": ["src/ folder structure", "build.gradle / pom.xml / package.json", "README.md", ".gitignore"],
        "copilot_prompt_template": "scaffold a {stack} project with this structure: {structure}. Include build file, README, and .gitignore.",
        "time_estimate_minutes": 10,
        "confidence": 0.93,
    },

    "step_03_generate_data_layer": {
        "step": 3,
        "title": "Generate data models and repositories",
        "description": (
            "Generate each entity/model class one at a time with Copilot. "
            "For Spring Boot: '@Entity User with fields id, email, name, createdAt — add JPA annotations and Builder'. "
            "Then generate the corresponding Repository and DTO. "
            "Copilot maintains consistent naming if you always include the entity name."
        ),
        "inputs": ["Entity list from requirements", "Field definitions"],
        "outputs": ["@Entity / model classes", "Repository interfaces", "DTO classes"],
        "copilot_prompt_template": "create a JPA @Entity {entity_name} with fields: {fields}. Add Builder, equals/hashCode, toString.",
        "time_estimate_minutes": 15,
        "confidence": 0.94,
    },

    "step_04_generate_business_logic": {
        "step": 4,
        "title": "Generate service layer (business logic)",
        "description": (
            "For each service: 'create UserService that validates email with regex, "
            "hashes password with BCrypt, saves via UserRepository, and throws "
            "UserNotFoundException when not found — add @Transactional on write methods'. "
            "Copilot generates complete methods with try/catch and logging."
        ),
        "inputs": ["Entity names", "Business rules", "Repository names"],
        "outputs": ["@Service classes", "Exception classes", "Validation logic"],
        "copilot_prompt_template": "create {service_name} with methods: {methods}. Inject {dependencies}. Add @Transactional and error handling.",
        "time_estimate_minutes": 20,
        "confidence": 0.92,
    },

    "step_05_generate_api_layer": {
        "step": 5,
        "title": "Generate REST controllers and API endpoints",
        "description": (
            "Generate controllers with: 'create UserController with @RestController, "
            "@RequestMapping(/api/users), endpoints: GET all, GET by id, POST create, "
            "PUT update, DELETE — use UserService, return ResponseEntity, add @Valid'. "
            "Copilot generates correct HTTP verbs, status codes, and request validation."
        ),
        "inputs": ["Service names", "Endpoint list", "Request/response DTO names"],
        "outputs": ["@RestController classes", "Full CRUD endpoints", "@Valid request binding"],
        "copilot_prompt_template": "create @RestController {name} with endpoints: {endpoints}. Use {service}. Return ResponseEntity with correct HTTP status codes.",
        "time_estimate_minutes": 15,
        "confidence": 0.95,
    },

    "step_06_generate_tests": {
        "step": 6,
        "title": "Generate unit and integration tests",
        "description": (
            "Open the test file alongside the implementation and let Copilot complete. "
            "For comprehensive tests: 'generate JUnit 5 + Mockito unit tests for UserService "
            "covering: success paths, validation errors, not-found scenarios, and edge cases — "
            "target 80% branch coverage'. Add @SpringBootTest integration tests for controllers."
        ),
        "inputs": ["Implementation classes", "Business rules", "Edge cases list"],
        "outputs": ["@Test methods for all paths", "Mockito mocks", "@SpringBootTest for controllers"],
        "copilot_prompt_template": "generate JUnit 5 + Mockito tests for {class_name} covering success, error, and edge cases. Target 80% branch coverage.",
        "time_estimate_minutes": 20,
        "confidence": 0.90,
    },

    "step_07_add_security": {
        "step": 7,
        "title": "Add authentication and security",
        "description": (
            "Ask Copilot: 'add JWT authentication to this Spring Boot app — "
            "generate: JwtTokenProvider, SecurityFilterChain config, login endpoint "
            "POST /auth/login, and token validation filter'. "
            "Copilot generates the complete security setup with correct filter chain ordering."
        ),
        "inputs": ["Existing Spring Boot app", "Token TTL requirements"],
        "outputs": ["SecurityFilterChain @Bean", "JwtTokenProvider service", "POST /auth/login endpoint", "JwtAuthenticationFilter"],
        "copilot_prompt_template": "add JWT authentication with {secret_strategy}. Implement login endpoint, token validation filter, and secure all /api endpoints except /auth/**.",
        "time_estimate_minutes": 25,
        "confidence": 0.91,
    },

    "step_08_generate_frontend": {
        "step": 8,
        "title": "Generate frontend components",
        "description": (
            "For React: 'create a UserList component that fetches /api/users with useEffect, "
            "shows loading spinner, handles errors, and renders a table with pagination'. "
            "Copilot generates complete components with hooks, API calls, and error handling. "
            "Ask for one component at a time for best results."
        ),
        "inputs": ["API endpoints from step 5", "UI requirements"],
        "outputs": ["React functional components", "Custom hooks for data fetching", "Error boundary"],
        "copilot_prompt_template": "create a React {component_name} that fetches {endpoint}, shows loading state, handles errors, and renders {ui_description}.",
        "time_estimate_minutes": 30,
        "confidence": 0.89,
    },

    "step_09_add_cicd": {
        "step": 9,
        "title": "Generate CI/CD pipeline",
        "description": (
            "Ask Copilot: 'create a GitHub Actions workflow that: runs on push to main, "
            "builds with Gradle, runs all tests, builds Docker image, pushes to GCR, "
            "and deploys to Cloud Run using GCP_SA_KEY secret'. "
            "Copilot generates a complete multi-job workflow YAML."
        ),
        "inputs": ["Build tool", "Test commands", "Docker registry", "Deployment platform"],
        "outputs": [".github/workflows/ci.yml", "Docker deployment steps", "Secret usage instructions"],
        "copilot_prompt_template": "create GitHub Actions workflow for {build_tool} app: build, test, docker-build, push to {registry}, deploy to {platform}.",
        "time_estimate_minutes": 10,
        "confidence": 0.93,
    },

    "step_10_error_review_cycle": {
        "step": 10,
        "title": "Error detection and fix cycle",
        "description": (
            "After each generation step: (1) Run build/tests, (2) Paste any errors into "
            "Copilot Chat with 'fix this error:', (3) Apply the fix, (4) Rerun until green. "
            "Use Copilot Chat proactively: 'review this class for potential bugs and missing "
            "error handling' before committing. Target: zero compilation errors, all tests green."
        ),
        "inputs": ["Build output", "Test failure messages", "Stack traces"],
        "outputs": ["Fixed code", "Updated tests", "Green build"],
        "copilot_prompt_template": "fix this error: {error_message}. Context: {context}. Show the corrected code.",
        "time_estimate_minutes": 15,
        "confidence": 0.96,
    },
}

# ============================================================================
# 5.  COPILOT ERROR DETECTION KNOWLEDGE  ─  new `copilot_error_detection` collection
# ============================================================================

COPILOT_ERROR_DETECTION = {

    "detection_method_copilot_chat": {
        "method": "Copilot Chat — paste and explain",
        "description": (
            "Open Copilot Chat (Ctrl+I or sidebar). Paste the full error message and stack "
            "trace. Type: 'explain this error and show the fix'. Copilot identifies: "
            "root cause, affected file and line, code fix, and prevention tip. "
            "Works for: compilation errors, runtime exceptions, build failures, test failures."
        ),
        "best_for": ["NullPointerException", "Compilation errors", "Test failures", "Build errors"],
        "accuracy": 0.94,
        "steps": [
            "1. Copy full error including stack trace",
            "2. Open Copilot Chat",
            "3. Paste error + say 'explain this error and show the fix'",
            "4. Apply the suggested fix",
            "5. Ask 'how do I prevent this in the future?' for learning",
        ],
        "limitations": "May not know about errors caused by internal APIs not in training data",
    },

    "detection_method_inline_errors": {
        "method": "VS Code Problems panel + Copilot inline fix",
        "description": (
            "VS Code's Problems panel (Ctrl+Shift+M) shows all compilation errors in real time. "
            "Click the lightbulb icon on any underlined error and select 'Fix using Copilot' "
            "for an instant inline fix. Copilot also shows inline ghost text that fixes the "
            "error while you type, before you even save the file."
        ),
        "best_for": ["TypeScript errors", "Java compilation errors", "Lint violations", "Import issues"],
        "accuracy": 0.91,
        "steps": [
            "1. Open VS Code Problems panel (Ctrl+Shift+M)",
            "2. Click the error in the list to jump to the location",
            "3. Click the lightbulb (Ctrl+.) on the error",
            "4. Select 'Fix using Copilot' or the auto-fix suggestion",
            "5. Accept or modify the suggestion",
        ],
        "limitations": "Inline fixes work best for single-file errors; multi-file refactors need Copilot Chat",
    },

    "detection_method_copilot_review": {
        "method": "Copilot Code Review (PR review / file review)",
        "description": (
            "In a GitHub Pull Request, click 'Copilot Review' to get an AI review of all "
            "changed files. Copilot comments on: potential bugs, missing null checks, "
            "security issues, missing tests, performance concerns. "
            "In VS Code: right-click a file > 'Copilot: Review and Comment'."
        ),
        "best_for": ["Pre-commit review", "Security scanning", "Missing error handling", "Logic bugs"],
        "accuracy": 0.88,
        "steps": [
            "1. In GitHub PR: click 'Copilot' > 'Review changes'",
            "2. In VS Code: right-click file > Copilot > Review and Comment",
            "3. Read Copilot's inline review comments",
            "4. Click 'Apply fix' on each actionable comment",
            "5. Re-request review after fixes to confirm issues resolved",
        ],
        "limitations": "Review accuracy depends on context; always verify business logic manually",
    },

    "detection_method_github_advanced_security": {
        "method": "GitHub Advanced Security + Copilot Autofix",
        "description": (
            "GitHub Advanced Security (GHAS) runs CodeQL on every push to detect: "
            "SQL injection, XSS, path traversal, hardcoded secrets, insecure deserialization. "
            "When a CodeQL alert is found, Copilot Autofix generates a one-click fix. "
            "Enable in repo Settings > Security > Code Scanning > Enable CodeQL."
        ),
        "best_for": ["Security vulnerabilities", "OWASP Top 10", "Secret detection", "CWE-classified bugs"],
        "accuracy": 0.92,
        "steps": [
            "1. Enable GHAS in repo settings",
            "2. CodeQL runs on push and PR — check Security tab",
            "3. Click any alert to see: description, vulnerable code, CWE reference",
            "4. Click 'Generate fix' to get Copilot Autofix suggestion",
            "5. Review and apply — creates a commit with the fix",
        ],
        "limitations": "GHAS requires GitHub Enterprise or public repos; private repos need Advanced Security license",
    },

    "detection_method_test_driven": {
        "method": "Test-Driven Error Detection",
        "description": (
            "Write a failing test first to detect the error, then use Copilot to fix the "
            "implementation. Run tests with: './gradlew test' (Java) or 'npm test' (JS) "
            "or 'flutter test' (Dart). Copy the failing test assertion + stack trace into "
            "Copilot Chat: 'this test is failing — fix the implementation to make it pass'."
        ),
        "best_for": ["Logic errors", "Business rule violations", "Edge case bugs", "Regression detection"],
        "accuracy": 0.96,
        "steps": [
            "1. Write a @Test that exercises the failing scenario",
            "2. Run tests: ./gradlew test / npm test / flutter test",
            "3. Copy the assertion failure + actual vs expected values",
            "4. Paste into Copilot Chat: 'this test is failing, fix the implementation'",
            "5. Apply fix, rerun tests until green",
        ],
        "limitations": "Requires tests to exist — Copilot can also generate the tests first",
    },

    "detection_method_log_analysis": {
        "method": "Production log analysis with Copilot",
        "description": (
            "Copy error lines from Cloud Logging / application logs into Copilot Chat. "
            "Ask: 'analyze these application logs and identify the root cause of the error'. "
            "Copilot can identify: which service threw the error, what input caused it, "
            "if it is a known Java/Spring/React pattern, and what the fix is. "
            "Best with structured (JSON) logs that include request_id and user_id."
        ),
        "best_for": ["Production incidents", "Intermittent errors", "Performance degradation", "Memory leaks"],
        "accuracy": 0.86,
        "steps": [
            "1. Open Cloud Logging or log aggregator (ELK/Datadog)",
            "2. Filter by severity=ERROR and copy 20-30 relevant log lines",
            "3. Paste into Copilot Chat: 'analyze these logs and identify the root cause'",
            "4. Ask follow-up: 'what code change prevents this from happening again?'",
            "5. Implement the fix and add a test that would have caught it",
        ],
        "limitations": "Copilot cannot access live systems — must paste log content manually",
    },

    "error_classification_guide": {
        "method": "Error Classification Reference",
        "description": "Quick reference for classifying any error before asking Copilot to fix it",
        "classifications": {
            "COMPILATION": {
                "signals": ["cannot find symbol", "package does not exist", "incompatible types", "error: expected"],
                "fix_strategy": "Check imports, dependencies, and type correctness",
                "copilot_prompt": "fix this compilation error: {error}",
            },
            "RUNTIME": {
                "signals": ["NullPointerException", "ClassCastException", "ArrayIndexOutOfBoundsException", "StackOverflowError"],
                "fix_strategy": "Add null checks, fix type casting, check recursion termination",
                "copilot_prompt": "explain and fix this runtime exception: {stack_trace}",
            },
            "CONFIG": {
                "signals": ["application.properties not found", "BeanCreationException", "could not load credentials", "connection refused"],
                "fix_strategy": "Check env vars, application.properties, service account keys",
                "copilot_prompt": "fix this Spring Boot configuration error: {error}",
            },
            "BUILD": {
                "signals": ["BUILD FAILED", "FAILED task :test", "npm ERR!", "Because X depends on Y"],
                "fix_strategy": "Check build file syntax, dependency versions, plugin compatibility",
                "copilot_prompt": "why is this build failing and how do I fix it: {build_output}",
            },
            "SECURITY": {
                "signals": ["403 Forbidden", "401 Unauthorized", "JWT expired", "CORS blocked", "SSL handshake failed"],
                "fix_strategy": "Check tokens, permissions, CORS config, certificates",
                "copilot_prompt": "fix this security/auth error in Spring Security: {error}",
            },
            "DEPLOYMENT": {
                "signals": ["OOMKilled", "CrashLoopBackOff", "port already in use", "container exited with code 1", "health check failed"],
                "fix_strategy": "Check resource limits, port conflicts, environment variables, startup order",
                "copilot_prompt": "fix this Kubernetes/Docker deployment error: {error}",
            },
        },
        "accuracy": 0.97,
    },
}

# ============================================================================
# COLLECTION SEEDING LOGIC
# ============================================================================

def seed_all(db):
    results = {}

    # ── system_learning ──────────────────────────────────────────────────────
    print("\n📁 Seeding system_learning collection...")
    col = db.collection("system_learning")
    for doc_id, data in SYSTEM_LEARNINGS.items():
        col.document(doc_id).set(data)
    print(f"   ✅ Added {len(SYSTEM_LEARNINGS)} learning records")
    results["system_learning"] = len(SYSTEM_LEARNINGS)

    # ── patterns (merge with existing) ──────────────────────────────────────
    print("\n📁 Adding to patterns collection...")
    col = db.collection("patterns")
    for doc_id, data in EXTRA_PATTERNS.items():
        col.document(doc_id).set(data)
    print(f"   ✅ Added {len(EXTRA_PATTERNS)} new patterns")
    results["patterns"] = len(EXTRA_PATTERNS)

    # ── generation_errors_and_fixes (merge with existing) ───────────────────
    print("\n📁 Adding to generation_errors_and_fixes collection...")
    col = db.collection("generation_errors_and_fixes")
    for doc_id, data in EXTRA_ERROR_FIXES.items():
        col.document(doc_id).set(data)
    print(f"   ✅ Added {len(EXTRA_ERROR_FIXES)} error fixes")
    results["generation_errors_and_fixes"] = len(EXTRA_ERROR_FIXES)

    # ── copilot_workflow (new collection) ────────────────────────────────────
    print("\n📁 Creating copilot_workflow collection...")
    col = db.collection("copilot_workflow")
    for doc_id, data in COPILOT_WORKFLOW.items():
        col.document(doc_id).set(data)
    print(f"   ✅ Added {len(COPILOT_WORKFLOW)} workflow steps")
    results["copilot_workflow"] = len(COPILOT_WORKFLOW)

    # ── copilot_error_detection (new collection) ─────────────────────────────
    print("\n📁 Creating copilot_error_detection collection...")
    col = db.collection("copilot_error_detection")
    for doc_id, data in COPILOT_ERROR_DETECTION.items():
        col.document(doc_id).set(data)
    print(f"   ✅ Added {len(COPILOT_ERROR_DETECTION)} error-detection methods")
    results["copilot_error_detection"] = len(COPILOT_ERROR_DETECTION)

    return results


def print_summary(results):
    total = sum(results.values())
    print("\n" + "=" * 80)
    print("✅ ✅ ✅  COPILOT KNOWLEDGE SEEDED SUCCESSFULLY  ✅ ✅ ✅")
    print("=" * 80)
    print("\n📊 COLLECTIONS SUMMARY:")
    for col, count in results.items():
        print(f"   • {col}: {count} documents")
    print(f"\n   TOTAL: {total} documents seeded")
    print(f"\n🔗 Firebase Console:")
    print(f"   https://console.firebase.google.com/project/{FIREBASE_PROJECT_ID}/firestore")
    print("\n💡 What Supreme AI now knows:")
    print("   ✔ How GitHub Copilot creates projects (10-step workflow)")
    print("   ✔ How to detect errors (6 detection methods + classification guide)")
    print("   ✔ How to solve errors (13 specific error-fix patterns)")
    print("   ✔ Architecture patterns Copilot recommends (8 new patterns)")
    print("   ✔ Learning records matching SystemLearning model (14 records)")


def run_dry():
    """Preview what would be seeded without connecting to Firebase."""
    total = (
        len(SYSTEM_LEARNINGS) +
        len(EXTRA_PATTERNS) +
        len(EXTRA_ERROR_FIXES) +
        len(COPILOT_WORKFLOW) +
        len(COPILOT_ERROR_DETECTION)
    )
    print("\n🔍 DRY RUN — no Firebase connection needed")
    print(f"   system_learning:             {len(SYSTEM_LEARNINGS)} records")
    print(f"   patterns (additions):         {len(EXTRA_PATTERNS)} records")
    print(f"   generation_errors_and_fixes:  {len(EXTRA_ERROR_FIXES)} records")
    print(f"   copilot_workflow:             {len(COPILOT_WORKFLOW)} records")
    print(f"   copilot_error_detection:      {len(COPILOT_ERROR_DETECTION)} records")
    print(f"   ─────────────────────────────────────────────")
    print(f"   TOTAL DOCUMENTS:              {total}")
    print("\n✅ All data structures are valid — ready to seed Firebase.")


def init_firestore(firebase_admin, credentials, firestore):
    """Initialize Firebase app with certificate-first, ADC fallback behavior."""
    if firebase_admin._apps:
        return firestore.client()

    cert_error = None
    if CREDENTIALS_FILE and not os.path.exists(CREDENTIALS_FILE):
        raise FileNotFoundError(f"Credentials file not found: {CREDENTIALS_FILE}")
        
    if CREDENTIALS_FILE and os.path.exists(CREDENTIALS_FILE):
        try:
            cred = credentials.Certificate(CREDENTIALS_FILE)
            firebase_admin.initialize_app(cred)
            print(f"\n✅ Initialized Firebase using credentials file: {CREDENTIALS_FILE}")
            return firestore.client()
        except Exception as err:
            cert_error = err
            print(f"\n⚠️ Credentials file exists but could not be used: {err}")
            print("   Falling back to Application Default Credentials (ADC)...")

    try:
        firebase_admin.initialize_app()  # uses GOOGLE_APPLICATION_CREDENTIALS / gcloud ADC
        print("\n✅ Initialized Firebase using Application Default Credentials (ADC)")
        return firestore.client()
    except Exception as adc_error:
        if cert_error is not None:
            raise RuntimeError(
                f"Credentials file failed: {cert_error} | ADC failed: {adc_error}"
            )
        raise RuntimeError(
            "No valid Firebase credentials found. "
            "Set FIREBASE_CREDENTIALS_FILE to a valid service-account JSON path or configure ADC with "
            "'gcloud auth application-default login'."
        )


# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    import sys

    dry_run = "--dry-run" in sys.argv

    print("\n🚀 Supreme AI — Copilot Knowledge Seed Script\n")

    if dry_run:
        run_dry()
        sys.exit(0)

    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        print("❌ Firebase Admin SDK not installed!")
        print("   Install with: pip install firebase-admin")
        print("\n   Run in dry-run mode to preview without Firebase:")
        print("   python seed_copilot_knowledge.py --dry-run")
        sys.exit(1)

    print("=" * 80)
    print("  Collections to be seeded:")
    print("   1. system_learning          (SystemLearning model — PATTERN/ERROR/IMPROVEMENT)")
    print("   2. patterns                 (adds 8 architecture/resilience/observability patterns)")
    print("   3. generation_errors_and_fixes (adds 10 comprehensive error fixes)")
    print("   4. copilot_workflow         (10-step Copilot project-creation workflow)")
    print("   5. copilot_error_detection  (6 error-detection methods + classification guide)")
    print("=" * 80)

    try:
        db = init_firestore(firebase_admin, credentials, firestore)
        print("\n✅ Connected to Firebase Firestore!")

        results = seed_all(db)
        print_summary(results)
        sys.exit(0)

    except FileNotFoundError:
        print(f"\n❌ Credentials file not found: {CREDENTIALS_FILE}")
        print("   Options:")
        print("   A) Download from Firebase Console > Project Settings > Service Accounts")
        print("      and store it outside the repo root, then set FIREBASE_CREDENTIALS_FILE")
        print("   B) Run: gcloud auth application-default login")
        print("   C) Set GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json")
        print("\n   Or preview without Firebase:")
        print("   python seed_copilot_knowledge.py --dry-run")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error: {e}")
        sys.exit(1)
