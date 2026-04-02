package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AppCreationKnowledgeSeeder
 *
 * Seeds ALL techniques into SupremeAI's learning memory on startup:
 *  - App creation workflows (Spring Boot, Flutter, React, Android, Desktop, Full-Stack)
 *  - Error solving patterns (compilation, runtime, CI/CD, dependency, DB)
 *  - AI model selection strategies (which AI for which task)
 *  - Code architecture patterns (Service, Controller, Model layers)
 *  - Build & CI/CD patterns (Gradle, Docker, Kubernetes, Cloud Run)
 *  - Security patterns (JWT, input validation, OWASP)
 *  - Test patterns (unit, integration, E2E)
 *
 * Runs ONCE after Spring context is ready. Skips if already seeded.
 */
@Component
public class AppCreationKnowledgeSeeder {

    private static final Logger logger = LoggerFactory.getLogger(AppCreationKnowledgeSeeder.class);

    @Autowired
    private SystemLearningService learningService;

    private static final AtomicBoolean seeded = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void seedAllKnowledge() {
        if (!seeded.compareAndSet(false, true)) {
            logger.info("⚡ Knowledge already seeded — skipping duplicate run");
            return;
        }
        logger.info("🌱 Starting SupremeAI knowledge seeding...");
        try {
            seedAppCreationTechniques();
            seedErrorSolvingPatterns();
            seedAIModelSelectionStrategies();
            seedCodeArchitecturePatterns();
            seedBuildAndCICDPatterns();
            seedSecurityPatterns();
            seedTestPatterns();
            seedSelfHealingPatterns();
            logger.info("✅ All knowledge seeded into SupremeAI learning memory");
        } catch (Exception e) {
            logger.error("❌ Knowledge seeding failed: {}", e.getMessage(), e);
        }
    }

    // ========================================================
    // 1. APP CREATION TECHNIQUES
    // ========================================================

    private void seedAppCreationTechniques() {
        logger.info("📚 Seeding app creation techniques...");

        learningService.recordPattern("APP_CREATION",
            "Spring Boot Backend App Creation Workflow",
            "STEP 1: Analyze requirements → extract entities, APIs, DB schema. " +
            "STEP 2: Generate project with Gradle + Spring Boot 3.x, Java 17+. " +
            "STEP 3: Create Model layer (JPA entities with validation). " +
            "STEP 4: Create Repository layer (JpaRepository interfaces). " +
            "STEP 5: Create Service layer (business logic, transactions, @Service). " +
            "STEP 6: Create Controller layer (@RestController, input validation, error handling). " +
            "STEP 7: Add security (Spring Security + JWT filter). " +
            "STEP 8: Add tests (unit for Services, integration for Controllers). " +
            "STEP 9: Dockerize + deploy. Always: Service → Controller → Model layers, never skip validation.");

        learningService.recordPattern("APP_CREATION",
            "Flutter Mobile App Creation Workflow",
            "STEP 1: flutter create <app_name> --org com.example. " +
            "STEP 2: Add dependencies to pubspec.yaml: dio (HTTP), provider (state), shared_preferences (storage), jwt_decoder. " +
            "STEP 3: Create lib/config/ (constants, endpoints), lib/models/ (data classes with fromJson/toJson), " +
            "lib/services/ (API, Auth, Storage), lib/providers/ (ChangeNotifier state), " +
            "lib/screens/ (UI), lib/widgets/ (reusable components). " +
            "STEP 4: Implement API service using Dio with interceptors for JWT auto-refresh. " +
            "STEP 5: Add Provider state management, wrap MaterialApp with MultiProvider. " +
            "STEP 6: Implement login screen → dashboard screen flow. " +
            "STEP 7: flutter build apk --release for Android, flutter build web --release for Web. " +
            "KEY RULE: Always run 'flutter pub get' after pubspec changes, validate with 'flutter analyze'.");

        learningService.recordPattern("APP_CREATION",
            "React Frontend App Creation Workflow",
            "STEP 1: npx create-react-app <name> --template typescript OR npx vite@latest <name> --template react-ts. " +
            "STEP 2: Add dependencies: axios (HTTP), react-router-dom (routing), @reduxjs/toolkit (state), " +
            "tailwindcss or Material-UI. " +
            "STEP 3: Create src/api/ (Axios instance with JWT interceptors), src/store/ (Redux slices), " +
            "src/pages/ (route components), src/components/ (reusable UI), src/hooks/ (custom hooks). " +
            "STEP 4: Implement auth flow: login form → store JWT → axios interceptor adds to headers. " +
            "STEP 5: Create protected routes that redirect to login if no token. " +
            "STEP 6: npm run build for production. Always: never store JWT in localStorage for secure apps, use httpOnly cookies.");

        learningService.recordPattern("APP_CREATION",
            "Android Native App Creation Workflow",
            "STEP 1: Create project in Android Studio with Empty Activity, Java/Kotlin, minSdk 24+. " +
            "STEP 2: Add to build.gradle: Retrofit2 (HTTP), Gson (JSON), Glide (images), ViewModel + LiveData (MVVM). " +
            "STEP 3: Structure: data/ (models, API, repository), ui/ (activities, fragments, viewmodels), " +
            "utils/ (helpers, constants). " +
            "STEP 4: Retrofit API interface → Repository → ViewModel → Activity/Fragment. " +
            "STEP 5: Implement JWT storage in EncryptedSharedPreferences (NOT regular SharedPreferences). " +
            "STEP 6: ./gradlew assembleRelease, sign with keystore. " +
            "KEY: Always use MVVM pattern, never put network calls in Activities.");

        learningService.recordPattern("APP_CREATION",
            "Full-Stack App Creation Workflow (Backend + Frontend + Mobile)",
            "PHASE 1 - Backend: Create Spring Boot API (see Spring Boot workflow). Deploy to Cloud Run. " +
            "PHASE 2 - Frontend: Create React/Vue web app. Point API base URL to Cloud Run endpoint. Deploy to Firebase Hosting. " +
            "PHASE 3 - Mobile: Create Flutter app. Point to same API. Build APK for Android, .xcarchive for iOS. " +
            "PHASE 4 - CI/CD: Set up GitHub Actions for automated build+deploy on push to main. " +
            "INTEGRATION RULES: " +
            "1. All clients share the same JWT auth (Spring Security issues tokens, all clients validate them). " +
            "2. API versioning: /api/v1/ for all endpoints. " +
            "3. CORS: Configure Spring Security to allow frontend origins. " +
            "4. Environment: Use .env files for secrets, never hardcode. " +
            "5. Database: Firebase Firestore for flexible data, PostgreSQL for relational.");

        learningService.recordPattern("APP_CREATION",
            "Requirement to App Analysis Technique",
            "When given a requirement, SupremeAI must: " +
            "1. EXTRACT: List all entities/models mentioned (nouns = models). " +
            "2. EXTRACT: List all actions/features (verbs = methods/endpoints). " +
            "3. EXTRACT: List all constraints (non-functional requirements: auth, performance, etc). " +
            "4. CLASSIFY: What type of app? (CRUD, dashboard, marketplace, social, analytics, IoT, etc). " +
            "5. RECOMMEND: Which stack? (mobile-first → Flutter, data-heavy → Spring Boot + React, rapid → Firebase). " +
            "6. ESTIMATE: Complexity score 1-10 (1=todo app, 10=enterprise ERP). " +
            "7. PLAN: Break into milestones (Week 1: models+auth, Week 2: core CRUD, Week 3: advanced features, Week 4: polish+deploy). " +
            "Always ask: What is the primary user action? Build that first.");

        logger.info("✅ App creation techniques seeded — 6 patterns");
    }

