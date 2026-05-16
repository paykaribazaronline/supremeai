package com.supremeai.config;

import com.supremeai.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeHandler.class);

    private final JwtUtil jwtUtil;

    public JwtHandshakeHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token != null) {
            try {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.getUsername(token);
                    return new StompPrincipal(username);
                }
            } catch (Exception e) {
                logger.warn("WebSocket JWT validation failed: {}", e.getMessage());
            }
        }
        return null;
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String header = httpRequest.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }
}
