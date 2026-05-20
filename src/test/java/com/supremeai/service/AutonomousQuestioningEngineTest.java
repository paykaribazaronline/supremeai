package com.supremeai.service;

import com.supremeai.provider.AIProviderFactory;
import com.supremeai.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutonomousQuestioningEngineTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private ConfigService configService;

    private AutonomousQuestioningEngine questioningEngine;

    @BeforeEach
    void setUp() {
        questioningEngine = new AutonomousQuestioningEngine();
        setField(questioningEngine, "providerFactory", providerFactory);
        setField(questioningEngine, "configService", configService);

        // Default config values
        when(configService.getSetting("min_prompt_length", 10)).thenReturn(10);
        when(configService.getSetting("min_code_requirement_length", 20)).thenReturn(20);
        when(configService.getThreshold("min_clarity", 0.6)).thenReturn(0.6);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = AutonomousQuestioningEngine.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== validateAndQuestion - General AI Tests ====================

    @Test
    void validateAndQuestion_ClearDetailedPrompt_ReturnsComplete() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Write a complete Python function which requires parameters to run a Flask REST API with user authentication, CRUD operations for products, and PostgreSQL database connection, and returns the response",
                        AutonomousQuestioningEngine.RequestType.CODE_GENERATION
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertTrue(r.isComplete());
                    assertFalse(r.hasQuestions());
                    assertTrue(r.getClarityScore() >= 0.6);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void validateAndQuestion_VeryShortPrompt_ReturnsIncompleteWithQuestions() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "hi",
                        AutonomousQuestioningEngine.RequestType.GENERAL_AI
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertFalse(r.isComplete());
                    assertTrue(r.hasQuestions());
                    assertTrue(r.getClarityScore() < 0.6);
                    return true;
                })
                .verifyComplete();
    }

    // ==================== CODE_GENERATION Request Type Tests ====================

    @Test
    void validateAndQuestion_CodeGenWithoutLanguage_AddsLanguageQuestion() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Create a function",
                        AutonomousQuestioningEngine.RequestType.CODE_GENERATION
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertFalse(r.isComplete());
                    List<String> questions = r.getClarifyingQuestions();
                    assertTrue(questions.stream().anyMatch(q -> q.contains("programming language")));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void validateAndQuestion_CodeGenCompletePrompt_ReturnsComplete() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Write a Python function which requires a list of integers as input and returns the sorted list in ascending order",
                        AutonomousQuestioningEngine.RequestType.CODE_GENERATION
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertTrue(r.isComplete());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== API_DESIGN Request Type Tests ====================

    @Test
    void validateAndQuestion_ApiDesignWithoutEndpoints_AddsEndpointQuestion() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Design an API",
                        AutonomousQuestioningEngine.RequestType.API_DESIGN
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertFalse(r.isComplete());
                    List<String> questions = r.getClarifyingQuestions();
                    assertTrue(questions.stream().anyMatch(q -> q.contains("endpoints")));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void validateAndQuestion_ApiDesignComplete_ReturnsComplete() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Design the REST API using Node.js tech stack with GET /users endpoint and POST /users endpoint for creating users which requires proper path handling and returns the expected JSON format.",
                        AutonomousQuestioningEngine.RequestType.API_DESIGN
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    System.out.println("DEBUG API_DESIGN Questions: " + r.getClarifyingQuestions());
                    System.out.println("DEBUG API_DESIGN Score: " + r.getClarityScore());
                    assertTrue(r.isComplete());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== DATABASE_SCHEMA Request Type Tests ====================

    @Test
    void validateAndQuestion_DbSchemaWithoutEntities_AddsEntityQuestion() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Create a database",
                        AutonomousQuestioningEngine.RequestType.DATABASE_SCHEMA
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertFalse(r.isComplete());
                    List<String> questions = r.getClarifyingQuestions();
                    assertTrue(questions.stream().anyMatch(q -> q.contains("entities")));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void validateAndQuestion_DbSchemaComplete_ReturnsComplete() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Design a database schema with User table and Order table with one-to-many relationship",
                        AutonomousQuestioningEngine.RequestType.DATABASE_SCHEMA
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertTrue(r.isComplete());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== BUG_FIX Request Type Tests ====================

    @Test
    void validateAndQuestion_BugFixWithoutError_AddsErrorQuestion() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "My code doesn't work",
                        AutonomousQuestioningEngine.RequestType.BUG_FIX
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    assertFalse(r.isComplete());
                    List<String> questions = r.getClarifyingQuestions();
                    assertTrue(questions.stream().anyMatch(q -> q.contains("error")));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void validateAndQuestion_BugFixComplete_ReturnsComplete() {
        Mono<AutonomousQuestioningEngine.ValidationResult> result =
                questioningEngine.validateAndQuestion(
                        "Please fix the NullPointerException at line 45 where the object reference is not set to an instance. The requirement is to resolve the error. Here is the code snippet: void myFunc() { String s = null; System.out.println(s.length()); }",
                        AutonomousQuestioningEngine.RequestType.BUG_FIX
                );

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    System.out.println("DEBUG BUG_FIX Questions: " + r.getClarifyingQuestions());
                    System.out.println("DEBUG BUG_FIX Score: " + r.getClarityScore());
                    assertTrue(r.isComplete());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== Clarity Score Tests ====================

    @Test
    void calculateClarityScore_DetailedInput_ReturnsHighScore() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("calculateClarityScore", String.class,
                            AutonomousQuestioningEngine.RequestType.class);
            method.setAccessible(true);

            double score = (double) method.invoke(questioningEngine,
                    "Write a Python function which requires authentication and database to return API responses",
                    AutonomousQuestioningEngine.RequestType.CODE_GENERATION);

            assertTrue(score > 0.5);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void calculateClarityScore_VagueInput_ReturnsLowScore() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("calculateClarityScore", String.class,
                            AutonomousQuestioningEngine.RequestType.class);
            method.setAccessible(true);

            double score = (double) method.invoke(questioningEngine,
                    "do stuff",
                    AutonomousQuestioningEngine.RequestType.GENERAL_AI);

            assertTrue(score < 0.5);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    // ==================== Ambiguity Detection Tests ====================

    @Test
    void checkAmbiguity_PronounUsage_ReturnsQuestion() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("checkAmbiguity", String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(questioningEngine,
                    "I want to process it and then send them the results");

            assertTrue(result.size() > 0);
            assertTrue(result.get(0).contains("clarify"));
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void checkAmbiguity_VagueTerms_ReturnsQuestion() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("checkAmbiguity", String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(questioningEngine,
                    "Do something with that stuff somehow");

            assertTrue(result.size() > 0);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void checkAmbiguity_ClearInput_ReturnsEmpty() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("checkAmbiguity", String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(questioningEngine,
                    "Create a Python function named calculateSum");

            assertEquals(0, result.size());
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    // ==================== Conflict Detection Tests ====================

    @Test
    void checkConflicts_ConflictingTerms_ReturnsQuestion() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("checkConflicts", String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(questioningEngine,
                    "Make it fast but also very detailed and comprehensive");

            assertTrue(result.size() > 0);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void checkConflicts_NoConflicts_ReturnsEmpty() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("checkConflicts", String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(questioningEngine,
                    "Create a simple REST API");

            assertEquals(0, result.size());
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    // ==================== Keyword Presence Tests ====================

    @Test
    void hasProgrammingLanguage_VariousLanguages_Detected() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("hasProgrammingLanguage", String.class);
            method.setAccessible(true);

            assertTrue((boolean) method.invoke(questioningEngine, "Write Python code".toLowerCase()));
            assertTrue((boolean) method.invoke(questioningEngine, "Java implementation".toLowerCase()));
            assertTrue((boolean) method.invoke(questioningEngine, "JavaScript function".toLowerCase()));
            assertTrue((boolean) method.invoke(questioningEngine, "Go program".toLowerCase()));
            assertTrue((boolean) method.invoke(questioningEngine, "Rust code".toLowerCase()));
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void hasProgrammingLanguage_NonCodeInput_ReturnsFalse() {
        try {
            java.lang.reflect.Method method = AutonomousQuestioningEngine.class
                    .getDeclaredMethod("hasProgrammingLanguage", String.class);
            method.setAccessible(true);

            assertFalse((boolean) method.invoke(questioningEngine, "What is machine learning?".toLowerCase()));
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }
}