    // ========================================================
    // 2. ERROR SOLVING PATTERNS
    // ========================================================

    private void seedErrorSolvingPatterns() {
        logger.info("📚 Seeding error solving patterns...");

        // Java / Backend errors
        learningService.recordPattern("ERROR_SOLVING",
            "Java 'cannot find symbol' compilation error",
            "CAUSE: Missing import, typo in class/method name, or wrong package. " +
            "FIX 1: Add correct import statement (import org.example.model.MyClass). " +
            "FIX 2: Check spelling — Java is case-sensitive. " +
            "FIX 3: Verify the class exists in the classpath (check build.gradle dependencies). " +
            "FIX 4: Run './gradlew clean build' to force full recompile. " +
            "AI PROMPT TO USE: 'Fix Java compilation error: cannot find symbol [class name] in file [filename]. Here is the relevant code: [code]'");

        learningService.recordPattern("ERROR_SOLVING",
            "Java NullPointerException (NPE)",
            "CAUSE: Accessing a method/field on a null object reference. " +
            "FIX 1: Add null check: if (obj != null) { ... }. " +
            "FIX 2: Use Optional: Optional.ofNullable(obj).map(o -> o.getField()).orElse(defaultValue). " +
            "FIX 3: Ensure @Autowired fields are actually injected (component must be @Service/@Component). " +
            "FIX 4: For Spring beans: check if the bean is being instantiated before Spring context is ready. " +
            "FIX 5: Add @NonNull annotations and use IDE null-analysis. " +
            "AI PROMPT: 'I have NPE at line X in class Y. The stack trace is [trace]. Fix the code.'");

        learningService.recordPattern("ERROR_SOLVING",
            "Spring Bean creation failure / UnsatisfiedDependencyException",
            "CAUSE: A required @Autowired bean cannot be found or has circular dependency. " +
            "FIX 1: Ensure the missing class is annotated with @Service, @Component, @Repository, or @Configuration. " +
            "FIX 2: Check base package scan — @SpringBootApplication searches from its package down. " +
            "FIX 3: For circular deps: use @Lazy on one @Autowired, or restructure to remove circular reference. " +
            "FIX 4: Use @Autowired(required=false) for optional dependencies. " +
            "AI PROMPT: 'Spring Boot fails to start with UnsatisfiedDependencyException for [BeanClass]. Show me full app structure and fix the dependency.'");

        learningService.recordPattern("ERROR_SOLVING",
            "Gradle BUILD FAILED — compilation errors",
            "CAUSE: Java files have syntax/type/import errors. " +
            "FIX 1: Run './gradlew compileJava 2>&1' to see ONLY compilation errors, not test errors. " +
            "FIX 2: Fix errors TOP to BOTTOM — later errors often cascade from first error. " +
            "FIX 3: './gradlew clean compileJava' if you suspect stale class files. " +
            "FIX 4: Check for missing .jar in dependencies (build.gradle implementation block). " +
            "AI PROMPT: 'Gradle build fails with these errors: [paste errors]. Fix all compilation issues.'");

        learningService.recordPattern("ERROR_SOLVING",
            "Gradle BUILD FAILED — test failures",
            "CAUSE: Unit/integration tests assert incorrect behavior or configuration is missing. " +
            "FIX 1: Run single test to isolate: './gradlew test --tests \"FullyQualified.TestClass\"'. " +
            "FIX 2: Check @MockBean vs real bean for Spring context tests. " +
            "FIX 3: For @SpringBootTest: ensure test application.properties has required properties. " +
            "FIX 4: AssertionError → check expected vs actual values, logic may have changed. " +
            "FIX 5: Add @Transactional to test class to auto-rollback DB changes. " +
            "AI PROMPT: 'These unit tests are failing: [test names + error messages]. Here is the implementation: [code]. Fix either the test or the implementation.'");

        learningService.recordPattern("ERROR_SOLVING",
            "OutOfMemoryError / Java heap space",
            "CAUSE: App consuming more memory than JVM heap allows. " +
            "FIX 1: Increase heap: add -Xmx2g to JVM args in build.gradle run task. " +
            "FIX 2: Find memory leak: check for static Maps/Lists that grow unbounded. " +
            "FIX 3: Use pagination for large DB queries instead of loading all records. " +
            "FIX 4: For Gradle OOM: add 'org.gradle.jvmargs=-Xmx2g' in gradle.properties. " +
            "AI PROMPT: 'OutOfMemoryError in [service]. Here is the code flow. Find the memory leak and fix it.'");

        learningService.recordPattern("ERROR_SOLVING",
            "Maven/Gradle dependency conflict",
            "CAUSE: Two libraries require different versions of the same transitive dependency. " +
            "FIX 1: Use 'dependencies { implementation(\"group:artifact\") { exclude group: \"conflicting.group\" } }'. " +
            "FIX 2: Declare the desired version explicitly to force override. " +
            "FIX 3: './gradlew dependencies' to see full dependency tree and find conflicts. " +
            "FIX 4: Use Spring Boot BOM (bill of materials) — it manages versions for you. " +
            "AI PROMPT: 'Dependency conflict between [lib A] and [lib B] both needing [dep C] different versions. Resolve in build.gradle.'");

        learningService.recordPattern("ERROR_SOLVING",
            "Connection refused / Service unreachable",
            "CAUSE: Target service not running, wrong host/port, firewall blocking. " +
            "FIX 1: Verify target service is running: check process list or health endpoint. " +
            "FIX 2: Confirm correct host and port in application.properties or environment variables. " +
            "FIX 3: For Docker: use container name as hostname, not 'localhost'. " +
            "FIX 4: For Cloud Run: check service URL, IAM permissions (invoker role), and allow-unauthenticated setting. " +
            "FIX 5: Test connectivity: curl -v http://host:port/health. " +
            "AI PROMPT: 'Getting ConnectionRefused to [URL]. App config is [config]. Diagnose and suggest fix.'");

        // Flutter errors
        learningService.recordPattern("ERROR_SOLVING",
            "Flutter 'pub get' dependency resolution failure",
            "CAUSE: Version constraint conflicts in pubspec.yaml, or outdated pub cache. " +
            "FIX 1: Run 'flutter pub cache repair' to fix corrupted cache. " +
            "FIX 2: Run 'flutter pub upgrade --major-versions' to resolve conflicts. " +
            "FIX 3: Check compatible versions on pub.dev for each conflicting package. " +
            "FIX 4: Use version ranges: ^1.0.0 means >=1.0.0 <2.0.0 — widen if needed. " +
            "FIX 5: Check Flutter SDK constraint in pubspec.yaml matches installed Flutter. " +
            "AI PROMPT: 'Flutter pub get failing with: [error]. My pubspec.yaml: [content]. Fix version constraints.'");

        learningService.recordPattern("ERROR_SOLVING",
            "Flutter build web/apk failed",
            "CAUSE: Compilation errors in Dart code, missing platform setup, or plugin incompatibility. " +
            "FIX 1: Run 'flutter analyze' first — fix all warnings/errors shown. " +
            "FIX 2: For Android: ensure android/app/build.gradle has minSdkVersion 21+. " +
            "FIX 3: For Web: some plugins don't support web — check 'flutter pub pub global run flutter_compat'. " +
            "FIX 4: 'flutter clean && flutter pub get && flutter build [apk|web]' — full clean build. " +
            "AI PROMPT: 'Flutter build [platform] fails with: [error]. Show how to fix the build configuration.'");

        // CI/CD errors
        learningService.recordPattern("ERROR_SOLVING",
            "GitHub Actions workflow failure",
            "CAUSE: Missing secrets, wrong runner OS, command not found, or permission error. " +
            "FIX 1: Check job logs → find the FIRST red X step — that is the root cause. " +
            "FIX 2: 'No such file or directory': check working-directory in workflow yaml. " +
            "FIX 3: 'Secret not found': add the secret in GitHub repo → Settings → Secrets → Actions. " +
            "FIX 4: 'Permission denied': add 'permissions: write-all' at job level or use GITHUB_TOKEN. " +
            "FIX 5: 'Gradle wrapper not executable': add 'chmod +x gradlew' step before build. " +
            "AI PROMPT: 'GitHub Actions job [job-name] failing with: [error log]. Here is the workflow yaml: [yaml]. Fix the workflow.'");

        learningService.recordPattern("ERROR_SOLVING",
            "Docker build failure",
            "CAUSE: Missing COPY files, wrong base image, layer cache issues, or port mismatch. " +
            "FIX 1: 'COPY failed': ensure the file path in Dockerfile matches the build context. " +
            "FIX 2: 'Port already in use': change EXPOSE in Dockerfile and application.properties. " +
            "FIX 3: 'can't exec java': use correct JDK base image (eclipse-temurin:17-jre-alpine). " +
            "FIX 4: Add '--no-cache' to docker build for fresh build without layer caching. " +
            "FIX 5: Multi-stage builds: ensure the jar artifact is correctly COPY --from=builder. " +
            "AI PROMPT: 'Docker build fails at step [step number]: [error]. Dockerfile is: [content]. Fix it.'");

        // Firebase errors  
        learningService.recordPattern("ERROR_SOLVING",
            "Firebase connection / authentication error",
            "CAUSE: Missing service account JSON, wrong project ID, or permissions issue. " +
            "FIX 1: Ensure FIREBASE_SERVICE_ACCOUNT_JSON environment variable is set with valid JSON. " +
            "FIX 2: Check project ID matches in FirebaseOptions and .firebaserc. " +
            "FIX 3: Service account must have Firestore + Realtime Database roles in GCP IAM. " +
            "FIX 4: For Cloud Run: mount service account as env var (not file) in Cloud Run configuration. " +
            "AI PROMPT: 'Firebase Admin SDK fails to initialize: [error]. Show me the correct initialization and permissions setup.'");

        logger.info("✅ Error solving patterns seeded — 12 patterns");
    }

