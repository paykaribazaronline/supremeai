package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataLifecycleServiceTest {

    private DataLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new DataLifecycleService();
        ReflectionTestUtils.setField(service, "defaultTtlDays", 30);
        ReflectionTestUtils.setField(service, "gracePeriodDays", 7);
    }

    // ─── register tests ───────────────────────────────────────────────────────

    @Test
    void register_newItem_isActive() {
        service.register("data-001", "chat_history");
        assertTrue(service.isActive("data-001"));
    }

    @Test
    void register_customTtl_isActive() {
        service.register("data-002", "simulator_session", 5);
        assertTrue(service.isActive("data-002"));
    }

    @Test
    void register_unknownId_isNotActive() {
        assertFalse(service.isActive("non-existent"));
    }

    // ─── softDelete tests ─────────────────────────────────────────────────────

    @Test
    void softDelete_registeredItem_isNoLongerActive() {
        service.register("data-003", "api_key");
        service.softDelete("data-003");
        assertFalse(service.isActive("data-003"));
    }

    @Test
    void softDelete_registeredItem_setsHardDeleteAfter() {
        service.register("data-004", "temp");
        service.softDelete("data-004");

        List<DataLifecycleService.LifecycleEntry> entries = service.getAllEntries();
        DataLifecycleService.LifecycleEntry entry = entries.stream()
            .filter(e -> e.getDataId().equals("data-004"))
            .findFirst().orElse(null);

        assertNotNull(entry);
        assertTrue(entry.isSoftDeleted());
        assertNotNull(entry.getHardDeleteAfter());
        assertTrue(entry.getHardDeleteAfter().isAfter(LocalDateTime.now()));
    }

    @Test
    void softDelete_unknownId_noException() {
        assertDoesNotThrow(() -> service.softDelete("unknown-id"));
    }

    // ─── restore tests ────────────────────────────────────────────────────────

    @Test
    void restore_withinGracePeriod_restoresSuccessfully() {
        service.register("data-005", "user_data");
        service.softDelete("data-005");
        assertTrue(service.restore("data-005"));
        assertTrue(service.isActive("data-005"));
    }

    @Test
    void restore_nonExistentItem_returnsFalse() {
        assertFalse(service.restore("does-not-exist"));
    }

    @Test
    void restore_nonSoftDeletedItem_returnsFalse() {
        service.register("data-006", "config");
        assertFalse(service.restore("data-006"));
    }

    // ─── getStats tests ───────────────────────────────────────────────────────

    @Test
    void getStats_mixedItems_returnsCorrectCounts() {
        service.register("stat-001", "type_a");
        service.register("stat-002", "type_b");
        service.register("stat-003", "type_c");
        service.softDelete("stat-002");

        Map<String, Object> stats = service.getStats();

        assertEquals(3L, stats.get("total"));
        assertEquals(2L, stats.get("active"));
        assertEquals(1L, stats.get("softDeleted"));
    }

    @Test
    void getStats_emptyRegistry_allZero() {
        Map<String, Object> stats = service.getStats();
        assertEquals(0L, stats.get("total"));
        assertEquals(0L, stats.get("active"));
        assertEquals(0L, stats.get("softDeleted"));
        assertEquals(0L, stats.get("expired"));
    }

    // ─── getAllEntries tests ──────────────────────────────────────────────────

    @Test
    void getAllEntries_afterRegistering_returnsAllEntries() {
        service.register("entry-001", "type_x");
        service.register("entry-002", "type_y");

        List<DataLifecycleService.LifecycleEntry> entries = service.getAllEntries();
        assertTrue(entries.stream().anyMatch(e -> e.getDataId().equals("entry-001")));
        assertTrue(entries.stream().anyMatch(e -> e.getDataId().equals("entry-002")));
    }
}
