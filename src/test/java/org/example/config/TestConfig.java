package org.example.config;

import org.example.service.FirebaseService;
import org.example.service.AdminMessagePusher;
import org.example.service.AgentOrchestrator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import static org.mockito.Mockito.*;

/**
 * Test Configuration for mocking Firebase and other external dependencies
 * in unit and integration tests.
 * 
 * This configuration ensures:
 * - Firebase SDK never initializes in test context
 * - All Firebase dependencies are mocked
 * - Test beans take priority over production beans
 * - Spring context can start without GOOGLE_APPLICATION_CREDENTIALS
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Mock FirebaseService to prevent actual Firebase calls in tests.
     * This bean takes priority over any real FirebaseService bean.
     * Prevents: "FirebaseApp with name [DEFAULT] doesn't exist" errors
     */
    @Bean
    @Primary
    public FirebaseService mockFirebaseService() {
        return mock(FirebaseService.class);
    }

    /**
     * Mock AdminMessagePusher to prevent Firebase Firestore initialization in tests.
     * This bean takes priority over any real AdminMessagePusher bean.
     * Prevents: Spring context failure during initialization
     */
    @Bean
    @Primary
    public AdminMessagePusher mockAdminMessagePusher() {
        return mock(AdminMessagePusher.class);
    }
}