    // ========================================================
    // 3. AI MODEL SELECTION STRATEGIES
    // ========================================================

    private void seedAIModelSelectionStrategies() {
        logger.info("📚 Seeding AI model selection strategies...");

        learningService.recordPattern("AI_SELECTION",
            "When to use GPT-4 / OpenAI",
            "BEST FOR: Complex code generation, refactoring large codebases, multi-file edits. " +
            "STRENGTHS: Best at following instructions precisely, consistent output format, code quality. " +
            "USE FOR: 'Generate a complete Spring Boot service with error handling', " +
            "'Refactor this 500-line class into smaller classes', " +
            "'Write production-ready tests for this service'. " +
            "AVOID: Extremely long documents, very latest framework versions (training cutoff). " +
            "API: POST https://api.openai.com/v1/chat/completions, model: gpt-4-turbo-preview.");

        learningService.recordPattern("AI_SELECTION",
            "When to use Claude (Anthropic)",
            "BEST FOR: Understanding complex requirements, architecture decisions, code review, long context. " +
            "STRENGTHS: Best at analyzing existing code and explaining what it does, nuanced reasoning. " +
            "USE FOR: 'Analyze this codebase and find security vulnerabilities', " +
            "'Review my architecture design and suggest improvements', " +
            "'Understand this complex legacy code and document it'. " +
            "STRENGTHS: 200K token context window — can read entire codebases at once. " +
            "API: POST https://api.anthropic.com/v1/messages, model: claude-3-5-sonnet-20241022.");

        learningService.recordPattern("AI_SELECTION",
            "When to use Google Gemini",
            "BEST FOR: Search-augmented tasks, multimodal (image+code), Google Cloud integration advice. " +
            "STRENGTHS: Real-time knowledge, best for 'latest version of X' questions. " +
            "USE FOR: 'What is the latest stable version of Spring Boot and what changed?', " +
            "'How do I integrate with Google Cloud Run / Firestore using latest SDK?', " +
            "'Analyze this screenshot of an error and explain it'. " +
            "API: POST https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent.");

        learningService.recordPattern("AI_SELECTION",
            "When to use Meta LLaMA / Open Source models",
            "BEST FOR: Tasks where data privacy matters, on-premise deployment, high-volume low-cost tasks. " +
            "STRENGTHS: No data sent to third parties, fast inference at scale, good for fine-tuning. " +
            "USE FOR: 'Generate boilerplate code that matches our internal style', " +
            "'Process thousands of log entries for pattern analysis'. " +
            "AVOID: Complex reasoning tasks requiring world knowledge, precise instruction following.");

        learningService.recordPattern("AI_SELECTION",
            "Multi-AI Consensus — when and how",
            "USE CONSENSUS WHEN: Critical architecture decisions, security-sensitive code, " +
            "choosing between multiple valid approaches, debugging complex errors with no obvious fix. " +
            "HOW CONSENSUS WORKS IN SUPREMEAI: " +
            "1. Ask all configured AIs the same question in parallel (5s timeout per AI). " +
            "2. Extract the core recommendation from each response. " +
            "3. Vote: each AI's response counts as 1 vote for its recommended approach. " +
            "4. Apply 70% threshold: if enough configured AIs agree → high confidence, proceed automatically. " +
            "5. If <70% agreement → low confidence, flag for admin review. " +
            "6. Learn from outcome: record which AI was most accurate for this category. " +
            "WHEN TO SKIP CONSENSUS: Simple boilerplate generation, well-known patterns (use single best AI)." +
            "GOLDEN RULE: Never let one AI make a critical decision alone — consensus prevents single-model bias.");

        learningService.recordPattern("AI_SELECTION",
            "AI routing by task category",
            "ARCHITECTURE_DESIGN → Use Claude (best reasoning) + GPT-4 (best precision) in consensus. " +
            "CODE_GENERATION → Use GPT-4 primary, Claude as reviewer. " +
            "ERROR_DIAGNOSIS → Use Claude (long context for full stacktrace) → confirm with GPT-4. " +
            "SECURITY_REVIEW → Use GPT-4 + Claude consensus (both trained on OWASP data). " +
            "TESTING → Use GPT-4 (precise, follows test patterns). " +
            "DOCUMENTATION → Use Claude (best writing quality). " +
            "PERFORMANCE_OPT → Use GPT-4 + DeepSeek (strong at algorithmic thinking). " +
            "LATEST_API_USAGE → Use Gemini (real-time knowledge of SDK versions). " +
            "HIGH_VOLUME_TASKS → Use Mistral or LLaMA (cost-effective). " +
            "STRATEGIC_PLANNING → Full 10-AI consensus required.");

        logger.info("✅ AI model selection strategies seeded — 6 patterns");
    }

