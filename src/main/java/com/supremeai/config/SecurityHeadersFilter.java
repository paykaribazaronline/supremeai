package com.supremeai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds security headers that are unavailable or problematic in the Spring Security 6.5.x
 * HeadersConfigurer API — specifically Permissions-Policy and Referrer-Policy, and the
 * X-Content-Type-Options nosniff flag whose enable() is private. Runs after the Spring Security
 * filter chain via SecurityConfig.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(SecurityHeadersFilter.class);

  private static final String PERMISSIONS_POLICY =
      "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()";

  private static final String REFERRER_POLICY = "strict-origin-when-cross-origin";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    response.setHeader("Permissions-Policy", PERMISSIONS_POLICY);
    response.setHeader("Referrer-Policy", REFERRER_POLICY);
    response.setHeader("X-Content-Type-Options", "nosniff");
    filterChain.doFilter(request, response);
  }
}
