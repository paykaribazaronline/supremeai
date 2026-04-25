
# SupremeAI Testing Guide

This guide provides comprehensive instructions for running tests, generating coverage reports, and understanding the testing strategy for the SupremeAI project.

## Table of Contents

1. [Test Structure](#test-structure)
2. [Running Tests](#running-tests)
3. [Test Coverage](#test-coverage)
4. [Writing New Tests](#writing-new-tests)
5. [CI/CD Integration](#cicd-integration)

## Test Structure

The SupremeAI project follows a layered testing approach:

```
src/test/java/com/supremeai/
├── service/          # Unit tests for service layer
├── controller/       # Unit tests for REST controllers
├── security/         # Tests for security components
├── ml/               # Tests for machine learning components
├── agentorchestration/ # Tests for agent orchestration
├── selfhealing/      # Tests for self-healing mechanisms
├── provider/         # Tests for AI provider implementations
└── integration/      # Integration tests for end-to-end flows
```

### Test Categories

1. **Unit Tests**: Test individual components in isolation using mocks
2. **Integration Tests**: Test the interaction between multiple components
3. **End-to-End Tests**: Test complete workflows from API to database

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Class

```bash
./gradlew test --tests MultiAIConsensusServiceTest
```

### Run Tests in a Specific Package

```bash
./gradlew test --tests com.supremeai.service.*
```

### Run Tests with Coverage Report

```bash
./gradlew clean test jacocoTestReport
```

The coverage report will be generated at `build/reports/jacoco/test/html/index.html`.

### Run Tests in Parallel

Tests are configured to run in parallel by default. To control the number of parallel forks:

```bash
./gradlew test -DmaxParallelForks=4
```

## Test Coverage

### Coverage Goals

- **Line Coverage**: 100%
- **Branch Coverage**: 100%

### Generating Coverage Report

```bash
./gradlew clean test jacocoTestReport
```

### Viewing Coverage Report

Open the HTML report in a browser:

```bash
open build/reports/jacoco/test/html/index.html
```

On Linux/Windows:

```bash
xdg-open build/reports/jacoco/test/html/index.html  # Linux
start build/reports/jacoco/test/html/index.html      # Windows
```

### Coverage Exclusions

The following packages are excluded from coverage calculations:

- `model/` - Data transfer objects
- `config/` - Configuration classes
- `exception/` - Exception classes
- `dto/` - Data transfer objects
- `*Configuration*` - Spring configuration classes
- `*Config*` - Configuration classes
- `*Exception*` - Exception classes
- `*Controller*` - Controllers (tested separately)
- `*Aspect*` - AOP aspects

## Writing New Tests

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
public class MyServiceTest {

    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyService service;

    @Test
    public void testDoSomething_Success() {
        // Arrange
        when(repository.findById("123")).thenReturn(Mono.just(new MyEntity()));

        // Act
        MyEntity result = service.doSomething("123").block();

        // Assert
        assertNotNull(result);
        verify(repository).findById("123");
    }
}
```

### Controller Test Example

```java
@WebMvcTest(MyController.class)
public class MyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MyService service;

    @Test
    public void testGetEndpoint_Success() throws Exception {
        // Arrange
        when(service.getData()).thenReturn("test data");

        // Act & Assert
        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(content().string("test data"));
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@ActiveProfiles("test")
public class MyIntegrationTest {

    @Autowired
    private MyRepository repository;

    @Test
    public void testEndToEndFlow() {
        // Arrange
        MyEntity entity = new MyEntity();
        entity.setName("Test");

        // Act
        MyEntity saved = repository.save(entity).block();

        // Assert
        assertNotNull(saved.getId());
        assertEquals("Test", saved.getName());
    }
}
```

## CI/CD Integration

### GitHub Actions

Tests are automatically run on every push and pull request. The workflow is defined in `.github/workflows/test.yml`.

### Local Pre-commit Hook

To run tests before committing, you can set up a pre-commit hook:

```bash
# Create .git/hooks/pre-commit
#!/bin/bash
./gradlew test
if [ $? -ne 0 ]; then
    echo "Tests failed. Commit aborted."
    exit 1
fi
```

Make the hook executable:

```bash
chmod +x .git/hooks/pre-commit
```

## Best Practices

1. **Test Naming**: Use descriptive names that explain what is being tested
2. **AAA Pattern**: Arrange, Act, Assert structure for test methods
3. **Mock External Dependencies**: Use mocks for external services and databases
4. **Test Isolation**: Each test should be independent and not rely on other tests
5. **Edge Cases**: Test not just happy paths but also error conditions
6. **Test Data Management**: Clean up test data after each test
7. **Test Speed**: Keep tests fast by avoiding unnecessary delays

## Troubleshooting

### Tests Fail Due to Database Connection

Ensure the test profile is active:

```bash
./gradlew test -Dspring.profiles.active=test
```

### Tests Fail Due to Missing Dependencies

Clean and rebuild the project:

```bash
./gradlew clean build --refresh-dependencies
```

### Coverage Report Not Generated

Ensure JaCoCo plugin is properly configured and run:

```bash
./gradlew clean test jacocoTestReport
```

## Additional Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Test Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