    // ========================================================
    // 4. CODE ARCHITECTURE PATTERNS
    // ========================================================

    private void seedCodeArchitecturePatterns() {
        logger.info("📚 Seeding code architecture patterns...");

        learningService.recordPattern("CODE_ARCHITECTURE",
            "Spring Boot 3-Layer Architecture (ALWAYS USE)",
            "LAYER 1 — Model (org.example.model): Pure data classes. " +
            "No business logic. Annotations: @Entity, @Id, validation (@NotNull, @Email). " +
            "LAYER 2 — Service (org.example.service): ALL business logic here. " +
            "Annotated @Service. Calls Repository or FirebaseService. " +
            "Validates inputs, throws BusinessException for errors. " +
            "Handles transactions (@Transactional where needed). " +
            "LAYER 3 — Controller (org.example.controller): ONLY HTTP concerns. " +
            "Annotated @RestController. Validates request format, calls Service, returns ResponseEntity. " +
            "Never puts business logic in controller. " +
            "RULE: Controller → Service → Model. Never skip layers. Never call Repository from Controller.");

        learningService.recordPattern("CODE_ARCHITECTURE",
            "Admin Control Mode Pattern (SupremeAI-specific)",
            "Every autonomous operation MUST check admin mode: " +
            "AUTO mode: proceed immediately without waiting. " +
            "WAIT mode: queue the operation, notify admin via WebSocket, wait for approval. " +
            "FORCE_STOP mode: reject all new autonomous operations immediately. " +
            "Implementation: adminControlService.getMode() → check before any autonomous action. " +
            "Audit trail: ALWAYS log who did what, when, with what result. " +
            "Use AdminControlService.checkAndProceed(operation) before any auto-action.");

        learningService.recordPattern("CODE_ARCHITECTURE",
            "API Error Handling Pattern",
            "Every Controller method must have try-catch: " +
            "try { validateInputs(params); result = service.doWork(params); return ResponseEntity.ok(result); } " +
            "catch (ValidationException e) { return ResponseEntity.badRequest().body(Map.of('error', e.getMessage())); } " +
            "catch (AuthenticationException e) { return ResponseEntity.status(401).body(Map.of('error', 'Unauthorized')); } " +
            "catch (Exception e) { logger.error('Unexpected error: {}', e.getMessage(), e); " +
            "return ResponseEntity.status(500).body(Map.of('error', 'Internal server error')); } " +
            "NEVER return stack traces to client. ALWAYS log the full exception server-side.");

        learningService.recordPattern("CODE_ARCHITECTURE",
            "Pagination Pattern for large data",
            "For any endpoint returning lists, ALWAYS support pagination: " +
            "GET /api/items?page=0&size=20&sort=createdAt,desc " +
            "Use Spring Data Pageable: @GetMapping public Page<Item> list(Pageable pageable) { return repo.findAll(pageable); } " +
            "Limit max page size: if (pageable.getPageSize() > 100) throw new ValidationException('Max page size is 100'). " +
            "Return metadata: { content: [...], totalElements: N, totalPages: N, page: N, size: N }. " +
            "NEVER load all records at once into memory for large collections.");

        learningService.recordPattern("CODE_ARCHITECTURE",
            "Service + Interface Pattern for testability",
            "For services that should be mocked in tests: " +
            "Create interface: public interface NotificationService { void send(String msg); } " +
            "Create implementation: @Service public class NotificationServiceImpl implements NotificationService { ... } " +
            "In tests: @MockBean NotificationService notificationService — Spring auto-mocks the interface. " +
            "Benefit: Easy to swap implementations (email vs SMS vs WebSocket notifications) without changing callers.");

        logger.info("✅ Code architecture patterns seeded — 5 patterns");
    }

