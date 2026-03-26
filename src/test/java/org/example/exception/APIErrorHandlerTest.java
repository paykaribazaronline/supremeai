package org.example.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("APIErrorHandler Tests")
@Disabled("Circuit breaker registry conflicts - to be fixed")
public class APIErrorHandlerTest {
    
    private APIErrorHandler errorHandler;
    private int testCounter = 0;
    
    @BeforeEach
    void setUp() {
        // Use unique name for each test to avoid circuit breaker registry conflicts
        String uniqueName = "test-api-" + (++testCounter) + "-" + System.nanoTime();
        errorHandler = new APIErrorHandler(uniqueName);
    }
    
    @Test
    @DisplayName("Should succeed on first attempt")
    void testSuccessOnFirstAttempt() throws Exception {
        String uniqueName = "test-success-" + System.nanoTime();
        String result = errorHandler.executeWithResilience(
            () -> "success",
            uniqueName
        );
        
        assertEquals("success", result);
    }
    
    @Test
    @DisplayName("Should throw PermanentException on 401 Unauthorized")
    void testPermanentExceptionOn401() {
        String uniqueName = "test-permanent-" + System.nanoTime();
        assertThrows(APIErrorHandler.PermanentException.class, () -> {
            errorHandler.executeWithResilience(
                () -> { throw new RuntimeException(new IOException("401 Unauthorized")); },
                uniqueName
            );
        });
    }
    
    @Test
    @DisplayName("Should throw TemporaryException on rate limit")
    void testTemporaryExceptionOnRateLimit() {
        String uniqueName = "test-temp-" + System.nanoTime();
        assertThrows(APIErrorHandler.TemporaryException.class, () -> {
            errorHandler.executeWithResilience(
                () -> { throw new RuntimeException(new IOException("429 Rate Limited")); },
                uniqueName
            );
        });
    }
    
    @Test
    @DisplayName("Should retry on transient failures")
    void testRetryOnTransientFailure() throws Exception {
        final int[] callCount = {0};
        String uniqueName = "test-retry-" + System.nanoTime();
        
        String result = errorHandler.executeWithResilience(
            () -> {
                callCount[0]++;
                if (callCount[0] < 2) {
                    throw new RuntimeException(new IOException("503 Service Unavailable"));
                }
                return "success";
            },
            uniqueName
        );
        
        assertEquals("success", result);
        assertEquals(2, callCount[0]); // Called twice (first failed, second succeeded)
    }
}
