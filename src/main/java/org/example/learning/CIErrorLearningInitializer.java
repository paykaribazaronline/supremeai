package org.example.learning;

import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * CI Error Learning Initializer
 *
 * Teaches SupremeAI the root causes and solutions for all CI failures
 * discovered during the Phase 8-10 fix session.  Runs once on startup
 * so the knowledge is persisted to Firebase and never forgotten.
 */
@Component
public class CIErrorLearningInitializer {

    private static final Logger logger = LoggerFactory.getLogger(CIErrorLearningInitializer.class);

    @Autowired
    private SystemLearningService systemLearningService;

    @EventListener(ApplicationReadyEvent.class)
    public void teachCIErrors() {
        logger.info("🧠 CIErrorLearningInitializer: teaching SupremeAI {} CI error lessons...", 10);

        teachJacksonVersionSplit();
        teachListFilesNPE();
        teachMapOfNullPointer();
        teachWrongMockMethodName();
        teachFilterSendErrorVsSetStatus();
        teachCaseInsensitiveBearerToken();
        teachMockitoStrictStubs();
        teachFirebaseInSpringBootTest();
        teachNodeJsActionDeprecation();
        teachMarkdownLintHeadings();

        logger.info("✅ CIErrorLearningInitializer: all lessons recorded.");
    }

    // -----------------------------------------------------------------------
    // Error 1 – Jackson version split (NoSuchMethodError: getNumberTypeFP)
    // -----------------------------------------------------------------------
    private void teachJacksonVersionSplit() {
        systemLearningService.recordError(
            "DEPENDENCY",
            "NoSuchMethodError: getNumberTypeFP() – jackson-databind and jackson-core are on different versions",
            null,
            "Pin ALL four Jackson artifacts to the SAME version in build.gradle.kts:\n" +
            "  implementation(\"com.fasterxml.jackson.core:jackson-core:2.17.0\")\n" +
            "  implementation(\"com.fasterxml.jackson.core:jackson-annotations:2.17.0\")\n" +
            "  implementation(\"com.fasterxml.jackson.core:jackson-databind:2.17.0\")\n" +
            "  implementation(\"com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0\")\n" +
            "Spring Boot BOM only manages some artifacts – always override ALL four together."
        );

        systemLearningService.recordPattern(
            "DEPENDENCY",
            "Always pin all jackson-core artifacts to the same explicit version",
            "Spring Boot BOM may manage jackson-databind but leave jackson-core on a different minor " +
            "version, causing NoSuchMethodError at runtime. Pinning all four artifacts prevents the split."
        );
    }

    // -----------------------------------------------------------------------
    // Error 2 – listFiles() NPE in ExecutionLogManager
    // -----------------------------------------------------------------------
    private void teachListFilesNPE() {
        systemLearningService.recordError(
            "TESTING",
            "NullPointerException: File.listFiles() can return null when directory does not exist or IO error occurs",
            null,
            "Always null-check listFiles() before iterating:\n" +
            "  File[] files = dir.listFiles();\n" +
            "  if (files != null) { for (File f : files) { ... } }\n" +
            "Never assume a directory is readable."
        );

        systemLearningService.recordPattern(
            "JAVA",
            "File.listFiles() returns null on IO error – always guard with null check",
            "File.listFiles() is documented to return null if the abstract pathname does not denote a " +
            "directory, or if an I/O error occurs. Forgetting this null check is a common NPE source."
        );
    }

    // -----------------------------------------------------------------------
    // Error 3 – Map.of() rejects null values (AdminMessagePusher)
    // -----------------------------------------------------------------------
    private void teachMapOfNullPointer() {
        systemLearningService.recordError(
            "JAVA",
            "NullPointerException: Map.of() / Map.copyOf() do not allow null keys or values",
            null,
            "Replace Map.of() with a null-safe alternative when data may contain nulls:\n" +
            "  Map<String, Object> safeData = (data != null) ? data : new HashMap<>();\n" +
            "Or filter out nulls before constructing the map. Never pass possibly-null values into Map.of()."
        );
    }

    // -----------------------------------------------------------------------
    // Error 4 – Wrong method name in Mockito verify() (WebhookListenerTest)
    // -----------------------------------------------------------------------
    private void teachWrongMockMethodName() {
        systemLearningService.recordError(
            "TESTING",
            "WantedButNotInvoked: verify() references getGitHubData() but implementation calls getGitHubDataWithHealing()",
            null,
            "When the production code is refactored (e.g. method renamed), update ALL verify() and " +
            "when(...).thenReturn() stubs in every test class to match the new method name. " +
            "Use IDE 'Find Usages' or grep to locate all stale references before committing."
        );

        systemLearningService.recordPattern(
            "TESTING",
            "After renaming a service method, grep test sources for the old name and update all verify() calls",
            "Mockito verify() uses the exact method name. A renamed method silently breaks tests with " +
            "'WantedButNotInvoked' which is hard to diagnose without knowing the rename happened."
        );
    }

    // -----------------------------------------------------------------------
    // Error 5 – AuthenticationFilter: setStatus+write vs sendError
    // -----------------------------------------------------------------------
    private void teachFilterSendErrorVsSetStatus() {
        systemLearningService.recordError(
            "AUTH",
            "WantedButNotInvoked: tests expect response.sendError(401, msg) but filter calls setStatus(401) + getWriter().write()",
            null,
            "In Spring Security / Servlet filters, use response.sendError(statusCode, message) " +
            "rather than setStatus() + getWriter().write().  Tests mock HttpServletResponse and " +
            "verify sendError() directly – using setStatus() bypasses that verification."
        );
    }