    // ========================================================
    // 5. BUILD & CI/CD PATTERNS
    // ========================================================

    private void seedBuildAndCICDPatterns() {
        logger.info("📚 Seeding build and CI/CD patterns...");

        learningService.recordPattern("BUILD_PATTERNS",
            "Gradle build recovery sequence (use this order)",
            "STEP 1: './gradlew clean' — remove all stale compiled outputs. " +
            "STEP 2: './gradlew compileJava' — compile only Java, see all errors cleanly. " +
            "STEP 3: Fix ALL compilation errors shown (top to bottom — cascading errors disappear). " +
            "STEP 4: './gradlew test' — run all tests. " +
            "STEP 5: Fix test failures (check test vs implementation logic). " +
            "STEP 6: './gradlew build' — full build including jar creation. " +
            "STEP 7: './gradlew bootRun' — run the application. " +
            "SHORTCUT: './gradlew build -x test' — build jar, skip tests (for deployment when tests pass in CI).");

        learningService.recordPattern("BUILD_PATTERNS",
            "GitHub Actions CI pipeline best practices",
            "WORKFLOW STRUCTURE: " +
            "1. trigger: on: push: branches: [main, develop], pull_request: branches: [main]. " +
            "2. Build job: checkout → setup-java → grant gradlew execute permissions → build → test → upload artifacts. " +
            "3. Deploy job: needs: [build] → only runs on main branch → deploy to cloud. " +
            "CRITICAL STEPS: " +
            "'- run: chmod +x gradlew' — ALWAYS before './gradlew' on Linux runners. " +
            "Store secrets: FIREBASE_TOKEN, GCP_SA_KEY, DOCKER_PASSWORD — NEVER hardcode. " +
            "Use 'if: github.ref == refs/heads/main' to prevent deploy from PRs. " +
            "Cache Gradle: 'uses: actions/cache@v3 with path: ~/.gradle/caches'.");

        learningService.recordPattern("BUILD_PATTERNS",
            "Docker multi-stage build for Spring Boot",
            "# Stage 1: Build " +
            "FROM eclipse-temurin:17 AS builder " +
            "WORKDIR /app " +
            "COPY gradlew build.gradle.kts settings.gradle.kts . " +
            "COPY gradle gradle " +
            "RUN chmod +x gradlew && ./gradlew dependencies --no-daemon " +
            "COPY src src " +
            "RUN ./gradlew bootJar --no-daemon -x test " +
            "# Stage 2: Runtime (smaller image) " +
            "FROM eclipse-temurin:17-jre-alpine " +
            "WORKDIR /app " +
            "COPY --from=builder /app/build/libs/*.jar app.jar " +
            "EXPOSE 8080 " +
            "ENV PORT=8080 " +
            "ENTRYPOINT [\"java\", \"-jar\", \"-Xmx512m\", \"app.jar\"] " +
            "BENEFIT: Final image is 5x smaller than single-stage build.");

        learningService.recordPattern("BUILD_PATTERNS",
            "Cloud Run deployment pattern",
            "STEPS: " +
            "1. Build Docker image: docker build -t gcr.io/PROJECT_ID/IMAGE_NAME:COMMIT_SHA . " +
            "2. Push: docker push gcr.io/PROJECT_ID/IMAGE_NAME:COMMIT_SHA. " +
            "3. Deploy: gcloud run deploy SERVICE_NAME --image gcr.io/PROJECT_ID/IMAGE_NAME:COMMIT_SHA " +
            "--region us-central1 --platform managed --allow-unauthenticated --memory 512Mi --port 8080. " +
            "ENV VARS: Set via --set-env-vars or Secret Manager (preferred). " +
            "HEALTH CHECK: Cloud Run checks / or /health endpoint — must return 200 with 60s timeout. " +
            "AUTO-SCALE: Cloud Run scales to 0 (cost-saving) — add min-instances=1 for no cold starts.");

        logger.info("✅ Build and CI/CD patterns seeded — 4 patterns");
    }

