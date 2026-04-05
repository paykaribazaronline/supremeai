package org.example.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecretManagerTest {

    @Test
    void cacheRoundTripWorksForEnvBackend() {
        SecretManager manager = new SecretManager();
        ReflectionTestUtils.setField(manager, "backend", "env");

        manager.updateSecret("TEST_SECRET", "value-1");

        assertEquals("value-1", manager.getSecret("TEST_SECRET"));

        manager.invalidateCache("TEST_SECRET");
        assertNull(manager.getSecret("TEST_SECRET"));
    }

    @Test
    void unknownBackendFallsBackWithoutThrowing() {
        SecretManager manager = new SecretManager();
        ReflectionTestUtils.setField(manager, "backend", "unknown-backend");

        assertDoesNotThrow(() -> manager.getSecret("NON_EXISTENT_SECRET"));
    }

    @Test
    void gcpBackendWithoutProjectIdDoesNotCrash() {
        SecretManager manager = new SecretManager();
        ReflectionTestUtils.setField(manager, "backend", "gcp");
        ReflectionTestUtils.setField(manager, "gcpProjectId", "");

        assertDoesNotThrow(() -> manager.getSecret("ANY_SECRET"));
        assertDoesNotThrow(() -> manager.updateSecret("ANY_SECRET", "new-value"));
        assertEquals("new-value", manager.getSecret("ANY_SECRET"));
    }
}
