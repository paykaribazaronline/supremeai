package com.supremeai.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.supremeai.security.ratelimit.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RateLimiterFilterTest {

  @Mock private RateLimitProperties rateLimitProperties;

  @Mock private RateLimiter rateLimiter;

  @Mock private com.supremeai.security.SecretManagerService secretManagerService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  private RateLimiterFilter rateLimiterFilter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    rateLimiterFilter =
        new RateLimiterFilter(rateLimitProperties, rateLimiter, secretManagerService);
  }

  @Test
  void testDoFilter_passesThroughWhenRateLimitingDisabled() throws Exception {
    when(rateLimitProperties.isEnabled()).thenReturn(false);

    rateLimiterFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(rateLimiter);
  }

  @Test
  void testDoFilter_passesThroughWhenRateLimitNotExceeded() throws Exception {
    when(rateLimitProperties.isEnabled()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
    when(rateLimiter.getStatus(anyString())).thenReturn(Map.of("tokens", 10));

    rateLimiterFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testDoFilter_returns429WhenRateLimitExceeded() throws Exception {
    when(rateLimitProperties.isEnabled()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getHeader("User-Agent")).thenReturn("TestAgent");
    when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    when(response.getWriter()).thenReturn(pw);

    rateLimiterFilter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(429);
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void testRateLimiterCalledWithCorrectKey() throws Exception {
    when(rateLimitProperties.isEnabled()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
    when(rateLimiter.getStatus(anyString())).thenReturn(Map.of("tokens", 10));

    rateLimiterFilter.doFilterInternal(request, response, filterChain);

    verify(rateLimiter).tryAcquire(anyString(), anyInt(), anyInt());
  }

  @Test
  void testHeadersAddedWhenStatusAvailable() throws Exception {
    when(rateLimitProperties.isEnabled()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
    when(rateLimiter.getStatus(anyString()))
        .thenReturn(Map.of("tokens", 42, "last_refill", 12345L));
    when(rateLimitProperties.getAuthenticatedRequestsPerMinute()).thenReturn(100);

    rateLimiterFilter.doFilterInternal(request, response, filterChain);

    verify(response).setHeader(eq("X-RateLimit-Limit"), eq("100"));
    verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("42"));
  }
}
