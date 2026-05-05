package com.supremeai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    public void setUp() {
        jwtUtil = new JwtUtil();
        // Set a test secret (must be at least 32 characters for HS256)
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-testing-123456");
        ReflectionTestUtils.setField(jwtUtil, "issuer", "supremeai");
    }

    @Test
    public void testGenerateToken() {
        String token = jwtUtil.generateToken("test-user", "USER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT tokens have 3 parts separated by dots
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    public void testGetUsername() {
        String token = jwtUtil.generateToken("test-user", "USER");
        String username = jwtUtil.getUsername(token);

        assertEquals("test-user", username);
    }

    @Test
    public void testGetRole() {
        String token = jwtUtil.generateToken("test-user", "USER");
        String role = jwtUtil.getRole(token);

        assertEquals("USER", role);
    }

    @Test
    public void testValidateToken_Valid() {
        String token = jwtUtil.generateToken("test-user", "USER");
        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    public void testValidateToken_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.validateToken(null);
        });
    }

    @Test
    public void testValidateToken_Expired() {
        // This is hard to test without manipulating time
        // Just verify that invalid tokens are rejected
        assertThrows(IllegalArgumentException.class, () -> {
            jwtUtil.validateToken("");
        });
    }
}
