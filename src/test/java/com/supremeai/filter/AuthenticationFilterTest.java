package com.supremeai.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

  private AuthenticationFilter filter;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new AuthenticationFilter();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotFilter_shouldSkipNonApiPaths() throws Exception {
    when(request.getRequestURI()).thenReturn("/index.html");

    boolean result = filter.shouldNotFilter(request);

    assertTrue(result);
  }

  @Test
  void shouldNotFilter_shouldSkipHealthEndpoint() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/health");

    boolean result = filter.shouldNotFilter(request);

    assertTrue(result);
  }

  @Test
  void shouldNotFilter_shouldRequireAuthForApiPaths() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/chat");

    boolean result = filter.shouldNotFilter(request);

    assertFalse(result);
  }

  @Test
  void shouldNotFilter_shouldAllowPublicAuthEndpoint() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");

    boolean result = filter.shouldNotFilter(request);

    assertTrue(result);
  }
}
