package org.example.service;

import org.example.model.SystemLearning;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SystemLearningServiceTest {

    @Test
    void recordError_allowsNullExceptionAndStoresMemoryOnlyStats() {
        SystemLearningService service = new SystemLearningService();

        service.recordError("AUTH", "Missing bearer token", null, "Return 401 with sendError");

        Map<String, Object> stats = service.getLearningStats();
        assertEquals("memory-only", stats.get("status"));
        assertEquals(1, stats.get("totalLearnings"));
        assertEquals(1L, stats.get("errorsResolved"));
        assertEquals(List.of("Return 401 with sendError"), service.getSolutionsFor("auth"));
    }

    @Test
    void recordTechnique_isIdempotentForSameTechniqueNameAndCategory() {
        SystemLearningService service = new SystemLearningService();

        service.recordTechnique(
            "APP_CREATION",
            "Generate in layers",
            "Create model, service, controller, validation, and tests in order.",
            Arrays.asList("Generate model", "Generate service"),
            0.95,
            Map.of("source", "test")
        );

        service.recordTechnique(
            "app_creation",
            "Generate in layers",
            "Create model, service, controller, validation, and tests in order.",
            Arrays.asList("Generate model", "Generate service", "Generate tests"),
            0.99,
            Map.of("source", "test")
        );

        Map<String, Object> stats = service.getLearningStats();
        assertEquals(1, stats.get("totalLearnings"));
        assertEquals(1L, stats.get("techniques"));
        assertTrue(service.getSolutionsFor("APP_CREATION").contains("Generate tests"));
    }

    @Test
    void criticalRequirements_areReturnedFromCacheWithoutFirebase() {
        SystemLearningService service = new SystemLearningService();

        service.recordRequirement("Verification is mandatory", "Compile, test, and smoke checks required.");

        List<SystemLearning> critical = service.getCriticalRequirements();
        assertEquals(1, critical.size());
        assertEquals("REQUIREMENT", critical.get(0).getType());
        assertEquals("ADMIN", critical.get(0).getCategory());
        assertEquals("Verification is mandatory", critical.get(0).getContent());
    }

    @Test
    void learningStats_includeCategoryBreakdownForTechniquesAndRequirements() {
        SystemLearningService service = new SystemLearningService();

        service.recordRequirement("Admin mode required", "AUTO WAIT FORCE_STOP must be respected.");
        service.recordTechnique(
            "QUOTA",
            "Use solo limits",
            "Respect the lower operating thresholds in solo mode.",
            Arrays.asList("Limit concurrent work", "Watch memory"),
            0.94,
            Map.of("source", "test")
        );

        Map<String, Object> stats = service.getLearningStats();
        @SuppressWarnings("unchecked")
        Map<String, Long> byCategory = (Map<String, Long>) stats.get("byCategory");

        assertEquals(2, stats.get("totalLearnings"));
        assertEquals(1L, stats.get("requirements"));
        assertEquals(1L, stats.get("techniques"));
        assertEquals(1L, byCategory.get("ADMIN"));
        assertEquals(1L, byCategory.get("QUOTA"));
    }

    @Test
    void learnFromIncident_recordsErrorAndIncidentPlaybook() {
        SystemLearningService service = new SystemLearningService();

        Map<String, Object> result = service.learnFromIncident(
            "SECURITY",
            "Any authenticated user can read all records",
            "Root rules were auth != null",
            "Set root deny and per-path authorization",
            List.of("Test unauth read denied", "Test non-admin denied on admin paths"),
            0.97,
            Map.of("environment", "prod")
        );

        assertEquals("success", result.get("status"));
        assertEquals("SECURITY", result.get("category"));

        List<SystemLearning> incidents = service.getIncidentPlaybooks("security");
        assertEquals(1, incidents.size());
        assertTrue(incidents.get(0).getContent().startsWith("Incident Playbook:"));

        List<String> solutions = service.getSolutionsFor("SECURITY");
        assertTrue(solutions.contains("Set root deny and per-path authorization"));
    }
}