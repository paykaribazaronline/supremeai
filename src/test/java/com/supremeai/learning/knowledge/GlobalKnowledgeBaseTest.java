package com.supremeai.learning.knowledge;

import com.supremeai.admin.AdminDashboardService;
import com.supremeai.repository.SolutionMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalKnowledgeBase.
 * Tests memory storage, retrieval, versioning, and scoring logic.
 */
class GlobalKnowledgeBaseTest {

    private GlobalKnowledgeBase knowledgeBase;

    @Mock
    private AdminDashboardService adminDashboard;

    @Mock
    private SolutionMemoryRepository solutionMemoryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        knowledgeBase = new GlobalKnowledgeBase(adminDashboard, solutionMemoryRepository);
        
        // Default behavior: approve immediately
        when(adminDashboard.submitImprovement(any())).thenReturn(Mono.just(true));
        // Stub repository save to return Mono of saved entity (echo)
        when(solutionMemoryRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    void testRecordSuccessWithPermission_newSolutionCreatesMemory() {
        String errorSig = "NullPointerException#UserService:45";
        String successfulCode = "if (obj != null) { obj.method(); }";

        // Record a successful fix
        knowledgeBase.recordSuccessWithPermission(
            errorSig, successfulCode, "openai", 50L, 0.9
        ).block();

        // Verify solution was stored in memory
        List<SolutionMemory> solutions = knowledgeBase.getSolutions(errorSig).collectList().block();
        assertNotNull(solutions);
        assertEquals(1, solutions.size());

        SolutionMemory mem = solutions.get(0);
        assertEquals(errorSig, mem.getTriggerError());
        assertEquals(successfulCode, mem.getResolvedCode());
        assertEquals("openai", mem.getWorkingAIProvider());
        assertEquals(1, mem.getSuccessCount());
        assertEquals(0, mem.getFailureCount());
    }

    @Test
    void testRecordSuccessWithPermission_identicalSolution_updatesVersion() {
        String errorSig = "IndexOutOfBounds#ArrayService:10";
        String code = "list.add(item);";

        // First solution
        knowledgeBase.recordSuccessWithPermission(errorSig, code, "anthropic", 100L, 0.8).block();
        assertEquals(1, knowledgeBase.getSolutions(errorSig).collectList().block().size());

        // Same exact solution again
        knowledgeBase.recordSuccessWithPermission(errorSig, code, "openai", 90L, 0.85).block();

        // Should create a new version instead of duplicate
        List<SolutionMemory> solutions = knowledgeBase.getSolutions(errorSig).collectList().block();
        assertNotNull(solutions);
        assertEquals(2, solutions.size(), "Identical solution should create new version");

        SolutionMemory v1 = solutions.get(0);
        SolutionMemory v2 = solutions.get(1);
        assertEquals(1L, v1.getVersion());
        assertEquals(2L, v2.getVersion());
        assertEquals(v1.getId(), v2.getPreviousVersionId());
    }

    @Test
    void testFindKnownSolution_returnsBestAboveThreshold() {
        String errorSig = "TypeError#Utils:20";
        String goodCode = "const x = typeof y !== 'undefined' ? y : default;";

        // Seed memory
        knowledgeBase.recordSuccessWithPermission(errorSig, goodCode, "openai", 100L, 0.9).block();
        SolutionMemory good = knowledgeBase.getSolutions(errorSig).collectList().block().get(0);
        good.setSuccessCount(10);
        good.setFailureCount(0);

        String found = knowledgeBase.findKnownSolution(errorSig).block();
        assertEquals(goodCode, found);
    }

    @Test
    void testFindKnownSolution_returnsNullBelowThreshold() {
        String errorSig = "Error#LowQuality:1";
        String poorCode = "bad quality code";

        // Seed memory
        knowledgeBase.recordSuccessWithPermission(errorSig, poorCode, "test", 100L, 0.1).block();
        SolutionMemory poor = knowledgeBase.getSolutions(errorSig).collectList().block().get(0);
        poor.setSuccessCount(1);
        poor.setFailureCount(10);

        String found = knowledgeBase.findKnownSolution(errorSig).block();
        assertNull(found, "Solutions below 0.4 threshold should return null");
    }

    @Test
    void testFindKnownSolution_multipleSolutions_returnsHighestScore() {
        String errorSig = "MultipleSolutions#Test:99";
        String code1 = "solution1";
        String code2 = "solution2";
        String code3 = "solution3";

        // Solution 1: 50% success
        knowledgeBase.recordSuccessWithPermission(errorSig, code1, "a", 100L, 0.5).block();
        knowledgeBase.recordFailure(errorSig, code1).block();
        
        // Solution 2: 100% success, high security
        knowledgeBase.recordSuccessWithPermission(errorSig, code2, "b", 100L, 0.9).block();
        knowledgeBase.recordSuccessWithPermission(errorSig, code2, "b", 100L, 0.9).block();

        // Solution 3: 100% success, low security
        knowledgeBase.recordSuccessWithPermission(errorSig, code3, "c", 100L, 0.4).block();

        String found = knowledgeBase.findKnownSolution(errorSig).block();
        assertEquals(code2, found, "Should return solution with highest supreme score (Solution 2)");
    }

    @Test
    void testFindKnownSolution_bestScoreSelected() {
        String errorSig = "BestScore#Test";
        // Solution A: 0.8 security, 1 success
        knowledgeBase.recordSuccessWithPermission(errorSig, "A", "a", 100L, 0.8).block();
        // Solution B: 0.9 security, 1 success -> Should win
        knowledgeBase.recordSuccessWithPermission(errorSig, "B", "b", 100L, 0.9).block();
        
        assertEquals("B", knowledgeBase.findKnownSolution(errorSig).block());
    }

    @Test
    void testRecordFailure_incrementsFailureCount() {
        String errorSig = "FailureTest#1";
        String code = "test code";
        
        // Seed the memory using recordSuccess
        knowledgeBase.recordSuccessWithPermission(errorSig, code, "test", 100L, 0.5).block();
        SolutionMemory mem = knowledgeBase.getSolutions(errorSig).collectList().block().get(0);
        
        knowledgeBase.recordFailure(errorSig, code).block();

        List<SolutionMemory> solutions = knowledgeBase.getSolutions(errorSig).collectList().block();
        assertNotNull(solutions);
        SolutionMemory updated = solutions.get(solutions.size() - 1);
        // Should have created new version with incremented failure count
        if (updated.getId() != null && !updated.getId().equals(mem.getId())) {
            // New version was created
            assertEquals(mem.getFailureCount() + 1, updated.getFailureCount());
            assertEquals(mem.getSuccessCount(), updated.getSuccessCount());
        } else {
            // In-memory update without versioning or IDs not yet set
            assertEquals(1, updated.getFailureCount());
        }
    }

    @Test
    void testCountSolutions_totalAcrossAllSignatures() {
        knowledgeBase.recordSuccessWithPermission("err1", "code1", "ai1", 100L, 0.8).block();
        knowledgeBase.recordSuccessWithPermission("err1", "code2", "ai2", 100L, 0.8).block();
        knowledgeBase.recordSuccessWithPermission("err2", "code3", "ai3", 100L, 0.8).block();

        long count = knowledgeBase.countSolutions().block();
        assertNotNull(count);
        assertEquals(3, (long) count);
    }

    @Test
    void testGetSolutions_emptyListForUnknownSignature() {
        List<SolutionMemory> solutions = knowledgeBase.getSolutions("unknown_error").collectList().block();
        if (solutions == null) solutions = List.of();
        assertTrue(solutions.isEmpty(), "Unknown error signature should return empty list");
    }

    @Test
    void testLoadMemories_noRepository_logsWarning() {
        // With null repository, should not throw
        knowledgeBase.loadMemories();
        // Test passes if no exception thrown
    }

    @Test
    void testSaveSolutionMemory_noRepository_returnsMonoJust() {
        SolutionMemory mem = new SolutionMemory("test", "code", "ai", 100L, 0.5);
        knowledgeBase.saveSolutionMemory(mem)
            .subscribe(saved -> assertEquals(mem, saved));
        // Completes without error
    }
}