    // ========================================================
    // 6. SECURITY PATTERNS
    // ========================================================

    private void seedSecurityPatterns() {
        logger.info("📚 Seeding security patterns...");

        learningService.recordPattern("SECURITY",
            "JWT Authentication implementation (correct pattern)",
            "DO: " +
            "1. Issue short-lived access tokens (15-60 min) + long-lived refresh tokens (7 days). " +
            "2. Sign with RS256 (asymmetric) for production, HS256 acceptable for dev. " +
            "3. Store access token in memory (JS), refresh token in httpOnly cookie. " +
            "4. Validate token on EVERY request in a Filter, not in every controller. " +
            "5. Include userId, roles, expiry in token claims — NOT passwords or sensitive data. " +
            "6. Invalidate refresh tokens on logout (store valid token IDs in Redis/Firebase). " +
            "DON'T: " +
            "- Store JWT in localStorage (XSS vulnerable). " +
            "- Put sensitive data in JWT claims (base64 decode reveals content). " +
            "- Use long expiry access tokens (>1 hour) for sensitive operations. " +
            "- Skip token validation on any endpoint (even 'internal' ones).");

        learningService.recordPattern("SECURITY",
            "Input validation — prevent injection attacks (OWASP A03)",
            "RULE: Validate ALL inputs at the controller entry point. " +
            "1. Never use String concatenation for SQL — use PreparedStatement or JPA. " +
            "2. Validate data type, length, format before processing. " +
            "3. For file uploads: validate MIME type by reading file header bytes, not just extension. " +
            "4. For shell commands: NEVER concatenate user input — use ProcessBuilder with array args. " +
            "5. For HTML output: use template engine escaping (Thymeleaf auto-escapes by default). " +
            "6. For JSON output: let Jackson serialize objects — never build JSON by string concat. " +
            "SupremeAI pattern: InputValidator.validate(request) → BusinessException if invalid.");

        learningService.recordPattern("SECURITY",
            "Open endpoint protection — NEVER create unauthenticated admin endpoints",
            "CRITICAL RULE (learned from SupremeAI past mistakes): " +
            "WRONG: POST /api/auth/init  — open endpoint to create first admin (SECURITY HOLE). " +
            "RIGHT: POST /api/auth/setup  — protected by SUPREMEAI_SETUP_TOKEN env variable. " +
            "For any /api/admin/* endpoint: ALWAYS require JWT with ADMIN role. " +
            "For any initialization endpoint: use setup token from environment variable. " +
            "For any /api/extend/* or /api/consensus/* endpoint: require admin authentication. " +
            "Deploy rule: SUPREMEAI_SETUP_TOKEN must be set before first deploy.");

        learningService.recordPattern("SECURITY",
            "Secret management — no hardcoded credentials",
            "RULE: Secrets in environment variables ONLY — never in code or committed files. " +
            "Development: .env file (add to .gitignore). " +
            "Production: Cloud Run env vars via Secret Manager. " +
            "Code pattern: String key = System.getenv('API_KEY'); if (key == null) { log.error('Missing API_KEY') } " +
            "GitHub Actions: Store in repo Secrets, reference as ${{ secrets.MY_SECRET }}. " +
            "Validation: On startup, check all required env vars exist — fail fast with clear error message. " +
            "NEVER: Hardcode API keys, DB passwords, JWT secrets, or signing certs in source code.");

        logger.info("✅ Security patterns seeded — 4 patterns");
    }

