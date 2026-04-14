package org.example.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.service.AuthenticationService;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
public class AuthenticationFilterTest {
    private static final String VALID_TOKEN = "valid-token";
    private static final String USER_TOKEN = "user-token";
    private static final String ADMIN_TOKEN = "admin-token";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuthenticationService authenticationService;

    private AuthenticationFilter authenticationFilter;
    private StringWriter responseContent;

    @BeforeEach
    void setUp() throws Exception {
        authenticationFilter = new AuthenticationFilter();
        // Spring 6 OncePerRequestFilter calls getDispatcherType() – stub it to avoid NPE
        lenient().when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        // Ensure the "already filtered" attribute returns null so filter is not skipped
        lenient().when(request.getAttribute(any(String.class))).thenReturn(null);
        
        // Mock getWriter() to avoid NPE during failure responses
        responseContent = new StringWriter();
        PrintWriter writer = new PrintWriter(responseContent);
        lenient().when(response.getWriter()).thenReturn(writer);

        // Inject mock auth service so dev-mode bypass is disabled.
        // "valid-token" is accepted; everything else throws (rejected).
        User validUser = new User();
        validUser.setUsername("test-user");
        validUser.setRole("user");
        User adminUser = new User();
        adminUser.setUsername("admin-user");
        adminUser.setRole("admin");
        lenient().when(authenticationService.validateToken(eq(VALID_TOKEN))).thenReturn(validUser);
        lenient().when(authenticationService.validateToken(eq(USER_TOKEN))).thenReturn(validUser);
        lenient().when(authenticationService.validateToken(eq(ADMIN_TOKEN))).thenReturn(adminUser);
        lenient().when(authenticationService.isAdmin(validUser)).thenReturn(false);
        lenient().when(authenticationService.isAdmin(adminUser)).thenReturn(true);
        lenient().doThrow(new RuntimeException("Invalid token"))
                .when(authenticationService).validateToken(
                argThat(t -> t != null && !t.equals(VALID_TOKEN)
                    && !t.equals(USER_TOKEN) && !t.equals(ADMIN_TOKEN)));
        // Null token should also be rejected
        lenient().doThrow(new RuntimeException("Null token"))
                .when(authenticationService).validateToken(null);
        authenticationFilter.setAuthService(authenticationService);
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
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testProtectedPathRejectsMissingToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testProtectedPathRejectsInvalidToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void testInvalidAuthorizationHeaderFormat() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
    }

    @Test
    void testBasicAuthNotSupported() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
    }

    @Test
    void testCacheClearRequiresAdminToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/cache/clear");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + USER_TOKEN);

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then - should still validate, admin check happens at controller level
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testMultiplePathsWithCorrectTokens() throws ServletException, IOException {
        // Test first path - each gets a fresh mock interaction
        when(request.getRequestURI()).thenReturn("/api/v1/data/github/owner/repo");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        authenticationFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testMultipleProtectedPathsWithValidToken_vercel() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/data/vercel/proj_123");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        authenticationFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testMultipleProtectedPathsWithValidToken_firebase() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/data/firebase");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        authenticationFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testMultipleProtectedPathsWithValidToken_stats() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        authenticationFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testTokenWithExtraWhitespace() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer  valid-token  ");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
    }

    @Test
    void testEmptyBearerToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(401);
    }

    @Test
    void testCaseInsensitiveBearer() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/data/protected");
        when(request.getHeader("Authorization")).thenReturn("bearer " + VALID_TOKEN);

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then - Bearer should be case-insensitive
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAdminHtmlRedirectsToLoginWhenMissingToken() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/admin.html");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("Accept")).thenReturn("text/html");

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).sendRedirect(argThat(s -> s.contains("/login.html")));
    }

    @Test
    void testAdminApiRejectsNonAdminUser() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/admin/control/mode");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + USER_TOKEN);

        // When
        authenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(response).setStatus(403);
    }

    @Test
    void testAdminPageAllowsCookieBackedAdminSession() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/admin.html");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("supremeai_admin_token", ADMIN_TOKEN) });

        authenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
