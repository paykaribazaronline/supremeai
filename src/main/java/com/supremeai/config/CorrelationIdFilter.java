package com.supremeai.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdFilter implements Filter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ID_KEY = "correlationId";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    String correlationId = resolveCorrelationId((HttpServletRequest) request);
    MDC.put(CORRELATION_ID_KEY, correlationId);

    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(CORRELATION_ID_KEY);
    }
  }

  private String resolveCorrelationId(HttpServletRequest request) {
    String header = request.getHeader(CORRELATION_ID_HEADER);
    if (header != null && !header.isBlank()) {
      return header;
    }
    return UUID.randomUUID().toString();
  }
}
