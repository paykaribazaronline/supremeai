package com.supremeai;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that performs a one-time Firestore initialisation check at the
 * start of a test class.
 *
 * <p>Use with {@code @ExtendWith(BaseFirestoreTest.class)}.  If the Firestore
 * emulator is running its host will be logged; the test suite will continue even
 * if the emulator is unreachable — individual Firestore-paying tests should add
 * {@code @MockBean} for Firestore beans or ensure the emulator is running.</p>
 */
public abstract class BaseFirestoreTest implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        // Nothing blocking here — we only emit a hint line so developers know
        // the emulator state; the real Firestore beans are provided by @MockBean
        // or TestFirestoreConfig in tests that require them.
        String emulatorHost = System.getenv().getOrDefault("FIRESTORE_EMULATOR_HOST", "");
        if (!emulatorHost.isBlank()) {
            log.info("Firestore emulator detected at {} — data-bean tests will hit the emulator", emulatorHost);
        } else {
            log.info("No FIRESTORE_EMULATOR_HOST set — Firestore beans should be mocked or the emulator started");
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // nothing to tear down
    }

    private static final Logger log = LoggerFactory.getLogger(BaseFirestoreTest.class);
}
