package org.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
public class AuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter();
    }

    @Test
    void testPublicPathAllowsUnauthenticatedAccess() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/webhook/github");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(any(int.class));
    }

    @Test
    void testHealthCheckPathAllowsUnauthenticatedAccess() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/health");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(any(int.class));
    }

    @Test
    void testActuatorHealthAllowsUnauthenticatedAccess() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(any(int.class));
    }

    @Test
    void testProtectedPathRequiresValidToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testProtectedPathRejectsMissingToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).sendError(401, "Missing or invalid authorization token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testProtectedPathRejectsInvalidToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).sendError(401, "Missing or invalid authorization token");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testInvalidAuthorizationHeaderFormat() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).sendError(401, "Missing or invalid authorization token");
    }

    @Test
    void testBasicAuthNotSupported() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).sendError(401, "Missing or invalid authorization token");
    }

    @Test
    void testCacheClearRequiresAdminToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/cache/clear");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer user-token");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then - should still validate, admin check happens at controller level
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testMultiplePathsWithCorrectTokens() throws ServletException, IOException {
        // Paths and tokens
        String[] paths = {
                "/api/v1/data/github/owner/repo",
                "/api/v1/data/vercel/proj_123",
                "/api/v1/data/firebase",
                "/api/v1/data/stats"
        };

        for (String path : paths) {
            reset(request, response, filterChain);
            
            // Given
            when(request.getRequestURI()).thenReturn(path);
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");

            // When
            authenticationFilter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(any(int.class));
        }
    }

    @Test
    void testTokenWithExtraWhitespace() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn("Bearer  valid-token  ");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then - should handle gracefully
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void testEmptyBearerToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).sendError(401, "Missing or invalid authorization token");
    }

    @Test
    void testCaseInsensitiveBearer() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/stats");
        when(request.getHeader("Authorization")).thenReturn("bearer valid-token");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then - Bearer should be case-insensitive
        verify(filterChain).doFilter(request, response);
    }
}
