#!/usr/bin/env python3
"""
Part 7 — Testing Strategies
Seeds SupremeAI Firebase with deep knowledge about:
  • Test pyramid (unit, integration, end-to-end)
  • Test-Driven Development (TDD) — Red-Green-Refactor
  • JUnit 5 + Mockito for Spring Boot
  • Spring Boot Test slices (@WebMvcTest, @DataJpaTest, @SpringBootTest)
  • Contract testing (Pact, Spring Cloud Contract)
  • End-to-end testing (Playwright, Cypress, Selenium)
  • Flutter testing (unit, widget, integration)
  • Test quality (coverage, mutation testing, flakiness)

Collections written:
  • system_learning   (SystemLearning model records)
  • testing_knowledge (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part7_testing.py [--dry-run]
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records
# ============================================================================

SYSTEM_LEARNINGS = {

    "testing_tdd_cycle": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "Test-Driven Development (TDD) — Red-Green-Refactor cycle: "
            "(1) RED: Write a failing test for the smallest meaningful behaviour. "
            "The test must fail for the right reason — compile error is not the right failure yet. "
            "(2) GREEN: Write the simplest possible code to make the test pass. "
            "Do not over-engineer; just make it green. "
            "(3) REFACTOR: Clean up the code and tests — no new behaviour, only structural improvement. "
            "Benefits: forces interface design before implementation, living documentation, "
            "regression safety net, reduced debugging time."
        ),
        solutions=[
            "Write @Test method first with expected behaviour; let IDE generate the production class",
            "One failing test at a time — do not write multiple failing tests ahead",
            "Commit on green — commit frequently in TDD, each passing test is a save point",
            "Refactor ruthlessly in step 3 — tests protect against regressions",
            "Use @DisplayName for tests to document behaviour: 'given valid user, when login, then token returned'",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=189,
        context={
            "coined_by": "Kent Beck, Extreme Programming 2000",
            "benefit": "TDD-written code has 40-80% fewer defects (Microsoft/IBM studies)",
        },
    ),

    "testing_junit5_mockito": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "JUnit 5 + Mockito unit testing for Spring Boot services: "
            "@ExtendWith(MockitoExtension.class) on test class. "
            "@Mock: create mock; @InjectMocks: create class under test with mocks injected. "
            "Given-When-Then structure: "
            "given(userRepo.findByEmail(email)).willReturn(Optional.of(user)); "
            "User result = userService.findByEmail(email); "
            "assertThat(result).isEqualTo(user); "
            "verify(userRepo).findByEmail(email); "
            "Use AssertJ (assertThat) not JUnit assertions — richer, fluent, better error messages."
        ),
        solutions=[
            "Use @ExtendWith(MockitoExtension.class) not @SpringBootTest for unit tests — 100x faster",
            "BDDMockito.given().willReturn() style for readability (BDD = Given/When/Then)",
            "ArgumentCaptor<T> to capture and verify arguments passed to mocks",
            "willThrow(new EntityNotFoundException()).given(repo).findById(999L) for error path testing",
            "verify(repo, times(1)).save(any(User.class)) — verify interactions",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=234,
        context={
            "assertj": "assertThat(list).hasSize(3).extracting('name').containsExactly('Alice','Bob','Carol')",
            "test_naming": "methodName_scenario_expectedBehaviour: findUser_whenNotFound_throwsException",
        },
    ),

    "testing_spring_web_mvc_test": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "@WebMvcTest test slice for Spring MVC controllers: "
            "Starts only the web layer (controllers, filters, @ControllerAdvice) — not services or repos. "
            "Use @MockBean to provide mock implementations of services. "
            "MockMvc: perform().andExpect() for request/response verification. "
            "Tests JSON serialisation/deserialisation, request mapping, validation, security. "
            "Faster than @SpringBootTest (no application context startup)."
        ),
        solutions=[
            "@WebMvcTest(UserController.class) — only loads the specified controller",
            "@MockBean UserService userService — inject mock service into controller",
            "mockMvc.perform(post('/api/users').contentType(APPLICATION_JSON).content(json))",
            ".andExpect(status().isCreated()).andExpect(jsonPath('$.id').exists())",
            "Use @WithMockUser(roles='ADMIN') for security context in tests",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=167,
        context={
            "speed": "@WebMvcTest: ~500ms context start; @SpringBootTest: ~5s+ context start",
            "json_helper": "Use ObjectMapper.writeValueAsString(dto) to serialize test request bodies",
        },
    ),

    "testing_spring_data_jpa_test": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "@DataJpaTest slice for JPA repository testing: "
            "Starts only JPA layer: EntityManager, DataSource, Flyway migrations. "
            "Uses in-memory H2 by default — configure to use Testcontainers PostgreSQL for production parity. "
            "Tests: custom @Query methods, derived query methods, pagination, entity relationships. "
            "@Transactional on @DataJpaTest automatically rolls back each test — no data contamination. "
            "@Sql annotation: run SQL scripts to set up test data."
        ),
        solutions=[
            "@DataJpaTest + @AutoConfigureTestDatabase(replace=NONE) + Testcontainers PostgreSQL for real DB",
            "@Sql('/test-data/users.sql') to populate test fixtures before each test",
            "Test Pageable: userRepo.findAll(PageRequest.of(0, 10)).getContent()",
            "Test custom query: assertThat(repo.findByEmail('a@b.com')).isPresent()",
            "Testcontainers: @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>('postgres:16')",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=134,
        context={
            "testcontainers": "Spin up real PostgreSQL in Docker for tests — production-parity",
            "dependency": "testImplementation 'org.testcontainers:postgresql:1.19.x'",
        },
    ),

    "testing_spring_boot_integration": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "@SpringBootTest integration testing: "
            "Loads the full application context — all beans, configurations, datasource. "
            "webEnvironment=RANDOM_PORT: starts real HTTP server on random port. "
            "Use TestRestTemplate or WebTestClient for HTTP calls to the real server. "
            "Slower than test slices — use sparingly for end-to-end flow testing. "
            "@DirtiesContext: reset Spring context after test (expensive — avoid if possible). "
            "Best practice: one or two @SpringBootTest tests per feature for happy path + critical error path."
        ),
        solutions=[
            "@SpringBootTest(webEnvironment=RANDOM_PORT) + @LocalServerPort int port",
            "Use WebTestClient for reactive testing; TestRestTemplate for servlet-based",
            "Inject mocks for external dependencies: @MockBean EmailService mockEmail",
            "Use @Sql or DatabasePopulator to set up test data for integration tests",
            "Run @SpringBootTest tests in a separate 'integration-test' Gradle source set",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=112,
        context={
            "speed": "@SpringBootTest: 3-10s startup per test class; use shared context with @DirtiesContext(classMode=AFTER_CLASS)",
            "tip": "Mark @SpringBootTest tests with @Tag('integration') to run separately from unit tests",
        },
    ),

    "testing_test_pyramid": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "Test Pyramid: more unit tests, fewer integration tests, fewest E2E tests. "
            "Unit tests (70%): test individual classes in isolation; fast (< 1ms each); "
            "run in every build. "
            "Integration tests (20%): test component interactions (controller+service+DB); "
            "slower (100ms-5s each); run on PR/merge. "
            "E2E tests (10%): test full user flows in real browser/app; "
            "slowest (5-60s each); run nightly or on release. "
            "Test Diamond (anti-pattern): heavy on integration, light on unit — "
            "slow feedback, hard to diagnose failures."
        ),
        solutions=[
            "Aim for > 80% unit test coverage; < 50 integration tests per service",
            "Each unit test should run in < 10ms — no Spring context, no DB, no network",
            "E2E tests cover only critical user paths: login, purchase, core feature",
            "Run unit tests locally on save; integration tests on PR; E2E on merge to main",
            "Flaky E2E tests are worse than no tests — quarantine and fix aggressively",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=178,
        context={
            "reference": "Test Pyramid — Martin Fowler: martinfowler.com/articles/practical-test-pyramid.html",
            "cost_ratio": "Unit: $0.001; Integration: $0.10; E2E: $1.00 per test (time+maintenance)",
        },
    ),

    "testing_playwright_e2e": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "Playwright for E2E testing of React/web applications: "
            "Cross-browser: Chromium, Firefox, WebKit — one test runs on all. "
            "Auto-wait: automatically waits for elements to be visible and stable — no manual sleep(). "
            "Parallel: tests run in parallel by default. "
            "Page Object Model (POM): encapsulate page interactions in classes for reusability. "
            "Screenshots + videos on failure for debugging. "
            "Component testing: @playwright/experimental-ct-react for React component isolation."
        ),
        solutions=[
            "npx playwright test — runs all tests; --ui for interactive mode",
            "Use page.getByRole(), page.getByLabel() instead of CSS selectors for accessibility",
            "Page Object: class LoginPage { async login(email, password) { ... } }",
            "Mock APIs: await page.route('/api/users', route => route.fulfill({body: JSON.stringify([])}));",
            "Add to CI: npx playwright install chromium && npx playwright test --reporter=html",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=67,
        context={
            "vs_cypress": "Playwright: multi-browser, out-of-process, faster; Cypress: better DX, JS only",
            "auth_fixture": "Use storageState to save and restore auth cookies across tests",
        },
    ),

    "testing_flutter": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "Flutter testing pyramid: "
            "Unit tests: test pure Dart classes, business logic, services — no widgets. "
            "Widget tests: test Flutter widgets with testWidgets + WidgetTester; "
            "no real device needed; pump widget + interact + expect. "
            "Integration tests: run on real device/emulator; test full app flows. "
            "Mockito for Flutter: @GenerateMocks([MyService]) + build_runner generates mocks. "
            "Golden tests: screenshot regression tests — expect(widget, matchesGoldenFile('golden.png'))."
        ),
        solutions=[
            "Unit test: test('adds two numbers', () { expect(add(1, 2), equals(3)); });",
            "Widget test: testWidgets('shows user name', (tester) async { await tester.pumpWidget(UserCard(name: 'Alice')); expect(find.text('Alice'), findsOneWidget); });",
            "Use flutter_test package for all test types",
            "Add golden tests for UI components — catch visual regressions in CI",
            "Run: flutter test (unit+widget) and flutter drive (integration)",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=89,
        context={
            "mock_library": "mockito (code-gen) or mocktail (no build_runner) for Dart/Flutter",
            "ci": "flutter test --coverage && lcov --list coverage/lcov.info",
        },
    ),

    "testing_testcontainers": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "Testcontainers: start real Docker containers (PostgreSQL, Redis, Kafka) in tests — "
            "no mocks for infrastructure. "
            "Benefits: tests against actual DB/broker behaviour, not simulated. "
            "Catches issues that H2 in-memory DB misses (Postgres-specific SQL, "
            "constraints, trigger behaviour). "
            "Spring Boot 3.1+: @ServiceConnection on @Bean automatically configures DataSource "
            "from the container — zero manual config."
        ),
        solutions=[
            "Add: testImplementation 'org.testcontainers:postgresql:1.19.x'",
            "@Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>('postgres:16-alpine')",
            "Spring Boot 3.1+: @Bean @ServiceConnection PostgreSQLContainer<?> pgContainer() { ... }",
            "Use @DynamicPropertySource to inject container connection details into Spring context",
            "Reuse containers across tests: withReuse(true) — container survives JVM restart",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=112,
        context={
            "spring_boot_31": "@ServiceConnection auto-configures DataSource from @Container — no @DynamicPropertySource needed",
            "supported": "PostgreSQL, MySQL, MongoDB, Redis, Kafka, RabbitMQ, Elasticsearch, and 60+ more",
        },
    ),

    "testing_contract_testing": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "Consumer-Driven Contract Testing with Spring Cloud Contract: "
            "Problem: integration tests between services are slow, brittle, require all services running. "
            "Solution: consumer writes a contract (request/response spec); provider verifies against it. "
            "Flow: consumer PR includes new contract → CI runs provider test against contract → "
            "provider must pass before consumer can deploy. "
            "Pact: polyglot alternative (supports Java, Go, JS, .NET). "
            "Benefits: fast (no real service needed), independent service deployment, "
            "catches breaking API changes early."
        ),
        solutions=[
            "Spring Cloud Contract: add spring-cloud-starter-contract-verifier to provider",
            "Write contract as Groovy DSL or YAML in provider/src/test/resources/contracts/",
            "Provider test: @SpringBootTest + ContractVerifierRule verifies all contracts",
            "Consumer: use generated WireMock stub from contract for isolated consumer tests",
            "Pact: @Pact annotation on consumer test; PactVerification on provider",
        ],
        severity="MEDIUM",
        confidence=0.89,
        times_applied=38,
        context={
            "tools": ["Spring Cloud Contract", "Pact (pact.io)", "Postman Contract Testing"],
            "use_when": "Multiple teams, independent deployments, shared API contracts",
        },
    ),

    "testing_mutation_testing": _learning(
        type_="IMPROVEMENT",
        category="TESTING",
        content=(
            "Mutation testing measures actual test quality — not just coverage. "
            "Tool injects tiny bugs (mutations) into production code and checks if tests catch them. "
            "Mutation Score = killed mutants / total mutants — target > 80%. "
            "Common mutations: replace arithmetic operators (+→-), invert conditions (>→<=), "
            "delete return statements, change boolean literals. "
            "PITest (Java): fast mutation testing; integrates with Gradle and Maven. "
            "A 90% line-coverage test suite with 40% mutation score means 60% of bugs would not be caught."
        ),
        solutions=[
            "Add PITest Gradle plugin: id 'info.solidsoft.pitest' with targetClasses=['com.example.*']",
            "Run: ./gradlew pitest — generates HTML mutation report in build/reports/pitest",
            "Focus on high-value mutations: service and domain logic; skip DTOs and mappers",
            "Address surviving mutants: they expose tests that don't assert enough",
            "Target mutation score > 80% for core business logic classes",
        ],
        severity="MEDIUM",
        confidence=0.88,
        times_applied=29,
        context={
            "tool": "PITest for Java: pitest.org; StrykerJS for JavaScript/TypeScript",
            "insight": "100% coverage + 0% mutation score = tests that never actually check results (assertion-free)",
        },
    ),

    "testing_test_data_management": _learning(
        type_="PATTERN",
        category="TESTING",
        content=(
            "Test data management strategies: "
            "Object Mother: factory class that creates ready-to-use test objects — "
            "UserMother.aValidUser(), UserMother.anAdminUser(). "
            "Builder variant: UserTestBuilder.aUser().withEmail('x@y.com').build(). "
            "Test Fixtures (@Sql): run SQL before tests to set up database state. "
            "Database Cleaner: reset DB between tests — @Transactional rollback or TRUNCATE. "
            "Randomised data: use Faker library to generate realistic test data. "
            "Test data should be self-contained — no dependency on order of test execution."
        ),
        solutions=[
            "Create TestDataFactory class with static factory methods for common test objects",
            "@Sql({'/sql/cleanup.sql', '/sql/users.sql'}) on test methods that need DB state",
            "Use @Transactional on @DataJpaTest to auto-rollback after each test",
            "Faker (java-faker) for realistic test data: faker.name().fullName(), faker.internet().email()",
            "Never use production data in tests — GDPR compliance + data changes break tests",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=134,
        context={
            "library": "java-faker, datafaker, instancio (auto-populates Java objects) for test data generation",
            "anti_pattern": "Shared mutable static test data — causes test order dependency and flakiness",
        },
    ),
}

# ============================================================================
# TESTING_KNOWLEDGE rich topic documents
# ============================================================================

TESTING_KNOWLEDGE_DOCS = {

    "testing_pyramid_guide": {
        "topic": "Test Pyramid — Strategy and Implementation",
        "category": "TESTING_STRATEGY",
        "description": "Building a balanced, effective test suite at all levels.",
        "pyramid_levels": {
            "Unit_70_percent": {
                "scope": "Single class, pure functions, business logic",
                "speed": "< 1ms per test — run on every file save",
                "tools": "JUnit 5, Mockito, AssertJ (Java); Jest (JS); pytest (Python); flutter_test (Dart)",
                "mocking": "Mock all external dependencies (DB, HTTP, email) — test only the class logic",
                "examples": ["Service method logic", "Domain model calculations", "Utility functions", "Validators"],
            },
            "Integration_20_percent": {
                "scope": "Multiple components, DB access, API endpoints",
                "speed": "100ms - 5s per test — run on PR",
                "tools": "@WebMvcTest, @DataJpaTest, Testcontainers, WireMock",
                "mocking": "Mock external services (third-party APIs); use real DB",
                "examples": ["Controller + Service + Repository", "DB queries", "Security rules", "Message consumers"],
            },
            "E2E_10_percent": {
                "scope": "Full user flow through real browser/app",
                "speed": "5-60s per test — run nightly or on release",
                "tools": "Playwright, Cypress (web); Appium, Flutter integration test (mobile)",
                "mocking": "No mocking — test against real deployed environment",
                "examples": ["Login flow", "Purchase flow", "Core feature demo path"],
            },
        },
        "anti_patterns": [
            "Test Ice Cream Cone: many E2E, few unit — slow, brittle, hard to debug",
            "Mocking too much in integration tests — no value if you mock the DB",
            "Sharing mutable test state — causes order-dependent flaky tests",
            "Tests that never fail — assertion-free tests give false confidence",
            "Not running tests in CI — tests only in local dev are useless as safety net",
        ],
        "spring_boot_test_slices": {
            "@WebMvcTest": "Controller layer only; mock services",
            "@DataJpaTest": "JPA layer only; in-memory or Testcontainers DB",
            "@JsonTest": "JSON serialisation/deserialisation only",
            "@RestClientTest": "RestTemplate/WebClient HTTP client layer only",
            "@SpringBootTest": "Full context; real HTTP server; use sparingly",
        },
        "confidence": 0.97,
    },

    "junit5_mockito_guide": {
        "topic": "JUnit 5 + Mockito — Complete Reference",
        "category": "UNIT_TESTING",
        "description": "The standard Java unit testing stack for Spring Boot applications.",
        "junit5_annotations": {
            "@Test": "Marks a test method",
            "@DisplayName": "Human-readable test name: @DisplayName('Given valid user, when login, then token returned')",
            "@BeforeEach": "Run before each test method — set up test data",
            "@AfterEach": "Run after each test — clean up",
            "@BeforeAll": "Run once before all tests in class (must be static)",
            "@Nested": "Nested test class for logical grouping of related tests",
            "@ParameterizedTest": "Run test with multiple inputs",
            "@ValueSource": "Simple parameter source: @ValueSource(strings={'a@b.com', 'c@d.com'})",
            "@CsvSource": "CSV parameters: @CsvSource({'Alice,admin', 'Bob,user'})",
            "@MethodSource": "Stream of arguments from static factory method",
            "@Tag": "Tag tests for filtering: @Tag('integration'), @Tag('slow')",
            "@Disabled": "Skip test with explanation",
            "@TempDir": "Inject temporary directory for file tests",
        },
        "mockito_cheat_sheet": {
            "create_mock": "@Mock MyService service  (with @ExtendWith(MockitoExtension.class))",
            "stub_return": "given(service.find(1L)).willReturn(Optional.of(user))",
            "stub_throw": "willThrow(new RuntimeException()).given(service).find(-1L)",
            "verify_call": "verify(service).save(any(User.class))",
            "verify_no_calls": "verifyNoInteractions(emailService)",
            "capture_arg": "ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class); verify(repo).save(captor.capture()); assertThat(captor.getValue().getEmail()).isEqualTo('a@b.com')",
            "answer": "given(service.process(any())).willAnswer(inv -> inv.getArgument(0))",
            "spy": "@Spy RealService realService — partial mock; real methods called unless stubbed",
        },
        "assertj_examples": [
            "assertThat(user.getEmail()).isEqualTo('alice@example.com')",
            "assertThat(list).hasSize(3).containsExactlyInAnyOrder('a', 'b', 'c')",
            "assertThat(optional).isPresent().hasValue(expectedUser)",
            "assertThatThrownBy(() -> service.find(-1L)).isInstanceOf(NotFoundException.class).hasMessageContaining('not found')",
            "assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED)",
        ],
        "confidence": 0.97,
    },

    "playwright_guide": {
        "topic": "Playwright — E2E Testing for Web Applications",
        "category": "E2E_TESTING",
        "description": "Modern E2E testing for React/web applications with Playwright.",
        "setup": (
            "npm init playwright@latest\n"
            "# Creates: playwright.config.ts, tests/ folder, GitHub Actions workflow\n\n"
            "# playwright.config.ts\n"
            "export default defineConfig({\n"
            "  testDir: './tests',\n"
            "  fullyParallel: true,\n"
            "  forbidOnly: !!process.env.CI,\n"
            "  retries: process.env.CI ? 2 : 0,\n"
            "  reporter: 'html',\n"
            "  use: {\n"
            "    baseURL: 'http://localhost:3000',\n"
            "    trace: 'on-first-retry',\n"
            "    screenshot: 'only-on-failure',\n"
            "  },\n"
            "  projects: [\n"
            "    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },\n"
            "    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },\n"
            "  ],\n"
            "});"
        ),
        "page_object_example": (
            "class LoginPage {\n"
            "  constructor(private page: Page) {}\n\n"
            "  async goto() { await this.page.goto('/login'); }\n\n"
            "  async login(email: string, password: string) {\n"
            "    await this.page.getByLabel('Email').fill(email);\n"
            "    await this.page.getByLabel('Password').fill(password);\n"
            "    await this.page.getByRole('button', { name: 'Log in' }).click();\n"
            "    await this.page.waitForURL('/dashboard');\n"
            "  }\n"
            "}\n\n"
            "test('user can log in', async ({ page }) => {\n"
            "  const login = new LoginPage(page);\n"
            "  await login.goto();\n"
            "  await login.login('alice@example.com', 'password');\n"
            "  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();\n"
            "});"
        ),
        "locator_best_practices": [
            "Prefer role-based: page.getByRole('button', {name: 'Submit'})",
            "Prefer label: page.getByLabel('Email address')",
            "Prefer test-id: page.getByTestId('user-card') with data-testid attribute",
            "Avoid CSS class selectors — they change with UI refactoring",
            "Avoid XPath — brittle and hard to read",
        ],
        "ci_integration": (
            "# .github/workflows/playwright.yml\n"
            "- uses: actions/checkout@v4\n"
            "- uses: actions/setup-node@v4\n"
            "- run: npm ci\n"
            "- run: npx playwright install chromium\n"
            "- run: npx playwright test\n"
            "- uses: actions/upload-artifact@v4\n"
            "  if: always()\n"
            "  with: {name: playwright-report, path: playwright-report/}\n"
        ),
        "confidence": 0.93,
    },

    "flutter_testing_guide": {
        "topic": "Flutter Testing — Unit, Widget, Integration",
        "category": "FLUTTER_TESTING",
        "description": "Comprehensive testing for Flutter mobile/web applications.",
        "unit_tests": {
            "description": "Test pure Dart business logic without widgets or UI",
            "file_location": "test/unit/",
            "example": (
                "test('calculateTotal returns sum of item prices', () {\n"
                "  final cart = Cart(items: [Item(price: 10.0), Item(price: 5.0)]);\n"
                "  expect(cart.calculateTotal(), equals(15.0));\n"
                "});"
            ),
        },
        "widget_tests": {
            "description": "Test Flutter widgets in isolation without a real device",
            "file_location": "test/widget/",
            "example": (
                "testWidgets('UserCard shows user name and email', (WidgetTester tester) async {\n"
                "  await tester.pumpWidget(\n"
                "    MaterialApp(home: UserCard(name: 'Alice', email: 'alice@example.com')),\n"
                "  );\n"
                "  expect(find.text('Alice'), findsOneWidget);\n"
                "  expect(find.text('alice@example.com'), findsOneWidget);\n"
                "});"
            ),
        },
        "integration_tests": {
            "description": "Full app tests on real device or emulator",
            "file_location": "integration_test/",
            "command": "flutter test integration_test/ -d chrome",
            "example": (
                "void main() {\n"
                "  IntegrationTestWidgetsFlutterBinding.ensureInitialized();\n"
                "  testWidgets('user can log in', (tester) async {\n"
                "    app.main();\n"
                "    await tester.pumpAndSettle();\n"
                "    await tester.enterText(find.byKey(Key('emailField')), 'alice@test.com');\n"
                "    await tester.tap(find.byKey(Key('loginButton')));\n"
                "    await tester.pumpAndSettle();\n"
                "    expect(find.text('Dashboard'), findsOneWidget);\n"
                "  });\n"
                "}"
            ),
        },
        "mocking_with_mocktail": (
            "class MockUserService extends Mock implements UserService {}\n\n"
            "setUp(() {\n"
            "  mockService = MockUserService();\n"
            "  when(() => mockService.getUser(any())).thenAnswer((_) async => testUser);\n"
            "});\n\n"
            "test('loads user on init', () async {\n"
            "  final cubit = UserCubit(mockService);\n"
            "  await cubit.loadUser(1);\n"
            "  expect(cubit.state, isA<UserLoaded>());\n"
            "  verify(() => mockService.getUser(1)).called(1);\n"
            "});"
        ),
        "golden_tests": {
            "description": "Screenshot regression tests",
            "usage": "await expectLater(find.byType(UserCard), matchesGoldenFile('goldens/user_card.png'))",
            "update": "flutter test --update-goldens to regenerate golden files",
        },
        "confidence": 0.93,
    },

    "test_quality_guide": {
        "topic": "Test Quality — Coverage, Mutation, and Reliability",
        "category": "TEST_QUALITY",
        "description": "Measuring and improving the quality of your test suite.",
        "code_coverage": {
            "types": {
                "line_coverage": "% of lines executed by tests",
                "branch_coverage": "% of conditional branches (if/else) tested",
                "mutation_score": "% of injected bugs caught by tests — best quality metric",
            },
            "targets": {
                "line_coverage": "> 80% for service/domain classes",
                "branch_coverage": "> 70% — focus on business logic",
                "mutation_score": "> 80% for core business classes",
            },
            "spring_boot": "JaCoCo plugin in Gradle generates HTML + XML coverage reports",
            "ci_gate": "Fail build if coverage drops below threshold",
        },
        "flaky_test_prevention": [
            "No timing-dependent tests (Thread.sleep) — use Awaitility for async assertions",
            "No shared mutable state between tests",
            "Mock external services — don't make real HTTP calls in unit tests",
            "Use deterministic test data — not random without seeded Random",
            "Idempotent DB tests — each test sets up its own data and cleans up",
        ],
        "test_naming_conventions": {
            "given_when_then": "givenValidUser_whenLogin_thenTokenReturned",
            "should_style": "shouldReturnToken_whenCredentialsAreValid",
            "display_name": "@DisplayName('Given valid credentials, when login called, then returns JWT token')",
            "bdd_style": "user_can_login_with_valid_credentials",
        },
        "test_smells": [
            "No assertions — test passes without checking anything",
            "Too many assertions in one test — test breaks for multiple reasons",
            "Mystery guest — test depends on external state not visible in the test",
            "Chatty test — excessive output; assertions on implementation details",
            "Slow test — test that takes > 500ms is doing too much",
        ],
        "confidence": 0.94,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 7 — Testing Strategies",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "testing_knowledge": TESTING_KNOWLEDGE_DOCS,
        },
    )