    // ========================================================
    // 7. TEST PATTERNS
    // ========================================================

    private void seedTestPatterns() {
        logger.info("📚 Seeding test patterns...");

        learningService.recordPattern("TEST_PATTERNS",
            "Unit test pattern for Spring Boot Services",
            "USE: @ExtendWith(MockitoExtension.class) — fast, no Spring context. " +
            "@Mock FirebaseService firebaseService; @InjectMocks MyService myService; " +
            "For each test: " +
            "1. ARRANGE: when(mock.method(any())).thenReturn(expectedValue). " +
            "2. ACT: result = myService.doSomething(input). " +
            "3. ASSERT: assertEquals(expected, result), verify(mock).method(captorArg). " +
            "TEST CASES TO ALWAYS INCLUDE: " +
            "- Happy path (valid input → success). " +
            "- Null input → NullPointerException or validation error. " +
            "- Invalid format input → ValidationException. " +
            "- Dependency throws exception → service handles it gracefully. " +
            "- Boundary values (empty string, max length, zero, negative numbers).");

        learningService.recordPattern("TEST_PATTERNS",
            "Integration test pattern for Spring Boot Controllers",
            "USE: @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT) + @AutoConfigureMockMvc. " +
            "For auth-protected endpoints: " +
            "1. First call POST /api/auth/login to get JWT token. " +
            "2. Add 'Authorization: Bearer {token}' header to all subsequent test requests. " +
            "Use @MockBean for Firebase or external services to keep tests independent. " +
            "Test HTTP status codes: 200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 404 Not Found, 500 Error. " +
            "Use MockMvc: mockMvc.perform(post('/api/x').header('Authorization', token).content(json))" +
            ".andExpect(status().isOk()).andExpect(jsonPath('$.status').value('success')).");

        learningService.recordPattern("TEST_PATTERNS",
            "AI model accuracy testing pattern",
            "To verify an AI model gives correct outputs: " +
            "1. Create a test dataset: List<Pair<String, String>> testCases (input → expected output). " +
            "2. Run each test case through the AI model. " +
            "3. Compare: exact match for deterministic outputs, semantic similarity for creative outputs. " +
            "4. Calculate accuracy score: passedTests / totalTests. " +
            "5. Target: 85%+ pass rate for the model to be trusted for production use. " +
            "6. For non-deterministic AI: run each test case 3x, take majority vote as 'model answer'. " +
            "7. Log failures for learning: what prompt caused failure, what was expected vs actual.");

        logger.info("✅ Test patterns seeded — 3 patterns");
    }

