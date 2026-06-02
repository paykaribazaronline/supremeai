package com.supremeai.config;

import com.supremeai.security.ratelimit.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimiterFilterTest {RateLimitPropertiespublic RateLimiterFilterTest(RateLimitProperties rateLimitProperties, RateLimiter rateLimiter, com.supremeai.security.SecretManagerService secretManagerService, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, RateLimiterFilter rateLimiterFilter) {
RateLimitProperties    this.rateLimitProperties = rateLimitProperties;
RateLimitProperties    this.rateLimiter = rateLimiter;
RateLimitProperties    this.secretManagerService = secretManagerService;
RateLimitProperties    this.request = request;
RateLimitProperties    this.response = response;
RateLimitProperties    this.filterChain = filterChain;
RateLimitProperties    this.rateLimiterFilter = rateLimiterFilter;
RateLimitProperties}
















    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimiterFilter = new RateLimiterFilter(rateLimitProperties, rateLimiter, secretManagerService);
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
        when(rateLimiter.getStatus(anyString())).thenReturn(Map.of("tokens", 42, "last_refill", 12345L));
        when(rateLimitProperties.getAuthenticatedRequestsPerMinute()).thenReturn(100);

        rateLimiterFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq("X-RateLimit-Limit"), eq("100"));
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("42"));
    }
}