package com.supremeai.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final long SLOW_REQUEST_THRESHOLD_MS = 2000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime == null) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (duration > SLOW_REQUEST_THRESHOLD_MS) {
            log.warn("Slow request: {} {} took {}ms (threshold: {}ms)", method, uri, duration, SLOW_REQUEST_THRESHOLD_MS);
        } else {
            log.debug("Request: {} {} took {}ms", method, uri, duration);
        }
    }
}
