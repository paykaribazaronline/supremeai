package org.example.config;

import org.example.service.FirebaseService;
import org.example.service.AdminMessagePusher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import static org.mockito.Mockito.mock;

/**
 * Test Configuration for mocking Firebase and other external dependencies
 * in unit and integration tests.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Mock FirebaseService to prevent actual Firebase calls in tests.
     * This bean takes priority over any real FirebaseService bean.
     */
    @Bean
    @Primary
    public FirebaseService mockFirebaseService() {
        return mock(FirebaseService.class);
    }

    /**
     * Mock AdminMessagePusher to prevent Firebase Firestore initialization in tests.
     * This bean takes priority over any real AdminMessagePusher bean.
     */
    @Bean
    @Primary
    public AdminMessagePusher mockAdminMessagePusher() {
        return mock(AdminMessagePusher.class);
    }
}