    // ========================================================
    // 8. SELF-HEALING PATTERNS
    // ========================================================

    private void seedSelfHealingPatterns() {
        logger.info("📚 Seeding self-healing patterns...");

        learningService.recordPattern("SELF_HEALING",
            "Auto-fix loop execution strategy",
            "WHEN an error is detected: " +
            "1. Parse error to determine type: COMPILATION, TEST_FAILURE, RUNTIME, BUILD, DEPENDENCY. " +
            "2. Query SystemLearningService for known solutions to this error type. " +
            "3. If solution found with confidence > 0.7: apply it directly (fast path). " +
            "4. If not found or low confidence: ask MultiAIConsensusService with full error context. " +
            "5. Apply suggested fix to code. " +
            "6. Re-run the failing command to verify fix. " +
            "7. If fixed: record solution to SystemLearningService with high confidence (0.95). " +
            "8. If not fixed after 3 attempts: escalate to admin via notification. " +
            "MAX RETRIES: 3 per error. TIMEOUT: 5 min total per error. " +
            "LEARN: Always record both successes and failures — failures prevent wasted retries next time.");

        learningService.recordPattern("SELF_HEALING",
            "Error pattern recognition — linking errors to solutions",
            "When recording an error for future prevention: " +
            "1. Extract: error class name, key phrases from message (first 100 chars), file/line if available. " +
            "2. Categorize: COMPILATION / RUNTIME / BUILD / DEPENDENCY / NETWORK / AUTH / DATA. " +
            "3. Link to solution: what exact code change or config change fixed it. " +
            "4. Track frequency: how many times this same error has occurred. " +
            "5. Track success rate: what % of times did the stored solution work. " +
            "Pattern matching: use contains() on error message — more reliable than regex for Java errors. " +
            "Similar error detection: same category + 80% overlap in key phrases = same error class.");

        learningService.recordPattern("SELF_HEALING",
            "Consensus voting for error solutions",
            "When asking AIs to fix an error, ALWAYS include: " +
            "1. The FULL error message and stack trace (not just first line). " +
            "2. The RELEVANT code section (not entire file — just the failing class/method). " +
            "3. The CONTEXT: what was the system trying to do when the error occurred. " +
            "4. CONSTRAINTS: 'The solution must not break existing tests', 'Must use Java 17'. " +
            "PROMPT TEMPLATE: " +
            "'I have a [ERROR_TYPE] in SupremeAI. Error: [MESSAGE]. Stack: [TRACE]. " +
            "Relevant code: [CODE]. Context: [CONTEXT]. Constraints: [CONSTRAINTS]. " +
            "Provide a specific code fix. Format: 1) What caused it. 2) Exact code change. 3) How to verify.'");

        learningService.recordRequirement(
            "SupremeAI must always be learning — never repeat the same mistake twice",
            "After every error occurrence: record it. After every fix: record the solution. " +
            "After every successful app creation: record the successful patterns. " +
            "Memory is persistent (Firebase + in-memory cache). " +
            "Before doing any complex operation: query memory for relevant patterns first. " +
            "This is the KEY difference between SupremeAI and a dumb code generator: " +
            "SupremeAI learns from every interaction and gets smarter over time.");

        logger.info("✅ Self-healing patterns seeded — 3 patterns + 1 requirement");
        logger.info("🧠 Total knowledge seeded: 39 patterns + requirements across 8 categories");
    }
}