    // -----------------------------------------------------------------------
    // Error 6 – Case-sensitive Bearer token check
    // -----------------------------------------------------------------------
    private void teachCaseInsensitiveBearerToken() {
        systemLearningService.recordError(
            "AUTH",
            "Authorization header Bearer prefix check is case-sensitive – valid tokens with different casing are rejected",
            null,
            "Always normalize the Authorization header before checking the prefix:\n" +
            "  authHeader.toLowerCase().startsWith(\"bearer \")\n" +
            "RFC 7235 defines header field names as case-insensitive; clients may send 'Bearer', 'bearer', or 'BEARER'."
        );

        systemLearningService.recordPattern(
            "AUTH",
            "Normalize Authorization header to lowercase before Bearer prefix check",
            "HTTP headers are case-insensitive per RFC 7230. A strict startsWith(\"Bearer \") check " +
            "will reject tokens sent with lowercase 'bearer', breaking valid client requests."
        );
    }

    // -----------------------------------------------------------------------
    // Error 7 – Mockito STRICT_STUBS / UnnecessaryStubbingException
    // -----------------------------------------------------------------------
    private void teachMockitoStrictStubs() {
        systemLearningService.recordError(
            "TESTING",
            "UnnecessaryStubbingException: stub set up in @BeforeEach is not used in every test method",
            null,
            "Option A – Use lenient() for stubs that are not needed in every test:\n" +
            "  lenient().when(mock.method()).thenReturn(value);\n" +
            "Option B – Move the stub into only the tests that need it.\n" +
            "Option C – Use @MockitoSettings(strictness = Strictness.LENIENT) at class level.\n" +
            "STRICT_STUBS (default with @ExtendWith(MockitoExtension.class)) improves test quality " +
            "but requires that every stub is actually exercised."
        );
    }

    // -----------------------------------------------------------------------
    // Error 8 – @SpringBootTest fails in CI without Firebase credentials
    // -----------------------------------------------------------------------
    private void teachFirebaseInSpringBootTest() {
        systemLearningService.recordError(
            "CI_CD",
            "@SpringBootTest context fails in CI because GOOGLE_APPLICATION_CREDENTIALS is not set – Firebase beans cannot initialize",
            null,
            "Option A – Add a src/test/resources/application-test.properties that sets " +
            "firebase.enabled=false and configure Firebase auto-config to skip when disabled.\n" +
            "Option B – Use @MockBean FirebaseApp / FirebaseDatabase in integration test classes.\n" +
            "Option C – Export GOOGLE_APPLICATION_CREDENTIALS as a GitHub Actions secret and " +
            "reference it in the workflow:\n" +
            "  env:\n" +
            "    GOOGLE_APPLICATION_CREDENTIALS: ${{ secrets.GCP_SA_KEY_FILE }}"
        );

        systemLearningService.recordPattern(
            "CI_CD",
            "Mock or disable Firebase in @SpringBootTest to keep CI independent of external credentials",
            "Spring Boot eagerly creates all beans on context load. Firebase beans that require " +
            "GOOGLE_APPLICATION_CREDENTIALS will fail instantly in CI unless mocked or disabled."
        );
    }

    // -----------------------------------------------------------------------
    // Error 9 – Deprecated Node.js 20 in GitHub Actions
    // -----------------------------------------------------------------------
    private void teachNodeJsActionDeprecation() {
        systemLearningService.recordError(
            "CI_CD",
            "GitHub Actions deprecation warning: actions/checkout@v4 uses Node.js 20 which is deprecated",
            null,
            "Upgrade all action references to the latest major version using @v5:\n" +
            "  actions/checkout@v5\n" +
            "  actions/setup-java@v5\n" +
            "  codecov/codecov-action@v5\n" +
            "Run: grep -r 'uses: actions/' .github/workflows/ to find all stale references."
        );

        systemLearningService.recordPattern(
            "CI_CD",
            "Pin GitHub Actions to @v5+ to avoid Node.js deprecation warnings that clutter CI logs",
            "GitHub periodically deprecates the Node.js runtime used by action versions. " +
            "Upgrading to @v5 ensures the latest runtime and removes deprecation noise."
        );
    }

    // -----------------------------------------------------------------------
    // Error 10 – Markdownlint: missing blank lines around headings/lists
    // -----------------------------------------------------------------------
    private void teachMarkdownLintHeadings() {
        systemLearningService.recordError(
            "DOCUMENTATION",
            "Markdownlint MD022/MD031/MD032: headings, fenced code blocks, and lists must be surrounded by blank lines",
            null,
            "Add a blank line BEFORE and AFTER every heading, every fenced code block (```), " +
            "and every list (ordered or unordered).\n" +
            "Rules to remember:\n" +
            "  MD022 – blank line required before/after headings\n" +
            "  MD031 – blank line required before/after fenced code blocks\n" +
            "  MD032 – blank line required before/after lists\n" +
            "Run markdownlint locally: npx markdownlint-cli '**/*.md'"
        );

        systemLearningService.recordPattern(
            "DOCUMENTATION",
            "Always surround Markdown headings, code blocks, and lists with blank lines",
            "markdownlint (used in CI lint checks) enforces blank-line rules MD022/MD031/MD032. " +
            "Violating these rules causes CI failures that are easy to miss locally."
        );
    }
}
