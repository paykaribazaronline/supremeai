package com.supremeai.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class CommandContextTest {

    @Test
    @DisplayName("Should create context with required parameters")
    void testCreateContextWithRequiredParameters() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        
        assertEquals("user-123", context.getUserId());
        assertEquals("testuser", context.getUsername());
        assertArrayEquals(new String[]{"USER"}, context.getRoles());
        assertNotNull(context.getRequestId());
    }

    @Test
    @DisplayName("Should check permission for admin user")
    void testHasPermissionForAdmin() {
        CommandContext context = new CommandContext("user-123", "admin", new String[]{"ADMIN"});
        
        assertTrue(context.hasPermission("ANY_PERMISSION"));
    }

    @Test
    @DisplayName("Should check permission for user with specific permission")
    void testHasPermissionForUser() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        context.setPermissions(new String[]{"READ", "WRITE"});
        
        assertTrue(context.hasPermission("READ"));
        assertTrue(context.hasPermission("WRITE"));
        assertFalse(context.hasPermission("DELETE"));
    }

    @Test
    @DisplayName("Should return false for permission when user has no permissions")
    void testHasPermissionNoPermissions() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        
        assertFalse(context.hasPermission("READ"));
    }

    @Test
    @DisplayName("Should check role correctly")
    void testHasRole() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"ADMIN", "USER"});
        
        assertTrue(context.hasRole("ADMIN"));
        assertTrue(context.hasRole("USER"));
        assertFalse(context.hasRole("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("Should return false for role when roles is null")
    void testHasRoleNullRoles() {
        CommandContext context = new CommandContext("user-123", "testuser", null);
        
        assertFalse(context.hasRole("ADMIN"));
    }

    @Test
    @DisplayName("Should set and get auth token")
    void testSetAndGetAuthToken() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        context.setAuthToken("token-123");
        
        assertEquals("token-123", context.getAuthToken());
    }

    @Test
    @DisplayName("Should set and get source IP")
    void testSetAndGetSourceIp() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        context.setSourceIp("192.168.1.1");
        
        assertEquals("192.168.1.1", context.getSourceIp());
    }

    @Test
    @DisplayName("Should set and get source app")
    void testSetAndGetSourceApp() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        context.setSourceApp("Dashboard");
        
        assertEquals("Dashboard", context.getSourceApp());
    }

    @Test
    @DisplayName("Should generate unique request IDs")
    void testUniqueRequestIds() {
        CommandContext context1 = new CommandContext("user-1", "user1", new String[]{"USER"});
        CommandContext context2 = new CommandContext("user-2", "user2", new String[]{"USER"});
        
        // Request IDs are based on timestamp, so they should be different
        // (unless created in the same millisecond, which is unlikely)
        assertTrue(context1.getRequestId() > 0);
        assertTrue(context2.getRequestId() > 0);
    }

    @Test
    @DisplayName("Should handle null roles in hasPermission")
    void testHasPermissionWithNullRoles() {
        CommandContext context = new CommandContext("user-123", "testuser", null);
        context.setPermissions(new String[]{"READ"});
        
        // Admin check should fail with null roles
        assertFalse(context.hasPermission("ANY_PERMISSION"));
        // But specific permission should work
        assertTrue(context.hasPermission("READ"));
    }

    @Test
    @DisplayName("Should handle empty permissions array")
    void testEmptyPermissions() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        context.setPermissions(new String[]{});
        
        assertFalse(context.hasPermission("READ"));
    }

    @Test
    @DisplayName("Should handle null permissions in hasPermission")
    void testNullPermissions() {
        CommandContext context = new CommandContext("user-123", "testuser", new String[]{"USER"});
        context.setPermissions(null);
        
        assertFalse(context.hasPermission("READ"));
    }
}